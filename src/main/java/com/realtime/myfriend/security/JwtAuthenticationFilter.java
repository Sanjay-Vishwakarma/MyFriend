package com.realtime.myfriend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;



    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        logger.debug("Processing request URI: {}", request.getRequestURI());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No Bearer token found in request");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        logger.debug("JWT token found: {}", jwt);

        try {
            username = jwtService.extractUsername(jwt);
            logger.debug("Extracted username from token: {}", username);

            if (username != null) {
                logger.debug("SecurityContext before: {}", SecurityContextHolder.getContext().getAuthentication());

                // Load user details from DB
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                logger.debug("Loaded user details: username={}, authorities={}", userDetails.getUsername(), userDetails.getAuthorities());

                // Validate token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Authentication set in SecurityContext: {}", SecurityContextHolder.getContext().getAuthentication());
                } else {
                    logger.debug("Token is not valid");
                }

                logger.debug("SecurityContext after: {}", SecurityContextHolder.getContext().getAuthentication());
            }

        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
        }

        filterChain.doFilter(request, response);
    }
}
