import React, { useContext, useState, useRef, useEffect } from "react";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  Toggle,
  Button,
  FileUploader,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableContainer,
  InlineNotification,
  ProgressBar,
} from "@carbon/react";
import { postToOpenElisServerFormData } from "../../utils/Utils";
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
    label: "eqa.results.batch.import",
    link: "/MasterListsPage/SampleTypeBulkImport",
  },
];

function SampleTypeBulkImport() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [isLoading, setIsLoading] = useState(false);
  const [showGuide, setShowGuide] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewData, setPreviewData] = useState([]);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [importErrors, setImportErrors] = useState([]);
  const [importSuccess, setImportSuccess] = useState(null);

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      if (file.type !== "text/csv" && !file.name.endsWith('.csv')) {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: "Please select a CSV file.",
        });
        setNotificationVisible(true);
        return;
      }

      setSelectedFile(file);
      parseCSVPreview(file);
    }
  };

  const parseCSVPreview = (file) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const csv = e.target.result;
        const lines = csv.split('\n').filter(line => line.trim());

        if (lines.length === 0) {
          setPreviewData([]);
          return;
        }

        // Parse header and first few rows for preview
        const headers = lines[0].split(',').map(h => h.trim().replace(/"/g, ''));
        const rows = lines.slice(1, Math.min(6, lines.length)).map((line, index) => {
          const values = line.split(',').map(v => v.trim().replace(/"/g, ''));
          return {
            id: `preview-${index}`,
            name: values[0] || '',
            description: values[1] || '',
            displayOrder: values[2] || '',
            whonetCode: values[3] || '',
            storageDefaults: values[4] || '',
            isActive: values[5] || 'true'
          };
        });

        setPreviewData(rows);
      } catch (error) {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: "Error parsing CSV file. Please check the format.",
        });
        setNotificationVisible(true);
        setPreviewData([]);
      }
    };
    reader.readAsText(file);
  };

  const handleImport = () => {
    if (!selectedFile) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Please select a CSV file to import.",
      });
      setNotificationVisible(true);
      return;
    }

    setIsUploading(true);
    setUploadProgress(0);
    setImportErrors([]);
    setImportSuccess(null);

    const formData = new FormData();
    formData.append('csvFile', selectedFile);

    // Simulate progress updates
    const progressInterval = setInterval(() => {
      setUploadProgress(prev => {
        if (prev >= 90) {
          clearInterval(progressInterval);
          return prev;
        }
        return prev + 10;
      });
    }, 200);

    postToOpenElisServerFormData(
      `/rest/SampleTypeManagement/bulkImport`,
      formData,
      (res) => {
        clearInterval(progressInterval);
        setUploadProgress(100);

        if (res && res.success) {
          setImportSuccess({
            imported: res.importedCount || 0,
            skipped: res.skippedCount || 0,
            errors: res.errorCount || 0
          });

          if (res.errors && res.errors.length > 0) {
            setImportErrors(res.errors);
          }

          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: `Import completed. ${res.importedCount || 0} sample types imported successfully.`,
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);

          // Reset form
          setTimeout(() => {
            setSelectedFile(null);
            setPreviewData([]);
            setUploadProgress(0);
          }, 2000);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: res?.message || "Import failed. Please check your file format.",
          });
          setNotificationVisible(true);
        }
        setIsUploading(false);
      }
    );
  };

  const handleCancel = () => {
    setSelectedFile(null);
    setPreviewData([]);
    setImportErrors([]);
    setImportSuccess(null);
    setUploadProgress(0);
  };

  const downloadTemplate = () => {
    const csvContent = "Name,Description,Display Order,WHONET Code,Storage Defaults,Active\n" +
                      "Serum,Blood serum sample,1,SER,Refrigerated 2-8°C,true\n" +
                      "Urine,Urine sample,2,URN,Room temperature,true\n" +
                      "Stool,Stool sample,3,STL,Refrigerated 2-8°C,false";

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'sample_types_template.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const rows = [
    {
      id: "csvFile",
      field: intl.formatMessage({ id: "eqa.results.import.upload" }),
      description: "Upload a CSV file with sample type data. Required columns: Name, Description, Display Order, WHONET Code, Storage Defaults, Active.",
    },
    {
      id: "preview",
      field: "Preview",
      description: "Review the data before importing. The first 5 rows will be shown for verification.",
    },
  ];

  const headers = [
    { key: "name", header: intl.formatMessage({ id: "sample.type.name" }) },
    { key: "description", header: intl.formatMessage({ id: "sample.type.description" }) },
    { key: "displayOrder", header: intl.formatMessage({ id: "sample.type.display.order" }) },
    { key: "whonetCode", header: intl.formatMessage({ id: "sample.type.whonet.code" }) },
    { key: "storageDefaults", header: intl.formatMessage({ id: "sample.type.storage.defaults" }) },
    { key: "isActive", header: intl.formatMessage({ id: "label.status.active" }) },
  ];

  useEffect(() => {
    componentMounted.current = true;
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
                  <FormattedMessage id="eqa.results.batch.import" />
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

          {/* File Upload Section */}
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section style={{
                backgroundColor: "#ffffff",
                padding: "2rem",
                borderRadius: "12px",
                border: "1px solid #e0e0e0",
                boxShadow: "0 4px 8px rgba(0, 0, 0, 0.08)"
              }}>
                <div style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.75rem",
                  marginBottom: "1.5rem"
                }}>
                  <div style={{
                    backgroundColor: "#0066cc",
                    color: "white",
                    borderRadius: "50%",
                    width: "28px",
                    height: "28px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "14px",
                    fontWeight: "bold"
                  }}>
                    📂
                  </div>
                  <div>
                    <Heading size="md" style={{ margin: 0, color: "#161616" }}>
                      Bulk Import Sample Types
                    </Heading>
                    <p style={{
                      margin: "0.25rem 0 0 0",
                      fontSize: "0.875rem",
                      color: "#6f6f6f"
                    }}>
                      Upload a CSV file to import multiple sample types at once
                    </p>
                  </div>
                </div>

                <div style={{
                  backgroundColor: "#f0f8ff",
                  padding: "1.25rem",
                  borderRadius: "8px",
                  border: "1px solid #d0e2ff",
                  marginBottom: "1.5rem"
                }}>
                  <div style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    marginBottom: "1rem"
                  }}>
                    <span style={{
                      backgroundColor: "#0066cc",
                      color: "white",
                      borderRadius: "50%",
                      width: "16px",
                      height: "16px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "10px",
                      fontWeight: "bold"
                    }}>
                      1
                    </span>
                    <strong style={{ color: "#0066cc", fontSize: "0.875rem" }}>
                      Download Template
                    </strong>
                  </div>
                  <Button
                    kind="secondary"
                    size="sm"
                    onClick={downloadTemplate}
                    style={{ marginBottom: "0.5rem" }}
                  >
                    📥 Download CSV Template
                  </Button>
                  <p style={{
                    margin: 0,
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontStyle: "italic"
                  }}>
                    Required columns: Name, Description, Display Order, WHONET Code, Storage Defaults, Active
                  </p>
                </div>

                <div style={{
                  backgroundColor: "#f8f9fa",
                  padding: "1.25rem",
                  borderRadius: "8px",
                  border: "1px solid #e0e0e0"
                }}>
                  <div style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    marginBottom: "1rem"
                  }}>
                    <span style={{
                      backgroundColor: "#0066cc",
                      color: "white",
                      borderRadius: "50%",
                      width: "16px",
                      height: "16px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "10px",
                      fontWeight: "bold"
                    }}>
                      2
                    </span>
                    <strong style={{ color: "#161616", fontSize: "0.875rem" }}>
                      Upload Your CSV File
                    </strong>
                  </div>
                  <FileUploader
                    labelTitle="Select CSV File"
                    labelDescription="Choose a CSV file containing sample type data"
                    buttonLabel="📎 Choose file"
                    filenameStatus="edit"
                    accept={['.csv']}
                    multiple={false}
                    onChange={handleFileChange}
                    disabled={isUploading}
                  />
                </div>
              </Section>
            </Column>
          </Grid>

          {/* Progress Section */}
          {isUploading && (
            <Grid fullWidth={true} style={{ marginTop: "2rem" }}>
              <Column lg={16} md={8} sm={4}>
                <Section style={{
                  backgroundColor: "#fff8e1",
                  padding: "1.5rem",
                  borderRadius: "8px",
                  border: "1px solid #fed7aa"
                }}>
                  <div style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.75rem",
                    marginBottom: "1rem"
                  }}>
                    <div style={{
                      backgroundColor: "#f59e0b",
                      color: "white",
                      borderRadius: "50%",
                      width: "24px",
                      height: "24px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "12px",
                      fontWeight: "bold"
                    }}>
                      ⏳
                    </div>
                    <Heading size="sm" style={{ margin: 0, color: "#d97706" }}>
                      Import in Progress
                    </Heading>
                  </div>
                  <ProgressBar
                    label="Importing sample types..."
                    value={uploadProgress}
                    max={100}
                    style={{ width: "100%" }}
                  />
                </Section>
              </Column>
            </Grid>
          )}

          {/* Success Section */}
          {importSuccess && (
            <Grid fullWidth={true} style={{ marginTop: "2rem" }}>
              <Column lg={16} md={8} sm={4}>
                <Section style={{
                  backgroundColor: "#f0fdf4",
                  padding: "1.5rem",
                  borderRadius: "8px",
                  border: "1px solid #86efac"
                }}>
                  <div style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.75rem",
                    marginBottom: "1rem"
                  }}>
                    <div style={{
                      backgroundColor: "#16a34a",
                      color: "white",
                      borderRadius: "50%",
                      width: "24px",
                      height: "24px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "12px",
                      fontWeight: "bold"
                    }}>
                      ✓
                    </div>
                    <Heading size="sm" style={{ margin: 0, color: "#15803d" }}>
                      Import Completed Successfully
                    </Heading>
                  </div>
                  <div style={{
                    display: "flex",
                    gap: "2rem",
                    marginBottom: "1rem"
                  }}>
                    <div style={{ textAlign: "center" }}>
                      <div style={{
                        fontSize: "1.5rem",
                        fontWeight: "bold",
                        color: "#15803d"
                      }}>
                        {importSuccess.imported}
                      </div>
                      <div style={{
                        fontSize: "0.75rem",
                        color: "#6b7280"
                      }}>
                        Imported
                      </div>
                    </div>
                    <div style={{ textAlign: "center" }}>
                      <div style={{
                        fontSize: "1.5rem",
                        fontWeight: "bold",
                        color: "#d97706"
                      }}>
                        {importSuccess.skipped}
                      </div>
                      <div style={{
                        fontSize: "0.75rem",
                        color: "#6b7280"
                      }}>
                        Skipped
                      </div>
                    </div>
                    <div style={{ textAlign: "center" }}>
                      <div style={{
                        fontSize: "1.5rem",
                        fontWeight: "bold",
                        color: "#dc2626"
                      }}>
                        {importSuccess.errors}
                      </div>
                      <div style={{
                        fontSize: "0.75rem",
                        color: "#6b7280"
                      }}>
                        Errors
                      </div>
                    </div>
                  </div>
                </Section>
              </Column>
            </Grid>
          )}

          {/* Error Section */}
          {importErrors.length > 0 && (
            <Grid fullWidth={true} style={{ marginTop: "1rem" }}>
              <Column lg={16} md={8} sm={4}>
                <Section style={{
                  backgroundColor: "#fef2f2",
                  padding: "1.5rem",
                  borderRadius: "8px",
                  border: "1px solid #fca5a5"
                }}>
                  <div style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.75rem",
                    marginBottom: "1rem"
                  }}>
                    <div style={{
                      backgroundColor: "#dc2626",
                      color: "white",
                      borderRadius: "50%",
                      width: "24px",
                      height: "24px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "12px",
                      fontWeight: "bold"
                    }}>
                      ⚠
                    </div>
                    <Heading size="sm" style={{ margin: 0, color: "#dc2626" }}>
                      Import Errors ({importErrors.length} rows)
                    </Heading>
                  </div>
                  <ul style={{
                    margin: 0,
                    paddingLeft: "1rem",
                    color: "#991b1b",
                    fontSize: "0.875rem"
                  }}>
                    {importErrors.slice(0, 5).map((error, index) => (
                      <li key={index} style={{ marginBottom: "0.25rem" }}>{error}</li>
                    ))}
                    {importErrors.length > 5 && (
                      <li style={{ fontStyle: "italic", color: "#6b7280" }}>
                        ... and {importErrors.length - 5} more errors
                      </li>
                    )}
                  </ul>
                </Section>
              </Column>
            </Grid>
          )}

          {/* Preview Section */}
          {previewData.length > 0 && (
            <Grid fullWidth={true} style={{ marginTop: "2rem" }}>
              <Column lg={16} md={8} sm={4}>
                <Section style={{
                  backgroundColor: "#ffffff",
                  padding: "2rem",
                  borderRadius: "12px",
                  border: "1px solid #e0e0e0",
                  boxShadow: "0 4px 8px rgba(0, 0, 0, 0.08)"
                }}>
                  <div style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.75rem",
                    marginBottom: "1.5rem"
                  }}>
                    <div style={{
                      backgroundColor: "#0066cc",
                      color: "white",
                      borderRadius: "50%",
                      width: "28px",
                      height: "28px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "14px",
                      fontWeight: "bold"
                    }}>
                      👁
                    </div>
                    <div>
                      <Heading size="md" style={{ margin: 0, color: "#161616" }}>
                        Data Preview
                      </Heading>
                      <p style={{
                        margin: "0.25rem 0 0 0",
                        fontSize: "0.875rem",
                        color: "#6f6f6f"
                      }}>
                        Review the first 5 rows before importing ({previewData.length} rows shown)
                      </p>
                    </div>
                  </div>

                  <DataTable
                    rows={previewData}
                    headers={headers}
                    render={({ rows, headers, getHeaderProps, getTableProps }) => (
                      <TableContainer style={{
                        backgroundColor: "#f8f9fa",
                        borderRadius: "8px",
                        border: "1px solid #e0e0e0"
                      }}>
                        <Table {...getTableProps()} style={{ backgroundColor: "white" }}>
                          <TableHead style={{ backgroundColor: "#f4f4f4" }}>
                            <TableRow>
                              {headers.map((header) => (
                                <TableHeader
                                  key={header.key}
                                  {...getHeaderProps({ header })}
                                  style={{
                                    backgroundColor: "#f4f4f4",
                                    color: "#161616",
                                    fontWeight: "600",
                                    fontSize: "0.875rem",
                                    borderBottom: "2px solid #e0e0e0"
                                  }}
                                >
                                  {header.header}
                                </TableHeader>
                              ))}
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {rows.map((row, index) => (
                              <TableRow
                                key={row.id}
                                style={{
                                  backgroundColor: index % 2 === 0 ? "white" : "#f8f9fa"
                                }}
                              >
                                {row.cells.map((cell) => (
                                  <TableCell
                                    key={cell.id}
                                    style={{
                                      padding: "1rem",
                                      color: "#374151",
                                      fontSize: "0.875rem"
                                    }}
                                  >
                                    {cell.value}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  />

                  {/* Action Buttons */}
                  <div style={{
                    marginTop: "2rem",
                    padding: "1.5rem",
                    backgroundColor: "#f0f8ff",
                    borderRadius: "8px",
                    border: "1px solid #d0e2ff"
                  }}>
                    <div style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.75rem",
                      marginBottom: "1rem"
                    }}>
                      <span style={{
                        backgroundColor: "#0066cc",
                        color: "white",
                        borderRadius: "50%",
                        width: "16px",
                        height: "16px",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: "10px",
                        fontWeight: "bold"
                      }}>
                        3
                      </span>
                      <strong style={{ color: "#0066cc", fontSize: "0.875rem" }}>
                        Ready to Import
                      </strong>
                    </div>

                    <p style={{
                      margin: "0 0 1rem 0",
                      fontSize: "0.875rem",
                      color: "#6f6f6f"
                    }}>
                      Review the data above and click "Import Sample Types" to proceed.
                    </p>

                    <div style={{ display: "flex", gap: "1rem" }}>
                      <Button
                        onClick={handleImport}
                        disabled={isUploading}
                        kind="primary"
                      >
                        {isUploading ? "⏳ Importing..." : "🚀 Import Sample Types"}
                      </Button>

                      <Button
                        kind="secondary"
                        onClick={handleCancel}
                        disabled={isUploading}
                      >
                        <FormattedMessage id="label.button.cancel" />
                      </Button>
                    </div>
                  </div>
                </Section>
              </Column>
            </Grid>
          )}
        </div>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeBulkImport);