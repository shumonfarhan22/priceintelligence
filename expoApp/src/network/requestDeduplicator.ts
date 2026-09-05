/**
 * Request Deduplicator / Coalescing Manager
 *
 * Prevents redundant simultaneous network requests to the same retailer URL.
 * When multiple callers initiate a fetch for the same resource simultaneously,
 * they share the same in-flight Promise.
 */
class RequestDeduplicator {
  private inFlight = new Map<string, Promise<any>>();

  async coalesce<T>(key: string, requestFn: () => Promise<T>): Promise<T> {
    const existing = this.inFlight.get(key);
    if (existing) {
      return existing as Promise<T>;
    }

    const promise = (async () => {
      try {
        return await requestFn();
      } finally {
        this.inFlight.delete(key);
      }
    })();

    this.inFlight.set(key, promise);
    return promise;
  }

  get pendingCount(): number {
    return this.inFlight.size;
  }

  clear(): void {
    this.inFlight.clear();
  }
}

export const requestDeduplicator = new RequestDeduplicator();
