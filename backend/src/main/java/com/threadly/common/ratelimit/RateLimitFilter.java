package com.threadly.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client request quotas.
 *
 * <p>Two buckets per client: a wide one for everything, and a tighter one for requests that write,
 * since those are the expensive and abusable ones.
 *
 * <p>Runs ahead of Spring Security, so that a flood of unauthenticated requests — credential
 * stuffing against the login endpoint, above all — is stopped before it reaches password hashing.
 * The cost is that clients are identified by address rather than by account, so callers behind one
 * NAT share a quota. Per-account quotas would need a second limiter positioned after
 * authentication; this one exists to blunt the unauthenticated case.
 *
 * <p>The address is taken from the connection, not from {@code X-Forwarded-For}, which any client
 * can set. Behind a reverse proxy, configure the proxy to be trusted rather than trusting the
 * header here.
 *
 * <p>State lives in this process, so a second instance would grant a second quota. That is a
 * deliberate trade for a single-instance deployment: the alternative is a round trip to Redis on
 * every request. Moving the buckets to Redis is the change to make when this runs behind more than
 * one instance.
 */
@Component
// Ahead of the Spring Security chain, which registers at -100.
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Duration IDLE_EVICTION = Duration.ofMinutes(30);
	private static final int EVICTION_SCAN_EVERY = 1_000;

	private final RateLimitProperties properties;
	private final Map<String, TokenBucket> readBuckets = new ConcurrentHashMap<>();
	private final Map<String, TokenBucket> writeBuckets = new ConcurrentHashMap<>();

	private int requestsSinceScan;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		// Health checks and the API description are infrastructure, not client traffic.
		return !properties.enabled()
				|| path.startsWith("/actuator")
				|| path.startsWith("/v3/api-docs")
				|| path.startsWith("/swagger-ui");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {

		long now = System.nanoTime();
		String client = clientKey(request);
		boolean write = isWrite(request);

		TokenBucket bucket = write
				? writeBuckets.computeIfAbsent(client,
						key -> new TokenBucket(properties.writeCapacity(), properties.period(), now))
				: readBuckets.computeIfAbsent(client,
						key -> new TokenBucket(properties.capacity(), properties.period(), now));

		evictIdleBucketsOccasionally(now);

		if (!bucket.tryConsume(now)) {
			reject(response, bucket.secondsUntilRefill(now));
			return;
		}

		int limit = write ? properties.writeCapacity() : properties.capacity();
		response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
		response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.remaining(now)));
		chain.doFilter(request, response);
	}

	private static String clientKey(HttpServletRequest request) {
		return request.getRemoteAddr();
	}

	private static boolean isWrite(HttpServletRequest request) {
		HttpMethod method = HttpMethod.valueOf(request.getMethod());
		return !(HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method)
				|| HttpMethod.OPTIONS.equals(method));
	}

	private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfterSeconds)));
		response.getWriter().write("""
				{"type":"https://threadly.dev/problems/rate-limited",\
				"title":"Too many requests",\
				"status":429,\
				"detail":"You are sending requests faster than this API allows. Try again shortly."}""");
	}

	/**
	 * Buckets are created per client and would otherwise accumulate forever. Scanning every
	 * thousand requests keeps that bounded without a scheduled job.
	 */
	private void evictIdleBucketsOccasionally(long now) {
		if (++requestsSinceScan < EVICTION_SCAN_EVERY) {
			return;
		}
		requestsSinceScan = 0;
		readBuckets.values().removeIf(bucket -> bucket.isIdleSince(now, IDLE_EVICTION));
		writeBuckets.values().removeIf(bucket -> bucket.isIdleSince(now, IDLE_EVICTION));
	}
}
