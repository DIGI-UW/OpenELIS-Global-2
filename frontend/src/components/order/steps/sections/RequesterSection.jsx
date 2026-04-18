import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  TextInput,
  Select,
  SelectItem,
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
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * RequesterSection - Site and Provider search with selection
 *
 * Implements:
 * - ORD-8: Provider search with inline disambiguation
 * - ORD-8a: Department/Ward/Unit field (disabled until facility selected)
 * - XC-2: Unified search pattern for Site and Provider
 */

const RequesterSection = ({ orderData, setOrderData, isReadOnly }) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Site search state
  const [siteSearchTerm, setSiteSearchTerm] = useState("");
  const [siteResults, setSiteResults] = useState([]);
  const [isSearchingSites, setIsSearchingSites] = useState(false);
  const [selectedSite, setSelectedSite] = useState(null);

  // Provider search state - simplified to just name and phone
  const [providerSearch, setProviderSearch] = useState({
    name: "",
    phone: "",
  });
  const [providerResults, setProviderResults] = useState([]);
  const [isSearchingProviders, setIsSearchingProviders] = useState(false);
  const [selectedProvider, setSelectedProvider] = useState(null);

  // Priority options - must match backend OrderPriority enum
  const priorityOptions = [
    { id: "ROUTINE", value: "Routine" },
    { id: "STAT", value: "STAT (Urgent)" },
    { id: "ASAP", value: "ASAP" },
    { id: "TIMED", value: "Timed" },
  ];

  // Component mounted tracking
  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Initialize from orderData when referringSiteId changes (e.g., from barcode scan)
  useEffect(() => {
    if (orderData?.sampleOrderItems?.referringSiteId && !selectedSite) {
      // Fetch site details
      getFromOpenElisServer(
        `/rest/organization/${orderData.sampleOrderItems.referringSiteId}`,
        (response) => {
          if (componentMounted.current && response) {
            setSelectedSite(response);
          }
        },
      );
    }
  }, [orderData?.sampleOrderItems?.referringSiteId]);

  // Initialize provider from orderData when provider info changes (e.g., from barcode scan)
  useEffect(() => {
    const providerPersonId = orderData?.sampleOrderItems?.providerPersonId;
    const providerFirstName = orderData?.sampleOrderItems?.providerFirstName;
    const providerLastName = orderData?.sampleOrderItems?.providerLastName;

    if (
      (providerPersonId || providerFirstName || providerLastName) &&
      !selectedProvider
    ) {
      setSelectedProvider({
        id: providerPersonId || "",
        firstName: providerFirstName || "",
        lastName: providerLastName || "",
        phone: orderData?.sampleOrderItems?.providerWorkPhone || "",
      });
    }
  }, [
    orderData?.sampleOrderItems?.providerPersonId,
    orderData?.sampleOrderItems?.providerFirstName,
    orderData?.sampleOrderItems?.providerLastName,
  ]);

  // Site search - can be triggered manually or by autocomplete
  const handleSiteSearch = (searchTerm = siteSearchTerm) => {
    const term = searchTerm?.trim?.() || siteSearchTerm.trim();
    if (!term) {
      setSiteResults([]);
      return;
    }

    setIsSearchingSites(true);

    getFromOpenElisServer(
      `/rest/organization/search?search=${encodeURIComponent(term)}`,
      (response) => {
        if (componentMounted.current) {
          setIsSearchingSites(false);
          if (response?.organizations && response.organizations.length > 0) {
            setSiteResults(response.organizations);
          } else {
            setSiteResults([]);
          }
        }
      },
    );
  };

  // Autocomplete effect for site search - debounced
  useEffect(() => {
    if (selectedSite || isReadOnly) return; // Don't search if already selected

    const debounceTimer = setTimeout(() => {
      if (siteSearchTerm.trim().length >= 2) {
        handleSiteSearch(siteSearchTerm);
      } else {
        setSiteResults([]);
      }
    }, 300); // 300ms debounce

    return () => clearTimeout(debounceTimer);
  }, [siteSearchTerm, selectedSite, isReadOnly]);

  // Site selection
  const handleSelectSite = (site) => {
    setSelectedSite(site);
    setSiteResults([]);

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        referringSiteId: site.id,
        referringSiteName: site.organizationName,
        referringSiteCode: site.shortName,
      },
    }));
  };

  // Clear site selection
  const handleClearSite = () => {
    setSelectedSite(null);
    setSiteSearchTerm("");

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        referringSiteId: "",
        referringSiteName: "",
        referringSiteCode: "",
      },
    }));
  };

  // Priority change
  const handlePriorityChange = (e) => {
    const value = e.target.value;
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        priority: value,
      },
    }));
  };

  // Provider search field change
  const handleProviderFieldChange = (field, value) => {
    setProviderSearch((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Provider search - can be triggered manually or by autocomplete
  // Supports searching by name or phone number
  const handleProviderSearch = (searchOverride = null) => {
    const { name, phone } = providerSearch;
    const searchTerm = searchOverride || name || "";

    if (!searchTerm.trim() && !phone.trim()) {
      setProviderResults([]);
      return;
    }

    setIsSearchingProviders(true);

    // Build query params - search by name or phone
    let queryParams = "";
    if (phone.trim()) {
      queryParams = `phone=${encodeURIComponent(phone.trim())}`;
    } else if (searchTerm.trim()) {
      queryParams = `search=${encodeURIComponent(searchTerm.trim())}`;
    }

    getFromOpenElisServer(
      `/rest/provider/search?${queryParams}`,
      (response) => {
        if (componentMounted.current) {
          setIsSearchingProviders(false);
          if (response?.providers) {
            // Filter out providers without valid IDs (required by Carbon DataTable)
            const validProviders = response.providers.filter(
              (p) => p.id && p.id !== "",
            );
            setProviderResults(validProviders);
          } else {
            setProviderResults([]);
          }
        }
      },
    );
  };

  // Autocomplete effect for provider search - debounced
  useEffect(() => {
    if (selectedProvider || isReadOnly) return; // Don't search if already selected

    const { name } = providerSearch;
    const searchTerm = name || "";

    const debounceTimer = setTimeout(() => {
      if (searchTerm.trim().length >= 2) {
        handleProviderSearch(searchTerm);
      } else {
        setProviderResults([]);
      }
    }, 300); // 300ms debounce

    return () => clearTimeout(debounceTimer);
  }, [providerSearch.name, selectedProvider, isReadOnly]);

  // Provider selection - use personId from search results if available, otherwise fetch from practitioner endpoint
  const handleSelectProvider = (provider) => {
    setSelectedProvider(provider);
    setProviderResults([]);

    // If personId is already in the search results (after backend rebuild), use it directly
    if (provider.personId) {
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          providerId: provider.id,
          providerPersonId: provider.personId,
          providerFirstName: provider.firstName,
          providerLastName: provider.lastName,
          providerWorkPhone: provider.phone,
        },
      }));
    } else {
      // Fallback: fetch from practitioner endpoint to get person.id
      getFromOpenElisServer(
        `/rest/practitioner?providerId=${provider.id}`,
        (data) => {
          if (data && data.person) {
            setOrderData((prev) => ({
              ...prev,
              sampleOrderItems: {
                ...prev.sampleOrderItems,
                providerId: data.id,
                providerPersonId: data.person.id,
                providerFirstName: data.person.firstName || provider.firstName,
                providerLastName: data.person.lastName || provider.lastName,
                providerWorkPhone: data.person.workPhone || provider.phone,
              },
            }));
          } else {
            setOrderData((prev) => ({
              ...prev,
              sampleOrderItems: {
                ...prev.sampleOrderItems,
                providerId: provider.id,
                providerFirstName: provider.firstName,
                providerLastName: provider.lastName,
                providerWorkPhone: provider.phone,
              },
            }));
          }
        },
      );
    }
  };

  // Clear provider selection
  const handleClearProvider = () => {
    setSelectedProvider(null);
    setProviderSearch({ lastName: "", firstName: "", phone: "" });

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        providerId: "",
        providerPersonId: "",
        providerFirstName: "",
        providerLastName: "",
        providerWorkPhone: "",
      },
    }));
  };

  // Clear site search
  const handleClearSiteSearch = () => {
    setSiteSearchTerm("");
    setSiteResults([]);
  };

  // Clear provider search
  const handleClearProviderSearch = () => {
    setProviderSearch({ name: "", phone: "" });
    setProviderResults([]);
  };

  // Site table headers
  const siteHeaders = [
    {
      key: "organizationName",
      header: intl.formatMessage({
        id: "site.name",
        defaultMessage: "Site Name",
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

  // Provider table headers - matches Provider Management (Name, Phone, Fax)
  const providerHeaders = [
    {
      key: "name",
      header: intl.formatMessage({
        id: "provider.name",
        defaultMessage: "Name",
      }),
    },
    {
      key: "phone",
      header: intl.formatMessage({
        id: "provider.phone",
        defaultMessage: "Phone",
      }),
    },
    {
      key: "fax",
      header: intl.formatMessage({
        id: "provider.fax",
        defaultMessage: "Fax",
      }),
    },
    { key: "actions", header: "" },
  ];

  return (
    <Tile className="order-section requester-section">
      <h4 className="section-title">
        <FormattedMessage
          id="order.requester"
          defaultMessage="Requester / Ordering Provider"
        />
      </h4>

      {/* Site Search */}
      <div className="subsection">
        <h5 className="subsection-title">
          <FormattedMessage id="site.search" defaultMessage="Site Search" />
        </h5>

        <Grid>
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="siteName"
              labelText={
                <span>
                  <FormattedMessage id="site.name" defaultMessage="Site Name" />
                  <span className="required-indicator"> *</span>
                </span>
              }
              placeholder={intl.formatMessage({
                id: "site.name.placeholder",
                defaultMessage: "Enter site name",
              })}
              value={siteSearchTerm}
              onChange={(e) => setSiteSearchTerm(e.target.value)}
              disabled={isReadOnly || selectedSite}
            />
          </Column>
          <Column lg={5} md={4} sm={4}>
            <Select
              id="priority"
              labelText={intl.formatMessage({
                id: "order.priority",
                defaultMessage: "Priority",
              })}
              value={orderData?.sampleOrderItems?.priority || "ROUTINE"}
              onChange={handlePriorityChange}
              disabled={isReadOnly}
            >
              {priorityOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.value} />
              ))}
            </Select>
          </Column>

          {/* Search Buttons */}
          <Column lg={16} md={8} sm={4}>
            <div className="search-buttons">
              <Button
                kind="primary"
                size="md"
                onClick={handleSiteSearch}
                disabled={isSearchingSites || isReadOnly || selectedSite}
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
                disabled={selectedSite}
              >
                <FormattedMessage
                  id="label.button.clear"
                  defaultMessage="Clear"
                />
              </Button>
            </div>
          </Column>
        </Grid>

        {/* Site Results */}
        {siteResults.length > 0 && !selectedSite && (
          <div className="search-results">
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
                              <TableCell key={cell.id}>{cell.value}</TableCell>
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
          <div className="selected-entity-card">
            <div className="selected-card-header">
              <Tag type="green" size="sm">
                <FormattedMessage id="selected" defaultMessage="Selected" />
              </Tag>
              <Link onClick={handleClearSite}>
                <FormattedMessage
                  id="label.button.clear"
                  defaultMessage="Clear"
                />
              </Link>
            </div>
            <div className="selected-card-content">
              <h5>{selectedSite.organizationName}</h5>
              <p>
                {selectedSite.city && `Location: ${selectedSite.city}`}
                {selectedSite.organizationType &&
                  ` · Type: ${selectedSite.organizationType}`}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Provider Search */}
      <div className="subsection">
        <h5 className="subsection-title">
          <FormattedMessage
            id="provider.search"
            defaultMessage="Provider Search"
          />
        </h5>

        <Grid>
          <Column lg={6} md={4} sm={4}>
            <TextInput
              id="providerName"
              labelText={intl.formatMessage({
                id: "provider.name",
                defaultMessage: "Provider Name",
              })}
              placeholder={intl.formatMessage({
                id: "provider.name.placeholder",
                defaultMessage: "Enter provider name",
              })}
              value={providerSearch.name}
              onChange={(e) =>
                handleProviderFieldChange("name", e.target.value)
              }
              disabled={isReadOnly || selectedProvider}
            />
          </Column>
          <Column lg={6} md={4} sm={4}>
            <TextInput
              id="providerPhone"
              labelText={intl.formatMessage({
                id: "provider.phone",
                defaultMessage: "Provider Phone",
              })}
              placeholder="+1 (555) 000-0000"
              value={providerSearch.phone}
              onChange={(e) =>
                handleProviderFieldChange("phone", e.target.value)
              }
              disabled={isReadOnly || selectedProvider}
            />
          </Column>

          {/* Search Buttons */}
          <Column lg={4} md={8} sm={4}>
            <div className="search-buttons">
              <Button
                kind="primary"
                size="md"
                onClick={() => handleProviderSearch()}
                disabled={
                  isSearchingProviders || isReadOnly || selectedProvider
                }
              >
                <FormattedMessage
                  id="label.button.search"
                  defaultMessage="Search"
                />
              </Button>
              <Button
                kind="ghost"
                size="md"
                onClick={handleClearProviderSearch}
                disabled={selectedProvider}
              >
                <FormattedMessage
                  id="label.button.clear"
                  defaultMessage="Clear"
                />
              </Button>
            </div>
          </Column>
        </Grid>

        {/* Provider Results */}
        {providerResults.length > 0 && !selectedProvider && (
          <div className="search-results">
            <p className="results-count">
              {providerResults.length}{" "}
              <FormattedMessage
                id="results.found.for"
                defaultMessage='results found for "{term}"'
                values={{ term: providerSearch.name || providerSearch.phone }}
              />
            </p>
            <DataTable rows={providerResults} headers={providerHeaders}>
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
                      const provider = providerResults.find(
                        (p) => p.id === row.id,
                      );
                      return (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells.map((cell) => {
                            if (cell.info.header === "actions") {
                              return (
                                <TableCell key={cell.id}>
                                  <Button
                                    kind="primary"
                                    size="sm"
                                    onClick={() =>
                                      handleSelectProvider(provider)
                                    }
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

        {/* Selected Provider Card */}
        {selectedProvider && (
          <div className="selected-entity-card">
            <div className="selected-card-header">
              <Tag type="green" size="sm">
                <FormattedMessage id="selected" defaultMessage="Selected" />
              </Tag>
              <Link onClick={handleClearProvider}>
                <FormattedMessage
                  id="label.button.clear"
                  defaultMessage="Clear"
                />
              </Link>
            </div>
            <div className="selected-card-content">
              <h5>
                {selectedProvider.firstName} {selectedProvider.lastName}
              </h5>
              <p>
                {selectedProvider.phone && `Phone: ${selectedProvider.phone}`}
                {selectedProvider.source &&
                  ` · Source: ${selectedProvider.source}`}
              </p>
            </div>
          </div>
        )}

        <p className="helper-text">
          <FormattedMessage
            id="provider.search.helper"
            defaultMessage="Enter provider name or phone number and press Search."
          />
        </p>
      </div>
    </Tile>
  );
};

export default RequesterSection;
