import React, { useState, useEffect } from "react";
import { useParams, useHistory } from "react-router-dom";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import { FormattedMessage, useIntl } from "react-intl";
import LabUnitEditor from "./LabUnitEditor.js";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

function LabUnitEditorPage() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    React.useContext(NotificationContext);
  const intl = useIntl();
  const { id } = useParams();
  const history = useHistory();
  const [unit, setUnit] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isNewUnit, setIsNewUnit] = useState(id === "add");

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "labUnit.management.title",
      link: "/MasterListsPage/labUnitManagement",
    },
    {
      label: isNewUnit ? "breadcrumb.add.lab.unit" : "breadcrumb.edit.lab.unit",
      link: isNewUnit ? "#" : `/MasterListsPage/labUnitManagement/edit/${id}`,
    },
  ];

  useEffect(() => {
    if (!isNewUnit) {
      loadLabUnit(id);
    } else {
      setLoading(false);
    }
  }, [id, isNewUnit]);

  const loadLabUnit = (unitId) => {
    setLoading(true);
    getFromOpenElisServer(`/rest/api/lab-units/${unitId}`, (response) => {
      if (response) {
        setUnit(response);
        setLoading(false);
      } else {
        addNotification({
          title: intl.formatMessage({ id: "labUnit.load.error" }),
          kind: "error",
          timestamp: new Date().toISOString(),
        });
        setLoading(false);
      }
    });
  };

  const handleBack = () => {
    history.push("/MasterListsPage/labUnitManagement");
  };

  const handleSave = (formData) => {
    if (isNewUnit) {
      // Create new lab unit
      postToOpenElisServer("/rest/api/lab-units", formData, (response) => {
        if (response && response.id) {
          addNotification({
            title: intl.formatMessage({ id: "labUnit.create.success" }),
            kind: "success",
            timestamp: new Date().toISOString(),
          });
          history.push(
            `/MasterListsPage/labUnitManagement/edit/${response.id}`,
          );
        } else {
          addNotification({
            title: intl.formatMessage({ id: "labUnit.create.error" }),
            kind: "error",
            timestamp: new Date().toISOString(),
          });
        }
      });
    } else {
      // Update existing lab unit
      postToOpenElisServer(
        `/rest/api/lab-units/${id}`,
        formData,
        (response) => {
          if (response && response.id) {
            setUnit(response);
            addNotification({
              title: intl.formatMessage({ id: "labUnit.update.success" }),
              kind: "success",
              timestamp: new Date().toISOString(),
            });
          } else {
            addNotification({
              title: intl.formatMessage({ id: "labUnit.update.error" }),
              kind: "error",
              timestamp: new Date().toISOString(),
            });
          }
        },
      );
    }
  };

  if (loading) {
    return (
      <div
        className="loading-container"
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          minHeight: "400px",
        }}
      >
        <FormattedMessage id="loading.lab.unit" />
      </div>
    );
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <LabUnitEditor
        unit={unit}
        onBack={handleBack}
        onSave={handleSave}
        isNewUnit={isNewUnit}
      />
    </>
  );
}

export default LabUnitEditorPage;
