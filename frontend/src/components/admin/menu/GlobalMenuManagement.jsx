import React from "react";
import MenuManagementForm from "./MenuManagementForm";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.menu.global",
    link: "/MasterListsPage/globalMenuManagement",
  },
];

// Lab nav visibility manager. Operators toggle is_active on rows where
// nav_scope='lab' via /rest/menu. Save semantics live in MenuManagementForm.
function GlobalMenuManagement() {
  return (
    <MenuManagementForm
      endpoint="/rest/menu"
      breadcrumbs={breadcrumbs}
      titleKey="menu.global.title"
      toggleId="toggleShowChildrenGlobal"
    />
  );
}

export default GlobalMenuManagement;
