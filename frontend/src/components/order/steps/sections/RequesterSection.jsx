import React, { useState, useEffect, useRef, useContext } from "react";
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
import { ConfigurationContext } from "../../../layout/Layout";

/**
 * RequesterSection - Site/Requesting-Organization, Requestor contact, and
 * Provider search with selection.
 *
 * Implements:
 * - ORD-8: Provider search with inline disambiguation
 * - ORD-8a: Department/Ward/Unit field (disabled until facility selected)
 * - XC-2: Unified search pattern for Site, Requestor, and Provider
 * - Requester element revamp — Env/Vector use the same
 *   Requesting-Organization + Requestor-contact element (Vector no longer
 *   hides the organization search); Provider (Clinical) surfaces Fax/Email.
 */

const RequesterSection = ({
  orderData,
  setOrderData,
  isReadOnly,
  workflowType,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const isEnvOrVector =
    workflowType === "environmental" || workflowType === "vector";

  // Admin-gated "+ Add new" affordances. Each restrict flag
  // defaults to "false" (unrestricted) server-side, so a missing/loading
  // config context must NOT accidentally restrict everyone — compare
  // strictly against the "true" string rather than treating undefined as
  // restrictive.
  const { configurationProperties } = useContext(ConfigurationContext) || {};
  const isOrganizationAddNewRestricted =
    configurationProperties?.restrictFreeTextRefSiteEntry === "true";
  const isRequestorAddNewRestricted =
    configurationProperties?.restrictFreeTextRequestorEntry === "true";
  const isProviderAddNewRestricted =
    configurationProperties?.restrictFreeTextProviderEntry === "true";

  // Site/Requesting Organization search state
  const [siteSearchTerm, setSiteSearchTerm] = useState("");
  const [siteResults, setSiteResults] = useState([]);
  const [isSearchingSites, setIsSearchingSites] = useState(false);
  const [selectedSite, setSelectedSite] = useState(null);
  const [departmentResponse, setDepartmentResponse] = useState({
    siteId: "",
    options: [],
  });
  // Edit-lock. A record pulled in from search/backend load
  // (an existing registry entry) starts locked/read-only; "Edit details"
  // unlocks it in place. A brand-new record (via "+ Add new X") starts
  // unlocked, since there's nothing pre-existing to protect from an
  // accidental overwrite.
  const [isSiteLocked, setIsSiteLocked] = useState(false);

  // Requestor contact search state (Environmental/Vector only)
  const [requestorSearchTerm, setRequestorSearchTerm] = useState("");
  const [requestorResults, setRequestorResults] = useState([]);
  const [isSearchingRequestors, setIsSearchingRequestors] = useState(false);
  const [selectedRequestor, setSelectedRequestor] = useState(null);
  const [isRequestorLocked, setIsRequestorLocked] = useState(false);

  // Provider search state - simplified to just name and phone
  const [providerSearch, setProviderSearch] = useState({
    name: "",
    phone: "",
  });
  const [providerResults, setProviderResults] = useState([]);
  const [isSearchingProviders, setIsSearchingProviders] = useState(false);
  const [selectedProvider, setSelectedProvider] = useState(null);
  const [isProviderLocked, setIsProviderLocked] = useState(false);
  const sampleOrderItems = orderData?.sampleOrderItems || {};
  const referringSiteId = sampleOrderItems.referringSiteId || "";
  const effectiveSelectedSite =
    selectedSite?.isNew ||
    (selectedSite &&
      (!referringSiteId || String(selectedSite.id) === String(referringSiteId)))
      ? selectedSite
      : null;
  const departmentOptions =
    departmentResponse.siteId === String(referringSiteId)
      ? departmentResponse.options
      : [];
  const isLoadingDepartments =
    Boolean(referringSiteId) &&
    departmentResponse.siteId !== String(referringSiteId);
  const providerPersonId =
    sampleOrderItems.providerPersonId || sampleOrderItems.providerId || "";
  const savedProvider =
    providerPersonId ||
    sampleOrderItems.providerFirstName ||
    sampleOrderItems.providerLastName
      ? {
          id: providerPersonId,
          firstName: sampleOrderItems.providerFirstName || "",
          lastName: sampleOrderItems.providerLastName || "",
          phone: sampleOrderItems.providerWorkPhone || "",
          fax: sampleOrderItems.providerFax || "",
          email: sampleOrderItems.providerEmail || "",
        }
      : null;
  const effectiveSelectedProvider =
    selectedProvider?.isNew ||
    (selectedProvider &&
      (!providerPersonId ||
        String(selectedProvider.id) === String(providerPersonId)))
      ? selectedProvider
      : savedProvider;

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
    if (!referringSiteId) {
      return undefined;
    }
    if (effectiveSelectedSite) {
      return undefined;
    }

    let active = true;
    getFromOpenElisServer(
      `/rest/organization/${referringSiteId}`,
      (response) => {
        if (active && componentMounted.current && response) {
          setSelectedSite(response);
          setIsSiteLocked(true);
        }
      },
    );
    return () => {
      active = false;
    };
  }, [effectiveSelectedSite, referringSiteId, selectedSite?.isNew]);

  useEffect(() => {
    if (!referringSiteId) {
      setDepartmentResponse({ siteId: "", options: [] });
      return undefined;
    }

    let active = true;
    getFromOpenElisServer(
      `/rest/departments-for-site?refferingSiteId=${encodeURIComponent(referringSiteId)}`,
      (response) => {
        if (active && componentMounted.current) {
          setDepartmentResponse({
            siteId: String(referringSiteId),
            options: Array.isArray(response) ? response : [],
          });
        }
      },
    );
    return () => {
      active = false;
    };
  }, [referringSiteId]);

  // Initialize provider from orderData when a providerPersonId arrives
  // (e.g., from barcode scan, or loading an existing order for edit). Gated
  // on providerPersonId ONLY, not first/last name — those are also driven
  // directly by the editable Provider First/Last Name fields below, and
  // reacting to them here would lock the fields as "selected" after the
  // very first keystroke (same bug as the earlier Requestor fix).
  useEffect(() => {
    if (
      providerPersonId &&
      (!selectedProvider ||
        String(selectedProvider.id) !== String(providerPersonId))
    ) {
      setSelectedProvider({
        id: providerPersonId,
        firstName: orderData?.sampleOrderItems?.providerFirstName || "",
        lastName: orderData?.sampleOrderItems?.providerLastName || "",
        phone: orderData?.sampleOrderItems?.providerWorkPhone || "",
        fax: orderData?.sampleOrderItems?.providerFax || "",
        email: orderData?.sampleOrderItems?.providerEmail || "",
      });
      setIsProviderLocked(true);
    }
  }, [providerPersonId, selectedProvider]);

  // Initialize Requestor from orderData when a requestorPersonId arrives
  // (e.g., from barcode scan, or loading an existing order for edit). Gated
  // on requestorPersonId ONLY, not first/last name — those are also driven
  // directly by the free-text "new Requestor" inputs below, and reacting to
  // them here would lock the field as "selected" after the very first
  // keystroke (bug: typing into First Name auto-"selected" the letter typed).
  useEffect(() => {
    const requestorPersonId = orderData?.sampleOrderItems?.requestorPersonId;

    if (requestorPersonId && !selectedRequestor) {
      setSelectedRequestor({
        id: requestorPersonId,
        firstName: orderData?.sampleOrderItems?.requestorFirstName || "",
        lastName: orderData?.sampleOrderItems?.requestorLastName || "",
        phone: orderData?.sampleOrderItems?.requestorPhone || "",
        fax: orderData?.sampleOrderItems?.requestorFax || "",
        email: orderData?.sampleOrderItems?.requestorEmail || "",
        department: orderData?.sampleOrderItems?.requestorDepartment || "",
      });
      setIsRequestorLocked(true);
    }
  }, [orderData?.sampleOrderItems?.requestorPersonId]);

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
    if (effectiveSelectedSite || isReadOnly) return;

    const debounceTimer = setTimeout(() => {
      if (siteSearchTerm.trim().length >= 2) {
        handleSiteSearch(siteSearchTerm);
      } else {
        setSiteResults([]);
      }
    }, 300); // 300ms debounce

    return () => clearTimeout(debounceTimer);
  }, [siteSearchTerm, effectiveSelectedSite, isReadOnly]);

  // Site selection
  const handleSelectSite = (site) => {
    setSelectedSite(site);
    setIsSiteLocked(true);
    setSiteResults([]);

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        referringSiteId: site.id,
        referringSiteName: site.organizationName,
        referringSiteCode: site.shortName,
        referringSitePhone: site.phone || "",
        referringSiteFax: site.fax || "",
        referringSiteEmail: site.email || "",
        referringSiteDepartmentId: "",
        referringSiteDepartmentName: "",
      },
    }));
  };

  // Clear site selection
  const handleClearSite = () => {
    setSelectedSite(null);
    setIsSiteLocked(false);
    setSiteSearchTerm("");

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        referringSiteId: "",
        referringSiteName: "",
        referringSiteCode: "",
        referringSitePhone: "",
        referringSiteFax: "",
        referringSiteEmail: "",
        referringSiteDepartmentId: "",
        referringSiteDepartmentName: "",
        newRequesterName: "",
      },
    }));
  };

  // "+ Add new organization" — no referringSiteId (doesn't exist yet); the
  // backend (SamplePatientUpdateData.initSampleRequester) creates a new
  // Organization row from newRequesterName on save.
  const handleUseAsNewOrganization = (name) => {
    const trimmedName = name.trim();
    if (!trimmedName) {
      return;
    }
    setSelectedSite({ organizationName: trimmedName, isNew: true });
    setSiteResults([]);

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        referringSiteId: "",
        referringSiteName: "",
        referringSiteDepartmentId: "",
        referringSiteDepartmentName: "",
        newRequesterName: trimmedName,
      },
    }));
  };

  // Unlock an existing (search-selected/loaded) org's
  // contact info for editing in place, instead of clearing and re-searching.
  const handleUnlockSite = () => {
    setIsSiteLocked(false);
  };

  // Requesting Organization contact-info field change (phone/fax/email) —
  // editable whether the org was searched-and-selected or newly added.
  const handleSiteContactFieldChange = (field, value) => {
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        [field]: value,
      },
    }));
  };

  const handleDepartmentChange = (event) => {
    const departmentId = event.target.value;
    const department = departmentOptions.find(
      (option) => String(option.id) === String(departmentId),
    );
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        referringSiteDepartmentId: departmentId,
        referringSiteDepartmentName: department?.value || "",
      },
    }));
  };

  // Requestor contact search - can be triggered manually or by autocomplete
  const handleRequestorSearch = (searchTerm = requestorSearchTerm) => {
    const term = searchTerm?.trim?.() || requestorSearchTerm.trim();
    if (!term) {
      setRequestorResults([]);
      return;
    }

    setIsSearchingRequestors(true);

    getFromOpenElisServer(
      `/rest/requestor/search?search=${encodeURIComponent(term)}`,
      (response) => {
        if (componentMounted.current) {
          setIsSearchingRequestors(false);
          if (response?.requestors && response.requestors.length > 0) {
            setRequestorResults(response.requestors);
          } else {
            setRequestorResults([]);
          }
        }
      },
    );
  };

  // Autocomplete effect for requestor search - debounced
  useEffect(() => {
    if (selectedRequestor || isReadOnly) return;

    const debounceTimer = setTimeout(() => {
      if (requestorSearchTerm.trim().length >= 2) {
        handleRequestorSearch(requestorSearchTerm);
      } else {
        setRequestorResults([]);
      }
    }, 300);

    return () => clearTimeout(debounceTimer);
  }, [requestorSearchTerm, selectedRequestor, isReadOnly]);

  // Requestor selection
  const handleSelectRequestor = (requestor) => {
    setSelectedRequestor(requestor);
    setIsRequestorLocked(true);
    setRequestorResults([]);

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        requestorPersonId: requestor.personId || requestor.id,
        requestorFirstName: requestor.firstName,
        requestorLastName: requestor.lastName,
        requestorPhone: requestor.phone || "",
        requestorFax: requestor.fax || "",
        requestorEmail: requestor.email || "",
        requestorDepartment: requestor.department || "",
      },
    }));
  };

  // Clear requestor selection
  const handleClearRequestor = () => {
    setSelectedRequestor(null);
    setIsRequestorLocked(false);
    setRequestorSearchTerm("");

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        requestorPersonId: "",
        requestorFirstName: "",
        requestorLastName: "",
        requestorPhone: "",
        requestorFax: "",
        requestorEmail: "",
        requestorDepartment: "",
      },
    }));
  };

  // Unlock an existing (search-selected/loaded) Requestor's
  // contact info for editing in place, instead of clearing and re-searching.
  const handleUnlockRequestor = () => {
    setIsRequestorLocked(false);
  };

  // "+ Add new requestor" — mirrors Organization/Provider: no
  // requestorPersonId (doesn't exist yet); the backend
  // (SamplePatientUpdateData.initRequestorContact) creates a new Person +
  // requestor_contact link from requestorFirstName/LastName/etc. on save.
  // Splits the typed search text on the last space, same as
  // handleUseAsNewProvider, since the search box is a single free-text field
  // but first/last name are needed separately; the name fields remain
  // editable afterward so a bad split can be corrected by hand.
  const handleUseAsNewRequestor = (name) => {
    const trimmedName = name.trim();
    if (!trimmedName) {
      return;
    }
    const lastSpaceIndex = trimmedName.lastIndexOf(" ");
    const firstName =
      lastSpaceIndex === -1
        ? trimmedName
        : trimmedName.slice(0, lastSpaceIndex);
    const lastName =
      lastSpaceIndex === -1 ? "" : trimmedName.slice(lastSpaceIndex + 1);

    setSelectedRequestor({ firstName, lastName, isNew: true });
    setRequestorResults([]);

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        requestorPersonId: "",
        requestorFirstName: firstName,
        requestorLastName: lastName,
      },
    }));
  };

  // Clear requestor search
  const handleClearRequestorSearch = () => {
    setRequestorSearchTerm("");
    setRequestorResults([]);
  };

  // Requestor contact field change (used when adding a new Requestor —
  // typed directly into requestorSearchTerm-adjacent free-text fields)
  const handleRequestorFieldChange = (field, value) => {
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        [field]: value,
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
    if (effectiveSelectedProvider || isReadOnly) return;

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
  }, [providerSearch.name, effectiveSelectedProvider, isReadOnly]);

  // Provider selection - use personId from search results if available, otherwise fetch from practitioner endpoint
  const handleSelectProvider = (provider) => {
    setSelectedProvider(provider);
    setIsProviderLocked(true);
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
          providerFax: provider.fax || "",
          providerEmail: provider.email || "",
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
                providerFax: data.person.fax || provider.fax || "",
                providerEmail: data.person.email || provider.email || "",
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
                providerFax: provider.fax || "",
                providerEmail: provider.email || "",
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
    setIsProviderLocked(false);
    setProviderSearch({ name: "", phone: "" });

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        providerId: "",
        providerPersonId: "",
        providerFirstName: "",
        providerLastName: "",
        providerWorkPhone: "",
        providerFax: "",
        providerEmail: "",
      },
    }));
  };

  // "+ Add new provider" — no providerPersonId (doesn't exist yet); the
  // backend (SamplePatientUpdateData.initProvider) creates a new Person +
  // Provider from providerFirstName/LastName/etc. on save. Splits the typed
  // search text on the last space ("Jane Doe" -> first="Jane", last="Doe")
  // since the search box is a single free-text field but the backend needs
  // first/last separately; the name fields remain editable afterward so a
  // bad split can be corrected by hand.
  const handleUseAsNewProvider = (name) => {
    const trimmedName = name.trim();
    if (!trimmedName) {
      return;
    }
    const lastSpaceIndex = trimmedName.lastIndexOf(" ");
    const firstName =
      lastSpaceIndex === -1
        ? trimmedName
        : trimmedName.slice(0, lastSpaceIndex);
    const lastName =
      lastSpaceIndex === -1 ? "" : trimmedName.slice(lastSpaceIndex + 1);

    setSelectedProvider({ firstName, lastName, isNew: true });
    setProviderResults([]);

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        providerId: "",
        providerPersonId: "",
        providerFirstName: firstName,
        providerLastName: lastName,
        providerWorkPhone: providerSearch.phone || "",
      },
    }));
  };

  // Unlock an existing (search-selected/loaded) provider's
  // contact info for editing in place, instead of clearing and re-searching.
  const handleUnlockProvider = () => {
    setIsProviderLocked(false);
  };

  // Provider contact-info field change (Fax/Email) — editable regardless of
  // whether the provider came from search or was newly typed in.
  const handleProviderContactFieldChange = (field, value) => {
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        [field]: value,
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

  // Provider table headers - matches Provider Management (Name, Phone, Fax, Email)
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
    {
      key: "email",
      header: intl.formatMessage({
        id: "provider.email",
        defaultMessage: "Email",
      }),
    },
    { key: "actions", header: "" },
  ];

  // Requestor table headers
  const requestorHeaders = [
    {
      key: "name",
      header: intl.formatMessage({
        id: "requestor.name",
        defaultMessage: "Name",
      }),
    },
    {
      key: "phone",
      header: intl.formatMessage({
        id: "requestor.phone",
        defaultMessage: "Phone",
      }),
    },
    {
      key: "email",
      header: intl.formatMessage({
        id: "requestor.email",
        defaultMessage: "Email",
      }),
    },
    {
      key: "department",
      header: intl.formatMessage({
        id: "requestor.department",
        defaultMessage: "Department",
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
      <p className="helper-text">
        <FormattedMessage
          id="order.requester.section.helper"
          defaultMessage="Select the facility that ordered the tests and the responsible provider."
        />
      </p>

      {/* Required By — order-level date, not tied to Organization/Site
          selection; kept outside that subsection so it always renders. */}
      <Grid>
        <Column lg={5} md={4} sm={4}>
          <div className="cds--form-item">
            <label htmlFor="requiredBy" className="cds--label">
              {intl.formatMessage({
                id: "sample.requiredBy",
                defaultMessage: "Required By",
              })}
            </label>
            <input
              id="requiredBy"
              type="date"
              className="env-manifest-datetime"
              min={new Date().toISOString().split("T")[0]}
              value={orderData?.sampleOrderItems?.requiredBy || ""}
              onChange={(e) => {
                setOrderData((prev) => ({
                  ...prev,
                  sampleOrderItems: {
                    ...prev.sampleOrderItems,
                    requiredBy: e.target.value,
                  },
                }));
              }}
              disabled={isReadOnly}
            />
          </div>
        </Column>
      </Grid>

      {/* Requesting Organization / Site Search — same element for all three
          domains. Env/Vector call it "Requesting Organization";
          Vector previously had this entire block hidden and forced through
          Provider only, which mis-framed the requester as "always a doctor". */}
      <div className="subsection">
        <h5 className="subsection-title">
          {isEnvOrVector ? (
            <FormattedMessage
              id="requester.organization.search"
              defaultMessage="Requesting Organization Search"
            />
          ) : (
            <FormattedMessage id="site.search" defaultMessage="Site Search" />
          )}
        </h5>

        <Grid>
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="siteName"
              labelText={
                <FormattedMessage id="site.name" defaultMessage="Site Name" />
              }
              placeholder={intl.formatMessage({
                id: "site.name.placeholder",
                defaultMessage: "Enter site name",
              })}
              helperText={intl.formatMessage({
                id: "site.search.helper",
                defaultMessage:
                  "Type at least 2 characters to auto-search. Required for routing results back to the requesting site.",
              })}
              value={siteSearchTerm}
              onChange={(e) => setSiteSearchTerm(e.target.value)}
              disabled={isReadOnly || effectiveSelectedSite}
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
              helperText={intl.formatMessage({
                id: "order.priority.helper",
                defaultMessage:
                  "ROUTINE: standard turnaround. STAT: process immediately.",
              })}
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
                disabled={
                  isSearchingSites || isReadOnly || effectiveSelectedSite
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
                onClick={handleClearSiteSearch}
                disabled={Boolean(effectiveSelectedSite)}
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
        {siteResults.length > 0 && !effectiveSelectedSite && (
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

        {/* "+ Add new organization" — shown once the user has typed a name
            that didn't match an existing site, so a fresh org can still be
            created and saved. Gated by the
            restrictFreeTextRefSiteEntry admin config — when restricted, the
            button stays visible but disabled with an explanatory message
            rather than being silently hidden. */}
        {!effectiveSelectedSite &&
          siteSearchTerm.trim().length >= 2 &&
          siteResults.length === 0 &&
          !isSearchingSites && (
            <div className="search-results">
              <p className="helper-text">
                <FormattedMessage
                  id="requester.organization.no.match"
                  defaultMessage="No matching organization found."
                />
              </p>
              <Button
                kind="tertiary"
                size="sm"
                onClick={() => handleUseAsNewOrganization(siteSearchTerm)}
                disabled={isReadOnly || isOrganizationAddNewRestricted}
              >
                <FormattedMessage
                  id="requester.organization.add.new"
                  defaultMessage='+ Add new organization "{name}"'
                  values={{ name: siteSearchTerm }}
                />
              </Button>
              {isOrganizationAddNewRestricted && (
                <p className="helper-text admin-restricted-message">
                  <FormattedMessage
                    id="requester.add.new.restricted"
                    defaultMessage="Adding a new organization has been disabled by your administrator."
                  />
                </p>
              )}
            </div>
          )}

        {/* Selected Site Card */}
        {effectiveSelectedSite && (
          <div className="selected-entity-card">
            <div className="selected-card-header">
              <Tag type="green" size="sm">
                {effectiveSelectedSite.isNew ? (
                  <FormattedMessage
                    id="requester.organization.new.tag"
                    defaultMessage="New"
                  />
                ) : (
                  <FormattedMessage id="selected" defaultMessage="Selected" />
                )}
              </Tag>
              {!effectiveSelectedSite.isNew && isSiteLocked && (
                <Link onClick={handleUnlockSite}>
                  <FormattedMessage
                    id="label.button.edit.details"
                    defaultMessage="Edit details"
                  />
                </Link>
              )}
              <Link onClick={handleClearSite}>
                <FormattedMessage
                  id="label.button.clear"
                  defaultMessage="Clear"
                />
              </Link>
            </div>
            <div className="selected-card-content">
              <h5>{effectiveSelectedSite.organizationName}</h5>
              <p>
                {effectiveSelectedSite.city &&
                  `Location: ${effectiveSelectedSite.city}`}
                {effectiveSelectedSite.organizationType &&
                  ` · Type: ${effectiveSelectedSite.organizationType}`}
                {effectiveSelectedSite.isNew && (
                  <FormattedMessage
                    id="requester.organization.new.helper"
                    defaultMessage="Will be created as a new organization when this order is saved."
                  />
                )}
              </p>
            </div>
          </div>
        )}

        <Grid>
          <Column lg={10} md={8} sm={4}>
            <Select
              id="referringSiteDepartment"
              labelText={intl.formatMessage({ id: "site.department" })}
              value={sampleOrderItems.referringSiteDepartmentId || ""}
              onChange={handleDepartmentChange}
              disabled={
                isReadOnly ||
                !referringSiteId ||
                isLoadingDepartments ||
                departmentOptions.length === 0
              }
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: !referringSiteId
                    ? "site.department.selectFirst"
                    : departmentOptions.length > 0
                      ? "site.department.choose"
                      : "site.department.none",
                })}
              />
              {departmentOptions.map((department) => (
                <SelectItem
                  key={department.id}
                  value={department.id}
                  text={department.value}
                />
              ))}
            </Select>
          </Column>
        </Grid>

        {/* Requesting Organization contact info — its own phone/fax/email,
            independent of any Requestor contact person below.
            restrictFreeTextRefSiteEntry only blocks
            CREATING a brand-new organization (no existing org selected from
            search yet); editing an already-selected organization's contact
            info is always allowed (subject to the edit-lock below).
            An org pulled in via search/backend load starts
            read-only (isSiteLocked) until "Edit details" is clicked; a
            brand-new org (isNew) is never locked, since it has no prior
            saved state to protect. */}
        {(() => {
          const isCreatingNewOrg =
            !effectiveSelectedSite || effectiveSelectedSite.isNew;
          const isOrgContactEntryRestricted =
            isCreatingNewOrg && isOrganizationAddNewRestricted;
          const isOrgContactEntryLocked = !isCreatingNewOrg && isSiteLocked;
          const isOrgContactFieldDisabled =
            isReadOnly ||
            isOrgContactEntryRestricted ||
            isOrgContactEntryLocked;
          return (
            <>
              <Grid>
                <Column lg={5} md={4} sm={4}>
                  <TextInput
                    id="siteContactPhone"
                    labelText={intl.formatMessage({
                      id: "requester.organization.phone",
                      defaultMessage: "Organization Phone",
                    })}
                    value={
                      orderData?.sampleOrderItems?.referringSitePhone || ""
                    }
                    onChange={(e) =>
                      handleSiteContactFieldChange(
                        "referringSitePhone",
                        e.target.value,
                      )
                    }
                    disabled={isOrgContactFieldDisabled}
                  />
                </Column>
                <Column lg={5} md={4} sm={4}>
                  <TextInput
                    id="siteContactFax"
                    labelText={intl.formatMessage({
                      id: "requester.organization.fax",
                      defaultMessage: "Organization Fax",
                    })}
                    value={orderData?.sampleOrderItems?.referringSiteFax || ""}
                    onChange={(e) =>
                      handleSiteContactFieldChange(
                        "referringSiteFax",
                        e.target.value,
                      )
                    }
                    disabled={isOrgContactFieldDisabled}
                  />
                </Column>
                <Column lg={6} md={4} sm={4}>
                  <TextInput
                    id="siteContactEmail"
                    labelText={intl.formatMessage({
                      id: "requester.organization.email",
                      defaultMessage: "Organization Email",
                    })}
                    value={
                      orderData?.sampleOrderItems?.referringSiteEmail || ""
                    }
                    onChange={(e) =>
                      handleSiteContactFieldChange(
                        "referringSiteEmail",
                        e.target.value,
                      )
                    }
                    disabled={isOrgContactFieldDisabled}
                  />
                </Column>
              </Grid>
              {isOrgContactEntryRestricted && (
                <p className="helper-text admin-restricted-message">
                  <FormattedMessage
                    id="requester.add.new.restricted.organization.contact"
                    defaultMessage="Entering contact info for a new organization has been disabled by your administrator. Search for an existing organization above."
                  />
                </p>
              )}
            </>
          );
        })()}
      </div>

      {isEnvOrVector && (
        <div className="subsection">
          <h5 className="subsection-title">
            <FormattedMessage
              id="requester.contact.search"
              defaultMessage="Requestor Search"
            />
          </h5>
          <p className="helper-text">
            <FormattedMessage
              id="requester.contact.search.helper"
              defaultMessage="Search for the contact person requesting these tests, or enter a new one below. At least one of Requesting Organization or Requestor is required."
            />
          </p>

          <Grid>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="requestorSearchTerm"
                labelText={intl.formatMessage({
                  id: "requestor.name",
                  defaultMessage: "Requestor Name",
                })}
                placeholder={intl.formatMessage({
                  id: "requestor.name.placeholder",
                  defaultMessage: "Enter requestor name",
                })}
                helperText={intl.formatMessage({
                  id: "requestor.search.helper",
                  defaultMessage: "Type at least 2 characters to auto-search.",
                })}
                value={requestorSearchTerm}
                onChange={(e) => setRequestorSearchTerm(e.target.value)}
                disabled={isReadOnly}
              />
            </Column>

            <Column lg={8} md={8} sm={4}>
              <div className="search-buttons">
                <Button
                  kind="primary"
                  size="md"
                  onClick={() => handleRequestorSearch()}
                  disabled={
                    isSearchingRequestors || isReadOnly || selectedRequestor
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
                  onClick={handleClearRequestorSearch}
                  disabled={selectedRequestor}
                >
                  <FormattedMessage
                    id="label.button.clear"
                    defaultMessage="Clear"
                  />
                </Button>
              </div>
            </Column>
          </Grid>

          {/* Requestor Results */}
          {requestorResults.length > 0 && !selectedRequestor && (
            <div className="search-results">
              <p className="results-count">
                {requestorResults.length}{" "}
                <FormattedMessage
                  id="results.found.for"
                  defaultMessage='results found for "{term}"'
                  values={{ term: requestorSearchTerm }}
                />
              </p>
              <DataTable rows={requestorResults} headers={requestorHeaders}>
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
                        const requestor = requestorResults.find(
                          (r) => r.id === row.id,
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
                                        handleSelectRequestor(requestor)
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

          {/* "+ Add new requestor" — shown once the user has typed a name
              that didn't match an existing Requestor, mirroring the
              Organization/Provider "+ Add new" affordance. Gated by the
              restrictFreeTextRequestorEntry admin config — when
              restricted, the button stays visible but disabled with an
              explanatory message rather than being silently hidden. */}
          {!selectedRequestor &&
            requestorSearchTerm.trim().length >= 2 &&
            requestorResults.length === 0 &&
            !isSearchingRequestors && (
              <div className="search-results">
                <p className="helper-text">
                  <FormattedMessage
                    id="requestor.no.match"
                    defaultMessage="No matching requestor found."
                  />
                </p>
                <Button
                  kind="tertiary"
                  size="sm"
                  onClick={() => handleUseAsNewRequestor(requestorSearchTerm)}
                  disabled={isReadOnly || isRequestorAddNewRestricted}
                >
                  <FormattedMessage
                    id="requestor.add.new"
                    defaultMessage='+ Add new requestor "{name}"'
                    values={{ name: requestorSearchTerm }}
                  />
                </Button>
                {isRequestorAddNewRestricted && (
                  <p className="helper-text admin-restricted-message">
                    <FormattedMessage
                      id="requestor.add.new.restricted"
                      defaultMessage="Adding a new requestor has been disabled by your administrator."
                    />
                  </p>
                )}
              </div>
            )}

          {/* Selected Requestor Card */}
          {selectedRequestor && (
            <div className="selected-entity-card">
              <div className="selected-card-header">
                <Tag type="green" size="sm">
                  {selectedRequestor.isNew ? (
                    <FormattedMessage
                      id="requestor.new.tag"
                      defaultMessage="New"
                    />
                  ) : (
                    <FormattedMessage id="selected" defaultMessage="Selected" />
                  )}
                </Tag>
                {isRequestorLocked && (
                  <Link onClick={handleUnlockRequestor}>
                    <FormattedMessage
                      id="label.button.edit.details"
                      defaultMessage="Edit details"
                    />
                  </Link>
                )}
                <Link onClick={handleClearRequestor}>
                  <FormattedMessage
                    id="label.button.clear"
                    defaultMessage="Clear"
                  />
                </Link>
              </div>
              <div className="selected-card-content">
                <h5>
                  {selectedRequestor.firstName} {selectedRequestor.lastName}
                </h5>
                <p>
                  {selectedRequestor.phone &&
                    `Phone: ${selectedRequestor.phone}`}
                  {selectedRequestor.email &&
                    ` · Email: ${selectedRequestor.email}`}
                  {selectedRequestor.department &&
                    ` · Department: ${selectedRequestor.department}`}
                  {selectedRequestor.isNew && (
                    <FormattedMessage
                      id="requestor.new.helper"
                      defaultMessage="Will be created as a new requestor when this order is saved."
                    />
                  )}
                </p>
              </div>
            </div>
          )}

          {/* New Requestor contact fields — used when not found in search.
              restrictFreeTextRequestorEntry only blocks
              CREATING a brand-new Requestor (no selection yet); editing an
              already-selected/loaded Requestor's details is always allowed —
              admins restrict free-text entry of new records, not edits to
              existing ones (subject to the edit-lock below).
              A Requestor pulled in via search/backend load
              starts read-only (isRequestorLocked) until "Edit details" is
              clicked. */}
          {(() => {
            const isCreatingNew = !selectedRequestor || selectedRequestor.isNew;
            const isNewEntryRestricted =
              isCreatingNew && isRequestorAddNewRestricted;
            const isEntryLocked = !isCreatingNew && isRequestorLocked;
            const isFieldDisabled =
              isReadOnly || isNewEntryRestricted || isEntryLocked;
            return (
              <>
                <Grid>
                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="requestorFirstName"
                      labelText={intl.formatMessage({
                        id: "requestor.firstName",
                        defaultMessage: "First Name",
                      })}
                      value={
                        orderData?.sampleOrderItems?.requestorFirstName || ""
                      }
                      onChange={(e) =>
                        handleRequestorFieldChange(
                          "requestorFirstName",
                          e.target.value,
                        )
                      }
                      disabled={isFieldDisabled}
                    />
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="requestorLastName"
                      labelText={intl.formatMessage({
                        id: "requestor.lastName",
                        defaultMessage: "Last Name",
                      })}
                      value={
                        orderData?.sampleOrderItems?.requestorLastName || ""
                      }
                      onChange={(e) =>
                        handleRequestorFieldChange(
                          "requestorLastName",
                          e.target.value,
                        )
                      }
                      disabled={isFieldDisabled}
                    />
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="requestorPhone"
                      labelText={intl.formatMessage({
                        id: "requestor.phone",
                        defaultMessage: "Phone",
                      })}
                      value={orderData?.sampleOrderItems?.requestorPhone || ""}
                      onChange={(e) =>
                        handleRequestorFieldChange(
                          "requestorPhone",
                          e.target.value,
                        )
                      }
                      disabled={isFieldDisabled}
                    />
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="requestorFax"
                      labelText={intl.formatMessage({
                        id: "requestor.fax",
                        defaultMessage: "Fax",
                      })}
                      value={orderData?.sampleOrderItems?.requestorFax || ""}
                      onChange={(e) =>
                        handleRequestorFieldChange(
                          "requestorFax",
                          e.target.value,
                        )
                      }
                      disabled={isFieldDisabled}
                    />
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="requestorEmail"
                      labelText={intl.formatMessage({
                        id: "requestor.email",
                        defaultMessage: "Email",
                      })}
                      value={orderData?.sampleOrderItems?.requestorEmail || ""}
                      onChange={(e) =>
                        handleRequestorFieldChange(
                          "requestorEmail",
                          e.target.value,
                        )
                      }
                      disabled={isFieldDisabled}
                    />
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="requestorDepartment"
                      labelText={intl.formatMessage({
                        id: "requestor.department",
                        defaultMessage: "Department",
                      })}
                      value={
                        orderData?.sampleOrderItems?.requestorDepartment || ""
                      }
                      onChange={(e) =>
                        handleRequestorFieldChange(
                          "requestorDepartment",
                          e.target.value,
                        )
                      }
                      disabled={isFieldDisabled}
                    />
                  </Column>
                </Grid>
                {isNewEntryRestricted && (
                  <p className="helper-text admin-restricted-message">
                    <FormattedMessage
                      id="requester.add.new.restricted.requestor"
                      defaultMessage="Adding a new requestor has been disabled by your administrator. Search for an existing requestor above."
                    />
                  </p>
                )}
              </>
            );
          })()}
        </div>
      )}

      {/* Provider Search — Clinical only. Env/Vector use the Requestor
          contact above instead (Provider and Requestor are
          distinct domain concepts, not interchangeable). */}
      {!isEnvOrVector && (
        <div className="subsection">
          <h5 className="subsection-title">
            <FormattedMessage
              id="provider.search"
              defaultMessage="Provider Search"
            />
          </h5>
          <p className="helper-text">
            <FormattedMessage
              id="provider.search.helper"
              defaultMessage="Enter provider name or phone number and press Search."
            />
          </p>

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
                disabled={isReadOnly || effectiveSelectedProvider}
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
                disabled={isReadOnly || effectiveSelectedProvider}
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
                    isSearchingProviders ||
                    isReadOnly ||
                    effectiveSelectedProvider
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
                  disabled={Boolean(effectiveSelectedProvider)}
                >
                  <FormattedMessage
                    id="label.button.clear"
                    defaultMessage="Clear"
                  />
                </Button>
              </div>
            </Column>
          </Grid>

          {/* Provider contact info — First/Last Name (editable so a newly
              added provider's search-derived name split can be corrected)
              plus Phone/Fax/Email (v1 dropped fax/email).
              restrictFreeTextProviderEntry only blocks
              CREATING a brand-new provider (no existing provider selected
              from search yet); editing an already-selected provider's
              contact info is always allowed (subject to the edit-lock
              below). A provider pulled in via search/
              backend load starts read-only (isProviderLocked) until
              "Edit details" is clicked. */}
          {(() => {
            const isCreatingNewProvider =
              !effectiveSelectedProvider || effectiveSelectedProvider.isNew;
            const isProviderContactEntryRestricted =
              isCreatingNewProvider && isProviderAddNewRestricted;
            const isProviderContactEntryLocked =
              !isCreatingNewProvider && isProviderLocked;
            const isProviderFieldDisabled =
              isReadOnly ||
              isProviderContactEntryRestricted ||
              isProviderContactEntryLocked;
            return (
              <>
                <Grid>
                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="providerFirstName"
                      labelText={intl.formatMessage({
                        id: "provider.firstName.field",
                        defaultMessage: "First Name",
                      })}
                      value={
                        orderData?.sampleOrderItems?.providerFirstName || ""
                      }
                      onChange={(e) =>
                        handleProviderContactFieldChange(
                          "providerFirstName",
                          e.target.value,
                        )
                      }
                      disabled={isProviderFieldDisabled}
                    />
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="providerLastName"
                      labelText={intl.formatMessage({
                        id: "provider.lastName.field",
                        defaultMessage: "Last Name",
                      })}
                      value={
                        orderData?.sampleOrderItems?.providerLastName || ""
                      }
                      onChange={(e) =>
                        handleProviderContactFieldChange(
                          "providerLastName",
                          e.target.value,
                        )
                      }
                      disabled={isProviderFieldDisabled}
                    />
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="providerWorkPhoneField"
                      labelText={intl.formatMessage({
                        id: "provider.phone.field",
                        defaultMessage: "Phone",
                      })}
                      value={
                        orderData?.sampleOrderItems?.providerWorkPhone || ""
                      }
                      onChange={(e) =>
                        handleProviderContactFieldChange(
                          "providerWorkPhone",
                          e.target.value,
                        )
                      }
                      disabled={isProviderFieldDisabled}
                    />
                  </Column>
                </Grid>
                <Grid>
                  <Column lg={6} md={4} sm={4}>
                    <TextInput
                      id="providerFax"
                      labelText={intl.formatMessage({
                        id: "provider.fax.field",
                        defaultMessage: "Provider Fax",
                      })}
                      value={orderData?.sampleOrderItems?.providerFax || ""}
                      onChange={(e) =>
                        handleProviderContactFieldChange(
                          "providerFax",
                          e.target.value,
                        )
                      }
                      disabled={isProviderFieldDisabled}
                    />
                  </Column>
                  <Column lg={6} md={4} sm={4}>
                    <TextInput
                      id="providerEmail"
                      labelText={intl.formatMessage({
                        id: "provider.email.field",
                        defaultMessage: "Provider Email",
                      })}
                      value={orderData?.sampleOrderItems?.providerEmail || ""}
                      onChange={(e) =>
                        handleProviderContactFieldChange(
                          "providerEmail",
                          e.target.value,
                        )
                      }
                      disabled={isProviderFieldDisabled}
                    />
                  </Column>
                </Grid>
                {isProviderContactEntryRestricted && (
                  <p className="helper-text admin-restricted-message">
                    <FormattedMessage
                      id="requester.add.new.restricted.provider.contact"
                      defaultMessage="Entering contact info for a new provider has been disabled by your administrator. Search for an existing provider above."
                    />
                  </p>
                )}
              </>
            );
          })()}

          {/* Provider Results */}
          {providerResults.length > 0 && !effectiveSelectedProvider && (
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

          {/* "+ Add new provider" — shown once the user has typed a name
              that didn't match an existing provider, so a fresh provider can
              still be created and saved. Gated by the
              restrictFreeTextProviderEntry admin config — when restricted,
              the button stays visible but disabled with an explanatory
              message rather than being silently hidden. */}
          {!effectiveSelectedProvider &&
            providerSearch.name.trim().length >= 2 &&
            providerResults.length === 0 &&
            !isSearchingProviders && (
              <div className="search-results">
                <p className="helper-text">
                  <FormattedMessage
                    id="provider.no.match"
                    defaultMessage="No matching provider found."
                  />
                </p>
                <Button
                  kind="tertiary"
                  size="sm"
                  onClick={() => handleUseAsNewProvider(providerSearch.name)}
                  disabled={isReadOnly || isProviderAddNewRestricted}
                >
                  <FormattedMessage
                    id="provider.add.new"
                    defaultMessage='+ Add new provider "{name}"'
                    values={{ name: providerSearch.name }}
                  />
                </Button>
                {isProviderAddNewRestricted && (
                  <p className="helper-text admin-restricted-message">
                    <FormattedMessage
                      id="requester.add.new.restricted.provider"
                      defaultMessage="Adding a new provider has been disabled by your administrator."
                    />
                  </p>
                )}
              </div>
            )}

          {/* Selected Provider Card */}
          {effectiveSelectedProvider && (
            <div className="selected-entity-card">
              <div className="selected-card-header">
                <Tag type="green" size="sm">
                  {effectiveSelectedProvider.isNew ? (
                    <FormattedMessage
                      id="provider.new.tag"
                      defaultMessage="New"
                    />
                  ) : (
                    <FormattedMessage id="selected" defaultMessage="Selected" />
                  )}
                </Tag>
                {!effectiveSelectedProvider.isNew && isProviderLocked && (
                  <Link onClick={handleUnlockProvider}>
                    <FormattedMessage
                      id="label.button.edit.details"
                      defaultMessage="Edit details"
                    />
                  </Link>
                )}
                <Link onClick={handleClearProvider}>
                  <FormattedMessage
                    id="label.button.clear"
                    defaultMessage="Clear"
                  />
                </Link>
              </div>
              <div className="selected-card-content">
                <h5>
                  {effectiveSelectedProvider.firstName}{" "}
                  {effectiveSelectedProvider.lastName}
                </h5>
                <p>
                  {effectiveSelectedProvider.phone &&
                    `Phone: ${effectiveSelectedProvider.phone}`}
                  {effectiveSelectedProvider.fax &&
                    ` · Fax: ${effectiveSelectedProvider.fax}`}
                  {effectiveSelectedProvider.email &&
                    ` · Email: ${effectiveSelectedProvider.email}`}
                  {effectiveSelectedProvider.source &&
                    ` · Source: ${effectiveSelectedProvider.source}`}
                  {effectiveSelectedProvider.isNew && (
                    <FormattedMessage
                      id="provider.new.helper"
                      defaultMessage="Will be created as a new provider when this order is saved."
                    />
                  )}
                </p>
              </div>
            </div>
          )}
        </div>
      )}
    </Tile>
  );
};

export default RequesterSection;
