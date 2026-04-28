package org.openelisglobal.rbac.context;

import java.util.List;

/**
 * TR-04/TR-05: Thread-local holder for the current user's RBAC context.
 * Populated by RbacRequestFilter on each request.
 */
public final class RbacContext {

    private static final ThreadLocal<RbacContext> CURRENT = new ThreadLocal<>();

    private String systemUserId;
    private String username;
    private String ipAddress;
    /** null = unrestricted (System Admin); empty = no access */
    private List<String> allowedDepartments;
    private List<String> allowedProjects;

    private RbacContext() {}

    public static void set(String systemUserId, String username, String ipAddress,
            List<String> allowedDepartments, List<String> allowedProjects) {
        RbacContext ctx = new RbacContext();
        ctx.systemUserId = systemUserId;
        ctx.username = username;
        ctx.ipAddress = ipAddress;
        ctx.allowedDepartments = allowedDepartments;
        ctx.allowedProjects = allowedProjects;
        CURRENT.set(ctx);
    }

    public static RbacContext get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public String getSystemUserId() { return systemUserId; }
    public String getUsername() { return username; }
    public String getIpAddress() { return ipAddress; }

    /** Returns null if user has unrestricted access (System Admin) */
    public List<String> getAllowedDepartments() { return allowedDepartments; }
    public List<String> getAllowedProjects() { return allowedProjects; }

    public boolean isUnrestricted() { return allowedDepartments == null; }

    public boolean canAccessDepartment(String departmentId) {
        if (allowedDepartments == null) return true; // unrestricted
        return allowedDepartments.contains(departmentId);
    }

    public boolean canAccessProject(String projectId) {
        if (allowedProjects == null) return true; // unrestricted
        return allowedProjects.contains(projectId);
    }
}
