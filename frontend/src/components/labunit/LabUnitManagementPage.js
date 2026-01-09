import React, { useState } from "react";
import { useHistory } from "react-router-dom";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout.js";
import LabUnitList from "./LabUnitList.js";
import LabUnitEditor from "./LabUnitEditor.js";
import PageBreadCrumb from "../common/PageBreadCrumb.js";

function LabUnitManagementPage() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    React.useContext(NotificationContext);
  const intl = useIntl();
  const history = useHistory();
  const [selectedUnit, setSelectedUnit] = useState(null);
  const [isEditing, setIsEditing] = useState(false);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "labUnit.management.title",
      link: "/MasterListsPage/labUnitManagement",
    },
  ];

  const handleEditUnit = (unit) => {
    if (unit) {
      history.push(`/admin/lab-units/edit/${unit.id}`);
    } else {
      history.push("/admin/lab-units/add");
    }
  };

  const handleBack = () => {
    setIsEditing(false);
    setSelectedUnit(null);
    history.push("/MasterListsPage/labUnitManagement");
  };

  const handleSave = (formData) => {
    // The actual save logic is handled in LabUnitEditorPage
    // This is just a callback for the editor
    console.log("Lab unit saved:", formData);
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      {!isEditing ? (
        <LabUnitList
          onSelectUnit={setSelectedUnit}
          onEditUnit={handleEditUnit}
        />
      ) : (
        <LabUnitEditor
          unit={selectedUnit}
          onBack={handleBack}
          onSave={handleSave}
          isNewUnit={!selectedUnit}
        />
      )}
    </>
  );
}

export default LabUnitManagementPage;
