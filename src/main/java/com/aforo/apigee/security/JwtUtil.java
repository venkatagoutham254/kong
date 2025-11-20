package com.aforo.apigee.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token utility for parsing and validating JWT tokens
 */
@Component
@Slf4j
public class JwtUtil {
    
    @Value("${aforo.jwt.secret}")
    private String jwtSecret;
    
    /**
     * Extract all claims from JWT token
     */
    public Claims extractClaims(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
    
    /**
     * Extract organization ID from JWT token
     */
    public Long extractOrganizationId(String token) {
        Claims claims = extractClaims(token);
        Object orgId = claims.get("orgId");  // Changed from "organizationId" to "orgId"
        
        if (orgId == null) {
            throw new RuntimeException("orgId not found in JWT token");
        }
        
        if (orgId instanceof Integer) {
            return ((Integer) orgId).longValue();
        } else if (orgId instanceof Long) {
            return (Long) orgId;
        } else {
            return Long.parseLong(orgId.toString());
        }
    }
    
    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
