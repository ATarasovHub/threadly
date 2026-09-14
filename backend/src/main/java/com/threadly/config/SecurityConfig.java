package com.threadly.config;

import com.threadly.auth.jwt.JwtAuthorityConverter;
import com.threadly.common.error.ProblemDetailEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final ProblemDetailEntryPoint problemDetailEntryPoint;
	private final JwtAuthorityConverter jwtAuthorityConverter;

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Strength 12: noticeably slower than the default 10, still well under 500 ms per hash.
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		// Turns "no such account" into the same BadCredentialsException a wrong password produces,
		// so responses cannot be used to discover which handles exist.
		provider.setHideUserNotFoundExceptions(true);
		return new ProviderManager(provider);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				// The API is token-based and stateless, so there is no session cookie to protect.
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
						.requestMatchers("/actuator/health/**").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthorityConverter))
						.authenticationEntryPoint(problemDetailEntryPoint))
				.exceptionHandling(handling -> handling.authenticationEntryPoint(problemDetailEntryPoint))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.build();
	}
}
