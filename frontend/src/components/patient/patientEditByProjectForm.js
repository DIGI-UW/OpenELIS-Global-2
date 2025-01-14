import React, { useEffect, useState } from "react";
import { 
  TextInput, 
  Select, 
  SelectItem, 
  Button, 
  Form, 
  DataTable, 
  Grid,
  Column
} from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb.js";

import { getFromOpenElisServer } from "../utils/Utils";
import { FormattedMessage, useIntl } from "react-intl";

const { TableContainer, Table, TableHead, TableRow, TableHeader, TableBody, TableCell } = DataTable;

const PatientEditByProject = () => {
  const [searchBy, setSearchBy] = useState("");
  const [query, setQuery] = useState("");
  const [data, setData] = useState([]);
  const [scan,setScan] = useState(false)

  const intl = useIntl();

  const headers = [
    { id: 0, header: " " },
    { id: 1, header: intl.formatMessage({ id: "patient.first.name" }), key: "lastName" },
    { id: 2, header: intl.formatMessage({ id: "patient.last.name" }), key: "firstName" },
    { id: 3, header: intl.formatMessage({ id: "patient.gender" }), key: "gender" },
    { id: 4, header: intl.formatMessage({ id: "patient.dob" }), key: "birthdate" },
    { id: 5, header: intl.formatMessage({ id: "patient.subject.number" }), key: "subjectNumber" },
    { id: 6, header: intl.formatMessage({ id: "patient.natioanalid" }), key: "nationalId" },
  ];

  const searchCriteria = [
    { id: "0", value: "Search by..." },
    { id: "2", value: "1. Last name" },
    { id: "1", value: "2. First name" },
    { id: "3", value: "3. label.select.last.first.name" },
    { id: "4", value: "4. Patient identification code" },
    { id: "5", value: "5. Lab No" },
  ];

  const handleSearchSubmit = () => {
    const endpoint = `/rest/patient-edit-by-project?searchBy=${searchBy}&query=${query}`;
    getFromOpenElisServer(endpoint, (response) => {
      setData(response || []);
    });
  };

  return (
    <div style={{ padding: "1rem" }}>
      <PageBreadCrumb breadcrumbs={[{ label: "home.label", link: "/" }]} />
      <h1>
        <b>
          <FormattedMessage id="banner.menu.patientConsult" />{" "}
          <FormattedMessage id="banner.menu.patient" />
        </b>
      </h1>
      <h2>
        <b>
          <FormattedMessage id="patient.label.info" />
        </b>
      </h2>

    
      <Grid>
      <Column lg={4} md={8} sm={4}>
          <Select
          style={{margin:"10px"}}
            id="search-criteria"
            labelText={intl.formatMessage({ id: "order.legend.selectMethod" })}
            value={searchBy}
            onChange={(e) => setSearchBy(e.target.value)}
          >
            {searchCriteria.map((item) => (
              <SelectItem key={item.id} value={item.id} text={item.value} />
            ))}
          </Select>
        </Column>
        {searchBy !== "0" && (
           <Column lg={6} md={4} sm={2}>
            <TextInput
              id="search-query"
              labelText={intl.formatMessage({ id: "search.patient.label" })}
              placeholder={intl.formatMessage({ id: "search.patient.label" })}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </Column>
        )}
        <Column lg={2} md={2} sm={1}>
          <Button onClick={handleSearchSubmit}>
            <FormattedMessage id="label.search.patient" />
          </Button>
        </Column>
      </Grid>
      {scan && (
             <Column lg={3} md={2} sm={1}>
             <p  style={{margin:"10px"}} ><b><FormattedMessage id="referral.input" /> </b></p>
           </Column>

          )}
<div  style={{margin:"5px"}}>
      <DataTable rows={data} headers={headers}>
        {({ rows, headers, getHeaderProps, getRowProps }) => (
          <TableContainer title="">
            <Table>
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
                  <TableRow key={row.id} {...getRowProps({ row })}>
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>{cell.value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>
      </div>
      <Grid>
        
        <Column  lg={4} >
    
        <Select
              id="search-criteria"
              labelText=""
              
              
            >
              <SelectItem
                text=""
              />
              <SelectItem
                
                text={intl.formatMessage({ id: "project.ARVStudy.name" })}
              />
              <SelectItem
                
                text={intl.formatMessage({ id: "project.ARVFollowupStudy.name" })}
              />
              <SelectItem
                
                text={intl.formatMessage({ id: "project.RTNStudy.name" })}
              />
              <SelectItem
                
                text={intl.formatMessage({ id: "banner.menu.resultvalidation.viralload" })}
              />
              <SelectItem
                
                text={intl.formatMessage({ id: "project.EIDStudy.name" })}
              />
               <SelectItem
                
                text={intl.formatMessage({ id: "project.Recency.name" })}
              />
            </Select>
        </Column>
      </Grid>
      
    </div>
  );
};

export default PatientEditByProject;
