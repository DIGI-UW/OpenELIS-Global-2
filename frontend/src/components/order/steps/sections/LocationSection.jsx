import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  Select,
  SelectItem,
  TextInput,
  Button,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Link,
  ContentSwitcher,
  Switch,
  InlineLoading,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import AddressSearch from "../../../patient/AddressSearch";

/**
 * LocationSection - Environmental workflow sampling site capture
 *
 * Replaces PatientSearchSection when Sample Category is "Environmental"
 *
 * Two modes:
 * 1. "Search for Site" - search existing organizations by name, select from table
 * 2. "New Site" - register a new sampling site with full form
 *
 * Below both modes: Collection Conditions fields
 *
 * Data is stored in orderData.sampleOrderItems.environmentalFields
 */

const LocationSection = ({ orderData, setOrderData, isReadOnly }) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Tab mode: 0 = Search for Site, 1 = New Site
  const [activeTab, setActiveTab] = useState(0);

  // --- Search for Site state ---
  const [siteSearchTerm, setSiteSearchTerm] = useState("");
  const [siteResults, setSiteResults] = useState([]);
  const [isSearchingSites, setIsSearchingSites] = useState(false);
  const [selectedSite, setSelectedSite] = useState(null);

  // --- New Site form state ---
  const [newSite, setNewSite] = useState({
    siteCode: "",
    siteName: "",
    siteType: "",
    siteSubtype: "",
    gpsLatitude: "",
    gpsLongitude: "",
    environmentalZone: "",
    siteDescription: "",
    streetAddress: "",
    contactPerson: "",
    contactPhone: "",
  });
  const [isSavingSite, setIsSavingSite] = useState(false);

  // Fetch a sequential site code from the backend
  const fetchSiteCode = () => {
    getFromOpenElisServer(
      "/rest/organization/generate-site-code",
      (response) => {
        if (componentMounted.current && response?.siteCode) {
          setNewSite((prev) => ({ ...prev, siteCode: response.siteCode }));
        }
      },
    );
  };

  // --- Dictionary-managed dropdown options (fetched from backend) ---
  const [siteTypeOptions, setSiteTypeOptions] = useState([]);
  const [zoneOptions, setZoneOptions] = useState([]);

  // --- Address hierarchy state (for New Site form) ---
  const [hierarchyLevels, setHierarchyLevels] = useState([]);
  const [hierarchyValues, setHierarchyValues] = useState({});
  const [selectedHierarchyValues, setSelectedHierarchyValues] = useState({});
  const [isLoadingHierarchy, setIsLoadingHierarchy] = useState(true);

  // Environmental fields from orderData
  const environmentalFields =
    orderData?.sampleOrderItems?.environmentalFields || {};

  // Update environmental fields in orderData
  const updateEnvironmentalField = (field, value) => {
    setOrderData((prev) => {
      const currentEnvFields = prev.sampleOrderItems?.environmentalFields || {};
      const updatedEnvFields = { ...currentEnvFields, [field]: value };

      if (
        updatedEnvFields.locationHierarchy &&
        typeof updatedEnvFields.locationHierarchy === "object"
      ) {
        delete updatedEnvFields.locationHierarchy;
      }

      return {
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          environmentalFields: updatedEnvFields,
        },
      };
    });
  };

  // Batch update multiple environmental fields at once
  const updateEnvironmentalFields = (fieldsObj) => {
    setOrderData((prev) => {
      const currentEnvFields = prev.sampleOrderItems?.environmentalFields || {};
      const updatedEnvFields = { ...currentEnvFields, ...fieldsObj };

      if (
        updatedEnvFields.locationHierarchy &&
        typeof updatedEnvFields.locationHierarchy === "object"
      ) {
        delete updatedEnvFields.locationHierarchy;
      }

      return {
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          environmentalFields: updatedEnvFields,
        },
      };
    });
  };

  // Sampling site org type ID (fetched from backend)
  const [samplingSiteTypeId, setSamplingSiteTypeId] = useState(null);

  // Component lifecycle
  useEffect(() => {
    componentMounted.current = true;

    // Fetch organization types to find "sampling site" type ID
    getFromOpenElisServer("/rest/organization/types", (response) => {
      if (componentMounted.current && Array.isArray(response)) {
        const samplingSiteType = response.find(
          (t) => t.name === "sampling site",
        );
        if (samplingSiteType) {
          setSamplingSiteTypeId(samplingSiteType.id);
        }
      }
    });

    // Fetch dictionary-managed dropdown values
    getFromOpenElisServer(
      "/rest/dictionary/category/Sampling Site Type",
      (response) => {
        if (componentMounted.current && Array.isArray(response)) {
          setSiteTypeOptions(response);
        }
      },
    );
    getFromOpenElisServer(
      "/rest/dictionary/category/Environmental Zone",
      (response) => {
        if (componentMounted.current && Array.isArray(response)) {
          setZoneOptions(response);
        }
      },
    );

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Fetch address hierarchy levels on mount
  useEffect(() => {
    setIsLoadingHierarchy(true);
    getFromOpenElisServer("/rest/address-hierarchy/levels", (response) => {
      if (componentMounted.current && response) {
        setHierarchyLevels(response);

        if (response.length > 0) {
          getFromOpenElisServer(
            "/rest/address-hierarchy/level/1",
            (levelData) => {
              if (componentMounted.current && levelData) {
                setHierarchyValues((prev) => ({ ...prev, 0: levelData }));
              }
            },
          );
        }

        // Initialize selected values from saved data
        const savedSelections = {};
        Object.entries(environmentalFields).forEach(([key, value]) => {
          if (key.startsWith("locationHierarchy.") && value) {
            const level = parseInt(key.split(".")[1]);
            const levelIndex = level - 1;
            savedSelections[levelIndex] = value;
          }
        });

        if (Object.keys(savedSelections).length > 0) {
          setSelectedHierarchyValues(savedSelections);
          Object.entries(savedSelections).forEach(
            ([levelIndex, selectedId]) => {
              if (selectedId) {
                const nextLevelIndex = parseInt(levelIndex) + 1;
                if (nextLevelIndex < response.length) {
                  getFromOpenElisServer(
                    `/rest/address-hierarchy/children?parentId=${selectedId}`,
                    (children) => {
                      if (componentMounted.current && children) {
                        setHierarchyValues((prev) => ({
                          ...prev,
                          [nextLevelIndex]: children,
                        }));
                      }
                    },
                  );
                }
              }
            },
          );
        }

        setIsLoadingHierarchy(false);
      }
    });
  }, []);

  // Initialize selected site from saved environmentalFields
  useEffect(() => {
    if (environmentalFields.samplingSiteId && !selectedSite) {
      getFromOpenElisServer(
        `/rest/organization/${environmentalFields.samplingSiteId}`,
        (response) => {
          if (componentMounted.current) {
            if (response && response.organizationName) {
              setSelectedSite(response);
            } else {
              // Fallback: build site card from saved environmental fields
              setSelectedSite({
                id: environmentalFields.samplingSiteId,
                organizationName:
                  environmentalFields.samplingSiteName || "Unknown Site",
                shortName: environmentalFields.samplingSiteCode || "",
                organizationType: environmentalFields.siteType || "",
              });
            }
          }
        },
      );
    } else if (environmentalFields.samplingSiteName && !selectedSite) {
      // No site ID but has a name (manually entered)
      setSelectedSite({
        organizationName: environmentalFields.samplingSiteName,
        shortName: environmentalFields.samplingSiteCode || "",
        organizationType: environmentalFields.siteType || "",
      });
    }
  }, [
    environmentalFields.samplingSiteId,
    environmentalFields.samplingSiteName,
  ]);

  // ==========================================
  // SEARCH FOR SITE
  // ==========================================

  const handleSiteSearch = (searchTerm = siteSearchTerm) => {
    const term = searchTerm?.trim?.() || siteSearchTerm.trim();
    if (!term) {
      setSiteResults([]);
      return;
    }

    setIsSearchingSites(true);
    getFromOpenElisServer(
      `/rest/organization/search?search=${encodeURIComponent(term)}&type=sampling site`,
      (response) => {
        if (componentMounted.current) {
          setIsSearchingSites(false);
          if (response?.organizations && response.organizations.length > 0) {
            setSiteResults(response.organizations);
          } else {
            // Fallback: search without type filter if no sampling sites found
            getFromOpenElisServer(
              `/rest/organization/search?search=${encodeURIComponent(term)}`,
              (fallbackResponse) => {
                if (componentMounted.current) {
                  if (
                    fallbackResponse?.organizations &&
                    fallbackResponse.organizations.length > 0
                  ) {
                    setSiteResults(fallbackResponse.organizations);
                  } else {
                    setSiteResults([]);
                  }
                }
              },
            );
          }
        }
      },
    );
  };

  // Debounced autocomplete for site search
  useEffect(() => {
    if (selectedSite || isReadOnly) return;

    const debounceTimer = setTimeout(() => {
      if (siteSearchTerm.trim().length >= 2) {
        handleSiteSearch(siteSearchTerm);
      } else {
        setSiteResults([]);
      }
    }, 300);

    return () => clearTimeout(debounceTimer);
  }, [siteSearchTerm, selectedSite, isReadOnly]);

  const handleSelectSite = (site) => {
    setSelectedSite(site);
    setSiteResults([]);

    // Populate environmental fields from selected site
    const fields = {
      samplingSiteId: site.id,
      samplingSiteName: site.organizationName,
      samplingSiteCode: site.shortName || site.code || "",
      siteType: site.organizationType || "",
      streetAddress: site.streetAddress || "",
      collectionSiteDescription: site.organizationName,
    };

    // Populate GPS and city if available
    if (site.city) {
      fields.siteCity = site.city;
    }

    updateEnvironmentalFields(fields);
  };

  const handleClearSite = () => {
    setSelectedSite(null);
    setSiteSearchTerm("");
    setSiteResults([]);

    updateEnvironmentalFields({
      samplingSiteId: "",
      samplingSiteName: "",
      samplingSiteCode: "",
      siteType: "",
      streetAddress: "",
      siteCity: "",
      collectionSiteDescription: "",
    });
  };

  const handleClearSiteSearch = () => {
    setSiteSearchTerm("");
    setSiteResults([]);
  };

  // Site search table headers
  const siteHeaders = [
    {
      key: "organizationName",
      header: intl.formatMessage({
        id: "site.name",
        defaultMessage: "Site Name",
      }),
    },
    {
      key: "shortName",
      header: intl.formatMessage({
        id: "site.code",
        defaultMessage: "Code",
      }),
    },
    {
      key: "city",
      header: intl.formatMessage({
        id: "site.location",
        defaultMessage: "Location",
      }),
    },
    {
      key: "organizationType",
      header: intl.formatMessage({ id: "site.type", defaultMessage: "Type" }),
    },
    { key: "actions", header: "" },
  ];

  // ==========================================
  // NEW SITE FORM
  // ==========================================

  const handleNewSiteFieldChange = (field, value) => {
    setNewSite((prev) => ({ ...prev, [field]: value }));
  };

  const handleHierarchySelection = (levelIndex, selectedId) => {
    const newSelected = { ...selectedHierarchyValues };
    newSelected[levelIndex] = selectedId;

    // Clear child levels
    for (let i = levelIndex + 1; i < hierarchyLevels.length; i++) {
      delete newSelected[i];
      setHierarchyValues((prev) => {
        const updated = { ...prev };
        delete updated[i];
        return updated;
      });
    }

    setSelectedHierarchyValues(newSelected);

    // Fetch children for next level
    if (selectedId && levelIndex < hierarchyLevels.length - 1) {
      getFromOpenElisServer(
        `/rest/address-hierarchy/children?parentId=${selectedId}`,
        (children) => {
          if (componentMounted.current && children) {
            setHierarchyValues((prev) => ({
              ...prev,
              [levelIndex + 1]: children,
            }));
          }
        },
      );
    }

    // Save to orderData
    const level = levelIndex + 1;
    updateEnvironmentalField(`locationHierarchy.${level}`, selectedId);

    const levelItems = hierarchyValues[levelIndex] || [];
    const selectedItem = levelItems.find((item) => item.id === selectedId);
    if (selectedItem) {
      updateEnvironmentalField(`locationName.${level}`, selectedItem.value);
    }

    for (let i = levelIndex + 1; i < hierarchyLevels.length; i++) {
      const childLevel = i + 1;
      updateEnvironmentalField(`locationHierarchy.${childLevel}`, "");
      updateEnvironmentalField(`locationName.${childLevel}`, "");
    }
  };

  // Handle address search selection - auto-fill all hierarchy dropdowns
  const handleAddressSearchSelect = (hierarchyLevelsData) => {
    if (!hierarchyLevelsData || hierarchyLevelsData.length === 0) return;

    const newSelected = {};
    hierarchyLevelsData.forEach((levelData) => {
      const levelIndex = levelData.level - 1;
      newSelected[levelIndex] = levelData.id;

      updateEnvironmentalField(
        `locationHierarchy.${levelData.level}`,
        levelData.id,
      );
      updateEnvironmentalField(
        `locationName.${levelData.level}`,
        levelData.name,
      );
    });

    setSelectedHierarchyValues(newSelected);

    // Fetch child options for each level to populate the dropdowns
    const fetchChildrenForLevels = (levelIndex) => {
      if (levelIndex >= hierarchyLevelsData.length) return;

      const levelData = hierarchyLevelsData[levelIndex];
      const nextLevelIndex = levelIndex + 1;

      if (nextLevelIndex < hierarchyLevels.length) {
        getFromOpenElisServer(
          `/rest/address-hierarchy/children?parentId=${levelData.id}`,
          (children) => {
            if (componentMounted.current && children) {
              setHierarchyValues((prev) => ({
                ...prev,
                [nextLevelIndex]: children,
              }));
              fetchChildrenForLevels(nextLevelIndex);
            }
          },
        );
      }
    };

    fetchChildrenForLevels(0);
  };

  const handleSaveAndSelectSite = () => {
    if (!newSite.siteName.trim()) return;

    setIsSavingSite(true);

    // Build the city from deepest selected hierarchy level name
    let city = "";
    for (let i = hierarchyLevels.length - 1; i >= 0; i--) {
      const selectedId = selectedHierarchyValues[i];
      if (selectedId) {
        const items = hierarchyValues[i] || [];
        const item = items.find((v) => v.id === selectedId);
        if (item) {
          city = item.value;
          break;
        }
      }
    }

    // Build OrganizationForm-compatible payload
    // short_name max 15 chars, local_abbrev max 10 chars
    const code = newSite.siteCode || newSite.siteName.substring(0, 15);
    const orgFormData = {
      id: "",
      organizationName: newSite.siteName,
      shortName: code.substring(0, 15),
      organizationLocalAbbreviation: "",
      streetAddress: (newSite.streetAddress || "").substring(0, 30),
      city: (city || "").substring(0, 30),
      isActive: "Y",
      selectedTypes: samplingSiteTypeId ? [samplingSiteTypeId] : [],
      department: selectedHierarchyValues[0] || "",
      commune: selectedHierarchyValues[1] || "",
      village: selectedHierarchyValues[2] || "",
    };

    postToOpenElisServerJsonResponse(
      "/rest/Organization",
      JSON.stringify(orgFormData),
      (response) => {
        if (componentMounted.current) {
          setIsSavingSite(false);

          const newId = response?.id;

          if (newId) {
            const createdSite = {
              id: newId,
              organizationName: newSite.siteName,
              shortName: newSite.siteCode,
              city: city,
              streetAddress: newSite.streetAddress,
              organizationType: newSite.siteType,
            };

            setSelectedSite(createdSite);
            setActiveTab(0);

            updateEnvironmentalFields({
              samplingSiteId: newId,
              samplingSiteName: newSite.siteName,
              samplingSiteCode: newSite.siteCode,
              siteType: newSite.siteType,
              siteSubtype: newSite.siteSubtype,
              gpsLatitude: newSite.gpsLatitude,
              gpsLongitude: newSite.gpsLongitude,
              environmentalZone: newSite.environmentalZone,
              collectionSiteDescription: newSite.siteDescription,
              streetAddress: newSite.streetAddress,
              contactPerson: newSite.contactPerson,
              contactPhone: newSite.contactPhone,
            });
          } else {
            console.error("Failed to create organization:", response);
          }
        }
      },
    );
  };

  const handleCancelNewSite = () => {
    setNewSite({
      siteCode: "",
      siteName: "",
      siteType: "",
      siteSubtype: "",
      gpsLatitude: "",
      gpsLongitude: "",
      environmentalZone: "",
      siteDescription: "",
      streetAddress: "",
      contactPerson: "",
      contactPhone: "",
    });
    setActiveTab(0);
  };

  // (Site Type and Environmental Zone options are fetched from backend into siteTypeOptions / zoneOptions state)

  return (
    <Tile className="order-section location-section">
      <h4 className="section-title">
        <FormattedMessage
          id="env.samplingSite.title"
          defaultMessage="Subject — Sampling Site"
        />
      </h4>

      {/* Tab switcher: Search for Site / New Site */}
      <ContentSwitcher
        onChange={(e) => {
          setActiveTab(e.index);
          if (e.index === 1 && !newSite.siteCode) {
            fetchSiteCode();
          }
        }}
        selectedIndex={activeTab}
        size="md"
        style={{ marginBottom: "1rem", maxWidth: "300px" }}
      >
        <Switch
          name="search"
          text={intl.formatMessage({
            id: "env.site.searchTab",
            defaultMessage: "Search for Site",
          })}
        />
        <Switch
          name="new"
          text={intl.formatMessage({
            id: "env.site.newTab",
            defaultMessage: "New Site",
          })}
        />
      </ContentSwitcher>

      <p className="helper-text" style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="env.site.helperText"
          defaultMessage="Register a new sampling site. The site will be saved to the Site Registry and can be reused for future orders."
        />
      </p>

      {/* ==========================================
          TAB 0: SEARCH FOR SITE
          ========================================== */}
      {activeTab === 0 && (
        <>
          {/* Search input */}
          <Grid>
            <Column lg={6} md={4} sm={4}>
              <TextInput
                id="samplingSiteSearch"
                labelText={intl.formatMessage({
                  id: "env.site.searchLabel",
                  defaultMessage: "Site Name",
                })}
                placeholder={intl.formatMessage({
                  id: "env.site.searchPlaceholder",
                  defaultMessage: "e.g., Sungai Ciliwung - Manggarai",
                })}
                value={siteSearchTerm}
                onChange={(e) => setSiteSearchTerm(e.target.value)}
                disabled={isReadOnly || !!selectedSite}
              />
            </Column>
            <Column lg={10} md={4} sm={4}>
              <div
                className="search-buttons"
                style={{
                  display: "flex",
                  gap: "0.5rem",
                  alignItems: "flex-end",
                  height: "100%",
                  paddingBottom: "0.25rem",
                }}
              >
                <Button
                  kind="primary"
                  size="md"
                  onClick={() => handleSiteSearch()}
                  disabled={isSearchingSites || isReadOnly || !!selectedSite}
                >
                  <FormattedMessage
                    id="label.button.search"
                    defaultMessage="Search"
                  />
                </Button>
                <Button
                  kind="ghost"
                  size="md"
                  onClick={handleClearSiteSearch}
                  disabled={!!selectedSite}
                >
                  <FormattedMessage
                    id="label.button.clear"
                    defaultMessage="Clear"
                  />
                </Button>
              </div>
            </Column>
          </Grid>

          {/* Loading indicator */}
          {isSearchingSites && (
            <InlineLoading
              description={intl.formatMessage({
                id: "loading",
                defaultMessage: "Loading...",
              })}
              style={{ marginTop: "0.5rem" }}
            />
          )}

          {/* Search Results Table */}
          {siteResults.length > 0 && !selectedSite && (
            <div className="search-results" style={{ marginTop: "1rem" }}>
              <p className="results-count">
                {siteResults.length}{" "}
                <FormattedMessage
                  id="results.found.for"
                  defaultMessage='results found for "{term}"'
                  values={{ term: siteSearchTerm }}
                />
              </p>
              <DataTable rows={siteResults} headers={siteHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <Table {...getTableProps()} size="sm">
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const site = siteResults.find((s) => s.id === row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            {row.cells.map((cell) => {
                              if (cell.info.header === "actions") {
                                return (
                                  <TableCell key={cell.id}>
                                    <Button
                                      kind="primary"
                                      size="sm"
                                      onClick={() => handleSelectSite(site)}
                                    >
                                      <FormattedMessage
                                        id="label.button.select"
                                        defaultMessage="Select"
                                      />
                                    </Button>
                                  </TableCell>
                                );
                              }
                              return (
                                <TableCell key={cell.id}>
                                  {cell.value || "-"}
                                </TableCell>
                              );
                            })}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                )}
              </DataTable>
            </div>
          )}

          {/* Selected Site Card */}
          {selectedSite && (
            <div className="selected-entity-card" style={{ marginTop: "1rem" }}>
              <div className="selected-card-header">
                <Tag type="green" size="sm">
                  <FormattedMessage id="selected" defaultMessage="Selected" />
                </Tag>
                {!isReadOnly && (
                  <Link onClick={handleClearSite}>
                    <FormattedMessage
                      id="label.button.clear"
                      defaultMessage="Clear"
                    />
                  </Link>
                )}
              </div>
              <div className="selected-card-content">
                <h5>{selectedSite.organizationName}</h5>
                <p>
                  {selectedSite.shortName && `Code: ${selectedSite.shortName}`}
                  {selectedSite.city && ` · Location: ${selectedSite.city}`}
                  {selectedSite.organizationType &&
                    ` · Type: ${selectedSite.organizationType}`}
                </p>
                {selectedSite.streetAddress && (
                  <p>
                    <FormattedMessage
                      id="env.site.address"
                      defaultMessage="Address"
                    />
                    : {selectedSite.streetAddress}
                  </p>
                )}
              </div>
            </div>
          )}
        </>
      )}

      {/* ==========================================
          TAB 1: NEW SITE FORM
          ========================================== */}
      {activeTab === 1 && (
        <>
          {/* Row 1: Site Code + Site Name */}
          <Grid>
            <Column lg={6} md={4} sm={4}>
              <TextInput
                id="newSiteCode"
                labelText={intl.formatMessage({
                  id: "env.site.code",
                  defaultMessage: "Site Code",
                })}
                placeholder={intl.formatMessage({
                  id: "env.site.code.placeholder",
                  defaultMessage: "Auto-generated (or enter manually)",
                })}
                value={newSite.siteCode}
                onChange={(e) =>
                  handleNewSiteFieldChange("siteCode", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>
            <Column lg={10} md={4} sm={4}>
              <TextInput
                id="newSiteName"
                labelText={
                  <span>
                    <FormattedMessage
                      id="env.site.name"
                      defaultMessage="Site Name"
                    />
                    <span className="required-indicator"> *</span>
                  </span>
                }
                placeholder={intl.formatMessage({
                  id: "env.site.name.placeholder",
                  defaultMessage: "e.g., Sungai Ciliwung — Manggarai",
                })}
                value={newSite.siteName}
                onChange={(e) =>
                  handleNewSiteFieldChange("siteName", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>
          </Grid>

          {/* Row 2: Site Type + Subtype */}
          <Grid>
            <Column lg={6} md={4} sm={4}>
              <Select
                id="newSiteType"
                labelText={
                  <span>
                    <FormattedMessage
                      id="env.site.type"
                      defaultMessage="Site Type"
                    />
                    <span className="required-indicator"> *</span>
                  </span>
                }
                value={newSite.siteType}
                onChange={(e) =>
                  handleNewSiteFieldChange("siteType", e.target.value)
                }
                disabled={isReadOnly}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "env.site.type.select",
                    defaultMessage: "Select Type...",
                  })}
                />
                {siteTypeOptions.map((opt) => (
                  <SelectItem key={opt.id} value={opt.id} text={opt.value} />
                ))}
              </Select>
            </Column>
            <Column lg={10} md={4} sm={4}>
              <TextInput
                id="newSiteSubtype"
                labelText={intl.formatMessage({
                  id: "env.site.subtype",
                  defaultMessage: "Subtype",
                })}
                placeholder={intl.formatMessage({
                  id: "env.site.subtype.placeholder",
                  defaultMessage:
                    "e.g., River, Well, BG-Sentinel, Fixed Station",
                })}
                value={newSite.siteSubtype}
                onChange={(e) =>
                  handleNewSiteFieldChange("siteSubtype", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>
          </Grid>

          {/* Row 3: Address Search + Address Hierarchy */}
          {hierarchyLevels.length > 0 && (
            <Grid style={{ marginBottom: "0.5rem" }}>
              <Column lg={16} md={8} sm={4}>
                <AddressSearch
                  onAddressSelect={handleAddressSearchSelect}
                  addressHierarchyLevels={hierarchyLevels}
                  placeholder={intl.formatMessage({
                    id: "env.site.addressSearch.placeholder",
                    defaultMessage:
                      "Search for location to auto-fill dropdowns below...",
                  })}
                  disabled={isReadOnly}
                />
              </Column>
            </Grid>
          )}
          {isLoadingHierarchy ? (
            <InlineLoading
              description={intl.formatMessage({
                id: "loading",
                defaultMessage: "Loading...",
              })}
            />
          ) : (
            hierarchyLevels.length > 0 && (
              <Grid>
                {hierarchyLevels.map((level, levelIndex) => {
                  const values = hierarchyValues[levelIndex] || [];
                  const selectedValue =
                    selectedHierarchyValues[levelIndex] || "";
                  const isDisabled =
                    isReadOnly ||
                    (levelIndex > 0 &&
                      !selectedHierarchyValues[levelIndex - 1]) ||
                    values.length === 0;

                  return (
                    <Column key={level.level} lg={5} md={4} sm={4}>
                      <Select
                        id={`new-site-level-${levelIndex}`}
                        labelText={level.typeName}
                        value={selectedValue}
                        onChange={(e) =>
                          handleHierarchySelection(levelIndex, e.target.value)
                        }
                        disabled={isDisabled}
                      >
                        <SelectItem
                          value=""
                          text={intl.formatMessage(
                            {
                              id: "location.select.placeholder",
                              defaultMessage: "Select {levelName}...",
                            },
                            { levelName: level.typeName },
                          )}
                        />
                        {values.map((item) => (
                          <SelectItem
                            key={item.id}
                            value={item.id}
                            text={item.value}
                          />
                        ))}
                      </Select>
                    </Column>
                  );
                })}
              </Grid>
            )
          )}

          {/* Row 4: GPS + Environmental Zone */}
          <Grid>
            <Column lg={4} md={2} sm={2}>
              <TextInput
                id="newSiteGpsLat"
                labelText={intl.formatMessage({
                  id: "location.gps.latitude",
                  defaultMessage: "GPS Latitude",
                })}
                placeholder="-6.2088"
                value={newSite.gpsLatitude}
                onChange={(e) =>
                  handleNewSiteFieldChange("gpsLatitude", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>
            <Column lg={4} md={2} sm={2}>
              <TextInput
                id="newSiteGpsLon"
                labelText={intl.formatMessage({
                  id: "location.gps.longitude",
                  defaultMessage: "GPS Longitude",
                })}
                placeholder="106.8456"
                value={newSite.gpsLongitude}
                onChange={(e) =>
                  handleNewSiteFieldChange("gpsLongitude", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>
            <Column lg={4} md={2} sm={2}>
              <Select
                id="newSiteZone"
                labelText={intl.formatMessage({
                  id: "env.site.zone",
                  defaultMessage: "Environmental Zone",
                })}
                value={newSite.environmentalZone}
                onChange={(e) =>
                  handleNewSiteFieldChange("environmentalZone", e.target.value)
                }
                disabled={isReadOnly}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "env.site.zone.select",
                    defaultMessage: "Select Zone...",
                  })}
                />
                {zoneOptions.map((opt) => (
                  <SelectItem key={opt.id} value={opt.id} text={opt.value} />
                ))}
              </Select>
            </Column>
          </Grid>

          {/* Row 5: Site Description + Address */}
          <Grid>
            <Column lg={6} md={4} sm={4}>
              <TextInput
                id="newSiteDescription"
                labelText={intl.formatMessage({
                  id: "env.site.description",
                  defaultMessage: "Site Description",
                })}
                placeholder={intl.formatMessage({
                  id: "env.site.description.placeholder",
                  defaultMessage:
                    "e.g., Downstream monitoring point near floodgate",
                })}
                value={newSite.siteDescription}
                onChange={(e) =>
                  handleNewSiteFieldChange("siteDescription", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>
            <Column lg={10} md={4} sm={4}>
              <TextInput
                id="newSiteAddress"
                labelText={intl.formatMessage({
                  id: "env.site.streetAddress",
                  defaultMessage: "Address",
                })}
                placeholder={intl.formatMessage({
                  id: "env.site.streetAddress.placeholder",
                  defaultMessage: "Street address or location reference",
                })}
                value={newSite.streetAddress}
                onChange={(e) =>
                  handleNewSiteFieldChange("streetAddress", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>
          </Grid>

          {/* Row 6: Contact Person + Contact Phone */}
          <Grid>
            <Column lg={6} md={4} sm={4}>
              <TextInput
                id="newSiteContact"
                labelText={intl.formatMessage({
                  id: "env.site.contactPerson",
                  defaultMessage: "Contact Person",
                })}
                placeholder={intl.formatMessage({
                  id: "env.site.contactPerson.placeholder",
                  defaultMessage: "Name of site custodian or contact",
                })}
                value={newSite.contactPerson}
                onChange={(e) =>
                  handleNewSiteFieldChange("contactPerson", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>
            <Column lg={10} md={4} sm={4}>
              <TextInput
                id="newSitePhone"
                labelText={intl.formatMessage({
                  id: "env.site.contactPhone",
                  defaultMessage: "Contact Phone",
                })}
                placeholder="+62 xxx xxxx xxxx"
                value={newSite.contactPhone}
                onChange={(e) =>
                  handleNewSiteFieldChange("contactPhone", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>
          </Grid>

          {/* Save & Cancel buttons */}
          <div
            style={{
              display: "flex",
              gap: "0.5rem",
              marginTop: "1rem",
            }}
          >
            <Button
              kind="primary"
              size="md"
              onClick={handleSaveAndSelectSite}
              disabled={isReadOnly || !newSite.siteName.trim() || isSavingSite}
            >
              {isSavingSite ? (
                <InlineLoading
                  description={intl.formatMessage({
                    id: "label.button.saving",
                    defaultMessage: "Saving...",
                  })}
                />
              ) : (
                <FormattedMessage
                  id="env.site.saveAndSelect"
                  defaultMessage="Save & Select Site"
                />
              )}
            </Button>
            <Button
              kind="secondary"
              size="md"
              onClick={handleCancelNewSite}
              disabled={isReadOnly || isSavingSite}
            >
              <FormattedMessage
                id="label.button.cancel"
                defaultMessage="Cancel"
              />
            </Button>
          </div>
        </>
      )}

      {/* ==========================================
          COLLECTION CONDITIONS (always visible)
          ========================================== */}
      <div
        style={{
          marginTop: "2rem",
          borderTop: "1px solid #e0e0e0",
          paddingTop: "1rem",
        }}
      >
        <h5 className="subsection-title">
          <FormattedMessage
            id="env.collectionConditions.title"
            defaultMessage="Collection Conditions"
          />
        </h5>
        <Grid>
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="environmentalConditions"
              labelText={intl.formatMessage({
                id: "env.conditions",
                defaultMessage: "Environmental Conditions",
              })}
              placeholder={intl.formatMessage({
                id: "env.conditions.placeholder",
                defaultMessage: "e.g., 20°C, clear weather, dry season",
              })}
              value={environmentalFields.environmentalConditions || ""}
              onChange={(e) =>
                updateEnvironmentalField(
                  "environmentalConditions",
                  e.target.value,
                )
              }
              disabled={isReadOnly}
            />
          </Column>
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="regulatoryReference"
              labelText={intl.formatMessage({
                id: "env.regulatoryReference",
                defaultMessage: "Regulatory Reference",
              })}
              placeholder={intl.formatMessage({
                id: "env.regulatoryReference.placeholder",
                defaultMessage: "e.g., PP No. 22/2021 Batu Mutu Air",
              })}
              value={environmentalFields.regulatoryReference || ""}
              onChange={(e) =>
                updateEnvironmentalField("regulatoryReference", e.target.value)
              }
              disabled={isReadOnly}
            />
          </Column>
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="collectionMethod"
              labelText={intl.formatMessage({
                id: "env.collectionMethod",
                defaultMessage: "Collection Method",
              })}
              placeholder={intl.formatMessage({
                id: "env.collectionMethod.placeholder",
                defaultMessage: "e.g., Grab sample, Composite 24h",
              })}
              value={environmentalFields.collectionMethod || ""}
              onChange={(e) =>
                updateEnvironmentalField("collectionMethod", e.target.value)
              }
              disabled={isReadOnly}
            />
          </Column>
        </Grid>
        <p className="helper-text" style={{ marginTop: "0.5rem" }}>
          <FormattedMessage
            id="env.collectionConditions.helper"
            defaultMessage="Search for an existing sampling site or create a new one. Sites are reusable across orders — like patients in clinical workflows."
          />
        </p>
      </div>
    </Tile>
  );
};

export default LocationSection;
