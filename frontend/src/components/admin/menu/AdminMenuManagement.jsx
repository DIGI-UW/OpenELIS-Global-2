import React from "react";
import MenuManagementForm from "./MenuManagementForm";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.menu.admin",
    link: "/admin/adminMenuManagement",
  },
];

// Admin nav visibility manager. Operators toggle is_active on rows where
// nav_scope='admin' via /rest/admin-menu. Save semantics live in
// MenuManagementForm.
function AdminMenuManagement() {
  return (
    <MenuManagementForm
      endpoint="/rest/admin-menu"
      breadcrumbs={breadcrumbs}
      titleKey="menu.admin.title"
      toggleId="toggleShowChildrenAdmin"
    />
  );
}

export default AdminMenuManagement;
