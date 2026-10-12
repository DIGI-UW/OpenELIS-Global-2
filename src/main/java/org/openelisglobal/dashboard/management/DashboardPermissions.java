package org.openelisglobal.dashboard.management;

public final class DashboardPermissions {

    public static final String KEY_VIEW_MANAGEMENT = "dashboards.view.management";

    public static final String VIEW_MANAGEMENT = "hasAuthority('" + KEY_VIEW_MANAGEMENT
            + "') or hasRole('GLOBAL_ADMIN')";

    private DashboardPermissions() {
    }
}
