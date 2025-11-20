package com.aforo.apigee.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Filter for extracting and validating JWT tokens
 * Sets tenant context and authentication in Spring Security
 */
@Component("apigeeJwtTenantFilter")
@RequiredArgsConstructor
@Slf4j
public class JwtTenantFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Validate token
                if (jwtUtil.validateToken(token)) {
                    // Extract organization ID and set in tenant context
                    Long organizationId = jwtUtil.extractOrganizationId(token);
                    TenantContext.setTenantId(organizationId);
                    
                    // Extract username
                    String username = jwtUtil.extractUsername(token);
                    
                    // Set authentication in Spring Security context
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT authenticated user: {}, organizationId: {}", username, organizationId);
                } else {
                    log.warn("Invalid JWT token");
                }
            }
            
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear tenant context after request
            TenantContext.clear();
        }
    }
}
