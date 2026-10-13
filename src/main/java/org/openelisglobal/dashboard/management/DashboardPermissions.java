package org.openelisglobal.dashboard.management;

public final class DashboardPermissions {

    public static final String VIEW_MANAGEMENT = "hasAuthority('dashboards.view.management') or hasRole('GLOBAL_ADMIN')";

    private DashboardPermissions() {
    }
}
