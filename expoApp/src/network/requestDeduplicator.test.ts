import { describe, expect, it, vi } from 'vitest';
import { requestDeduplicator } from './requestDeduplicator';

describe('requestDeduplicator', () => {
  it('coalesces multiple identical simultaneous requests into a single execution', async () => {
    requestDeduplicator.clear();
    const mockWorker = vi.fn(async () => {
      await new Promise((resolve) => setTimeout(resolve, 50));
      return { price: 999, retailer: 'AMAZON' };
    });

    const [res1, res2, res3] = await Promise.all([
      requestDeduplicator.coalesce('key-1', mockWorker),
      requestDeduplicator.coalesce('key-1', mockWorker),
      requestDeduplicator.coalesce('key-1', mockWorker),
    ]);

    expect(res1).toEqual({ price: 999, retailer: 'AMAZON' });
    expect(res2).toEqual({ price: 999, retailer: 'AMAZON' });
    expect(res3).toEqual({ price: 999, retailer: 'AMAZON' });
    expect(mockWorker).toHaveBeenCalledTimes(1);
    expect(requestDeduplicator.pendingCount).toBe(0);
  });

  it('runs separate requests for different keys', async () => {
    requestDeduplicator.clear();
    const worker1 = vi.fn(async () => 'result-1');
    const worker2 = vi.fn(async () => 'result-2');

    const [res1, res2] = await Promise.all([
      requestDeduplicator.coalesce('key-A', worker1),
      requestDeduplicator.coalesce('key-B', worker2),
    ]);

    expect(res1).toBe('result-1');
    expect(res2).toBe('result-2');
    expect(worker1).toHaveBeenCalledTimes(1);
    expect(worker2).toHaveBeenCalledTimes(1);
  });
});
