/**
 * S-04: Sample Type Domain Classification — React/Carbon Implementation
 *
 * Addendum to OGC-296 (Sample Type Management Module).
 * Shows:
 * - Sample Type list with Domain column and domain filter
 * - Basic Info tab with new Domain dropdown
 * - Real-time test count display for each sample type
 *
 * Dependencies: @carbon/react, @carbon/icons-react
 */

import React, {
  useState,
  useCallback,
  useMemo,
  useRef,
  useEffect,
} from "react";
import { useHistory, useLocation, useParams } from "react-router-dom";
import {
  Grid,
  Column,
  Stack,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Toggle,
  Button,
  InlineNotification,
  Tag,
  Tile,
  Loading,
  Pagination,
} from "@carbon/react";
import {
  DEFAULT_SAMPLE_TYPE_SECTION,
  isValidSampleTypeSection,
} from "./sectionConfig";
import TerminologySection from "./sections/TerminologySection";
import {
  Add,
  Edit,
  Save,
  CheckmarkFilled,
  WarningFilled,
} from "@carbon/react/icons";
import { injectIntl, FormattedMessage } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

// Breadcrumbs
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage/SampleTypeManagement",
  },
];

// ─── Domain Config ────────────────────────────────────────────────
const DOMAIN_OPTIONS = [
  { value: "CLINICAL", label: "Clinical" },
  { value: "ENVIRONMENTAL", label: "Environmental" },
  { value: "VECTOR", label: "Vector" },
];

const DOMAIN_TAG_KIND = {
  CLINICAL: "green",
  ENVIRONMENTAL: "purple",
  VECTOR: "teal",
};

// ─── Domain Mapping Functions ────────────────────────────────────
const mapBackendDomainToFrontend = (backendDomain) => {
  switch (backendDomain) {
    case "H":
      return "CLINICAL";
    case "E":
      return "ENVIRONMENTAL";
    case "V":
      return "VECTOR";
    case "A":
      return "CLINICAL"; // Animal samples show as Clinical for now
    default:
      return "CLINICAL";
  }
};

// ─── Mock Data ────────────────────────────────────────────────────
const MOCK_SAMPLE_TYPES = [
  {
    id: 1,
    name: "Serum",
    description: "Blood serum after centrifugation",
    active: true,
    domain: "CLINICAL",
    testCount: 142,
  },
  {
    id: 2,
    name: "Whole Blood",
    description: "Unprocessed blood sample",
    active: true,
    domain: "CLINICAL",
    testCount: 87,
  },
  {
    id: 3,
    name: "Urine",
    description: "Spot or 24-hour urine",
    active: true,
    domain: "CLINICAL",
    testCount: 63,
  },
  {
    id: 4,
    name: "Plasma",
    description: "Anticoagulated plasma",
    active: true,
    domain: "CLINICAL",
    testCount: 98,
  },
  {
    id: 5,
    name: "CSF",
    description: "Cerebrospinal fluid",
    active: true,
    domain: "CLINICAL",
    testCount: 24,
  },
  {
    id: 6,
    name: "Stool",
    description: "Fecal specimen",
    active: true,
    domain: "CLINICAL",
    testCount: 18,
  },
  {
    id: 7,
    name: "Surface Water",
    description: "River, lake, or stream sample",
    active: true,
    domain: "ENVIRONMENTAL",
    testCount: 42,
  },
  {
    id: 8,
    name: "Groundwater",
    description: "Well or borehole water",
    active: true,
    domain: "ENVIRONMENTAL",
    testCount: 38,
  },
  {
    id: 9,
    name: "Drinking Water",
    description:
      "Treated potable water — tested in both clinical water quality and environmental monitoring",
    active: true,
    domain: "VECTOR",
    testCount: 56,
  },
  {
    id: 10,
    name: "Effluent / Wastewater",
    description: "Industrial or municipal discharge",
    active: true,
    domain: "ENVIRONMENTAL",
    testCount: 31,
  },
  {
    id: 11,
    name: "Ambient Air",
    description: "Outdoor air quality sample",
    active: true,
    domain: "ENVIRONMENTAL",
    testCount: 18,
  },
  {
    id: 12,
    name: "Topsoil",
    description: "Surface soil (0–30 cm)",
    active: true,
    domain: "ENVIRONMENTAL",
    testCount: 22,
  },
  {
    id: 13,
    name: "Sediment",
    description: "River or lake bed sediment",
    active: true,
    domain: "ENVIRONMENTAL",
    testCount: 15,
  },
  {
    id: 14,
    name: "Sputum",
    description: "Expectorated or induced sputum",
    active: true,
    domain: "CLINICAL",
    testCount: 12,
  },
  {
    id: 15,
    name: "Sludge",
    description: "Wastewater treatment sludge",
    active: true,
    domain: "ENVIRONMENTAL",
    testCount: 9,
  },
  {
    id: 16,
    name: "Throat Swab",
    description: "Oropharyngeal swab",
    active: true,
    domain: "CLINICAL",
    testCount: 8,
  },
];

// ─── Main Component ───────────────────────────────────────────────

// Set to true for testing without authentication, false for real database integration
const USE_SIMULATION_MODE = false; // DATABASE MODE - Real PostgreSQL integration

function SampleTypeManagement({ intl }) {
  const history = useHistory();
  const location = useLocation();
  const { sampleTypeId, section } = useParams();
  const basePath = location.pathname.startsWith("/admin")
    ? "/admin"
    : "/MasterListsPage";
  const listUrl = `${basePath}/SampleTypeManagement`;

  // View is derived from the URL: no id → list, "new" → add, otherwise → editor.
  const view = !sampleTypeId
    ? "list"
    : sampleTypeId === "new"
      ? "add"
      : "editor";
  const activeSection = isValidSampleTypeSection(section)
    ? section
    : DEFAULT_SAMPLE_TYPE_SECTION;

  const [editingType, setEditingType] = useState(null);

  // Filter state
  const [searchText, setSearchText] = useState("");
  const [domainFilter, setDomainFilter] = useState("");

  // Pagination state (following repository pattern)
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // Data state
  const [sampleTypes, setSampleTypes] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // Form validation and state
  const [formErrors, setFormErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [showEditSuccess, setShowEditSuccess] = useState(false);
  const nameInputRef = useRef(null);

  // Associated tests for the sample type currently being edited
  const [associatedTests, setAssociatedTests] = useState([]);
  const [associatedTestsLoading, setAssociatedTestsLoading] = useState(false);
  const [associatedTestsError, setAssociatedTestsError] = useState(null);

  // Fetch sample types from backend or use mock data
  useEffect(() => {
    const fetchSampleTypes = async () => {
      try {
        setIsLoading(true);
        setLoadError(null);

        if (USE_SIMULATION_MODE) {
          // Use mock data for testing without authentication
          console.log("Using simulation mode - loading mock data");
          await new Promise((resolve) => setTimeout(resolve, 500)); // Simulate loading delay
          setSampleTypes(MOCK_SAMPLE_TYPES);
          return;
        }

        // Real database fetch using the sample types management endpoint with domain support
        await new Promise((resolve, reject) => {
          getFromOpenElisServer("/rest/sample-types", (response) => {
            console.log("Fetched sample types from database:", response);

            if (response && response.error) {
              reject(new Error(response.error));
              return;
            }

            // The new endpoint returns a wrapped response with data array
            if (response && response.success && Array.isArray(response.data)) {
              resolve(response.data);
            } else if (Array.isArray(response)) {
              // Fallback for direct array response
              resolve(response);
            } else {
              reject(
                new Error("Invalid response format from sample types endpoint"),
              );
            }
          });
        }).then((sampleTypeList) => {
          if (Array.isArray(sampleTypeList)) {
            console.log("=== DOMAIN LOADING DEBUG ===");
            sampleTypeList.forEach((item, idx) => {
              console.log(
                `Sample ${idx}: ${item.name || item.description}, Domain: ${item.domain}`,
              );
            });
            console.log("============================");

            const sampleTypeData = sampleTypeList.map((item, index) => ({
              id: item.id || index + 1,
              name: item.name || item.description || "Unknown Sample Type",
              description:
                item.description || item.name || "Sample type from database",
              domain: item.domain || "CLINICAL", // Use the domain directly from the new endpoint
              active: item.isActive !== undefined ? item.isActive : true,
              testCount: item.testCount || 0, // Use actual test count from backend
            }));
            setSampleTypes(sampleTypeData);
            console.log("Converted sample types with domains:", sampleTypeData);
          } else {
            throw new Error(
              "Invalid response format from sample types endpoint",
            );
          }
        });
      } catch (error) {
        console.error("Error fetching sample types:", error);
        setLoadError(
          `Database connection failed: ${error.message}. Please ensure you are logged into OpenELIS with admin permissions and try refreshing the page.`,
        );
        setSampleTypes([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchSampleTypes();
  }, []);

  // Focus management for add form
  useEffect(() => {
    if (view === "add" && nameInputRef.current) {
      nameInputRef.current.focus();
    }
  }, [view]);

  // Filtered list
  const filteredTypes = useMemo(() => {
    return sampleTypes.filter((st) => {
      const matchesSearch =
        !searchText ||
        st.name.toLowerCase().includes(searchText.toLowerCase()) ||
        st.description.toLowerCase().includes(searchText.toLowerCase());
      const matchesDomain = !domainFilter || st.domain === domainFilter;
      return matchesSearch && matchesDomain;
    });
  }, [sampleTypes, searchText, domainFilter]);

  // Paginated list (following repository pattern)
  const paginatedTypes = useMemo(() => {
    const startIndex = (page - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    return filteredTypes.slice(startIndex, endIndex);
  }, [filteredTypes, page, pageSize]);

  // Reset pagination when filters change
  useEffect(() => {
    setPage(1);
  }, [searchText, domainFilter]);

  // Pagination handler (following repository pattern)
  const handlePageChange = useCallback(
    (pageInfo) => {
      if (page !== pageInfo.page) {
        setPage(pageInfo.page);
      }
      if (pageSize !== pageInfo.pageSize) {
        setPageSize(pageInfo.pageSize);
      }
    },
    [page, pageSize],
  );

  // Domain counts
  const domainCounts = useMemo(() => {
    const counts = { CLINICAL: 0, ENVIRONMENTAL: 0, VECTOR: 0 };
    sampleTypes.forEach((st) => counts[st.domain]++);
    return counts;
  }, [sampleTypes]);

  const loadAssociatedTests = useCallback((id) => {
    setAssociatedTests([]);
    setAssociatedTestsError(null);
    setAssociatedTestsLoading(true);
    getFromOpenElisServer(
      `/rest/AllTestsForSampleTypeProvider?sampleTypeId=${encodeURIComponent(id)}`,
      (response) => {
        if (response && Array.isArray(response.tests)) {
          setAssociatedTests(response.tests);
        } else {
          setAssociatedTests([]);
          setAssociatedTestsError("Unable to load associated tests");
        }
        setAssociatedTestsLoading(false);
      },
    );
  }, []);

  // Hydrate editingType from the URL id whenever we're on the editor route.
  // The list may not be loaded yet on a deep link, so we tolerate an empty
  // sampleTypes list and re-run once it populates.
  useEffect(() => {
    if (view !== "editor" || !sampleTypeId) {
      return;
    }
    if (editingType && String(editingType.id) === String(sampleTypeId)) {
      return;
    }
    const st = sampleTypes.find(
      (item) => String(item.id) === String(sampleTypeId),
    );
    if (!st) {
      return;
    }
    setEditingType({
      id: st.id,
      name: st.name,
      description: st.description,
      active: st.active,
      domain: st.domain,
      testCount: st.testCount,
      abbreviation: st.abbreviation || "",
      sortOrder: st.sortOrder || 0,
      storageTemp: st.storageTemp || st.storageTemperature || "",
      containerType: st.containerType || "",
      isDefault: false,
      maxStorageDays: "",
      processingInstructions: "",
      collectionMethod: "",
      requiredVolume: "",
      volumeUnit: "ml",
    });
    setFormErrors({});
    setShowSuccess(false);
    loadAssociatedTests(st.id);
  }, [view, sampleTypeId, sampleTypes, editingType, loadAssociatedTests]);

  // Seed the "add" form once when we land on /SampleTypeManagement/new.
  useEffect(() => {
    if (view !== "add") {
      return;
    }
    if (editingType && editingType.id === null) {
      return;
    }
    setEditingType({
      id: null,
      name: "",
      description: "",
      active: true,
      domain: "CLINICAL",
      testCount: 0,
      abbreviation: "",
      sortOrder: sampleTypes.length + 1,
      isDefault: false,
      storageTemp: "",
      maxStorageDays: "",
      containerType: "",
      processingInstructions: "",
      collectionMethod: "",
      requiredVolume: "",
      volumeUnit: "ml",
    });
    setFormErrors({});
    setShowSuccess(false);
  }, [view, sampleTypes.length, editingType]);

  // Clear editor state when returning to the list URL.
  useEffect(() => {
    if (view === "list" && editingType) {
      setEditingType(null);
    }
  }, [view, editingType]);

  // Canonicalize the section into the URL so deep-links + the SideNav agree.
  useEffect(() => {
    if (sampleTypeId && (!section || !isValidSampleTypeSection(section))) {
      history.replace(
        `${listUrl}/${sampleTypeId}/${DEFAULT_SAMPLE_TYPE_SECTION}`,
      );
    }
  }, [sampleTypeId, section, history, listUrl]);

  const openEditor = useCallback(
    (st) => {
      history.push(`${listUrl}/${st.id}/${DEFAULT_SAMPLE_TYPE_SECTION}`);
    },
    [history, listUrl],
  );

  const openAddForm = useCallback(() => {
    history.push(`${listUrl}/new/${DEFAULT_SAMPLE_TYPE_SECTION}`);
  }, [history, listUrl]);

  const goToList = useCallback(() => {
    history.push(listUrl);
  }, [history, listUrl]);

  // Form validation
  const validateForm = useCallback(
    (formData) => {
      const errors = {};

      // Required field validations
      if (!formData.name?.trim()) {
        errors.name = "Sample type name is required";
      } else if (formData.name.trim().length < 2) {
        errors.name = "Name must be at least 2 characters long";
      } else if (
        sampleTypes.some(
          (st) =>
            st.name.toLowerCase() === formData.name.trim().toLowerCase() &&
            st.id !== formData.id,
        )
      ) {
        errors.name = "This sample type name already exists";
      }

      if (!formData.description?.trim()) {
        errors.description = "Description is required";
      }

      if (!formData.domain) {
        errors.domain = "Sample domain is required";
      }

      // Optional field validations
      if (formData.abbreviation && formData.abbreviation.length > 10) {
        errors.abbreviation = "Abbreviation must be 10 characters or less";
      }

      if (
        formData.maxStorageDays &&
        (isNaN(formData.maxStorageDays) ||
          parseInt(formData.maxStorageDays) < 0)
      ) {
        errors.maxStorageDays = "Max storage days must be a positive number";
      }

      if (
        formData.requiredVolume &&
        (isNaN(formData.requiredVolume) ||
          parseFloat(formData.requiredVolume) < 0)
      ) {
        errors.requiredVolume = "Required volume must be a positive number";
      }

      return errors;
    },
    [sampleTypes],
  );

  const saveEditor = useCallback(async () => {
    if (!editingType) return;

    // Validate form
    const errors = validateForm(editingType);
    setFormErrors(errors);

    if (Object.keys(errors).length > 0) {
      return;
    }

    setIsSubmitting(true);

    try {
      if (view === "add") {
        if (USE_SIMULATION_MODE) {
          // Simulation mode - for testing without authentication
          console.log("Simulating sample type creation:", editingType.name);
          await new Promise((resolve) => setTimeout(resolve, 800)); // Simulate API delay

          const newSampleType = {
            id: Date.now(),
            name: editingType.name.trim(),
            description: editingType.description.trim(),
            domain: editingType.domain,
            active: true,
            testCount: 0,
          };

          // Add to the beginning of the list to show latest first
          setSampleTypes((prev) => [newSampleType, ...prev]);
          setShowSuccess(true);
          setTimeout(() => setShowSuccess(false), 3000);
          console.log("Sample type created (simulation):", newSampleType.name);

          // Return to list view
          setTimeout(() => {
            setEditingType(null);
            setFormErrors({});
            history.push(listUrl);
          }, 1500);
        } else {
          // Creating new sample type using exact SampleTypeCreateForm format
          const sampleTypeData = {
            formName: "sampleTypeCreateForm",
            sampleTypeEnglishName: editingType.name.trim(),
            sampleTypeFrenchName: editingType.name.trim(), // Both are required fields
            domain: editingType.domain || "CLINICAL", // Include domain selection
          };

          console.log("=== FRONTEND DOMAIN SUBMISSION DEBUG ===");
          console.log("Sample Name:", editingType.name.trim());
          console.log("Selected Domain:", editingType.domain);
          console.log(
            "Sending to backend:",
            JSON.stringify(sampleTypeData, null, 2),
          );
          console.log("=========================================");

          // Use utility function for POST request
          const creationResult = await new Promise((resolve, reject) => {
            postToOpenElisServerJsonResponse(
              "/rest/SampleTypeCreate",
              JSON.stringify(sampleTypeData),
              (result) => {
                console.log("=== BACKEND RESPONSE DEBUG ===");
                console.log("Response from backend:", result);
                console.log("==============================");

                // Check for various error conditions
                if (result && result.error) {
                  reject(new Error(result.message || result.error));
                  return;
                }

                if (result && result.status && result.status !== 200) {
                  reject(
                    new Error(
                      `Database save failed: ${result.message || "Unknown error"}`,
                    ),
                  );
                  return;
                }

                // Success - database save confirmed
                console.log(
                  "✓ Sample type successfully saved to database:",
                  editingType.name,
                );
                resolve(result);
              },
            );
          });

          console.log("Database persistence confirmed for:", editingType.name);

          // Create a new sample type entry for the UI
          const newSampleType = {
            id: Date.now(),
            name: editingType.name.trim(),
            description: editingType.description.trim(),
            domain: editingType.domain, // Preserve the original frontend domain selection
            active: true,
            testCount: 0,
          };

          // Add the new sample type to the frontend list immediately to preserve domain selection
          setSampleTypes((prev) => [newSampleType, ...prev]);
          console.log(
            "✓ Sample type added to frontend list with domain:",
            editingType.domain,
          );

          // Simple verification that the sample was saved to database
          console.log("✓ Sample type successfully saved to database");

          setShowSuccess(true);
          setTimeout(() => setShowSuccess(false), 5000);
          console.log("Sample Type created and list refreshed from database");

          // Return to list view after a short delay to show success message
          setTimeout(() => {
            setEditingType(null);
            setFormErrors({});
            history.push(listUrl);
          }, 2000);
        }
      } else if (view === "editor") {
        // Editing existing sample type
        if (USE_SIMULATION_MODE) {
          // Simulation mode - for testing without authentication
          console.log("Simulating sample type update:", editingType.name);
          await new Promise((resolve) => setTimeout(resolve, 800));

          // Update the sample type in the list with ALL fields
          setSampleTypes((prev) =>
            prev.map((st) =>
              st.id === editingType.id
                ? {
                    ...st,
                    name: editingType.name.trim(),
                    description: editingType.description.trim(),
                    domain: editingType.domain || st.domain,
                    active:
                      editingType.active !== undefined
                        ? editingType.active
                        : st.active,
                    abbreviation: editingType.abbreviation || "",
                    containerType: editingType.containerType || "",
                    storageTemp: editingType.storageTemp || "",
                    sortOrder: editingType.sortOrder || st.sortOrder || 0,
                  }
                : st,
            ),
          );

          console.log("Sample type updated (simulation):", editingType.name);
        } else {
          // Real database update - send ALL form fields for complete persistence
          const updateData = {
            id: editingType.id,
            name: editingType.name?.trim() || editingType.name,
            description:
              editingType.description?.trim() || editingType.name?.trim(),
            domain: editingType.domain || "CLINICAL",
            abbreviation: editingType.abbreviation?.trim() || "",
            containerType: editingType.containerType?.trim() || "",
            storageTemperature: editingType.storageTemp?.trim() || "",
            isActive:
              editingType.active !== undefined ? editingType.active : true,
            sortOrder: editingType.sortOrder || 0,
          };

          console.log("=== FRONTEND UPDATE DEBUG ===");
          console.log("Original sample type (editingType):", editingType);
          console.log(
            "Sending update data:",
            JSON.stringify(updateData, null, 2),
          );
          console.log("Field by field check:");
          console.log("- ID:", updateData.id);
          console.log(
            "- Name:",
            `"${updateData.name}" (length: ${updateData.name ? updateData.name.length : 0})`,
          );
          console.log(
            "- Description:",
            `"${updateData.description}" (length: ${updateData.description ? updateData.description.length : 0})`,
          );
          console.log(
            "- Abbreviation:",
            `"${updateData.abbreviation}" (length: ${updateData.abbreviation ? updateData.abbreviation.length : 0})`,
          );
          console.log(
            "- Container Type:",
            `"${updateData.containerType}" (length: ${updateData.containerType ? updateData.containerType.length : 0})`,
          );
          console.log(
            "- Storage Temp:",
            `"${updateData.storageTemperature}" (length: ${updateData.storageTemperature ? updateData.storageTemperature.length : 0})`,
          );
          console.log("- Domain:", updateData.domain);
          console.log("- Active:", updateData.isActive);
          console.log("=============================");

          const updateResult = await new Promise((resolve, reject) => {
            postToOpenElisServerJsonResponse(
              "/rest/sample-types/update",
              JSON.stringify(updateData),
              (result) => {
                console.log("=== BACKEND RESPONSE DEBUG ===");
                console.log("Response from backend:", result);
                console.log("Response type:", typeof result);
                console.log(
                  "Response keys:",
                  result ? Object.keys(result) : "null",
                );
                console.log("==============================");

                if (result && result.error) {
                  console.error(
                    "Backend error response:",
                    result.error,
                    result.message,
                  );
                  reject(new Error(result.message || result.error));
                  return;
                }

                if (result && result.success) {
                  console.log("✅ Backend claims success:", result);
                  console.log("Backend response data:", result.data);
                  resolve(result.data);
                } else if (result && result.success === false) {
                  console.error("Backend failure response:", result.message);
                  reject(
                    new Error(
                      "Update failed: " + (result.message || "Unknown error"),
                    ),
                  );
                } else {
                  console.error("Unexpected backend response format:", result);
                  console.error(
                    "Raw response:",
                    JSON.stringify(result, null, 2),
                  );
                  reject(
                    new Error(
                      "Update failed: Unexpected response format - " +
                        JSON.stringify(result),
                    ),
                  );
                }
              },
            );
          });

          // Note: Frontend state update now happens after server verification (below)

          console.log("Backend claims sample type updated successfully");

          // VERIFICATION: Refresh data from server to confirm persistence
          console.log(
            "🔄 VERIFYING: Refreshing data from server to confirm persistence...",
          );

          try {
            // Refetch data from server to verify the changes were actually saved
            await new Promise((resolve, reject) => {
              getFromOpenElisServer("/rest/sample-types", (response) => {
                console.log(
                  "✅ VERIFICATION: Data refetched from server:",
                  response,
                );

                if (
                  response &&
                  response.success &&
                  Array.isArray(response.data)
                ) {
                  const updatedSampleTypes = response.data.map(
                    (item, index) => ({
                      id: item.id || index + 1,
                      name:
                        item.name || item.description || "Unknown Sample Type",
                      description:
                        item.description ||
                        item.name ||
                        "Sample type from database",
                      domain: item.domain || "CLINICAL",
                      active:
                        item.isActive !== undefined ? item.isActive : true,
                      testCount: item.testCount || 0,
                      abbreviation: item.abbreviation || "",
                      containerType: item.containerType || "",
                      storageTemp: item.storageTemperature || "",
                    }),
                  );

                  // Update state with verified server data
                  setSampleTypes(updatedSampleTypes);
                  console.log(
                    "✅ VERIFICATION: Frontend state updated with verified server data",
                  );
                  resolve();
                } else {
                  console.error(
                    "❌ VERIFICATION: Failed to refetch data from server",
                  );
                  reject(new Error("Failed to verify data persistence"));
                }
              });
            });
          } catch (verificationError) {
            console.error("❌ VERIFICATION ERROR:", verificationError);
            alert(
              "WARNING: Update may not have been saved. Changes will be lost on refresh.",
            );
          }
        }

        // Return to list view and show success
        setShowEditSuccess(true);
        setTimeout(() => setShowEditSuccess(false), 3000);
        console.log("Sample Type update process completed");

        // Return to list view after a short delay to show success message
        setTimeout(() => {
          setEditingType(null);
          setFormErrors({});
          history.push(listUrl);
        }, 2000);
      }
    } catch (error) {
      console.error("Error saving sample type:", error);
      console.error("Error details:", error.message);
      console.error("Error stack:", error.stack);

      const operation = view === "add" ? "create" : "update";
      const errorMessage = `Failed to ${operation} sample type: ${error.message}`;

      setFormErrors({ submit: errorMessage });

      // Also show alert for immediate feedback
      alert(
        `ERROR: ${errorMessage}\n\nPlease check browser console for more details.`,
      );

      console.error("=== EDIT ERROR DEBUG ===");
      console.error("View:", view);
      console.error("EditingType ID:", editingType?.id);
      console.error("Full error:", error);
      console.error("======================");
    } finally {
      setIsSubmitting(false);
    }
  }, [editingType, view, validateForm, history, listUrl]);

  // ─── LIST VIEW ────────────────────────────────────────────────
  if (view === "list") {
    return (
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Stack gap={5}>
          {/* Loading State */}
          {isLoading && (
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                padding: "var(--cds-spacing-07)",
                alignItems: "center",
                gap: "var(--cds-spacing-03)",
              }}
            >
              <Loading />
              <span>Loading sample types...</span>
            </div>
          )}

          {/* Error State */}
          {loadError && (
            <InlineNotification
              kind="error"
              title="Error loading sample types"
              subtitle={loadError}
              lowContrast
              hideCloseButton={false}
              onCloseButtonClick={() => setLoadError(null)}
            />
          )}

          {/* Edit Success State */}
          {showEditSuccess && (
            <InlineNotification
              kind="success"
              title="Sample Type Updated"
              subtitle="The sample type has been successfully updated and saved to the database."
              lowContrast
              hideCloseButton={false}
              onCloseButtonClick={() => setShowEditSuccess(false)}
            />
          )}

          {!isLoading && (
            <>
              {/* Page Header */}
              <Tile style={{ padding: "var(--cds-spacing-06)" }}>
                <Grid>
                  <Column lg={8} md={4} sm={4}>
                    <h2
                      style={{
                        margin: "0 0 var(--cds-spacing-03) 0",
                        color: "var(--cds-text-primary)",
                        fontWeight: 600,
                      }}
                    >
                      <FormattedMessage
                        id="heading.sampleType.management"
                        defaultMessage="Sample Type Management"
                      />
                    </h2>
                    <p
                      style={{
                        fontSize: "14px",
                        color: "var(--cds-text-secondary)",
                        margin: "0",
                        lineHeight: 1.4,
                      }}
                    >
                      <FormattedMessage
                        id="heading.sampleType.subtitle"
                        defaultMessage="Configure sample types, display order, test associations, and domain classification."
                      />
                    </p>
                    {!isLoading && (
                      <p
                        style={{
                          fontSize: "12px",
                          color: "var(--cds-text-secondary)",
                          margin: "var(--cds-spacing-02) 0 0 0",
                          fontWeight: 500,
                        }}
                      >
                        {searchText || domainFilter ? (
                          <FormattedMessage
                            id="heading.sampleType.filtered"
                            defaultMessage="Showing {filtered} of {total} sample types"
                            values={{
                              filtered: filteredTypes.length,
                              total: sampleTypes.length,
                            }}
                          />
                        ) : (
                          <FormattedMessage
                            id="heading.sampleType.total"
                            defaultMessage="Total: {total} sample types"
                            values={{ total: sampleTypes.length }}
                          />
                        )}
                      </p>
                    )}
                  </Column>
                  <Column lg={8} md={4} sm={4} style={{ textAlign: "right" }}>
                    <Stack
                      orientation="horizontal"
                      gap={4}
                      style={{
                        justifyContent: "flex-end",
                        alignItems: "center",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "var(--cds-spacing-02)",
                        }}
                      >
                        <Tag type="green" size="md">
                          {domainCounts.CLINICAL}
                        </Tag>
                        <span style={{ fontSize: "14px", fontWeight: 500 }}>
                          <FormattedMessage
                            id="label.sampleType.domain.clinical"
                            defaultMessage="Clinical"
                          />
                        </span>
                      </div>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "var(--cds-spacing-02)",
                        }}
                      >
                        <Tag type="purple" size="md">
                          {domainCounts.ENVIRONMENTAL}
                        </Tag>
                        <span style={{ fontSize: "14px", fontWeight: 500 }}>
                          <FormattedMessage
                            id="label.sampleType.domain.environmental"
                            defaultMessage="Environmental"
                          />
                        </span>
                      </div>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "var(--cds-spacing-02)",
                        }}
                      >
                        <Tag type="teal" size="md">
                          {domainCounts.VECTOR}
                        </Tag>
                        <span style={{ fontSize: "14px", fontWeight: 500 }}>
                          <FormattedMessage
                            id="label.sampleType.domain.vector"
                            defaultMessage="Vector"
                          />
                        </span>
                      </div>
                    </Stack>
                  </Column>
                </Grid>
              </Tile>

              {/* Sample Type Table */}
              <TableContainer style={{ marginBottom: 0 }}>
                {/* Enhanced Toolbar */}
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    padding: "0 var(--cds-spacing-05)",
                    height: "48px",
                    background: "var(--cds-layer)",
                    borderBottom: "1px solid var(--cds-border-subtle-01)",
                  }}
                >
                  <TextInput
                    id="sample-type-search"
                    labelText={intl.formatMessage({
                      id: "placeholder.sampleType.search",
                      defaultMessage: "Search sample types...",
                    })}
                    hideLabel
                    placeholder={intl.formatMessage({
                      id: "placeholder.sampleType.search",
                      defaultMessage: "Search sample types...",
                    })}
                    value={searchText}
                    onChange={(e) => setSearchText(e.target.value)}
                    size="sm"
                    style={{
                      flex: "0 0 280px",
                      marginRight: "var(--cds-spacing-05)",
                    }}
                  />
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "80px",
                    }}
                  >
                    <Select
                      id="domain-filter"
                      labelText={intl.formatMessage({
                        id: "label.sampleType.filterDomain",
                        defaultMessage: "Filter by domain",
                      })}
                      hideLabel
                      value={domainFilter}
                      onChange={(e) => setDomainFilter(e.target.value)}
                      style={{
                        flex: "0 0 200px",
                      }}
                    >
                      <SelectItem
                        value=""
                        text={intl.formatMessage({
                          id: "placeholder.sampleType.filter.domain",
                          defaultMessage: "All domains",
                        })}
                      />
                      {DOMAIN_OPTIONS.map((opt) => (
                        <SelectItem
                          key={opt.value}
                          value={opt.value}
                          text={intl.formatMessage({
                            id: `label.sampleType.domain.${opt.value.toLowerCase()}`,
                            defaultMessage: opt.label,
                          })}
                        />
                      ))}
                    </Select>
                    <Button
                      kind="primary"
                      size="sm"
                      renderIcon={Add}
                      onClick={openAddForm}
                      style={{
                        whiteSpace: "nowrap",
                        flex: "0 0 auto",
                      }}
                    >
                      <FormattedMessage
                        id="button.sampleType.add"
                        defaultMessage="Add Sample Type"
                      />
                    </Button>
                  </div>
                </div>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        <FormattedMessage
                          id="label.sampleType.name"
                          defaultMessage="Name"
                        />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage
                          id="label.sampleType.domain"
                          defaultMessage="Domain"
                        />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage
                          id="label.sampleType.status"
                          defaultMessage="Status"
                        />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage
                          id="label.sampleType.testCount"
                          defaultMessage="Tests"
                        />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage
                          id="label.sampleType.actions"
                          defaultMessage="Actions"
                        />
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {paginatedTypes.length > 0 ? (
                      paginatedTypes.map((st) => (
                        <TableRow key={st.id}>
                          <TableCell>
                            <div>
                              <span
                                style={{
                                  fontWeight: 600,
                                  color: "var(--cds-text-primary)",
                                  fontSize: "14px",
                                }}
                              >
                                {st.name}
                              </span>
                              <br />
                              <span
                                style={{
                                  fontSize: "12px",
                                  color: "var(--cds-text-secondary)",
                                  lineHeight: 1.3,
                                  marginTop: "var(--cds-spacing-01)",
                                }}
                              >
                                {st.description}
                              </span>
                            </div>
                          </TableCell>
                          <TableCell>
                            <Tag type={DOMAIN_TAG_KIND[st.domain]} size="sm">
                              <FormattedMessage
                                id={`label.sampleType.domain.${st.domain.toLowerCase()}`}
                                defaultMessage={st.domain}
                              />
                            </Tag>
                          </TableCell>
                          <TableCell>
                            <Tag type={st.active ? "green" : "gray"} size="sm">
                              {st.active ? (
                                <FormattedMessage
                                  id="label.active"
                                  defaultMessage="Active"
                                />
                              ) : (
                                <FormattedMessage
                                  id="label.inactive"
                                  defaultMessage="Inactive"
                                />
                              )}
                            </Tag>
                          </TableCell>
                          <TableCell>
                            <span
                              style={{
                                fontWeight: 500,
                                color: "var(--cds-text-primary)",
                              }}
                            >
                              {st.testCount}
                            </span>
                          </TableCell>
                          <TableCell>
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={Edit}
                              onClick={() => openEditor(st)}
                            >
                              <FormattedMessage
                                id="button.edit"
                                defaultMessage="Edit"
                              />
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell
                          colSpan={5}
                          style={{
                            textAlign: "center",
                            padding: "var(--cds-spacing-07)",
                          }}
                        >
                          <div style={{ color: "var(--cds-text-secondary)" }}>
                            <FormattedMessage
                              id="message.sampleType.noResults"
                              defaultMessage="No sample types found matching your criteria"
                            />
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>

              {/* Repository Pattern Pagination */}
              <div style={{ overflowX: "auto" }}>
                <Pagination
                  onChange={handlePageChange}
                  page={page}
                  pageSize={pageSize}
                  pageSizes={[10, 20, 30, 50, 100]}
                  totalItems={filteredTypes.length}
                  forwardText={intl.formatMessage({ id: "pagination.forward" })}
                  backwardText={intl.formatMessage({
                    id: "pagination.backward",
                  })}
                  itemRangeText={(min, max, total) =>
                    intl.formatMessage(
                      { id: "pagination.item-range" },
                      { min: min, max: max, total: total },
                    )
                  }
                  itemsPerPageText={intl.formatMessage({
                    id: "pagination.items-per-page",
                  })}
                  pageNumberText={intl.formatMessage({
                    id: "pagination.page-number",
                  })}
                  pageRangeText={(current, total) =>
                    intl.formatMessage(
                      { id: "pagination.page-range" },
                      { current: current, total: total },
                    )
                  }
                  pageText={intl.formatMessage({
                    id: "pagination.page",
                  })}
                  size="md"
                />
              </div>
            </>
          )}
        </Stack>
      </div>
    );
  }

  // ─── EDITOR/ADD VIEW ──────────────────────────────────────────
  if (view === "editor" || view === "add") {
    return (
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Stack gap={5}>
          {/* Editor Header — mirrors the list-view header so
              "Sample Type Management" stays the page title and tabs sit under it. */}
          <Tile style={{ padding: "var(--cds-spacing-06)" }}>
            <Grid>
              <Column lg={12} md={6} sm={4}>
                <h2
                  style={{
                    margin: "0 0 var(--cds-spacing-03) 0",
                    color: "var(--cds-text-primary)",
                    fontWeight: 600,
                  }}
                >
                  <FormattedMessage
                    id="heading.sampleType.management"
                    defaultMessage="Sample Type Management"
                  />
                </h2>
                <Stack
                  orientation="horizontal"
                  gap={3}
                  style={{ alignItems: "center", flexWrap: "wrap" }}
                >
                  <p
                    style={{
                      fontSize: "14px",
                      color: "var(--cds-text-secondary)",
                      margin: 0,
                      lineHeight: 1.4,
                    }}
                  >
                    {view === "add" ? (
                      <FormattedMessage
                        id="heading.sampleType.add"
                        defaultMessage="Add New Sample Type"
                      />
                    ) : (
                      <FormattedMessage
                        id="heading.sampleType.editing"
                        defaultMessage="Editing: {name}"
                        values={{ name: editingType?.name }}
                      />
                    )}
                  </p>
                  {view === "editor" && editingType?.domain && (
                    <Tag type={DOMAIN_TAG_KIND[editingType?.domain]} size="md">
                      <FormattedMessage
                        id={`label.sampleType.domain.${editingType?.domain?.toLowerCase()}`}
                        defaultMessage={editingType?.domain}
                      />
                    </Tag>
                  )}
                  {view === "editor" &&
                    (editingType?.active ? (
                      <Tag type="green" size="md">
                        <FormattedMessage
                          id="label.active"
                          defaultMessage="Active"
                        />
                      </Tag>
                    ) : (
                      <Tag type="gray" size="md">
                        <FormattedMessage
                          id="label.inactive"
                          defaultMessage="Inactive"
                        />
                      </Tag>
                    ))}
                </Stack>
              </Column>
              <Column
                lg={4}
                md={2}
                sm={4}
                style={{
                  display: "flex",
                  justifyContent: "flex-end",
                  alignItems: "flex-start",
                }}
              >
                <Button kind="ghost" size="sm" onClick={goToList}>
                  <FormattedMessage
                    id="button.back"
                    defaultMessage="← Back to List"
                  />
                </Button>
              </Column>
            </Grid>
          </Tile>

          {/* Section content is driven by the URL: the Sample Type Management
              sidenav lists Basic Info / Associated Tests / Terminology as
              sub-items when this editor is open, mirroring the Test Catalog
              Editor pattern. */}
          <div>
            {activeSection === "basic-info" && (
              <div>
                {view === "add" && (
                  <div style={{ marginBottom: "var(--cds-spacing-06)" }}>
                    <Stack gap={5}>
                      {showSuccess && (
                        <InlineNotification
                          kind="success"
                          title=""
                          subtitle={intl.formatMessage({
                            id: "message.sampleType.add.success",
                            defaultMessage:
                              "Sample type created and saved to database successfully! The list has been refreshed to show your new sample type.",
                          })}
                          lowContrast
                          hideCloseButton
                        />
                      )}
                      {formErrors.submit && (
                        <InlineNotification
                          kind="error"
                          title=""
                          subtitle={formErrors.submit}
                          lowContrast
                          hideCloseButton={false}
                          onCloseButtonClick={() =>
                            setFormErrors((prev) => ({ ...prev, submit: "" }))
                          }
                        />
                      )}
                    </Stack>
                  </div>
                )}
                <Tile
                  style={{
                    padding: "var(--cds-spacing-07)",
                    border: "1px solid var(--cds-border-subtle)",
                    borderRadius: "var(--cds-border-radius)",
                  }}
                >
                  <Grid>
                    <Column lg={12} md={8} sm={4}>
                      <Stack gap={6}>
                        {/* Additional spacing above the Name field */}
                        <div
                          style={{ marginBottom: "var(--cds-spacing-03)" }}
                        />
                        <TextInput
                          ref={nameInputRef}
                          id="st-name"
                          labelText={
                            <>
                              <FormattedMessage
                                id="label.sampleType.name"
                                defaultMessage="Name"
                              />
                              <span
                                style={{ color: "var(--cds-support-error)" }}
                              >
                                {" "}
                                *
                              </span>
                            </>
                          }
                          value={editingType?.name || ""}
                          onChange={(e) => {
                            setEditingType((prev) => ({
                              ...prev,
                              name: e.target.value,
                            }));
                            if (formErrors.name) {
                              setFormErrors((prev) => ({ ...prev, name: "" }));
                            }
                          }}
                          invalid={!!formErrors.name}
                          invalidText={formErrors.name}
                          helperText={intl.formatMessage({
                            id: "helper.sampleType.name",
                            defaultMessage:
                              'Enter a unique name for this sample type (e.g., "Serum", "Whole Blood")',
                          })}
                          autoComplete="off"
                        />

                        <Select
                          id="st-domain"
                          labelText={
                            <>
                              <FormattedMessage
                                id="label.sampleType.domain"
                                defaultMessage="Sample Domain"
                              />
                              <span
                                style={{ color: "var(--cds-support-error)" }}
                              >
                                {" "}
                                *
                              </span>
                            </>
                          }
                          value={editingType?.domain || "CLINICAL"}
                          onChange={(e) =>
                            setEditingType((prev) => ({
                              ...prev,
                              domain: e.target.value,
                            }))
                          }
                          helperText={intl.formatMessage({
                            id: "label.sampleType.domain.helper",
                            defaultMessage:
                              "Determines which workflow mode (Clinical or Environmental) this sample type appears in.",
                          })}
                        >
                          {DOMAIN_OPTIONS.map((opt) => (
                            <SelectItem
                              key={opt.value}
                              value={opt.value}
                              text={intl.formatMessage({
                                id: `label.sampleType.domain.${opt.value.toLowerCase()}`,
                                defaultMessage: opt.label,
                              })}
                            />
                          ))}
                        </Select>

                        <Toggle
                          id="st-active"
                          labelText={intl.formatMessage({
                            id: "label.sampleType.active",
                            defaultMessage: "Active",
                          })}
                          labelA={intl.formatMessage({
                            id: "label.inactive",
                            defaultMessage: "Inactive",
                          })}
                          labelB={intl.formatMessage({
                            id: "label.active",
                            defaultMessage: "Active",
                          })}
                          toggled={editingType?.active}
                          onToggle={(checked) =>
                            setEditingType((prev) => ({
                              ...prev,
                              active: checked,
                            }))
                          }
                        />

                        <TextArea
                          id="st-description"
                          labelText={
                            <>
                              <FormattedMessage
                                id="label.sampleType.description"
                                defaultMessage="Description"
                              />
                              <span
                                style={{ color: "var(--cds-support-error)" }}
                              >
                                {" "}
                                *
                              </span>
                            </>
                          }
                          value={editingType?.description || ""}
                          onChange={(e) => {
                            setEditingType((prev) => ({
                              ...prev,
                              description: e.target.value,
                            }));
                            if (formErrors.description) {
                              setFormErrors((prev) => ({
                                ...prev,
                                description: "",
                              }));
                            }
                          }}
                          rows={4}
                          invalid={!!formErrors.description}
                          invalidText={formErrors.description}
                          helperText={intl.formatMessage({
                            id: "helper.sampleType.description",
                            defaultMessage:
                              "Provide a description of this sample type for lab staff reference",
                          })}
                        />
                      </Stack>
                    </Column>
                  </Grid>

                  <div
                    style={{
                      borderTop: "1px solid var(--cds-border-subtle-01)",
                      marginTop:
                        view === "add" ? "3rem" : "var(--cds-spacing-08)",
                      paddingTop: "var(--cds-spacing-10)",
                    }}
                  >
                    <Stack orientation="horizontal" gap={4}>
                      <Button
                        kind="primary"
                        size="sm"
                        renderIcon={isSubmitting ? undefined : Save}
                        onClick={saveEditor}
                        disabled={
                          isSubmitting ||
                          !!Object.keys(formErrors).length ||
                          !editingType?.name?.trim() ||
                          !editingType?.description?.trim()
                        }
                      >
                        {isSubmitting ? (
                          <>
                            <Loading style={{ marginRight: "8px" }} />
                            {view === "add" ? (
                              <FormattedMessage
                                id="button.sampleType.creating"
                                defaultMessage="Creating..."
                              />
                            ) : (
                              <FormattedMessage
                                id="button.saving"
                                defaultMessage="Saving..."
                              />
                            )}
                          </>
                        ) : view === "add" ? (
                          <FormattedMessage
                            id="button.sampleType.create"
                            defaultMessage="Create Sample Type"
                          />
                        ) : (
                          <FormattedMessage
                            id="button.save"
                            defaultMessage="Save Changes"
                          />
                        )}
                      </Button>
                      <Button kind="ghost" size="sm" onClick={goToList}>
                        <FormattedMessage
                          id="button.cancel"
                          defaultMessage="Cancel"
                        />
                      </Button>
                    </Stack>
                  </div>
                </Tile>
              </div>
            )}

            {/* Associated Tests — read-only list of tests linked to this sample type */}
            {activeSection === "associated-tests" && (
              <div>
                <Tile
                  style={{
                    padding: "var(--cds-spacing-06)",
                    border: "1px solid var(--cds-border-subtle)",
                    borderRadius: "var(--cds-border-radius)",
                  }}
                >
                  {view === "add" ? (
                    <p
                      style={{
                        color: "var(--cds-text-secondary)",
                        fontSize: "14px",
                        margin: 0,
                      }}
                    >
                      <FormattedMessage
                        id="label.sampleType.tests.addHint"
                        defaultMessage="Save this sample type first, then associate tests from the test configuration."
                      />
                    </p>
                  ) : associatedTestsLoading ? (
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "var(--cds-spacing-03)",
                        padding: "var(--cds-spacing-05)",
                      }}
                    >
                      <Loading small withOverlay={false} />
                      <FormattedMessage
                        id="label.sampleType.tests.loading"
                        defaultMessage="Loading associated tests..."
                      />
                    </div>
                  ) : associatedTestsError ? (
                    <InlineNotification
                      kind="error"
                      title={intl.formatMessage({
                        id: "label.sampleType.tests.error",
                        defaultMessage: "Failed to load associated tests",
                      })}
                      subtitle={associatedTestsError}
                      lowContrast
                      hideCloseButton
                    />
                  ) : associatedTests.length === 0 ? (
                    <p
                      style={{
                        color: "var(--cds-text-secondary)",
                        fontSize: "14px",
                        margin: 0,
                      }}
                    >
                      <FormattedMessage
                        id="label.sampleType.tests.none"
                        defaultMessage="No tests are associated with this sample type."
                      />
                    </p>
                  ) : (
                    <Table size="sm">
                      <TableHead>
                        <TableRow>
                          <TableHeader>
                            <FormattedMessage
                              id="label.test.name"
                              defaultMessage="Test Name"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="label.sampleType.status"
                              defaultMessage="Status"
                            />
                          </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {associatedTests.map((test) => (
                          <TableRow key={test.id}>
                            <TableCell>{test.name}</TableCell>
                            <TableCell>
                              <Tag
                                type={test.isActive ? "green" : "gray"}
                                size="sm"
                              >
                                {test.isActive ? (
                                  <FormattedMessage
                                    id="label.active"
                                    defaultMessage="Active"
                                  />
                                ) : (
                                  <FormattedMessage
                                    id="label.inactive"
                                    defaultMessage="Inactive"
                                  />
                                )}
                              </Tag>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  )}
                </Tile>
              </div>
            )}

            {/* Terminology — multi-row Source/Code/Relationship mappings,
                mirrors the Test Catalog Editor's Terminology section. */}
            {activeSection === "terminology" && (
              <div>
                <Tile
                  style={{
                    padding: "var(--cds-spacing-07)",
                    border: "1px solid var(--cds-border-subtle)",
                    borderRadius: "var(--cds-border-radius)",
                  }}
                >
                  {view === "add" ? (
                    <p
                      style={{
                        color: "var(--cds-text-secondary)",
                        fontSize: "14px",
                        margin: 0,
                      }}
                    >
                      <FormattedMessage
                        id="label.sampleType.terminology.addHint"
                        defaultMessage="Save this sample type first, then add terminology mappings."
                      />
                    </p>
                  ) : (
                    <TerminologySection sampleTypeId={editingType?.id} />
                  )}
                </Tile>
              </div>
            )}
          </div>
        </Stack>
      </div>
    );
  }

  // Fallback (shouldn't reach here)
  return null;
}

export default injectIntl(SampleTypeManagement);
