import * as React from "react";
import {
  DataTable,
  Table,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableCell,
  TableBody,
} from "@carbon/react";
import { OBSERVATION_INTERPRETATION, useLayoutType } from "../commons";
import { OverviewPanelData } from "../overview/useOverviewData";
//import styles from './common-datatable.scss';
import "./common-datatable.scss";

interface CommonDataTableProps {
  data: Array<OverviewPanelData>;
  tableHeaders: Array<{
    key: string;
    header: string;
  }>;
  title?: string;
  toolbar?: React.ReactNode;
  description?: React.ReactNode;
}

const CommonDataTable: React.FC<CommonDataTableProps> = ({
  title,
  data,
  description,
  toolbar,
  tableHeaders,
}) => {
  const interpretationToCSS = {
    OFF_SCALE_HIGH: "offScaleHigh",
    CRITICALLY_HIGH: "criticallyHigh",
    HIGH: "high",
    OFF_SCALE_LOW: "offScaleLow",
    CRITICALLY_LOW: "criticallyLow",
    LOW: "low",
    NORMAL: "",
  };

  const isTablet = useLayoutType() === "tablet";

  return (
    <DataTable rows={data} headers={tableHeaders} size="sm" useZebraStyles>
      {({
        rows,
        headers,
        getHeaderProps,
        getRowProps,
        getTableProps,
        getTableContainerProps,
      }) => (
        <TableContainer
          className="tableContainer desktop"
          title={title}
          description={description}
          {...getTableContainerProps()}
        >
          {toolbar}
          <Table {...getTableProps()} isSortable>
            <colgroup className="columns">
              <col span={1} />
              <col span={1} />
              <col span={1} />
            </colgroup>
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader
                    key={header.key}
                    {...getHeaderProps({ header })}
                    isSortable
                  >
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row, i) => (
                <TypedTableRow
                  key={row.id}
                  interpretation={data[i]?.interpretation}
                  {...getRowProps({ row })}
                >
                  {row.cells.map((cell) => {
                    return (
                      <TableCell
                        className={
                          cell.value?.interpretation
                            ? interpretationToCSS[cell.value.interpretation]
                            : ""
                        }
                        key={cell.id}
                      >
                        {cell.value?.value ?? cell.value}
                      </TableCell>
                    );
                  })}
                </TypedTableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </DataTable>
  );
};

const TypedTableRow: React.FC<{
  interpretation: OBSERVATION_INTERPRETATION;
}> = ({ interpretation, ...props }) => {
  switch (interpretation) {
    case "OFF_SCALE_HIGH":
      return <TableRow {...props} className="off-scale-high" />;

    case "CRITICALLY_HIGH":
      return <TableRow {...props} className="critically-high" />;

    case "HIGH":
      return <TableRow {...props} className="high" />;

    case "OFF_SCALE_LOW":
      return <TableRow {...props} className="off-scale-low" />;

    case "CRITICALLY_LOW":
      return <TableRow {...props} className="critically-low" />;

    case "LOW":
      return <TableRow {...props} className="low" />;

    case "NORMAL":
    default:
      return <TableRow {...props} />;
  }
};

export default CommonDataTable;
