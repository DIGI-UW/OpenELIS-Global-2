import { ArrowRight, Save } from "@carbon/icons-react";
import {
  Button,
  ClickableTile,
  Column,
  Dropdown,
  Grid,
  Loading,
  TextInput,
  Tile,
} from "@carbon/react";
import { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import  {OEToastNotification} from "../common/OEToastNotification";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";
import "./ShipmentDashboard.css";
import ShipmentNavigation from "./ShipmentNavigation";

const ShipmentSettings = () => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [boxLabelPrefix, setBoxLabelPrefix] = useState("");
  const [originalPrefix, setOriginalPrefix] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Site organization — orgId for dropdown, fhirUuid for display/storage
  const [siteOrgId, setSiteOrgId] = useState("");
  const [originalSiteOrgId, setOriginalSiteOrgId] = useState("");
  const [siteOrgFhirUuid, setSiteOrgFhirUuid] = useState("");
  const [organizations, setOrganizations] = useState([]);
  const [savingSiteOrg, setSavingSiteOrg] = useState(false);

  // FHIR mapping config
  const [fhirConfig, setFhirConfig] = useState({
    containerTypeCode: "434711009",
    containerTypeDisplay: "Specimen container",
    nonConformityCodes: {
      RECEIVED_DAMAGED: "281411007",
      RECEIVED_LEAKED: "281412000",
      MISSING: "281264009",
      REJECTED: "123840003",
    },
  });
  const [originalFhirConfig, setOriginalFhirConfig] = useState(null);
  const [savingFhir, setSavingFhir] = useState(false);

  useEffect(() => {
    fetchPrefix();
    fetchSiteOrgUuid();
    fetchOrganizations();
    fetchFhirConfig();
  }, []);

  const fetchPrefix = () => {
    setLoading(true);
    getFromOpenElisServer("/rest/shipping-box/box-label-prefix", (response) => {
      if (response) {
        setBoxLabelPrefix(response);
        setOriginalPrefix(response);
      }
      setLoading(false);
    });
  };

  const fetchSiteOrgUuid = () => {
    getFromOpenElisServer(
      "/rest/shipping-box/site-organization-uuid",
      (response) => {
        if (response && response.orgId !== undefined) {
          setSiteOrgId(response.orgId || "");
          setOriginalSiteOrgId(response.orgId || "");
          setSiteOrgFhirUuid(response.fhirUuid || "");
        }
      },
    );
  };

  const fetchOrganizations = () => {
    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_ORGANIZATIONS",
      (response) => {
        if (response && Array.isArray(response)) {
          setOrganizations(response);
        }
      },
    );
  };

  const handleSaveSiteOrgUuid = () => {
    setSavingSiteOrg(true);
    putToOpenElisServerFullResponse(
      "/rest/shipping-box/site-organization-uuid",
      JSON.stringify(siteOrgId),
      async (response) => {
        try {
          if (response.ok) {
            const result = await response.json();
            setSiteOrgId(result.orgId || "");
            setOriginalSiteOrgId(result.orgId || "");
            setSiteOrgFhirUuid(result.fhirUuid || "");
            addNotification({
              kind: "success",
              title: intl.formatMessage({ id: "notification.success" }),
              message: intl.formatMessage(
                { id: "shipment.settings.siteOrgSaved" },
                { uuid: result.fhirUuid || "" },
              ),
            });
          } else {
            const errorText = await response.text();
            addNotification({
              kind: "error",
              title: intl.formatMessage({ id: "notification.error" }),
              message:
                errorText ||
                intl.formatMessage({
                  id: "shipment.settings.siteOrgSaveError",
                }),
            });
          }
        } catch (error) {
          console.error("Error saving site org UUID:", error);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message: intl.formatMessage({
              id: "shipment.settings.siteOrgSaveError",
            }),
          });
        } finally {
          setSavingSiteOrg(false);
        }
      },
    );
  };

  const handleSavePrefix = () => {
    const trimmed = boxLabelPrefix.trim().toUpperCase();
    if (!trimmed) {
      addNotification({
        kind: "warning",
        title: intl.formatMessage({ id: "notification.warning" }),
        message: intl.formatMessage({
          id: "shipment.settings.prefixRequired",
        }),
      });
      return;
    }

    setSaving(true);
    putToOpenElisServerFullResponse(
      "/rest/shipping-box/box-label-prefix",
      JSON.stringify(trimmed),
      async (response) => {
        try {
          if (response.ok) {
            const savedPrefix = await response.text();
            setBoxLabelPrefix(savedPrefix);
            setOriginalPrefix(savedPrefix);
            addNotification({
              kind: "success",
              title: intl.formatMessage({ id: "notification.success" }),
              message: intl.formatMessage({
                id: "shipment.settings.prefixSaved",
              }),
            });
          } else {
            addNotification({
              kind: "error",
              title: intl.formatMessage({ id: "notification.error" }),
              message: intl.formatMessage({
                id: "shipment.settings.prefixSaveError",
              }),
            });
          }
        } catch (error) {
          console.error("Error saving prefix:", error);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message: intl.formatMessage({
              id: "shipment.settings.prefixSaveError",
            }),
          });
        } finally {
          setSaving(false);
        }
      },
    );
  };

  const fetchFhirConfig = () => {
    getFromOpenElisServer(
      "/rest/shipping-box/fhir-mapping-config",
      (response) => {
        if (response && response.containerTypeCode) {
          let ncCodes = {};
          try {
            ncCodes =
              typeof response.nonConformityCodes === "string"
                ? JSON.parse(response.nonConformityCodes)
                : response.nonConformityCodes;
          } catch {
            ncCodes = {};
          }
          const cfg = {
            containerTypeCode: response.containerTypeCode || "434711009",
            containerTypeDisplay:
              response.containerTypeDisplay || "Specimen container",
            nonConformityCodes: {
              RECEIVED_DAMAGED: ncCodes.RECEIVED_DAMAGED || "281411007",
              RECEIVED_LEAKED: ncCodes.RECEIVED_LEAKED || "281412000",
              MISSING: ncCodes.MISSING || "281264009",
              REJECTED: ncCodes.REJECTED || "123840003",
            },
          };
          setFhirConfig(cfg);
          setOriginalFhirConfig(JSON.stringify(cfg));
        }
      },
    );
  };

  const handleSaveFhirConfig = () => {
    setSavingFhir(true);
    putToOpenElisServerFullResponse(
      "/rest/shipping-box/fhir-mapping-config",
      JSON.stringify({
        containerTypeCode: fhirConfig.containerTypeCode,
        containerTypeDisplay: fhirConfig.containerTypeDisplay,
        nonConformityCodes: JSON.stringify(fhirConfig.nonConformityCodes),
      }),
      (response) => {
        if (response.ok) {
          setOriginalFhirConfig(JSON.stringify(fhirConfig));
          addNotification({
            kind: "success",
            title: intl.formatMessage({ id: "notification.success" }),
            message: intl.formatMessage({
              id: "shipment.settings.fhirConfigSaved",
            }),
          });
        } else {
          console.error("Error saving FHIR config:", response.status);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message: intl.formatMessage({
              id: "shipment.settings.fhirConfigSaveError",
            }),
          });
        }
        setSavingFhir(false);
      },
    );
  };

  const fhirConfigChanged =
    originalFhirConfig !== null &&
    JSON.stringify(fhirConfig) !== originalFhirConfig;

  const hasChanges = boxLabelPrefix.trim().toUpperCase() !== originalPrefix;

  return (
    <div className="shipment-dashboard">
      <OEToastNotification />
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "shipment.breadcrumb", link: "/SampleShipment" },
          {
            label: "shipment.settings.title",
            link: "/SampleShipment/settings",
          },
        ]}
      />
      <ShipmentNavigation />

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Tile className="dashboard-header">
            <h2>
              <FormattedMessage id="shipment.settings.title" />
            </h2>
            <p>
              <FormattedMessage id="shipment.settings.description" />
            </p>
          </Tile>
        </Column>
      </Grid>

      {loading ? (
        <Loading />
      ) : (
        <Grid fullWidth style={{ marginTop: "1rem" }}>
          <Column lg={8} md={6} sm={4}>
            <Tile>
              <h4 style={{ marginBottom: "1rem" }}>
                <FormattedMessage id="shipment.settings.boxLabelSection" />
              </h4>
              <TextInput
                id="box-label-prefix"
                labelText={intl.formatMessage({
                  id: "shipment.box.labelPrefix",
                })}
                helperText={intl.formatMessage({
                  id: "shipment.settings.prefixHelperText",
                })}
                value={boxLabelPrefix}
                onChange={(e) => setBoxLabelPrefix(e.target.value)}
                maxLength={10}
                placeholder={intl.formatMessage({
                  id: "shipment.settings.prefixPlaceholder",
                })}
              />
              <p
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="shipment.settings.prefixPreview"
                  values={{
                    preview: `${boxLabelPrefix.trim().toUpperCase() || "BOX"}-${new Date().getFullYear()}-0001`,
                  }}
                />
              </p>
              <Button
                renderIcon={Save}
                onClick={handleSavePrefix}
                disabled={saving || !hasChanges}
                style={{ marginTop: "1rem" }}
              >
                {saving ? (
                  <FormattedMessage id="label.saving" />
                ) : (
                  <FormattedMessage id="label.button.save" />
                )}
              </Button>
            </Tile>
          </Column>

          <Column lg={8} md={6} sm={4}>
            <Tile style={{ marginTop: "1rem" }}>
              <h4 style={{ marginBottom: "1rem" }}>
                <FormattedMessage id="shipment.settings.siteOrgSection" />
              </h4>
              <p
                style={{
                  fontSize: "0.875rem",
                  color: "#525252",
                  marginBottom: "1rem",
                }}
              >
                <FormattedMessage id="shipment.settings.siteOrgDescription" />
              </p>
              <Dropdown
                id="site-org-uuid"
                titleText={intl.formatMessage({
                  id: "shipment.settings.siteOrgLabel",
                })}
                label={intl.formatMessage({ id: "label.select" })}
                items={[
                  { id: "", value: intl.formatMessage({ id: "label.none" }) },
                  ...organizations.map((org) => ({
                    id: org.id,
                    value: org.value,
                  })),
                ]}
                itemToString={(item) => (item ? item.value : "")}
                selectedItem={
                  organizations.find((o) => o.id === siteOrgId) || {
                    id: "",
                    value: intl.formatMessage({ id: "label.select" }),
                  }
                }
                onChange={({ selectedItem }) =>
                  setSiteOrgId(selectedItem?.id || "")
                }
              />
              {siteOrgFhirUuid && (
                <p
                  style={{
                    marginTop: "0.5rem",
                    fontSize: "0.75rem",
                    color: "#525252",
                    fontFamily: "monospace",
                  }}
                >
                  FHIR UUID: {siteOrgFhirUuid}
                </p>
              )}
              <Button
                renderIcon={Save}
                onClick={handleSaveSiteOrgUuid}
                disabled={savingSiteOrg || siteOrgId === originalSiteOrgId}
                style={{ marginTop: "1rem" }}
              >
                {savingSiteOrg ? (
                  <FormattedMessage id="label.saving" />
                ) : (
                  <FormattedMessage id="label.button.save" />
                )}
              </Button>
            </Tile>
          </Column>

          <Column lg={8} md={6} sm={4}>
            <h4 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage id="shipment.settings.administration" />
            </h4>

            <ClickableTile
              href="/MasterListsPage/organizationManagement"
              style={{ marginBottom: "0.5rem" }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <div>
                  <strong>
                    <FormattedMessage id="shipment.settings.manageFacilities" />
                  </strong>
                  <p
                    style={{
                      fontSize: "0.875rem",
                      color: "#525252",
                      marginTop: "0.25rem",
                    }}
                  >
                    <FormattedMessage id="shipment.settings.manageFacilitiesDesc" />
                  </p>
                </div>
                <ArrowRight size={20} />
              </div>
            </ClickableTile>

            <ClickableTile
              href="/MasterListsPage/userManagement"
              style={{ marginBottom: "0.5rem" }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <div>
                  <strong>
                    <FormattedMessage id="shipment.settings.manageUsers" />
                  </strong>
                  <p
                    style={{
                      fontSize: "0.875rem",
                      color: "#525252",
                      marginTop: "0.25rem",
                    }}
                  >
                    <FormattedMessage id="shipment.settings.manageUsersDesc" />
                  </p>
                </div>
                <ArrowRight size={20} />
              </div>
            </ClickableTile>

            <Tile style={{ marginBottom: "0.5rem" }}>
              <h4 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage id="shipment.settings.fhirConfig" />
              </h4>
              <p
                style={{
                  fontSize: "0.875rem",
                  color: "#525252",
                  marginBottom: "1rem",
                }}
              >
                <FormattedMessage id="shipment.settings.fhirConfigDesc" />
              </p>

              <h5 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage id="shipment.settings.fhirContainerType" />
              </h5>
              <div
                style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}
              >
                <TextInput
                  id="containerTypeCode"
                  labelText={intl.formatMessage({
                    id: "shipment.settings.fhirSnomedCode",
                  })}
                  value={fhirConfig.containerTypeCode}
                  onChange={(e) =>
                    setFhirConfig({
                      ...fhirConfig,
                      containerTypeCode: e.target.value,
                    })
                  }
                  size="sm"
                  style={{ flex: 1 }}
                />
                <TextInput
                  id="containerTypeDisplay"
                  labelText={intl.formatMessage({
                    id: "shipment.settings.fhirDisplayName",
                  })}
                  value={fhirConfig.containerTypeDisplay}
                  onChange={(e) =>
                    setFhirConfig({
                      ...fhirConfig,
                      containerTypeDisplay: e.target.value,
                    })
                  }
                  size="sm"
                  style={{ flex: 2 }}
                />
              </div>

              <h5 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage id="shipment.settings.fhirNonConformity" />
              </h5>
              {Object.entries(fhirConfig.nonConformityCodes).map(
                ([key, code]) => (
                  <div
                    key={key}
                    style={{
                      display: "flex",
                      gap: "1rem",
                      marginBottom: "0.5rem",
                      alignItems: "flex-end",
                    }}
                  >
                    <span
                      style={{
                        fontSize: "0.875rem",
                        minWidth: "160px",
                        paddingBottom: "0.5rem",
                      }}
                    >
                      {intl.formatMessage({
                        id: `shipment.reception.${key === "RECEIVED_DAMAGED" ? "damaged" : key === "RECEIVED_LEAKED" ? "leaked" : key === "MISSING" ? "missing" : "rejected"}`,
                      })}
                    </span>
                    <TextInput
                      id={`nc-${key}`}
                      labelText={intl.formatMessage({
                        id: "shipment.settings.fhirSnomedCode",
                      })}
                      value={code}
                      onChange={(e) =>
                        setFhirConfig({
                          ...fhirConfig,
                          nonConformityCodes: {
                            ...fhirConfig.nonConformityCodes,
                            [key]: e.target.value,
                          },
                        })
                      }
                      size="sm"
                      hideLabel
                    />
                  </div>
                ),
              )}

              <Button
                renderIcon={Save}
                onClick={handleSaveFhirConfig}
                disabled={savingFhir || !fhirConfigChanged}
                style={{ marginTop: "1rem" }}
                size="sm"
              >
                {savingFhir ? (
                  <FormattedMessage id="label.saving" />
                ) : (
                  <FormattedMessage id="label.button.save" />
                )}
              </Button>
            </Tile>
          </Column>
        </Grid>
      )}
    </div>
  );
};

export default ShipmentSettings;
