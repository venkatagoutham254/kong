package aforo.kong.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class TenantContext {
    private static final ThreadLocal<Long> ORG = new ThreadLocal<>();
    private TenantContext() {}
    public static void set(Long organizationId) { ORG.set(organizationId); }
    public static Long get() { return ORG.get(); }
    public static Long require() {
        Long id = ORG.get();
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing tenant");
        return id;
    }
    public static void clear() { ORG.remove(); }
}
