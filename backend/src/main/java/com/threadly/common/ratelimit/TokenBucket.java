package com.threadly.common.ratelimit;

import java.time.Duration;

/**
 * A token bucket: {@code capacity} requests may be spent at once, and tokens refill continuously
 * at {@code capacity / period}.
 *
 * <p>Refilling continuously rather than resetting on a fixed schedule avoids the burst that a
 * fixed window allows at its boundary, where a client can spend a full quota just before the reset
 * and another immediately after.
 */
final class TokenBucket {

	private final int capacity;
	private final double tokensPerNano;

	private double tokens;
	private long lastRefillNanos;

	TokenBucket(int capacity, Duration period, long nowNanos) {
		this.capacity = capacity;
		this.tokensPerNano = (double) capacity / period.toNanos();
		this.tokens = capacity;
		this.lastRefillNanos = nowNanos;
	}

	/** @return true if a token was available and has been spent */
	synchronized boolean tryConsume(long nowNanos) {
		refill(nowNanos);
		if (tokens < 1.0) {
			return false;
		}
		tokens -= 1.0;
		return true;
	}

	/** Seconds until at least one token is available; 0 when the bucket is not empty. */
	synchronized long secondsUntilRefill(long nowNanos) {
		refill(nowNanos);
		if (tokens >= 1.0) {
			return 0;
		}
		double missing = 1.0 - tokens;
		return (long) Math.ceil(missing / tokensPerNano / 1_000_000_000d);
	}

	synchronized boolean isIdleSince(long nowNanos, Duration idleFor) {
		return nowNanos - lastRefillNanos > idleFor.toNanos();
	}

	synchronized int remaining(long nowNanos) {
		refill(nowNanos);
		return (int) Math.floor(tokens);
	}

	private void refill(long nowNanos) {
		long elapsed = nowNanos - lastRefillNanos;
		if (elapsed <= 0) {
			return;
		}
		tokens = Math.min(capacity, tokens + elapsed * tokensPerNano);
		lastRefillNanos = nowNanos;
	}
}
