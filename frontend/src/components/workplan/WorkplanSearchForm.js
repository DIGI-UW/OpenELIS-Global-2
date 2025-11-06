import React, { useEffect, useRef, useState } from "react";
import { Column, Form, Grid, Section, Button, Link } from "@carbon/react";
import { ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { FormattedMessage } from "react-intl";
import "../Style.css";
import TestSectionSelectForm from "./TestSectionSelectForm";
import TestSelectForm from "./TestSelectForm";
import PanelSelectForm from "./PanelSelectForm";
import PrioritySelectForm from "./PrioritySelectForm";
import { getFromOpenElisServer } from "../utils/Utils";

export default function WorkplanSearchForm(props) {
  const mounted = useRef(false);
  const [selectedValue, setSelectedValue] = useState("");
  const [selectedLabel, setSelectedLabel] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [url, setUrl] = useState("");

  let title = "";
  let urlToPost = "";
  const type = props.type;
  switch (type) {
    case "test":
      title = <FormattedMessage id="workplan.test.types" />;
      urlToPost = "/rest/WorkPlanByTest?test_id=";
      break;
    case "panel":
      title = <FormattedMessage id="workplan.panel.types" />;
      urlToPost = "/rest/WorkPlanByPanel?panel_id=";
      break;
    case "unit":
      title = <FormattedMessage id="workplan.unit.types" />;
      urlToPost = "/rest/WorkPlanByTestSection?test_section_id=";
      break;
    case "priority":
      title = <FormattedMessage id="workplan.priority.list" />;
      urlToPost = "/rest/WorkPlanByPriority?priority=";
      break;
    default:
      title = "";
  }

  const handleSelectedValue = (v, l) => {
    if (mounted.current) {
      setSelectedValue(v);
      setSelectedLabel(l);
      props.selectedValue(v);
      props.selectedLabel(l);
    }
  };

  const getTestsList = (res) => {
    if (mounted.current) {
      props.createTestsList(res);
      if (res.paging) {
        var { totalPages, currentPage } = res.paging;
        if (totalPages > 1) {
          setPagination(true);
          setCurrentApiPage(currentPage);
          setTotalApiPages(totalPages);
          if (parseInt(currentPage) < parseInt(totalPages)) {
            setNextPage(parseInt(currentPage) + 1);
          } else {
            setNextPage(null);
          }
          if (parseInt(currentPage) > 1) {
            setPreviousPage(parseInt(currentPage) - 1);
          } else {
            setPreviousPage(null);
          }
        }
      }
      setIsLoading(false);
    }
  };

  const loadNextResultsPage = () => {
    setIsLoading(true);
    getFromOpenElisServer(url + "&page=" + nextPage, getTestsList);
  };

  const loadPreviousResultsPage = () => {
    setIsLoading(true);
    getFromOpenElisServer(url + "&page=" + previousPage, getTestsList);
  };

  useEffect(() => {
    mounted.current = true;
    setIsLoading(true);
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    setUrl(urlToPost + selectedValue);
    getFromOpenElisServer(urlToPost + selectedValue, getTestsList);
    return () => {
      mounted.current = false;
    };
  }, [selectedValue]);

  useEffect(() => {
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
  }, []);

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <h5 className="contentHeader2">
              <FormattedMessage id="label.form.searchby" />
              &nbsp; {title}{" "}
            </h5>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column sm={4} md={4} lg={6}>
          <Form className="container-form">
            {type === "test" && (
              <TestSelectForm title={title} value={handleSelectedValue} />
            )}
            {type === "panel" && (
              <PanelSelectForm title={title} value={handleSelectedValue} />
            )}
            {type === "unit" && (
              <TestSectionSelectForm
                title={title}
                value={handleSelectedValue}
              />
            )}
            {type === "priority" && (
              <PrioritySelectForm title={title} value={handleSelectedValue} />
            )}
          </Form>
        </Column>
        <Column sm={1} md={2} lg={4}>
          {isLoading && (
            <img
              src={`images/loading.gif`}
              alt="Loading ..."
              width="60"
              height="60"
            />
          )}
        </Column>
      </Grid>
      <hr />
      <br />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          {selectedLabel && (
            <Section>
              <h4 className="contentHeader1">&nbsp;</h4>
            </Section>
          )}
        </Column>
      </Grid>
      <>
        {pagination && (
          <Grid>
            <Column lg={14} />
            <Column
              lg={2}
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "10px",
                width: "110%",
              }}
            >
              <Link>
                {currentApiPage} / {totalApiPages}
              </Link>
              <div style={{ display: "flex", gap: "10px" }}>
                <Button
                  hasIconOnly
                  id="loadpreviousresults"
                  onClick={loadPreviousResultsPage}
                  disabled={previousPage != null ? false : true}
                  renderIcon={ArrowLeft}
                  iconDescription="previous"
                ></Button>
                <Button
                  hasIconOnly
                  id="loadnextresults"
                  onClick={loadNextResultsPage}
                  disabled={nextPage != null ? false : true}
                  renderIcon={ArrowRight}
                  iconDescription="next"
                ></Button>
              </div>
            </Column>
          </Grid>
        )}
      </>
    </>
  );
}
