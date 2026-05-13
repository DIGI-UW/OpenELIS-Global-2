import React, { useContext, useState, useRef, useEffect } from "react";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  Toggle,
  Select,
  SelectItem,
  TextInput,
  Button,
} from "@carbon/react";
import { getFromOpenElisServer, postToOpenElisServerJsonResponse } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { CustomShowGuide } from "../testManagementConfigMenu/customComponents/CustomShowGuide";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage/SampleTypeManagement",
  },
  {
    label: "configuration.type.rename",
    link: "/MasterListsPage/SampleTypeRenameEntry",
  },
];

function SampleTypeRenameEntry() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [isLoading, setIsLoading] = useState(false);
  const [sampleTypeList, setSampleTypeList] = useState([]);
  const [selectedSampleType, setSelectedSampleType] = useState(null);
  const [newName, setNewName] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [showGuide, setShowGuide] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  const loadSampleTypes = () => {
    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/SampleTypeManagement`,
      (res) => {
        if (componentMounted.current) {
          setSampleTypeList(res.menuList || []);
          setIsLoading(false);
        }
      }
    );
  };

  const handleSampleTypeSelect = (value) => {
    const sampleType = sampleTypeList.find(item => item.id === value);
    setSelectedSampleType(sampleType);
    if (sampleType) {
      setNewName(sampleType.description || sampleType.name || "");
      setNewDescription(sampleType.description || "");
    } else {
      setNewName("");
      setNewDescription("");
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!selectedSampleType || !newName.trim()) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Please select a sample type and enter a new name.",
      });
      setNotificationVisible(true);
      return;
    }

    setIsSubmitting(true);

    const submitData = {
      id: selectedSampleType.id,
      name: newName.trim(),
      description: newDescription.trim(),
    };

    postToOpenElisServerJsonResponse(
      `/rest/SampleTypeManagement/rename`,
      submitData,
      (res) => {
        if (res && res.success) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "sample.type.update.success" }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);

          // Reset form and reload data
          setSelectedSampleType(null);
          setNewName("");
          setNewDescription("");
          loadSampleTypes();
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: res?.message || intl.formatMessage({ id: "server.error.msg" }),
          });
          setNotificationVisible(true);
        }
        setIsSubmitting(false);
      }
    );
  };

  const handleCancel = () => {
    window.history.back();
  };

  const rows = [
    {
      id: "sampleTypeSelect",
      field: intl.formatMessage({ id: "sample.type" }),
      description: <FormattedMessage id="configuration.type.rename.explain" />,
    },
    {
      id: "name",
      field: intl.formatMessage({ id: "sample.type.name" }),
      description: <FormattedMessage id="description.sampleType" />,
    },
  ];

  useEffect(() => {
    componentMounted.current = true;
    loadSampleTypes();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  if (isLoading) {
    return <Loading />;
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.type.rename" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />

          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Toggle
                id="toggle"
                labelText={<FormattedMessage id="test.show.guide" />}
                onClick={handleToggleShowGuide}
              />
            </Column>
          </Grid>
          {showGuide && <CustomShowGuide rows={rows} />}

          <br />

          <form onSubmit={handleSubmit}>
            <Grid fullWidth={true}>
              <Column lg={8} md={4} sm={2}>
                <Select
                  id="sample-type-select"
                  labelText={<FormattedMessage id="sample.type" />}
                  placeholder="Select a sample type to rename"
                  onChange={(e) => handleSampleTypeSelect(e.target.value)}
                  value={selectedSampleType?.id || ""}
                  required
                >
                  <SelectItem value="" text="Select a sample type..." />
                  {sampleTypeList.map((sampleType) => (
                    <SelectItem
                      key={sampleType.id}
                      value={sampleType.id}
                      text={sampleType.description || sampleType.name || `Sample Type ${sampleType.id}`}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>

            {selectedSampleType && (
              <>
                <br />
                <hr />
                <br />

                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={2}>
                    <TextInput
                      id="sample-type-name"
                      labelText={<FormattedMessage id="sample.type.name" />}
                      placeholder={intl.formatMessage({ id: "sample.type.name.placeholder" })}
                      value={newName}
                      onChange={(e) => setNewName(e.target.value)}
                      required
                    />
                  </Column>

                  <Column lg={8} md={4} sm={2}>
                    <TextInput
                      id="sample-type-description"
                      labelText={<FormattedMessage id="sample.type.description" />}
                      placeholder="Enter description (optional)"
                      value={newDescription}
                      onChange={(e) => setNewDescription(e.target.value)}
                    />
                  </Column>
                </Grid>

                <br />
                <br />

                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <Button
                      type="submit"
                      disabled={isSubmitting}
                    >
                      {isSubmitting ? (
                        <FormattedMessage id="sample.type.updating" />
                      ) : (
                        <FormattedMessage id="label.button.save" />
                      )}
                    </Button>

                    <Button
                      kind="secondary"
                      onClick={handleCancel}
                      style={{ marginLeft: "1rem" }}
                      disabled={isSubmitting}
                    >
                      <FormattedMessage id="label.button.cancel" />
                    </Button>
                  </Column>
                </Grid>
              </>
            )}
          </form>
        </div>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeRenameEntry);