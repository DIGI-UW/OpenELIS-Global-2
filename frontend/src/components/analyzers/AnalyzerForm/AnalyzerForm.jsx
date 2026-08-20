import React, { useState, useEffect, useRef, useMemo } from "react";
import {
  Button,
  ButtonSet,
  TextInput,
  Dropdown,
  InlineNotification,
  FormGroup,
  Loading,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useLocation, useParams } from "react-router-dom";
import {
  createAnalyzer,
  updateAnalyzer,
  getAnalyzer,
  getAnalyzerTypeCatalog,
} from "../../../services/analyzerService";
import TestConnectionModal from "../TestConnectionModal/TestConnectionModal";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { COMMUNICATION_MODES, resolveAnalyzerApiMessage } from "../constants";
import "./AnalyzerForm.css";

const AnalyzerForm = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { id: analyzerId } = useParams();
  const isEditMode = !!analyzerId;
  const [analyzer, setAnalyzer] = useState(null);
  const [loadingAnalyzer, setLoadingAnalyzer] = useState(isEditMode);

  const [formData, setFormData] = useState({
    name: "",
    profileId: "",
    profileRevision: null,
    ipAddress: "",
    port: null,
    protocolVersion: null,
    communicationMode: null,
    testUnitIds: [],
    status: "SETUP",
    importDirectory: "",
  });

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notification, setNotification] = useState(null);
  const [testConnectionModalOpen, setTestConnectionModalOpen] = useState(false);
  const closeTimeoutRef = useRef(null);

  const [profileCatalog, setProfileCatalog] = useState(null);
  const [loadingProfiles, setLoadingProfiles] = useState(true);

  // Unified status options (manual transitions only - ACTIVE, ERROR_PENDING, OFFLINE are automatic).
  // PENDING_REGISTRATION stubs (discovered by the bridge) can only transition to SETUP or
  // INACTIVE per backend rules in AnalyzerServiceImpl.isValidTransition.
  const statusOptions =
    analyzer?.status === "PENDING_REGISTRATION"
      ? [
          {
            id: "SETUP",
            text: intl.formatMessage({ id: "analyzer.status.setup" }),
          },
          {
            id: "INACTIVE",
            text: intl.formatMessage({ id: "analyzer.status.inactive" }),
          },
        ]
      : [
          {
            id: "INACTIVE",
            text: intl.formatMessage({ id: "analyzer.status.inactive" }),
          },
          {
            id: "SETUP",
            text: intl.formatMessage({ id: "analyzer.status.setup" }),
          },
          {
            id: "VALIDATION",
            text: intl.formatMessage({ id: "analyzer.status.validation" }),
          },
        ];

  // Fetch analyzer data when editing (route-param driven)
  useEffect(() => {
    if (isEditMode) {
      getAnalyzer(analyzerId, (data) => {
        setLoadingAnalyzer(false);
        const a = data?.analyzers?.[0] || data;
        setAnalyzer(a);
        setFormData({
          name: a.name || "",
          profileId: a.profileId || "",
          profileRevision: a.profileRevision || null,
          ipAddress: a.ipAddress || "",
          port: a.port == null ? null : String(a.port),
          protocolVersion: a.protocolVersion || null,
          communicationMode: a.communicationMode || null,
          testUnitIds: a.testUnitIds || [],
          status: a.status || "SETUP",
          importDirectory: a.importDirectory || "",
        });
      });
    }
  }, [analyzerId, isEditMode]);

  useEffect(() => {
    return () => {
      if (closeTimeoutRef.current) {
        clearTimeout(closeTimeoutRef.current);
        closeTimeoutRef.current = null;
      }
    };
  }, []);

  const navigateBack = () => {
    if (closeTimeoutRef.current) {
      clearTimeout(closeTimeoutRef.current);
      closeTimeoutRef.current = null;
    }
    history.push("/analyzers");
  };

  useEffect(() => {
    getAnalyzerTypeCatalog((data) => {
      setLoadingProfiles(false);
      if (data && Array.isArray(data.types)) {
        setProfileCatalog(data);
      } else {
        setProfileCatalog(null);
        setNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "analyzer.form.type.loadError",
          }),
        });
      }
    });
  }, [intl]);

  const activeProfiles = useMemo(
    () =>
      (profileCatalog?.types || [])
        .filter((profile) => profile.status === "ACTIVE")
        .sort((left, right) =>
          left.displayName.localeCompare(right.displayName),
        ),
    [profileCatalog],
  );

  const requestedPin = useMemo(() => {
    if (isEditMode) {
      return null;
    }
    const params = new URLSearchParams(location.search);
    const profileId = params.get("profile");
    const revisionText = params.get("revision");
    if (!profileId && !revisionText) {
      return null;
    }
    return {
      profileId,
      revision: Number(revisionText),
    };
  }, [isEditMode, location.search]);

  const requestedProfile = useMemo(
    () =>
      requestedPin
        ? activeProfiles.find(
            (profile) =>
              profile.profileId === requestedPin.profileId &&
              profile.revision === requestedPin.revision,
          ) || null
        : null,
    [activeProfiles, requestedPin],
  );

  const selectedProfile = useMemo(() => {
    if (!isEditMode && requestedProfile) {
      return requestedProfile;
    }
    const exact = (profileCatalog?.types || []).find(
      (profile) =>
        profile.profileId === formData.profileId &&
        profile.revision === formData.profileRevision,
    );
    if (exact) {
      return exact;
    }
    if (!isEditMode || !formData.profileId || !formData.profileRevision) {
      return null;
    }
    const currentIdentity = (profileCatalog?.types || []).find(
      (profile) => profile.profileId === formData.profileId,
    );
    return currentIdentity
      ? { ...currentIdentity, revision: formData.profileRevision }
      : null;
  }, [
    formData.profileId,
    formData.profileRevision,
    isEditMode,
    profileCatalog,
    requestedProfile,
  ]);

  const formMatchesSelectedProfile =
    Boolean(selectedProfile) &&
    (isEditMode ||
      (formData.profileId === selectedProfile.profileId &&
        formData.profileRevision === selectedProfile.revision));
  const selectedDefaults = selectedProfile?.instanceDefaults || {};
  const effectiveProtocolVersion =
    (formMatchesSelectedProfile && formData.protocolVersion) ||
    selectedDefaults.protocolVersion ||
    null;
  const effectiveCommunicationMode =
    (formMatchesSelectedProfile && formData.communicationMode) ||
    selectedDefaults.communicationMode ||
    "";
  const effectivePort =
    formMatchesSelectedProfile && formData.port != null
      ? formData.port
      : selectedDefaults.port == null
        ? ""
        : String(selectedDefaults.port);

  const isFileProtocol = selectedProfile?.protocol?.toUpperCase() === "FILE";
  const invalidProfileSelection =
    Boolean(profileCatalog) && Boolean(requestedPin) && !requestedProfile;
  const visibleNotification =
    notification ||
    (invalidProfileSelection
      ? {
          kind: "error",
          title: intl.formatMessage({
            id: "analyzer.form.type.invalidSelection",
          }),
        }
      : null);

  const communicationModeItems = useMemo(
    () =>
      COMMUNICATION_MODES.map((m) => ({
        ...m,
        label: intl.formatMessage({ id: m.labelId }),
      })),
    [intl],
  );

  const validateIPAddress = (ip) => {
    const ipRegex = /^(\d{1,3}\.){3}\d{1,3}$/;
    if (!ipRegex.test(ip)) {
      return intl.formatMessage({
        id: "analyzer.form.validation.ipAddress.invalid",
      });
    }
    const parts = ip.split(".");
    for (const part of parts) {
      const num = parseInt(part, 10);
      if (num < 0 || num > 255) {
        return intl.formatMessage({
          id: "analyzer.form.validation.ipAddress.invalid",
        });
      }
    }
    return null;
  };

  const handleFieldChange = (field, value) => {
    setFormData((previous) => ({
      ...previous,
      ...(!isEditMode && selectedProfile
        ? {
            profileId: selectedProfile.profileId,
            profileRevision: selectedProfile.revision,
          }
        : {}),
      [field]: value,
    }));
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const handleProfileSelect = (profile) => {
    if (!profile) {
      return;
    }
    setFormData((previous) => ({
      ...previous,
      profileId: profile.profileId,
      profileRevision: profile.revision,
      protocolVersion: null,
      communicationMode: null,
      port: null,
    }));
    setErrors((previous) => {
      const next = { ...previous };
      delete next.profileId;
      return next;
    });

    const params = new URLSearchParams(location.search);
    params.set("profile", profile.profileId);
    params.set("revision", String(profile.revision));
    history.push({
      pathname: location.pathname,
      search: `?${params.toString()}`,
    });
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = intl.formatMessage({
        id: "analyzer.form.validation.name.required",
      });
    }

    if (!selectedProfile) {
      newErrors.profileId = intl.formatMessage({
        id: "analyzer.form.validation.type.required",
      });
    }

    if (isFileProtocol && !formData.importDirectory.trim()) {
      newErrors.importDirectory = intl.formatMessage({
        id: "analyzer.form.validation.importDirectory.required",
        defaultMessage: "Import directory is required for file-based analyzers",
      });
    }

    if (!isFileProtocol && formData.ipAddress) {
      const ipError = validateIPAddress(formData.ipAddress);
      if (ipError) {
        newErrors.ipAddress = ipError;
      }
    }

    if (!isFileProtocol && effectivePort) {
      const portNum = parseInt(effectivePort, 10);
      if (isNaN(portNum) || portNum < 1 || portNum > 65535) {
        newErrors.port = intl.formatMessage({
          id: "analyzer.form.validation.port.invalid",
        });
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = () => {
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setNotification(null);

    const submitData = {
      ...formData,
      profileId: selectedProfile.profileId,
      profileRevision: selectedProfile.revision,
      protocolVersion: effectiveProtocolVersion,
      communicationMode: effectiveCommunicationMode || null,
      port: effectivePort ? parseInt(effectivePort, 10) : null,
      // Clear network/protocol fields for FILE protocol — not applicable
      ...(isFileProtocol && {
        ipAddress: null,
        port: null,
        protocolVersion: null,
      }),
      // Only the Bridge watches FILE directories. OpenELIS stores and
      // registers the configured directory for that installed connection.
      ...(!isFileProtocol && {
        importDirectory: null,
      }),
    };

    const callback = (response, _extraParams) => {
      setIsSubmitting(false);
      if (response.error || response.statusCode >= 400) {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "analyzer.form.error.save" }),
          subtitle: resolveAnalyzerApiMessage(
            intl,
            response,
            "analyzer.form.error.unknown",
          ),
        });
      } else {
        setNotification({
          kind: "success",
          title: intl.formatMessage({ id: "analyzer.form.success.save" }),
        });
        // Navigate back after short delay so user sees the success notification.
        if (closeTimeoutRef.current) {
          clearTimeout(closeTimeoutRef.current);
        }
        closeTimeoutRef.current = setTimeout(() => {
          closeTimeoutRef.current = null;
          navigateBack();
        }, 1000);
      }
    };

    if (isEditMode) {
      updateAnalyzer(analyzer.id, submitData, callback);
    } else {
      createAnalyzer(submitData, callback);
    }
  };

  if (loadingAnalyzer || loadingProfiles) {
    return <Loading withOverlay={false} />;
  }

  return (
    <>
      <div
        data-testid="analyzer-form"
        className="analyzer-form-page pageContent"
      >
        <div data-testid="analyzer-form-header">
          <PageBreadCrumb
            breadcrumbs={[
              { label: "home.label", link: "/" },
              { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
              {
                label: isEditMode
                  ? "analyzer.form.editTitle"
                  : "analyzer.form.addTitle",
                link: "",
                isCurrentPage: true,
              },
            ]}
          />
          <div className="analyzer-form-heading">
            <h1>
              {intl.formatMessage({
                id: isEditMode
                  ? "analyzer.form.editTitle"
                  : "analyzer.form.addTitle",
              })}
            </h1>
            <p>{intl.formatMessage({ id: "analyzer.form.subtitle" })}</p>
          </div>
        </div>
        <div className="analyzer-form-content">
          {visibleNotification && (
            <InlineNotification
              kind={visibleNotification.kind}
              title={visibleNotification.title}
              subtitle={visibleNotification.subtitle}
              onClose={() => setNotification(null)}
              data-testid="analyzer-form-notification"
            />
          )}

          {/* Section 1 — Instance Identity */}
          <FormGroup legendText="">
            <TextInput
              id="analyzer-name"
              data-testid="analyzer-form-name-input"
              labelText={intl.formatMessage({ id: "analyzer.form.name" })}
              placeholder={intl.formatMessage({
                id: "analyzer.form.name.placeholder",
              })}
              value={formData.name}
              onChange={(e) => handleFieldChange("name", e.target.value)}
              invalid={!!errors.name}
              invalidText={errors.name}
              required
            />

            <Dropdown
              id="analyzer-status"
              data-testid="analyzer-form-status-dropdown"
              titleText={intl.formatMessage({
                id: "analyzer.form.status",
              })}
              label={intl.formatMessage({
                id: "analyzer.form.status",
              })}
              items={statusOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                statusOptions.find((opt) => opt.id === formData.status) ||
                statusOptions[1] // Default to SETUP
              }
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  handleFieldChange("status", selectedItem.id);
                }
              }}
              helperText={intl.formatMessage({
                id: "analyzer.form.status.helperText",
              })}
            />
          </FormGroup>

          {/* Section 2 — Reusable Analyzer Type */}
          <FormGroup legendText="">
            <Dropdown
              id="analyzer-type"
              data-testid="analyzer-form-type-dropdown"
              titleText={intl.formatMessage({ id: "analyzer.form.type" })}
              label={intl.formatMessage({
                id: "analyzer.form.type.placeholder",
              })}
              items={activeProfiles}
              selectedItem={selectedProfile}
              itemToString={(item) =>
                item
                  ? intl.formatMessage(
                      { id: "analyzer.form.type.revision" },
                      {
                        name: item.displayName,
                        revision: item.revision,
                        protocol: item.protocol,
                      },
                    )
                  : ""
              }
              onChange={({ selectedItem }) => handleProfileSelect(selectedItem)}
              disabled={
                loadingProfiles ||
                Boolean(
                  isEditMode && formData.profileId && formData.profileRevision,
                )
              }
              helperText={intl.formatMessage({
                id: "analyzer.form.type.helperText",
              })}
              invalid={!!errors.profileId}
              invalidText={errors.profileId}
              required
            />
          </FormGroup>

          {/* Section 3 — Connection (hidden for FILE protocol) */}
          {!isFileProtocol && (
            <FormGroup legendText="">
              <Dropdown
                id="analyzer-communication-mode"
                data-testid="analyzer-form-communication-mode-dropdown"
                titleText={intl.formatMessage({
                  id: "analyzer.form.communicationMode",
                })}
                label={intl.formatMessage({
                  id: "analyzer.form.communicationMode",
                })}
                items={communicationModeItems}
                selectedItem={
                  communicationModeItems.find(
                    (opt) => opt.value === effectiveCommunicationMode,
                  ) || null
                }
                itemToString={(item) => (item ? item.label : "")}
                onChange={({ selectedItem }) => {
                  if (selectedItem) {
                    handleFieldChange("communicationMode", selectedItem.value);
                  }
                }}
                helperText={intl.formatMessage({
                  id: "analyzer.form.communicationMode.help",
                })}
              />
              <div
                className="connection-fields"
                data-testid="analyzer-form-connection-fields"
              >
                <TextInput
                  id="analyzer-ip"
                  data-testid="analyzer-form-ip-input"
                  labelText={intl.formatMessage({
                    id: "analyzer.form.ipAddress",
                  })}
                  placeholder={intl.formatMessage({
                    id: "analyzer.form.ipAddress.placeholder",
                  })}
                  value={formData.ipAddress}
                  onChange={(e) =>
                    handleFieldChange("ipAddress", e.target.value)
                  }
                  invalid={!!errors.ipAddress}
                  invalidText={errors.ipAddress}
                />

                <TextInput
                  id="analyzer-port"
                  data-testid="analyzer-form-port-input"
                  labelText={intl.formatMessage({ id: "analyzer.form.port" })}
                  placeholder={intl.formatMessage({
                    id: "analyzer.form.port.placeholder",
                  })}
                  value={effectivePort}
                  onChange={(e) => handleFieldChange("port", e.target.value)}
                  invalid={!!errors.port}
                  invalidText={errors.port}
                />

                <Button
                  kind="tertiary"
                  onClick={() => setTestConnectionModalOpen(true)}
                  data-testid="analyzer-form-test-connection-button"
                >
                  {intl.formatMessage({ id: "analyzer.form.testConnection" })}
                </Button>
              </div>
            </FormGroup>
          )}

          {/* Section 3b — FILE protocol: import configuration */}
          {isFileProtocol && (
            <FormGroup
              legendText={intl.formatMessage({
                id: "analyzer.form.fileImport.title",
                defaultMessage: "File Import Settings",
              })}
            >
              <TextInput
                id="analyzer-import-directory"
                data-testid="analyzer-form-import-directory-input"
                labelText={intl.formatMessage({
                  id: "analyzer.form.importDirectory",
                  defaultMessage: "Import Directory",
                })}
                placeholder="/data/analyzer-imports/my-analyzer/incoming"
                value={formData.importDirectory}
                onChange={(e) =>
                  handleFieldChange("importDirectory", e.target.value)
                }
                invalid={!!errors.importDirectory}
                invalidText={errors.importDirectory}
                helperText={intl.formatMessage({
                  id: "analyzer.form.importDirectory.helperText",
                  defaultMessage:
                    "Directory the bridge watches for incoming result files",
                })}
              />
            </FormGroup>
          )}
        </div>
        <ButtonSet className="analyzer-form-actions">
          <Button
            kind="secondary"
            onClick={navigateBack}
            data-testid="analyzer-form-cancel-button"
          >
            {intl.formatMessage({ id: "analyzer.form.cancel" })}
          </Button>
          <Button
            kind="primary"
            onClick={handleSubmit}
            disabled={isSubmitting}
            data-testid="analyzer-form-save-button"
          >
            {intl.formatMessage({ id: "analyzer.form.save" })}
          </Button>
        </ButtonSet>
      </div>
      <TestConnectionModal
        analyzer={
          formData.ipAddress && effectivePort
            ? {
                id: analyzer?.id || "test",
                ipAddress: formData.ipAddress,
                port: parseInt(effectivePort, 10),
              }
            : null
        }
        open={testConnectionModalOpen}
        onClose={() => setTestConnectionModalOpen(false)}
      />
    </>
  );
};

export default AnalyzerForm;
