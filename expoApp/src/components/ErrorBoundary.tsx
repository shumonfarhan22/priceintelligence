import React, { Component, type ErrorInfo, type ReactNode } from 'react';
import {
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { colors, radius, spacing, type as typography } from '../theme/tokens';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
  showDetails: boolean;
}

/**
 * Application-level ErrorBoundary following EAS Observe patterns.
 * Captures unhandled runtime errors, logs diagnostic information,
 * and provides a graceful one-tap recovery screen.
 */
export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
    errorInfo: null,
    showDetails: false,
  };

  public static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[ErrorBoundary] Uncaught application error:', error, errorInfo);
    this.setState({ errorInfo });
    this.props.onError?.(error, errorInfo);
  }

  private handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      showDetails: false,
    });
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <SafeAreaView style={styles.container}>
          <View style={styles.content}>
            <View style={styles.badge}>
              <Text style={styles.badgeText}>APPLICATION NOTICE</Text>
            </View>

            <Text style={[styles.title, { fontFamily: typography.bold }]}>
              Something went wrong
            </Text>

            <Text style={[styles.subtitle, { fontFamily: typography.regular }]}>
              An unexpected issue occurred. Your saved prices and inventory data in the database remain safe and intact.
            </Text>

            <Pressable
              onPress={this.handleReset}
              style={({ pressed }) => [
                styles.button,
                pressed && { opacity: 0.85, transform: [{ scale: 0.98 }] },
              ]}
            >
              <Text style={styles.buttonText}>Reload Application</Text>
            </Pressable>

            <Pressable
              onPress={() => this.setState((s) => ({ showDetails: !s.showDetails }))}
              style={styles.detailsToggle}
            >
              <Text style={styles.detailsToggleText}>
                {this.state.showDetails ? 'Hide Technical Diagnostics' : 'Show Technical Diagnostics'}
              </Text>
            </Pressable>

            {this.state.showDetails && (
              <ScrollView style={styles.diagnosticsBox}>
                <Text style={styles.errorName}>
                  {this.state.error?.name}: {this.state.error?.message}
                </Text>
                {this.state.errorInfo?.componentStack && (
                  <Text style={styles.stackTrace}>
                    {this.state.errorInfo.componentStack.trim()}
                  </Text>
                )}
              </ScrollView>
            )}
          </View>
        </SafeAreaView>
      );
    }

    return this.props.children;
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0B0F14',
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.lg,
  },
  content: {
    width: '100%',
    maxWidth: 480,
    backgroundColor: '#14181D',
    borderRadius: radius.lg,
    borderWidth: 1,
    borderColor: '#2A313C',
    padding: spacing.xl,
    alignItems: 'center',
  },
  badge: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    borderColor: 'rgba(239, 68, 68, 0.35)',
    borderWidth: 1,
    paddingHorizontal: spacing.sm,
    paddingVertical: 3,
    borderRadius: radius.pill,
    marginBottom: spacing.md,
  },
  badgeText: {
    color: '#F87171',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  title: {
    color: '#FFFFFF',
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: spacing.sm,
  },
  subtitle: {
    color: '#94A3B8',
    textAlign: 'center',
    marginBottom: spacing.xl,
    lineHeight: 20,
  },
  button: {
    backgroundColor: colors.primary,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.xl,
    borderRadius: radius.md,
    width: '100%',
    alignItems: 'center',
  },
  buttonText: {
    color: '#000000',
    fontWeight: '700',
    fontSize: 15,
  },
  detailsToggle: {
    marginTop: spacing.md,
    padding: spacing.xs,
  },
  detailsToggleText: {
    color: '#64748B',
    fontSize: 12,
  },
  diagnosticsBox: {
    width: '100%',
    maxHeight: 180,
    backgroundColor: '#080B0E',
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: '#1E293B',
    padding: spacing.sm,
    marginTop: spacing.md,
  },
  errorName: {
    color: '#F87171',
    fontFamily: 'monospace',
    fontSize: 11,
    marginBottom: 6,
  },
  stackTrace: {
    color: '#64748B',
    fontFamily: 'monospace',
    fontSize: 10,
    lineHeight: 14,
  },
});
