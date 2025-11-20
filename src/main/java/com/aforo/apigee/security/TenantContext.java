package com.aforo.apigee.security;

/**
 * Thread-local storage for tenant (organization) context
 * Allows multi-tenant data isolation throughout the request lifecycle
 */
public class TenantContext {
    
    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();
    
    public static void setTenantId(Long tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static Long getTenantId() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}
