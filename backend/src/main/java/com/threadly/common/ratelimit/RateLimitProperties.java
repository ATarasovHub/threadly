package com.threadly.common.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param enabled       turn the filter off entirely, which tests and local work sometimes want
 * @param capacity      requests allowed per period, per client
 * @param period        how long a full quota takes to refill
 * @param writeCapacity a tighter quota for requests that change state
 */
@ConfigurationProperties(prefix = "threadly.rate-limit")
public record RateLimitProperties(
		boolean enabled,
		int capacity,
		Duration period,
		int writeCapacity) {

	public RateLimitProperties {
		capacity = capacity <= 0 ? 300 : capacity;
		writeCapacity = writeCapacity <= 0 ? 60 : writeCapacity;
		period = period == null ? Duration.ofMinutes(1) : period;
	}
}
