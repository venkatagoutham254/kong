package aforo.kong.security;

import aforo.kong.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class JwtTenantFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtTenantFilter.class);
    private static final List<String> CLAIM_KEYS = Arrays.asList(
            "organizationId", "orgId", "tenantId", "organization_id", "org_id", "tenant");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1) Optional header override for easy testing from Swagger/curl
        String headerOrg = request.getHeader("X-Organization-Id");
        if (headerOrg != null && !headerOrg.isBlank()) {
            try {
                TenantContext.set(Long.parseLong(headerOrg.trim()));
                logger.debug("Tenant set from X-Organization-Id header: {}", headerOrg);
            } catch (NumberFormatException e) {
                logger.warn("Invalid X-Organization-Id header value: {}", headerOrg);
            }
        } else if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            // 2) Extract from JWT using multiple possible claim keys
            Map<String, Object> claims = jwt.getClaims();
            Object found = null;
            for (String key : CLAIM_KEYS) {
                if (claims.containsKey(key)) {
                    found = claims.get(key);
                    if (found != null) {
                        try {
                            Long parsed = Long.parseLong(found.toString());
                            TenantContext.set(parsed);
                            logger.debug("Tenant set from JWT claim '{}': {}", key, found);
                            break;
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid numeric value for JWT claim '{}': {}", key, found);
                        }
                    }
                }
            }
            if (TenantContext.get() == null) {
                logger.debug("No tenant claim found in JWT. Checked keys: {}", CLAIM_KEYS);
            }
        } else {
            logger.debug("No Authentication/JWT present in SecurityContext at tenant filter stage");
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
