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

          <Grid fullWidth={true}>
            <Column lg={8} md={4} sm={2}>
              <Button
                kind="secondary"
                onClick={downloadTemplate}
                style={{ marginBottom: "1rem" }}
              >
                Download CSV Template
              </Button>
            </Column>
          </Grid>

          <Grid fullWidth={true}>
            <Column lg={8} md={4} sm={2}>
              <FileUploader
                labelTitle="Upload CSV File"
                labelDescription="Select a CSV file containing sample type data"
                buttonLabel="Choose file"
                filenameStatus="edit"
                accept={['.csv']}
                multiple={false}
                onChange={handleFileChange}
                disabled={isUploading}
              />
            </Column>
          </Grid>

          {isUploading && (
            <>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <ProgressBar
                    label="Importing sample types..."
                    value={uploadProgress}
                    max={100}
                  />
                </Column>
              </Grid>
            </>
          )}

          {importSuccess && (
            <>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <InlineNotification
                    kind="success"
                    title="Import Completed"
                    subtitle={`Successfully imported ${importSuccess.imported} sample types. ${importSuccess.skipped} skipped, ${importSuccess.errors} errors.`}
                    hideCloseButton
                  />
                </Column>
              </Grid>
            </>
          )}

          {importErrors.length > 0 && (
            <>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <InlineNotification
                    kind="error"
                    title="Import Errors"
                    subtitle={`${importErrors.length} rows had errors`}
                    hideCloseButton
                  />
                  <ul style={{ marginTop: "0.5rem" }}>
                    {importErrors.slice(0, 5).map((error, index) => (
                      <li key={index}>{error}</li>
                    ))}
                    {importErrors.length > 5 && <li>... and {importErrors.length - 5} more</li>}
                  </ul>
                </Column>
              </Grid>
            </>
          )}

          {previewData.length > 0 && (
            <>
              <br />
              <hr />
              <br />

              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Heading size="sm">Preview (First 5 rows)</Heading>
                  <br />
                  <DataTable
                    rows={previewData}
                    headers={headers}
                    render={({ rows, headers, getHeaderProps, getTableProps }) => (
                      <TableContainer>
                        <Table {...getTableProps()}>
                          <TableHead>
                            <TableRow>
                              {headers.map((header) => (
                                <TableHeader key={header.key} {...getHeaderProps({ header })}>
                                  {header.header}
                                </TableHeader>
                              ))}
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {rows.map((row) => (
                              <TableRow key={row.id}>
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>{cell.value}</TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  />
                </Column>
              </Grid>

              <br />

              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Button
                    onClick={handleImport}
                    disabled={isUploading}
                  >
                    {isUploading ? "Importing..." : "Import Sample Types"}
                  </Button>

                  <Button
                    kind="secondary"
                    onClick={handleCancel}
                    style={{ marginLeft: "1rem" }}
                    disabled={isUploading}
                  >
                    <FormattedMessage id="label.button.cancel" />
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

export default injectIntl(SampleTypeBulkImport);