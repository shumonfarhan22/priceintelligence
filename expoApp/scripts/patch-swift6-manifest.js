const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

let swiftMajor = 6;
let swiftMinor = 0;
try {
  const out = execSync('swift --version', { encoding: 'utf8' });
  const match = out.match(/Swift version (\d+)\.(\d+)/);
  if (match) {
    swiftMajor = parseInt(match[1], 10);
    swiftMinor = parseInt(match[2], 10);
  }
} catch (e) {}

const isSwift62OrHigher = swiftMajor > 6 || (swiftMajor === 6 && swiftMinor >= 2);
console.log(
  `[patch] Detected Swift ${swiftMajor}.${swiftMinor} (isSwift62OrHigher: ${isSwift62OrHigher})`
);

function findFiles(dir, matchName, visited = new Set(), fileList = []) {
  if (!fs.existsSync(dir)) return fileList;
  let real;
  try {
    real = fs.realpathSync(dir);
  } catch (e) {
    return fileList;
  }
  if (visited.has(real)) return fileList;
  visited.add(real);

  let entries;
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true });
  } catch (e) {
    return fileList;
  }

  for (const entry of entries) {
    if (
      entry.name === '.git' ||
      entry.name === 'Pods' ||
      entry.name === '.DerivedData' ||
      entry.name === '.build' ||
      entry.name === 'DerivedData'
    ) {
      continue;
    }
    const fullPath = path.join(dir, entry.name);
    let isDir = false;
    try {
      isDir = fs.statSync(fullPath).isDirectory();
    } catch (e) {}

    if (isDir) {
      findFiles(fullPath, matchName, visited, fileList);
    } else if (matchName(entry.name)) {
      fileList.push(fullPath);
    }
  }
  return fileList;
}

const nodeModulesDir = path.resolve(__dirname, '../node_modules');
console.log('[patch] Searching node_modules at:', nodeModulesDir);

// 1. Patch Package.swift files: down-pin and adapt if older Swift toolchain
const pkgFiles = findFiles(nodeModulesDir, (n) => n === 'Package.swift');
console.log(`[patch] Found ${pkgFiles.length} Package.swift files`);

for (const f of pkgFiles) {
  let content = fs.readFileSync(f, 'utf8');
  const original = content;

  if (!isSwift62OrHigher) {
    content = content.replace(
      /swift-tools-version:\s*6\.[1-9]/g,
      `swift-tools-version: ${swiftMajor}.${swiftMinor}`
    );
    content = content.replace(/\.enableUpcomingFeature\(/g, '// .enableUpcomingFeature(');

    // Remove trailing commas before closing parentheses
    const lines = content.split(/\r?\n/);
    for (let i = 0; i < lines.length - 1; i++) {
      let nextIdx = i + 1;
      while (
        nextIdx < lines.length &&
        (lines[nextIdx].trim() === '' || lines[nextIdx].trim().startsWith('//'))
      ) {
        nextIdx++;
      }
      if (nextIdx < lines.length) {
        const nextTrimmed = lines[nextIdx].trim();
        if (nextTrimmed.startsWith(')')) {
          lines[i] = lines[i].replace(/,\s*(\/\/.*)?$/, (match, p1) => (p1 ? ' ' + p1.trim() : ''));
        }
      }
    }
    content = lines.join('\n');
  }

  if (content !== original) {
    fs.writeFileSync(f, content, 'utf8');
    console.log(`[patch] Patched Package.swift: ${path.relative(nodeModulesDir, f)}`);
  }
}

// 2. Patch Swift files: only on Swift < 6.2 (Swift 6.2+ natively supports `weak let` on Sendable types)
if (!isSwift62OrHigher) {
  const swiftFiles = findFiles(nodeModulesDir, (n) => n.endsWith('.swift'));
  console.log(`[patch] Checking ${swiftFiles.length} Swift files for 'weak let'...`);

  let weakLetCount = 0;
  for (const f of swiftFiles) {
    let content = fs.readFileSync(f, 'utf8');
    if (content.includes('weak let')) {
      content = content.replace(/\bweak\s+let\b/g, 'nonisolated(unsafe) weak var');
      fs.writeFileSync(f, content, 'utf8');
      weakLetCount++;
    }
  }
  console.log(`[patch] Patched 'weak let' in ${weakLetCount} Swift files`);
} else {
  console.log('[patch] Swift 6.2+ detected: keeping native `weak let` properties intact');
}

// 3. Patch RuntimeScheduler.h: remove SWIFT_RETURNS_RETAINED from constructors
const schedulerFiles = findFiles(nodeModulesDir, (n) => n === 'RuntimeScheduler.h');
for (const f of schedulerFiles) {
  let content = fs.readFileSync(f, 'utf8');
  if (content.includes('SWIFT_RETURNS_RETAINED RuntimeScheduler')) {
    content = content.replace(/SWIFT_RETURNS_RETAINED\s+RuntimeScheduler/g, 'RuntimeScheduler');
    fs.writeFileSync(f, content, 'utf8');
    console.log(`[patch] Patched RuntimeScheduler.h: ${path.relative(nodeModulesDir, f)}`);
  }
}

// 4. Patch build-xcframework.sh: remove -quiet flag cleanly without leaving empty argument lines
const buildScripts = findFiles(nodeModulesDir, (n) => n === 'build-xcframework.sh');
for (const f of buildScripts) {
  let content = fs.readFileSync(f, 'utf8');
  if (content.includes('-quiet')) {
    content = content.replace(/^[ \t]*-quiet[ \t]*(\\)?\r?\n/gm, '');
    fs.writeFileSync(f, content, 'utf8');
    console.log(`[patch] Patched build-xcframework.sh: ${path.relative(nodeModulesDir, f)}`);
  }
}

console.log('[patch] All patches processed successfully!');
