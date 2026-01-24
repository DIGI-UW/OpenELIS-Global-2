import React from "react";
import { useIntl } from "react-intl";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Loading,
  InlineNotification,
  Pagination,
} from "@carbon/react";
import "../../Style.css";

const ReportPreview = ({
  reportData,
  isLoading = false,
  error = null,
  currentPage = 1,
  pageSize = 25,
  onPageChange,
  onPageSizeChange,
}) => {
  const intl = useIntl();

  if (isLoading) {
    return (
      <div className="adhoc-report-preview">
        <h3 className="adhoc-section-title">
          {intl.formatMessage({ id: "adhoc.step.preview" })}
        </h3>
        <div className="adhoc-preview-loading">
          <Loading
            description={intl.formatMessage({ id: "adhoc.preview.loading" })}
            withOverlay={false}
          />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="adhoc-report-preview">
        <h3 className="adhoc-section-title">
          {intl.formatMessage({ id: "adhoc.step.preview" })}
        </h3>
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "adhoc.preview.error" })}
          subtitle={error}
          hideCloseButton
        />
      </div>
    );
  }

  if (!reportData || !reportData.columns || reportData.columns.length === 0) {
    return (
      <div className="adhoc-report-preview">
        <h3 className="adhoc-section-title">
          {intl.formatMessage({ id: "adhoc.step.preview" })}
        </h3>
        <div className="adhoc-preview-empty">
          {intl.formatMessage({ id: "adhoc.preview.selectFieldsFirst" })}
        </div>
      </div>
    );
  }

  if (!reportData.rows || reportData.rows.length === 0) {
    return (
      <div className="adhoc-report-preview">
        <h3 className="adhoc-section-title">
          {intl.formatMessage({ id: "adhoc.step.preview" })}
        </h3>
        <div className="adhoc-preview-header">
          <span className="adhoc-preview-info">
            {intl.formatMessage({ id: "adhoc.preview.noResults" })}
          </span>
        </div>
        <div className="adhoc-preview-empty">
          {intl.formatMessage({ id: "adhoc.preview.noData" })}
        </div>
      </div>
    );
  }

  const headers = reportData.columns.map((col, index) => ({
    key: `col_${index}`,
    header: col.displayName,
  }));

  const rows = reportData.rows.map((row, rowIndex) => {
    const rowObj = { id: `row_${rowIndex}` };
    row.forEach((cellValue, colIndex) => {
      rowObj[`col_${colIndex}`] =
        cellValue !== null && cellValue !== undefined ? cellValue : "";
    });
    return rowObj;
  });

  const handlePaginationChange = (paginationData) => {
    if (onPageChange && paginationData.page !== currentPage) {
      onPageChange(paginationData.page);
    }
    if (onPageSizeChange && paginationData.pageSize !== pageSize) {
      onPageSizeChange(paginationData.pageSize);
    }
  };

  return (
    <div className="adhoc-report-preview">
      <h3 className="adhoc-section-title">
        {intl.formatMessage({ id: "adhoc.step.preview" })}
      </h3>

      <div className="adhoc-preview-header">
        <span className="adhoc-preview-info">
          {intl.formatMessage(
            { id: "adhoc.preview.showingResults" },
            {
              showing: reportData.returnedCount,
              total: reportData.totalCount,
            },
          )}
        </span>
        {reportData.hasMore && (
          <span className="adhoc-preview-info">
            {intl.formatMessage({ id: "adhoc.preview.moreAvailable" })}
          </span>
        )}
      </div>

      <div className="adhoc-preview-table-container">
        <DataTable rows={rows} headers={headers} isSortable>
          {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
            <Table {...getTableProps()} size="md">
              <TableHead>
                <TableRow>
                  {headers.map((header) => (
                    <TableHeader
                      {...getHeaderProps({ header })}
                      key={header.key}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow {...getRowProps({ row })} key={row.id}>
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>{cell.value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </DataTable>
      </div>

      {reportData.totalCount > pageSize && (
        <Pagination
          totalItems={reportData.totalCount}
          backwardText={intl.formatMessage({ id: "pagination.previous" })}
          forwardText={intl.formatMessage({ id: "pagination.next" })}
          pageSize={pageSize}
          pageSizes={[10, 25, 50, 100]}
          itemsPerPageText={intl.formatMessage({ id: "pagination.itemsPerPage" })}
          page={currentPage}
          onChange={handlePaginationChange}
        />
      )}
    </div>
  );
};

export default ReportPreview;
