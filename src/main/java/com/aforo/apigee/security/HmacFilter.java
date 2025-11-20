package com.aforo.apigee.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class HmacFilter extends OncePerRequestFilter {
    
    @Value("${aforo.hmac.secret}")
    private String hmacSecret;
    
    private static final String HMAC_HEADER = "X-Aforo-Signature";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only apply HMAC verification to webhook endpoints
        if (!path.contains("/webhooks/usage")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        log.debug("Verifying HMAC for webhook request");
        
        // Wrap request to allow reading body multiple times
        CachedBodyHttpServletRequest cachedRequest;
        try {
            cachedRequest = new CachedBodyHttpServletRequest(request);
        } catch (IOException e) {
            log.error("Error caching request body", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Error reading request body\"}");
            return;
        }
        
        String providedSignature = cachedRequest.getHeader(HMAC_HEADER);
        
        if (providedSignature == null || providedSignature.isEmpty()) {
            log.warn("Missing HMAC signature header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing HMAC signature\"}");
            return;
        }
        
        // Get cached body
        byte[] body = cachedRequest.getCachedBody();
        
        // Calculate expected signature
        String expectedSignature = calculateHmac(body);
        
        if (!expectedSignature.equals(providedSignature)) {
            log.warn("HMAC signature mismatch. Expected: {}, Provided: {}", expectedSignature, providedSignature);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid HMAC signature\"}");
            return;
        }
        
        log.debug("HMAC verification successful");
        filterChain.doFilter(cachedRequest, response);
    }
    
    private String calculateHmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data);
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("Error calculating HMAC", e);
            throw new RuntimeException("Error calculating HMAC", e);
        }
    }
}
