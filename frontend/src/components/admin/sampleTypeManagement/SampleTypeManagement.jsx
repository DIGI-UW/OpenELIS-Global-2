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

import React, { useState, useCallback, useMemo, useRef, useEffect } from 'react';
import {
  Grid, Column, Stack,
  TableContainer, Table, TableHead, TableRow, TableHeader,
  TableBody, TableCell,
  TextInput, TextArea, Select, SelectItem, Toggle,
  Button, InlineNotification, Tag, Tile,
  Tabs, Tab, TabList, TabPanels, TabPanel, Loading,
  FormGroup, FormLabel, NumberInput, Pagination,
} from '@carbon/react';
import { Add, Edit, Save, CheckmarkFilled, WarningFilled } from '@carbon/react/icons';
import { injectIntl, FormattedMessage } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { getFromOpenElisServer, postToOpenElisServerJsonResponse } from "../../utils/Utils";

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
  { value: 'CLINICAL', label: 'Clinical' },
  { value: 'ENVIRONMENTAL', label: 'Environmental' },
  { value: 'VECTOR', label: 'Vector' },
];

const DOMAIN_TAG_KIND = {
  CLINICAL: 'green',
  ENVIRONMENTAL: 'purple',
  VECTOR: 'teal',
};

// ─── Domain Mapping Functions ────────────────────────────────────
const mapBackendDomainToFrontend = (backendDomain) => {
  switch (backendDomain) {
    case 'H': return 'CLINICAL';
    case 'E': return 'ENVIRONMENTAL';
    case 'V': return 'VECTOR';
    case 'A': return 'CLINICAL'; // Animal samples show as Clinical for now
    default: return 'CLINICAL';
  }
};

// ─── Mock Data ────────────────────────────────────────────────────
const MOCK_SAMPLE_TYPES = [
  { id: 1, name: 'Serum', description: 'Blood serum after centrifugation', active: true, domain: 'CLINICAL', testCount: 142, whonet: 'SER' },
  { id: 2, name: 'Whole Blood', description: 'Unprocessed blood sample', active: true, domain: 'CLINICAL', testCount: 87, whonet: 'BLD' },
  { id: 3, name: 'Urine', description: 'Spot or 24-hour urine', active: true, domain: 'CLINICAL', testCount: 63, whonet: 'URI' },
  { id: 4, name: 'Plasma', description: 'Anticoagulated plasma', active: true, domain: 'CLINICAL', testCount: 98, whonet: 'PLA' },
  { id: 5, name: 'CSF', description: 'Cerebrospinal fluid', active: true, domain: 'CLINICAL', testCount: 24, whonet: 'CSF' },
  { id: 6, name: 'Stool', description: 'Fecal specimen', active: true, domain: 'CLINICAL', testCount: 18, whonet: 'STL' },
  { id: 7, name: 'Surface Water', description: 'River, lake, or stream sample', active: true, domain: 'ENVIRONMENTAL', testCount: 42, whonet: null },
  { id: 8, name: 'Groundwater', description: 'Well or borehole water', active: true, domain: 'ENVIRONMENTAL', testCount: 38, whonet: null },
  { id: 9, name: 'Drinking Water', description: 'Treated potable water — tested in both clinical water quality and environmental monitoring', active: true, domain: 'VECTOR', testCount: 56, whonet: null },
  { id: 10, name: 'Effluent / Wastewater', description: 'Industrial or municipal discharge', active: true, domain: 'ENVIRONMENTAL', testCount: 31, whonet: null },
  { id: 11, name: 'Ambient Air', description: 'Outdoor air quality sample', active: true, domain: 'ENVIRONMENTAL', testCount: 18, whonet: null },
  { id: 12, name: 'Topsoil', description: 'Surface soil (0–30 cm)', active: true, domain: 'ENVIRONMENTAL', testCount: 22, whonet: null },
  { id: 13, name: 'Sediment', description: 'River or lake bed sediment', active: true, domain: 'ENVIRONMENTAL', testCount: 15, whonet: null },
  { id: 14, name: 'Sputum', description: 'Expectorated or induced sputum', active: true, domain: 'CLINICAL', testCount: 12, whonet: 'SPT' },
  { id: 15, name: 'Sludge', description: 'Wastewater treatment sludge', active: true, domain: 'ENVIRONMENTAL', testCount: 9, whonet: null },
  { id: 16, name: 'Throat Swab', description: 'Oropharyngeal swab', active: true, domain: 'CLINICAL', testCount: 8, whonet: 'THR' },
];

// ─── Main Component ───────────────────────────────────────────────

// Set to true for testing without authentication, false for real database integration
const USE_SIMULATION_MODE = false; // DATABASE MODE - Real PostgreSQL integration

function SampleTypeManagement({ intl }) {
  // View state
  const [view, setView] = useState('list'); // 'list' | 'editor' | 'add'
  const [editingType, setEditingType] = useState(null);

  // Filter state
  const [searchText, setSearchText] = useState('');
  const [domainFilter, setDomainFilter] = useState('');

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



  // Fetch sample types from backend or use mock data
  useEffect(() => {
    const fetchSampleTypes = async () => {
      try {
        setIsLoading(true);
        setLoadError(null);

        if (USE_SIMULATION_MODE) {
          // Use mock data for testing without authentication
          console.log('Using simulation mode - loading mock data');
          await new Promise(resolve => setTimeout(resolve, 500)); // Simulate loading delay
          setSampleTypes(MOCK_SAMPLE_TYPES);
          return;
        }

        // Real database fetch using the sample types management endpoint with domain support
        await new Promise((resolve, reject) => {
          getFromOpenElisServer('/rest/sample-types', (response) => {
            console.log('Fetched sample types from database:', response);

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
              reject(new Error('Invalid response format from sample types endpoint'));
            }
          });
        }).then((sampleTypeList) => {

          if (Array.isArray(sampleTypeList)) {
            console.log('=== DOMAIN LOADING DEBUG ===');
            sampleTypeList.forEach((item, idx) => {
              console.log(`Sample ${idx}: ${item.name || item.description}, Domain: ${item.domain}`);
            });
            console.log('============================');

            const sampleTypeData = sampleTypeList.map((item, index) => ({
              id: item.id || (index + 1),
              name: item.name || item.description || 'Unknown Sample Type',
              description: item.description || item.name || 'Sample type from database',
              domain: item.domain || 'CLINICAL', // Use the domain directly from the new endpoint
              active: item.isActive !== undefined ? item.isActive : true,
              testCount: item.testCount || 0, // Use actual test count from backend
              whonet: item.whonetCode || null
            }));
            setSampleTypes(sampleTypeData);
            console.log('Converted sample types with domains:', sampleTypeData);
          } else {
            throw new Error('Invalid response format from sample types endpoint');
          }
        });
      } catch (error) {
        console.error('Error fetching sample types:', error);
        setLoadError(`Database connection failed: ${error.message}. Please ensure you are logged into OpenELIS with admin permissions and try refreshing the page.`);
        setSampleTypes([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchSampleTypes();
  }, []);

  // Focus management for add form
  useEffect(() => {
    if (view === 'add' && nameInputRef.current) {
      nameInputRef.current.focus();
    }
  }, [view]);

  // Filtered list
  const filteredTypes = useMemo(() => {
    return sampleTypes.filter(st => {
      const matchesSearch = !searchText ||
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
  const handlePageChange = useCallback((pageInfo) => {
    if (page !== pageInfo.page) {
      setPage(pageInfo.page);
    }
    if (pageSize !== pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  }, [page, pageSize]);

  // Domain counts
  const domainCounts = useMemo(() => {
    const counts = { CLINICAL: 0, ENVIRONMENTAL: 0, VECTOR: 0 };
    sampleTypes.forEach(st => counts[st.domain]++);
    return counts;
  }, [sampleTypes]);

  const envOrVectorCount = domainCounts.ENVIRONMENTAL + domainCounts.VECTOR;



  // Edit a sample type
  const openEditor = useCallback((st) => {
    setEditingType({
      id: st.id,
      name: st.name,
      description: st.description,
      active: st.active,
      domain: st.domain,
      testCount: st.testCount,
      whonet: st.whonet || '',
      // Load existing values for additional fields (if available from backend)
      abbreviation: st.abbreviation || '',
      sortOrder: st.sortOrder || 0,
      storageTemp: st.storageTemp || st.storageTemperature || '',
      containerType: st.containerType || '',
      // Default values for fields not yet implemented
      isDefault: false,
      maxStorageDays: '',
      processingInstructions: '',
      collectionMethod: '',
      requiredVolume: '',
      volumeUnit: 'ml'
    });
    setFormErrors({});
    setShowSuccess(false);
    setView('editor');
  }, []);

  // Add a new sample type
  const openAddForm = useCallback(() => {
    setEditingType({
      id: null,
      name: '',
      description: '',
      active: true,
      domain: 'CLINICAL',
      testCount: 0,
      whonet: '',
      // Additional fields for enhanced form
      abbreviation: '',
      sortOrder: sampleTypes.length + 1,
      isDefault: false,
      storageTemp: '',
      maxStorageDays: '',
      containerType: '',
      processingInstructions: '',
      collectionMethod: '',
      requiredVolume: '',
      volumeUnit: 'ml'
    });
    setFormErrors({});
    setShowSuccess(false);
    setView('add');
  }, [sampleTypes.length]);

  // Form validation
  const validateForm = useCallback((formData) => {
    const errors = {};

    // Required field validations
    if (!formData.name?.trim()) {
      errors.name = 'Sample type name is required';
    } else if (formData.name.trim().length < 2) {
      errors.name = 'Name must be at least 2 characters long';
    } else if (sampleTypes.some(st => st.name.toLowerCase() === formData.name.trim().toLowerCase() && st.id !== formData.id)) {
      errors.name = 'This sample type name already exists';
    }


    if (!formData.description?.trim()) {
      errors.description = 'Description is required';
    }

    if (!formData.domain) {
      errors.domain = 'Sample domain is required';
    }

    // Optional field validations
    if (formData.abbreviation && formData.abbreviation.length > 10) {
      errors.abbreviation = 'Abbreviation must be 10 characters or less';
    }

    if (formData.whonet && formData.whonet.length > 5) {
      errors.whonet = 'WHONET code must be 5 characters or less';
    }

    if (formData.maxStorageDays && (isNaN(formData.maxStorageDays) || parseInt(formData.maxStorageDays) < 0)) {
      errors.maxStorageDays = 'Max storage days must be a positive number';
    }

    if (formData.requiredVolume && (isNaN(formData.requiredVolume) || parseFloat(formData.requiredVolume) < 0)) {
      errors.requiredVolume = 'Required volume must be a positive number';
    }

    return errors;
  }, [sampleTypes]);

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
      if (view === 'add') {
        if (USE_SIMULATION_MODE) {
          // Simulation mode - for testing without authentication
          console.log('Simulating sample type creation:', editingType.name);
          await new Promise(resolve => setTimeout(resolve, 800)); // Simulate API delay

          const newSampleType = {
            id: Date.now(),
            name: editingType.name.trim(),
            description: editingType.description.trim(),
            domain: editingType.domain,
            active: true,
            testCount: 0,
            whonet: editingType.whonet?.trim() || null
          };

          // Add to the beginning of the list to show latest first
          setSampleTypes(prev => [newSampleType, ...prev]);
          setShowSuccess(true);
          setTimeout(() => setShowSuccess(false), 3000);
          console.log('Sample type created (simulation):', newSampleType.name);

          // Return to list view
          setTimeout(() => {
            setView('list');
            setEditingType(null);
            setFormErrors({});
          }, 1500);

        } else {
        // Creating new sample type using exact SampleTypeCreateForm format
        const sampleTypeData = {
          formName: "sampleTypeCreateForm",
          sampleTypeEnglishName: editingType.name.trim(),
          sampleTypeFrenchName: editingType.name.trim(), // Both are required fields
          domain: editingType.domain || 'CLINICAL' // Include domain selection
        };

        console.log('=== FRONTEND DOMAIN SUBMISSION DEBUG ===');
        console.log('Sample Name:', editingType.name.trim());
        console.log('Selected Domain:', editingType.domain);
        console.log('Sending to backend:', JSON.stringify(sampleTypeData, null, 2));
        console.log('=========================================');

        // Use utility function for POST request
        const creationResult = await new Promise((resolve, reject) => {
          postToOpenElisServerJsonResponse('/rest/SampleTypeCreate', JSON.stringify(sampleTypeData), (result) => {
            console.log('=== BACKEND RESPONSE DEBUG ===');
            console.log('Response from backend:', result);
            console.log('==============================');

            // Check for various error conditions
            if (result && result.error) {
              reject(new Error(result.message || result.error));
              return;
            }

            if (result && result.status && result.status !== 200) {
              reject(new Error(`Database save failed: ${result.message || 'Unknown error'}`));
              return;
            }

            // Success - database save confirmed
            console.log('✓ Sample type successfully saved to database:', editingType.name);
            resolve(result);
          });
        });

        console.log('Database persistence confirmed for:', editingType.name);

        // Create a new sample type entry for the UI
        const newSampleType = {
          id: Date.now(),
          name: editingType.name.trim(),
          description: editingType.description.trim(),
          domain: editingType.domain, // Preserve the original frontend domain selection
          active: true,
          testCount: 0,
          whonet: editingType.whonet?.trim() || null
        };

        // Add the new sample type to the frontend list immediately to preserve domain selection
        setSampleTypes(prev => [newSampleType, ...prev]);
        console.log('✓ Sample type added to frontend list with domain:', editingType.domain);

        // Simple verification that the sample was saved to database
        console.log('✓ Sample type successfully saved to database');

        setShowSuccess(true);
        setTimeout(() => setShowSuccess(false), 5000);
        console.log('Sample Type created and list refreshed from database');

        // Return to list view after a short delay to show success message
        setTimeout(() => {
          setView('list');
          setEditingType(null);
          setFormErrors({});
        }, 2000);
        }
      } else if (view === 'editor') {
        // Editing existing sample type
        if (USE_SIMULATION_MODE) {
          // Simulation mode - for testing without authentication
          console.log('Simulating sample type update:', editingType.name);
          await new Promise(resolve => setTimeout(resolve, 800));

          // Update the sample type in the list with ALL fields
          setSampleTypes(prev => prev.map(st =>
            st.id === editingType.id
              ? {
                  ...st,
                  name: editingType.name.trim(),
                  description: editingType.description.trim(),
                  domain: editingType.domain || st.domain,
                  active: editingType.active !== undefined ? editingType.active : st.active,
                  whonet: editingType.whonet || null,
                  abbreviation: editingType.abbreviation || '',
                  containerType: editingType.containerType || '',
                  storageTemp: editingType.storageTemp || '',
                  sortOrder: editingType.sortOrder || st.sortOrder || 0
                }
              : st
          ));

          console.log('Sample type updated (simulation):', editingType.name);
        } else {
          // Real database update - send ALL form fields for complete persistence
          const updateData = {
            id: editingType.id,
            name: editingType.name?.trim() || editingType.name,
            description: (editingType.description?.trim() || editingType.name?.trim()),
            domain: editingType.domain || 'CLINICAL',
            abbreviation: editingType.abbreviation?.trim() || '',
            whonetCode: editingType.whonet?.trim() || '',
            containerType: editingType.containerType?.trim() || '',
            storageTemperature: editingType.storageTemp?.trim() || '',
            isActive: editingType.active !== undefined ? editingType.active : true,
            sortOrder: editingType.sortOrder || 0
          };

          console.log('=== FRONTEND UPDATE DEBUG ===');
          console.log('Original sample type (editingType):', editingType);
          console.log('Sending update data:', JSON.stringify(updateData, null, 2));
          console.log('Field by field check:');
          console.log('- ID:', updateData.id);
          console.log('- Name:', `"${updateData.name}" (length: ${updateData.name ? updateData.name.length : 0})`);
          console.log('- Description:', `"${updateData.description}" (length: ${updateData.description ? updateData.description.length : 0})`);
          console.log('- Abbreviation:', `"${updateData.abbreviation}" (length: ${updateData.abbreviation ? updateData.abbreviation.length : 0})`);
          console.log('- WHONET:', `"${updateData.whonetCode}" (length: ${updateData.whonetCode ? updateData.whonetCode.length : 0})`);
          console.log('- Container Type:', `"${updateData.containerType}" (length: ${updateData.containerType ? updateData.containerType.length : 0})`);
          console.log('- Storage Temp:', `"${updateData.storageTemperature}" (length: ${updateData.storageTemperature ? updateData.storageTemperature.length : 0})`);
          console.log('- Domain:', updateData.domain);
          console.log('- Active:', updateData.isActive);
          console.log('=============================');

          const updateResult = await new Promise((resolve, reject) => {
            postToOpenElisServerJsonResponse('/rest/sample-types/update', JSON.stringify(updateData), (result) => {
              console.log('=== BACKEND RESPONSE DEBUG ===');
              console.log('Response from backend:', result);
              console.log('Response type:', typeof result);
              console.log('Response keys:', result ? Object.keys(result) : 'null');
              console.log('==============================');

              if (result && result.error) {
                console.error('Backend error response:', result.error, result.message);
                reject(new Error(result.message || result.error));
                return;
              }

              if (result && result.success) {
                console.log('✅ Backend claims success:', result);
                console.log('Backend response data:', result.data);
                resolve(result.data);
              } else if (result && result.success === false) {
                console.error('Backend failure response:', result.message);
                reject(new Error('Update failed: ' + (result.message || 'Unknown error')));
              } else {
                console.error('Unexpected backend response format:', result);
                console.error('Raw response:', JSON.stringify(result, null, 2));
                reject(new Error('Update failed: Unexpected response format - ' + JSON.stringify(result)));
              }
            });
          });

          // Note: Frontend state update now happens after server verification (below)

          console.log('Backend claims sample type updated successfully');

          // VERIFICATION: Refresh data from server to confirm persistence
          console.log('🔄 VERIFYING: Refreshing data from server to confirm persistence...');

          try {
            // Refetch data from server to verify the changes were actually saved
            await new Promise((resolve, reject) => {
              getFromOpenElisServer('/rest/sample-types', (response) => {
                console.log('✅ VERIFICATION: Data refetched from server:', response);

                if (response && response.success && Array.isArray(response.data)) {
                  const updatedSampleTypes = response.data.map((item, index) => ({
                    id: item.id || (index + 1),
                    name: item.name || item.description || 'Unknown Sample Type',
                    description: item.description || item.name || 'Sample type from database',
                    domain: item.domain || 'CLINICAL',
                    active: item.isActive !== undefined ? item.isActive : true,
                    testCount: item.testCount || 0,
                    whonet: item.whonetCode || null,
                    abbreviation: item.abbreviation || '',
                    containerType: item.containerType || '',
                    storageTemp: item.storageTemperature || ''
                  }));

                  // Update state with verified server data
                  setSampleTypes(updatedSampleTypes);
                  console.log('✅ VERIFICATION: Frontend state updated with verified server data');
                  resolve();
                } else {
                  console.error('❌ VERIFICATION: Failed to refetch data from server');
                  reject(new Error('Failed to verify data persistence'));
                }
              });
            });

          } catch (verificationError) {
            console.error('❌ VERIFICATION ERROR:', verificationError);
            alert('WARNING: Update may not have been saved. Changes will be lost on refresh.');
          }
        }

        // Return to list view and show success
        setShowEditSuccess(true);
        setTimeout(() => setShowEditSuccess(false), 3000);
        console.log('Sample Type update process completed');

        // Return to list view after a short delay to show success message
        setTimeout(() => {
          setView('list');
          setEditingType(null);
          setFormErrors({});
        }, 2000);
      }
    } catch (error) {
      console.error('Error saving sample type:', error);
      console.error('Error details:', error.message);
      console.error('Error stack:', error.stack);

      const operation = view === 'add' ? 'create' : 'update';
      const errorMessage = `Failed to ${operation} sample type: ${error.message}`;

      setFormErrors({ submit: errorMessage });

      // Also show alert for immediate feedback
      alert(`ERROR: ${errorMessage}\n\nPlease check browser console for more details.`);

      console.error('=== EDIT ERROR DEBUG ===');
      console.error('View:', view);
      console.error('EditingType ID:', editingType?.id);
      console.error('Full error:', error);
      console.error('======================');
    } finally {
      setIsSubmitting(false);
    }
  }, [editingType, view, validateForm]);

  // ─── LIST VIEW ────────────────────────────────────────────────
  if (view === 'list') {
    return (
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Stack gap={5}>
          {/* Loading State */}
          {isLoading && (
            <div style={{
              display: 'flex',
              justifyContent: 'center',
              padding: 'var(--cds-spacing-07)',
              alignItems: 'center',
              gap: 'var(--cds-spacing-03)'
            }}>
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
          <Tile style={{ padding: 'var(--cds-spacing-06)' }}>
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <h2 style={{
                  margin: '0 0 var(--cds-spacing-03) 0',
                  color: 'var(--cds-text-primary)',
                  fontWeight: 600
                }}>
                  <FormattedMessage id="heading.sampleType.management" defaultMessage="Sample Type Management" />
                </h2>
                <p style={{
                  fontSize: '14px',
                  color: 'var(--cds-text-secondary)',
                  margin: '0',
                  lineHeight: 1.4
                }}>
                  <FormattedMessage
                    id="heading.sampleType.subtitle"
                    defaultMessage="Configure sample types, display order, test associations, and domain classification."
                  />
                </p>
                {!isLoading && (
                  <p style={{
                    fontSize: '12px',
                    color: 'var(--cds-text-secondary)',
                    margin: 'var(--cds-spacing-02) 0 0 0',
                    fontWeight: 500
                  }}>
                    {searchText || domainFilter ? (
                      <FormattedMessage
                        id="heading.sampleType.filtered"
                        defaultMessage="Showing {filtered} of {total} sample types"
                        values={{
                          filtered: filteredTypes.length,
                          total: sampleTypes.length
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
              <Column lg={8} md={4} sm={4} style={{ textAlign: 'right' }}>
                <Stack orientation="horizontal" gap={4} style={{ justifyContent: 'flex-end', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--cds-spacing-02)' }}>
                    <Tag type="green" size="md">{domainCounts.CLINICAL}</Tag>
                    <span style={{ fontSize: '14px', fontWeight: 500 }}>
                      <FormattedMessage id="label.sampleType.domain.clinical" defaultMessage="Clinical" />
                    </span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--cds-spacing-02)' }}>
                    <Tag type="purple" size="md">{domainCounts.ENVIRONMENTAL}</Tag>
                    <span style={{ fontSize: '14px', fontWeight: 500 }}>
                      <FormattedMessage id="label.sampleType.domain.environmental" defaultMessage="Environmental" />
                    </span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--cds-spacing-02)' }}>
                    <Tag type="teal" size="md">{domainCounts.VECTOR}</Tag>
                    <span style={{ fontSize: '14px', fontWeight: 500 }}>
                      <FormattedMessage id="label.sampleType.domain.vector" defaultMessage="Vector" />
                    </span>
                  </div>
                </Stack>
              </Column>
            </Grid>
          </Tile>

          {/* Sample Type Table */}
          <TableContainer style={{ marginBottom: 0 }}>
            {/* Enhanced Toolbar */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0 var(--cds-spacing-05)',
              height: '48px',
              background: 'var(--cds-layer)',
              borderBottom: '1px solid var(--cds-border-subtle-01)',
            }}>
              <TextInput
                id="sample-type-search"
                labelText={intl.formatMessage({
                  id: 'placeholder.sampleType.search',
                  defaultMessage: 'Search sample types...'
                })}
                hideLabel
                placeholder={intl.formatMessage({
                  id: 'placeholder.sampleType.search',
                  defaultMessage: 'Search sample types...'
                })}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                size="sm"
                style={{
                  flex: '0 0 280px',
                  marginRight: 'var(--cds-spacing-05)'
                }}
              />
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '80px'
              }}>
                <Select
                  id="domain-filter"
                  labelText={intl.formatMessage({
                    id: 'label.sampleType.filterDomain',
                    defaultMessage: 'Filter by domain'
                  })}
                  hideLabel
                  value={domainFilter}
                  onChange={(e) => setDomainFilter(e.target.value)}
                  style={{
                    flex: '0 0 200px'
                  }}
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: 'placeholder.sampleType.filter.domain',
                      defaultMessage: 'All domains'
                    })}
                  />
                  {DOMAIN_OPTIONS.map(opt => (
                    <SelectItem
                      key={opt.value}
                      value={opt.value}
                      text={intl.formatMessage({
                        id: `label.sampleType.domain.${opt.value.toLowerCase()}`,
                        defaultMessage: opt.label
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
                    whiteSpace: 'nowrap',
                    flex: '0 0 auto'
                  }}
                >
                  <FormattedMessage id="button.sampleType.add" defaultMessage="Add Sample Type" />
                </Button>
              </div>
            </div>
            <Table>
              <TableHead>
                <TableRow>
                  <TableHeader>
                    <FormattedMessage id="label.sampleType.name" defaultMessage="Name" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.sampleType.domain" defaultMessage="Domain" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.sampleType.status" defaultMessage="Status" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.sampleType.testCount" defaultMessage="Tests" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.sampleType.whonet" defaultMessage="WHONET" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.sampleType.actions" defaultMessage="Actions" />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {paginatedTypes.length > 0 ? (
                  paginatedTypes.map(st => (
                    <TableRow key={st.id}>
                      <TableCell>
                        <div>
                          <span style={{
                            fontWeight: 600,
                            color: 'var(--cds-text-primary)',
                            fontSize: '14px'
                          }}>
                            {st.name}
                          </span>
                          <br />
                          <span style={{
                            fontSize: '12px',
                            color: 'var(--cds-text-secondary)',
                            lineHeight: 1.3,
                            marginTop: 'var(--cds-spacing-01)'
                          }}>
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
                        <Tag type={st.active ? 'green' : 'gray'} size="sm">
                          {st.active ? (
                            <FormattedMessage id="label.active" defaultMessage="Active" />
                          ) : (
                            <FormattedMessage id="label.inactive" defaultMessage="Inactive" />
                          )}
                        </Tag>
                      </TableCell>
                      <TableCell>
                        <span style={{ fontWeight: 500, color: 'var(--cds-text-primary)' }}>
                          {st.testCount}
                        </span>
                      </TableCell>
                      <TableCell>
                        {st.whonet ? (
                          <span style={{ fontWeight: 500, color: 'var(--cds-text-primary)' }}>
                            {st.whonet}
                          </span>
                        ) : (
                          <span style={{ color: 'var(--cds-text-disabled)' }}>—</span>
                        )}
                      </TableCell>
                      <TableCell>
                        <Button
                          kind="ghost"
                          size="sm"
                          renderIcon={Edit}
                          onClick={() => openEditor(st)}
                        >
                          <FormattedMessage id="button.edit" defaultMessage="Edit" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={6} style={{ textAlign: 'center', padding: 'var(--cds-spacing-07)' }}>
                      <div style={{ color: 'var(--cds-text-secondary)' }}>
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
          <div style={{ overflowX: 'auto' }}>
            <Pagination
              onChange={handlePageChange}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 30, 50, 100]}
              totalItems={filteredTypes.length}
              forwardText={intl.formatMessage({ id: "pagination.forward" })}
              backwardText={intl.formatMessage({ id: "pagination.backward" })}
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
  if (view === 'editor' || view === 'add') {
    return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Stack gap={5}>
        {/* Editor Header */}
        <Tile style={{ padding: 'var(--cds-spacing-06)' }}>
          <Stack orientation="horizontal" gap={5} style={{ alignItems: 'center' }}>
            <Button
              kind="ghost"
              size="sm"
              onClick={() => { setView('list'); setEditingType(null); }}
              style={{ minWidth: 'auto' }}
            >
              <FormattedMessage id="button.back" defaultMessage="← Back to List" />
            </Button>
            <div style={{ borderLeft: '1px solid var(--cds-border-subtle)', height: '24px' }} />
            <h3 style={{
              margin: 0,
              color: 'var(--cds-text-primary)',
              fontWeight: 600
            }}>
              {view === 'add' ? (
                <FormattedMessage id="heading.sampleType.add" defaultMessage="Add New Sample Type" />
              ) : (
                editingType?.name
              )}
            </h3>
            {view === 'editor' && editingType?.domain && (
              <Tag type={DOMAIN_TAG_KIND[editingType?.domain]} size="md">
                <FormattedMessage
                  id={`label.sampleType.domain.${editingType?.domain?.toLowerCase()}`}
                  defaultMessage={editingType?.domain}
                />
              </Tag>
            )}
            {view === 'editor' && (
              editingType?.active ? (
                <Tag type="green" size="md">
                  <FormattedMessage id="label.active" defaultMessage="Active" />
                </Tag>
              ) : (
                <Tag type="gray" size="md">
                  <FormattedMessage id="label.inactive" defaultMessage="Inactive" />
                </Tag>
              )
            )}
          </Stack>
        </Tile>

        {/* 5-Tab Editor (OGC-296 structure) */}
        <Tabs>
          <TabList>
            <Tab>
              <FormattedMessage id="tab.sampleType.basicInfo" defaultMessage="Basic Info" />
            </Tab>
            <Tab>
              <FormattedMessage id="tab.sampleType.displayOrder" defaultMessage="Display Order" />
            </Tab>
            <Tab>
              <FormattedMessage id="tab.sampleType.tests" defaultMessage="Associated Tests" />
            </Tab>
            <Tab>
              <FormattedMessage id="tab.sampleType.storage" defaultMessage="Storage & Disposal" />
            </Tab>
            <Tab>
              <FormattedMessage id="tab.sampleType.whonet" defaultMessage="WHONET Mapping" />
            </Tab>
          </TabList>
          <TabPanels>
            {/* ── Basic Info Tab (STD-2-001 — Domain field added) ── */}
            <TabPanel>
              {view === 'add' && (
                <div style={{ marginBottom: 'var(--cds-spacing-06)' }}>
                  <Stack gap={5}>
                    {showSuccess && (
                      <InlineNotification
                        kind="success"
                        title=""
                        subtitle={intl.formatMessage({
                          id: 'message.sampleType.add.success',
                          defaultMessage: 'Sample type created and saved to database successfully! The list has been refreshed to show your new sample type.'
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
                        onCloseButtonClick={() => setFormErrors(prev => ({ ...prev, submit: '' }))}
                      />
                    )}
                  </Stack>
                </div>
              )}
              <Tile style={{
                padding: 'var(--cds-spacing-07)',
                border: '1px solid var(--cds-border-subtle)',
                borderRadius: 'var(--cds-border-radius)'
              }}>
                <Grid>
                  <Column lg={12} md={8} sm={4}>
                    <Stack gap={6}>
                      {/* Additional spacing above the Name field */}
                      <div style={{ marginBottom: 'var(--cds-spacing-03)' }} />
                      <TextInput
                        ref={nameInputRef}
                        id="st-name"
                        labelText={
                          <>
                            <FormattedMessage id="label.sampleType.name" defaultMessage="Name" />
                            <span style={{ color: 'var(--cds-support-error)' }}> *</span>
                          </>
                        }
                        value={editingType?.name || ''}
                        onChange={(e) => {
                          setEditingType(prev => ({ ...prev, name: e.target.value }));
                          if (formErrors.name) {
                            setFormErrors(prev => ({ ...prev, name: '' }));
                          }
                        }}
                        invalid={!!formErrors.name}
                        invalidText={formErrors.name}
                        helperText={intl.formatMessage({
                          id: 'helper.sampleType.name',
                          defaultMessage: 'Enter a unique name for this sample type (e.g., "Serum", "Whole Blood")'
                        })}
                        autoComplete="off"
                      />

                      <Select
                        id="st-domain"
                        labelText={
                          <>
                            <FormattedMessage id="label.sampleType.domain" defaultMessage="Sample Domain" />
                            <span style={{ color: 'var(--cds-support-error)' }}> *</span>
                          </>
                        }
                        value={editingType?.domain || 'CLINICAL'}
                        onChange={(e) => setEditingType(prev => ({ ...prev, domain: e.target.value }))}
                        helperText={intl.formatMessage({
                          id: 'label.sampleType.domain.helper',
                          defaultMessage: 'Determines which workflow mode (Clinical or Environmental) this sample type appears in.'
                        })}
                      >
                        {DOMAIN_OPTIONS.map(opt => (
                          <SelectItem
                            key={opt.value}
                            value={opt.value}
                            text={intl.formatMessage({
                              id: `label.sampleType.domain.${opt.value.toLowerCase()}`,
                              defaultMessage: opt.label
                            })}
                          />
                        ))}
                      </Select>

                      <Toggle
                        id="st-active"
                        labelText={intl.formatMessage({
                          id: 'label.sampleType.active',
                          defaultMessage: 'Active'
                        })}
                        labelA={intl.formatMessage({
                          id: 'label.inactive',
                          defaultMessage: 'Inactive'
                        })}
                        labelB={intl.formatMessage({
                          id: 'label.active',
                          defaultMessage: 'Active'
                        })}
                        toggled={editingType?.active}
                        onToggle={(checked) => setEditingType(prev => ({ ...prev, active: checked }))}
                      />

                      <TextArea
                        id="st-description"
                        labelText={
                          <>
                            <FormattedMessage id="label.sampleType.description" defaultMessage="Description" />
                            <span style={{ color: 'var(--cds-support-error)' }}> *</span>
                          </>
                        }
                        value={editingType?.description || ''}
                        onChange={(e) => {
                          setEditingType(prev => ({ ...prev, description: e.target.value }));
                          if (formErrors.description) {
                            setFormErrors(prev => ({ ...prev, description: '' }));
                          }
                        }}
                        rows={4}
                        invalid={!!formErrors.description}
                        invalidText={formErrors.description}
                        helperText={intl.formatMessage({
                          id: 'helper.sampleType.description',
                          defaultMessage: 'Provide a description of this sample type for lab staff reference'
                        })}
                      />

                      {/* Additional Basic Fields Section */}
                      <div style={{
                        borderTop: '1px solid var(--cds-border-subtle-01)',
                        paddingTop: 'var(--cds-spacing-06)',
                        marginTop: 'var(--cds-spacing-07)'
                      }}>
                        <div style={{ marginBottom: 'var(--cds-spacing-05)' }}>
                          <h5 style={{
                            fontSize: '14px',
                            fontWeight: 600,
                            margin: '0',
                            color: 'var(--cds-text-secondary)',
                            textTransform: 'uppercase',
                            letterSpacing: '0.5px'
                          }}>
                            <FormattedMessage id="section.sampleType.additional" defaultMessage="Additional Information" />
                          </h5>
                        </div>
                        <Grid>
                          <Column lg={6} md={4} sm={4}>
                            <TextInput
                              id="st-abbreviation"
                              labelText={intl.formatMessage({
                                id: 'label.sampleType.abbreviation',
                                defaultMessage: 'Abbreviation'
                              })}
                              value={editingType?.abbreviation || ''}
                              onChange={(e) => {
                                setEditingType(prev => ({ ...prev, abbreviation: e.target.value }));
                                if (formErrors.abbreviation) {
                                  setFormErrors(prev => ({ ...prev, abbreviation: '' }));
                                }
                              }}
                              invalid={!!formErrors.abbreviation}
                              invalidText={formErrors.abbreviation}
                              helperText={intl.formatMessage({
                                id: 'helper.sampleType.abbreviation',
                                defaultMessage: 'Optional short code (max 10 characters)'
                              })}
                              maxLength={10}
                            />
                          </Column>
                          <Column lg={6} md={4} sm={4}>
                            <TextInput
                              id="st-whonet"
                              labelText={intl.formatMessage({
                                id: 'label.sampleType.whonet',
                                defaultMessage: 'WHONET Code'
                              })}
                              value={editingType?.whonet || ''}
                              onChange={(e) => {
                                setEditingType(prev => ({ ...prev, whonet: e.target.value }));
                                if (formErrors.whonet) {
                                  setFormErrors(prev => ({ ...prev, whonet: '' }));
                                }
                              }}
                              invalid={!!formErrors.whonet}
                              invalidText={formErrors.whonet}
                              helperText={intl.formatMessage({
                                id: 'helper.sampleType.whonet',
                                defaultMessage: 'WHONET specimen code (max 5 chars)'
                              })}
                              maxLength={5}
                            />
                          </Column>
                        </Grid>
                      </div>

                      {/* Collection Information Section */}
                      <div style={{
                        borderTop: '1px solid var(--cds-border-subtle-01)',
                        paddingTop: 'var(--cds-spacing-06)',
                        marginTop: 'var(--cds-spacing-07)'
                      }}>
                        <div style={{ marginBottom: 'var(--cds-spacing-05)' }}>
                          <h5 style={{
                            fontSize: '14px',
                            fontWeight: 600,
                            margin: '0',
                            color: 'var(--cds-text-secondary)',
                            textTransform: 'uppercase',
                            letterSpacing: '0.5px'
                          }}>
                            <FormattedMessage id="section.sampleType.collection" defaultMessage="Collection Information (Optional)" />
                          </h5>
                        </div>

                        <Stack gap={5}>
                          <Grid>
                            <Column lg={6} md={4} sm={4}>
                              <TextInput
                                id="st-container-type"
                                labelText={intl.formatMessage({
                                  id: 'label.sampleType.containerType',
                                  defaultMessage: 'Container Type'
                                })}
                                value={editingType?.containerType || ''}
                                onChange={(e) => setEditingType(prev => ({ ...prev, containerType: e.target.value }))}
                                placeholder="e.g., EDTA tube, Red top"
                                helperText={intl.formatMessage({
                                  id: 'helper.sampleType.containerType',
                                  defaultMessage: 'Recommended collection container'
                                })}
                              />
                            </Column>
                            <Column lg={6} md={4} sm={4}>
                              <TextInput
                                id="st-storage-temp"
                                labelText={intl.formatMessage({
                                  id: 'label.sampleType.storageTemp',
                                  defaultMessage: 'Storage Temperature'
                                })}
                                value={editingType?.storageTemp || ''}
                                onChange={(e) => setEditingType(prev => ({ ...prev, storageTemp: e.target.value }))}
                                placeholder="e.g., 2-8°C, -20°C"
                                helperText={intl.formatMessage({
                                  id: 'helper.sampleType.storageTemp',
                                  defaultMessage: 'Storage temperature requirements'
                                })}
                              />
                            </Column>
                          </Grid>
                        </Stack>

                        {/* Additional spacing after Collection Information */}
                        <div style={{ marginBottom: 'var(--cds-spacing-13)' }} />
                      </div>
                    </Stack>
                  </Column>
                </Grid>

                <div style={{
                  borderTop: '1px solid var(--cds-border-subtle-01)',
                  marginTop: view === 'add' ? '3rem' : 'var(--cds-spacing-08)',
                  paddingTop: 'var(--cds-spacing-10)'
                }}>
                  <Stack orientation="horizontal" gap={4}>
                    <Button
                      kind="primary"
                      size="sm"
                      renderIcon={isSubmitting ? undefined : Save}
                      onClick={saveEditor}
                      disabled={isSubmitting || !!Object.keys(formErrors).length || !editingType?.name?.trim() || !editingType?.description?.trim()}
                    >
                      {isSubmitting ? (
                        <>
                          <Loading style={{ marginRight: '8px' }} />
                          {view === 'add' ? (
                            <FormattedMessage id="button.sampleType.creating" defaultMessage="Creating..." />
                          ) : (
                            <FormattedMessage id="button.saving" defaultMessage="Saving..." />
                          )}
                        </>
                      ) : view === 'add' ? (
                        <FormattedMessage id="button.sampleType.create" defaultMessage="Create Sample Type" />
                      ) : (
                        <FormattedMessage id="button.save" defaultMessage="Save Changes" />
                      )}
                    </Button>
                    <Button
                      kind="ghost"
                      size="sm"
                      onClick={() => { setView('list'); setEditingType(null); }}
                    >
                      <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
                    </Button>
                  </Stack>
                </div>
              </Tile>
            </TabPanel>

            {/* Other tabs — placeholder content (already defined by OGC-296) */}
            <TabPanel>
              <Tile style={{
                padding: 'var(--cds-spacing-06)',
                border: '1px solid var(--cds-border-subtle)',
                borderRadius: 'var(--cds-border-radius)',
                textAlign: 'center'
              }}>
                <div style={{ padding: 'var(--cds-spacing-07)' }}>
                  <h5 style={{
                    color: 'var(--cds-text-secondary)',
                    fontWeight: 400,
                    margin: '0 0 var(--cds-spacing-03) 0'
                  }}>
                    <FormattedMessage id="tab.sampleType.displayOrder" defaultMessage="Display Order" />
                  </h5>
                  <p style={{
                    color: 'var(--cds-text-secondary)',
                    fontSize: '14px',
                    lineHeight: 1.4,
                    margin: 0
                  }}>
                    <FormattedMessage
                      id="label.sampleType.displayOrder.placeholder"
                      defaultMessage="Display Order — Drag-and-drop reordering for dropdown appearance. See OGC-296 for full specification."
                    />
                  </p>
                </div>
              </Tile>
            </TabPanel>
            <TabPanel>
              <Tile style={{
                padding: 'var(--cds-spacing-06)',
                border: '1px solid var(--cds-border-subtle)',
                borderRadius: 'var(--cds-border-radius)',
                textAlign: 'center'
              }}>
                <div style={{ padding: 'var(--cds-spacing-07)' }}>
                  <h5 style={{
                    color: 'var(--cds-text-secondary)',
                    fontWeight: 400,
                    margin: '0 0 var(--cds-spacing-03) 0'
                  }}>
                    <FormattedMessage id="tab.sampleType.tests" defaultMessage="Associated Tests" />
                  </h5>
                  <p style={{
                    color: 'var(--cds-text-secondary)',
                    fontSize: '14px',
                    lineHeight: 1.4,
                    margin: 0
                  }}>
                    <FormattedMessage
                      id="label.sampleType.tests.placeholder"
                      defaultMessage="Associated Tests — Bidirectional test association management. See OGC-296 for full specification."
                    />
                  </p>
                </div>
              </Tile>
            </TabPanel>
            <TabPanel>
              <Tile style={{
                padding: 'var(--cds-spacing-06)',
                border: '1px solid var(--cds-border-subtle)',
                borderRadius: 'var(--cds-border-radius)',
                textAlign: 'center'
              }}>
                <div style={{ padding: 'var(--cds-spacing-07)' }}>
                  <h5 style={{
                    color: 'var(--cds-text-secondary)',
                    fontWeight: 400,
                    margin: '0 0 var(--cds-spacing-03) 0'
                  }}>
                    <FormattedMessage id="tab.sampleType.storage" defaultMessage="Storage & Disposal" />
                  </h5>
                  <p style={{
                    color: 'var(--cds-text-secondary)',
                    fontSize: '14px',
                    lineHeight: 1.4,
                    margin: 0
                  }}>
                    <FormattedMessage
                      id="label.sampleType.storage.placeholder"
                      defaultMessage="Storage & Disposal — Default storage conditions, duration, disposal method. See OGC-296 for full specification."
                    />
                  </p>
                </div>
              </Tile>
            </TabPanel>
            <TabPanel>
              <Tile style={{
                padding: 'var(--cds-spacing-06)',
                border: '1px solid var(--cds-border-subtle)',
                borderRadius: 'var(--cds-border-radius)',
                textAlign: 'center'
              }}>
                <div style={{ padding: 'var(--cds-spacing-07)' }}>
                  <h5 style={{
                    color: 'var(--cds-text-secondary)',
                    fontWeight: 400,
                    margin: '0 0 var(--cds-spacing-03) 0'
                  }}>
                    <FormattedMessage id="tab.sampleType.whonet" defaultMessage="WHONET Mapping" />
                  </h5>
                  <p style={{
                    color: 'var(--cds-text-secondary)',
                    fontSize: '14px',
                    lineHeight: 1.4,
                    margin: 0
                  }}>
                    <FormattedMessage
                      id="label.sampleType.whonet.placeholder"
                      defaultMessage="WHONET Mapping — Map to WHONET specimen codes for AMR surveillance exports. See OGC-296 for full specification."
                    />
                  </p>
                </div>
              </Tile>
            </TabPanel>
          </TabPanels>
        </Tabs>
      </Stack>
    </div>
    );
  }

  // Fallback (shouldn't reach here)
  return null;
}

export default injectIntl(SampleTypeManagement);