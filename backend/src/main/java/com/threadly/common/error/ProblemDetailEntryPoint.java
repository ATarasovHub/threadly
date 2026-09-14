package com.threadly.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Answers unauthenticated requests with 401 and an RFC 7807 body.
 *
 * <p>Without an explicit entry point Spring Security falls back to a bare 403, which is misleading
 * for an API whose clients need to tell "log in" apart from "not allowed".
 */
@Component
public class ProblemDetailEntryPoint implements AuthenticationEntryPoint {

	private static final String BODY = """
			{"type":"https://threadly.dev/problems/unauthenticated",\
			"title":"Authentication required",\
			"status":401,\
			"detail":"This endpoint requires a valid access token."}""";

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(BODY);
	}
}
