import { ArrowLeft, ArrowRight } from "@carbon/icons-react";
import { Button, Column, Grid, Section } from "@carbon/react";
import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";

const ActionPaginationButtonType = ({
  selectedRowIds,
  modifyButton,
  deactivateButton,
  deleteDeactivate,
  openUpdateModal,
  openAddModal,
  handlePreviousPage,
  handleNextPage,
  fromRecordCount,
  toRecordCount,
  totalRecordCount,
  addButtonRedirectLink,
  modifyButtonRedirectLink,
  otherParmsInLink,
  id,
  type,
}) => {
  const intl = useIntl();
  const [isMobile, setIsMobile] = useState(window.innerWidth < 530);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 530);
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  return (
    <Grid fullWidth>
      <Column lg={16} md={8} sm={4}>
        <Section
          style={{
            display: "flex",
            flexDirection: isMobile ? "column" : "row",
            gap: isMobile ? "1rem" : "2rem",
            justifyContent: "space-between",
            alignItems: isMobile ? "stretch" : "center",
            flexWrap: "wrap",
          }}
        >
          <div
            style={{
              display: "flex",
              gap: isMobile ? "0.75rem" : "0.5rem",
              flexDirection: isMobile ? "column" : "row",
              width: isMobile ? "100%" : "auto",
            }}
          >
            {type === "type1" ? (
              <>
                <Button
                  style={{ width: isMobile ? "100%" : "auto" }}
                  data-cy="modify-Button"
                  onClick={() => openUpdateModal(selectedRowIds[0])}
                  disabled={selectedRowIds.length !== 1}
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.modify" />
                </Button>
                <Button
                  style={{ width: isMobile ? "100%" : "auto" }}
                  data-cy="deactivate-Button"
                  disabled={deactivateButton}
                  onClick={deleteDeactivate}
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.deactivate" />
                </Button>
                <Button
                  style={{ width: isMobile ? "100%" : "auto" }}
                  data-cy="add-Button"
                  onClick={openAddModal}
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.add" />
                </Button>
              </>
            ) : (
              <>
                <Button
                  data-cy="modify-button"
                  style={{ width: isMobile ? "100%" : "auto" }}
                  onClick={() => {
                    if (selectedRowIds.length === 1) {
                      const url = `${modifyButtonRedirectLink}${id}${otherParmsInLink}`;
                      window.location.href = url;
                    }
                  }}
                  disabled={modifyButton}
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.modify" />
                </Button>
                <Button
                  data-cy="deactivate-button"
                  style={{ width: isMobile ? "100%" : "auto" }}
                  onClick={deleteDeactivate}
                  disabled={deactivateButton}
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.deactivate" />
                </Button>
                <Button
                  style={{ width: isMobile ? "100%" : "auto" }}
                  data-cy="add-button"
                  onClick={() => {
                    window.location.href = `${addButtonRedirectLink}`;
                  }}
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.add" />
                </Button>
              </>
            )}
          </div>

          <div
            style={{
              display: "flex",
              justifyContent: isMobile ? "space-between" : "center",
              gap: isMobile ? "0.5rem" : "1rem",
              padding: isMobile ? "0.5rem 0" : "1rem 0",
              flexWrap: "wrap",
              width: isMobile ? "100%" : "auto",
              flexDirection: isMobile ? "column" : "row",
            }}
          >
            <h4
              style={{
                margin: 0,
                fontSize: isMobile ? "1.2rem" : "1.2rem",
                textAlign: isMobile ? "center" : "left",
              }}
            >
              <FormattedMessage id="showing" /> {fromRecordCount} -{" "}
              {toRecordCount} <FormattedMessage id="of" /> {totalRecordCount}
            </h4>

            <div
              style={{
                display: "flex",
                gap: "0.5rem",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <Button
                style={{
                  minWidth: isMobile ? "2rem" : "2.5rem",
                  minHeight: isMobile ? "2rem" : "2.5rem",
                  padding: "0.5rem",
                }}
                hasIconOnly
                disabled={parseInt(fromRecordCount) <= 1}
                onClick={handlePreviousPage}
                renderIcon={ArrowLeft}
                iconDescription={intl.formatMessage({
                  id: "organization.previous",
                })}
              />
              <Button
                style={{
                  minWidth: isMobile ? "2rem" : "2.5rem",
                  minHeight: isMobile ? "2rem" : "2.5rem",
                  padding: "0.5rem",
                }}
                hasIconOnly
                renderIcon={ArrowRight}
                onClick={handleNextPage}
                disabled={parseInt(toRecordCount) >= parseInt(totalRecordCount)}
                iconDescription={intl.formatMessage({
                  id: "organization.next",
                })}
              />
            </div>
          </div>
        </Section>
      </Column>
    </Grid>
  );
};

ActionPaginationButtonType.propTypes = {
  selectedRowIds: PropTypes.array.isRequired,
  modifyButton: PropTypes.bool.isRequired,
  deactivateButton: PropTypes.bool.isRequired,
  deleteDeactivate: PropTypes.func.isRequired,
  handlePreviousPage: PropTypes.func.isRequired,
  handleNextPage: PropTypes.func.isRequired,
  fromRecordCount: PropTypes.string.isRequired,
  toRecordCount: PropTypes.string.isRequired,
  totalRecordCount: PropTypes.string.isRequired,
  addButtonRedirectLink: PropTypes.string,
  modifyButtonRedirectLink: PropTypes.string,
  openUpdateModal: PropTypes.func,
  openAddModal: PropTypes.func,
  id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  otherParmsInLink: PropTypes.string,
  type: PropTypes.oneOf(["type1", "type2"]).isRequired,
};

export default injectIntl(ActionPaginationButtonType);
