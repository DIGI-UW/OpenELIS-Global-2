import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Grid,
  Column,
  Section,
  RadioButtonGroup,
  RadioButton,
  Select,
  SelectItem,
  Modal,
  InlineLoading,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const DOMAIN_VALUES = ["CLINICAL", "ENVIRONMENTAL", "VECTOR"];

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "configuration.testUnit.manage",
    link: "/MasterListsPage/TestSectionManagement",
  },
  {
    label: "admin.labUnit.edit.title",
    link: "/MasterListsPage/TestSectionEdit",
  },
];

function TestSectionEdit() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [sectionList, setSectionList] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const [originalDomain, setOriginalDomain] = useState("");
  const [domain, setDomain] = useState("");
  const [nameEnglish, setNameEnglish] = useState("");
  const [nameFrench, setNameFrench] = useState("");
  const [loadingSection, setLoadingSection] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/TestSectionEdit", (res) => {
      if (componentMounted.current && res) {
        setSectionList(res.testSectionList || []);
      }
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const handleSectionSelect = (e) => {
    const id = e.target.value;
    setSelectedId(id);
    setDomain("");
    setOriginalDomain("");
    setNameEnglish("");
    setNameFrench("");
    if (!id) return;

    setLoadingSection(true);
    getFromOpenElisServer(
      `/rest/TestSectionEdit/section?testSectionId=${id}`,
      (res) => {
        if (componentMounted.current && res) {
          setDomain(res.domain || "");
          setOriginalDomain(res.domain || "");
          setNameEnglish(res.nameEnglish || "");
          setNameFrench(res.nameFrench || "");
        }
        setLoadingSection(false);
      },
    );
  };

  const handleSaveClick = () => {
    if (domain !== originalDomain) {
      setShowConfirm(true);
    } else {
      submitDomainUpdate();
    }
  };

  const submitDomainUpdate = () => {
    setSaving(true);
    setShowConfirm(false);
    postToOpenElisServerJsonResponse(
      "/rest/TestSectionEdit",
      JSON.stringify({ testSectionId: selectedId, domain }),
      (res) => {
        setSaving(false);
        if (res) {
          setOriginalDomain(domain);
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.user.post.save.success",
            }),
            kind: NotificationKinds.success,
          });
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  const domainLabel = (value) =>
    intl.formatMessage({ id: `admin.labUnit.basicInfo.domain.${value.toLowerCase()}` });

  const hasChanges = domain && domain !== originalDomain;

  return (
    <>
      {notificationVisible && <AlertDialog />}

      <Modal
        open={showConfirm}
        modalHeading={intl.formatMessage({
          id: "admin.labUnit.domain.confirm.heading",
        })}
        primaryButtonText={intl.formatMessage({ id: "accept.action.button" })}
        secondaryButtonText={intl.formatMessage({
          id: "reject.action.button",
        })}
        onRequestSubmit={submitDomainUpdate}
        onRequestClose={() => setShowConfirm(false)}
      >
        <p>
          <FormattedMessage id="admin.labUnit.domain.confirm.body" />
        </p>
      </Modal>

      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="admin.labUnit.edit.title" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />

          <Grid fullWidth={true}>
            <Column lg={8} md={4} sm={4}>
              <FormattedMessage id="admin.labUnit.edit.select" />
              <span className="requiredlabel">*</span> :
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="test-section-select"
                labelText=""
                hideLabel
                value={selectedId}
                onChange={handleSectionSelect}
              >
                <SelectItem value="" text="" />
                {sectionList.map((item) => (
                  <SelectItem key={item.id} value={item.id} text={item.value} />
                ))}
              </Select>
            </Column>
          </Grid>

          {loadingSection && (
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <InlineLoading description="Loading..." />
              </Column>
            </Grid>
          )}

          {selectedId && !loadingSection && domain && (
            <>
              <br />
              {(nameEnglish || nameFrench) && (
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <strong>{nameEnglish}</strong>
                    {nameFrench && nameFrench !== nameEnglish && (
                      <> / {nameFrench}</>
                    )}
                  </Column>
                </Grid>
              )}
              <br />
              <Grid fullWidth={true}>
                <Column lg={8} md={4} sm={4}>
                  <FormattedMessage id="admin.labUnit.basicInfo.domain.label" />
                  <span className="requiredlabel">*</span> :
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    name="domain"
                    legendText=""
                    valueSelected={domain}
                    onChange={(value) => setDomain(value)}
                  >
                    {DOMAIN_VALUES.map((val) => (
                      <RadioButton
                        key={val}
                        labelText={domainLabel(val)}
                        value={val}
                        id={`domain-edit-${val.toLowerCase()}`}
                      />
                    ))}
                  </RadioButtonGroup>
                </Column>
              </Grid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={8} md={8} sm={4}>
                  <Button
                    kind="primary"
                    onClick={handleSaveClick}
                    disabled={saving || !hasChanges}
                  >
                    {saving ? (
                      <InlineLoading description="Saving..." />
                    ) : (
                      <FormattedMessage id="label.button.save" />
                    )}
                  </Button>
                </Column>
              </Grid>
            </>
          )}
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestSectionEdit);
