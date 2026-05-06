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
    /** The department the user is actively working in this session */
    private String activeDepartmentId;

    private RbacContext() {
    }

    public static void set(String systemUserId, String username, String ipAddress, List<String> allowedDepartments,
            List<String> allowedProjects) {
        set(systemUserId, username, ipAddress, allowedDepartments, allowedProjects, null);
    }

    public static void set(String systemUserId, String username, String ipAddress, List<String> allowedDepartments,
            List<String> allowedProjects, String activeDepartmentId) {
        RbacContext ctx = new RbacContext();
        ctx.systemUserId = systemUserId;
        ctx.username = username;
        ctx.ipAddress = ipAddress;
        ctx.allowedDepartments = allowedDepartments;
        ctx.allowedProjects = allowedProjects;
        ctx.activeDepartmentId = activeDepartmentId;
        CURRENT.set(ctx);
    }

    public static RbacContext get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public String getSystemUserId() {
        return systemUserId;
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    /** Returns null if user has unrestricted access (System Admin) */
    public List<String> getAllowedDepartments() {
        return allowedDepartments;
    }

    public List<String> getAllowedProjects() {
        return allowedProjects;
    }

    /**
     * The department the user is actively working in. This is the data-scope
     * boundary for storage, inventory, equipment, samples. null means unrestricted
     * (System Admin / global roles).
     */
    public String getActiveDepartmentId() {
        return activeDepartmentId;
    }

    public boolean isUnrestricted() {
        return allowedDepartments == null;
    }

    public boolean canAccessDepartment(String departmentId) {
        if (allowedDepartments == null)
            return true; // unrestricted
        return allowedDepartments.contains(departmentId);
    }

    /**
     * For data access (storage, inventory, equipment, samples): restricted users
     * may only see records belonging to their active department.
     */
    public boolean isInActiveDepartment(String departmentId) {
        if (activeDepartmentId == null)
            return true; // unrestricted
        return activeDepartmentId.equals(departmentId);
    }

    public boolean canAccessProject(String projectId) {
        if (allowedProjects == null)
            return true; // unrestricted
        return allowedProjects.contains(projectId);
    }
}
