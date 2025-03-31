import { ArrowLeft, ArrowRight } from "@carbon/icons-react";
import { Button, Column, Grid, Section } from "@carbon/react";
import PropTypes from "prop-types";
import React from "react";
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

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Section
          style={{
            display: "flex",
            flexDirection: window.innerWidth < 768 ? "column" : "row",
            gap: window.innerWidth < 768 ? "1rem" : "2rem",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div
            style={{
              display: "flex",
              gap: window.innerWidth < 768 ? "0.81rem" : "0.4rem",
            }}
          >
            {type === "type1" ? (
              <>
                <Button
                  data-cy="modify-Button"
                  onClick={() => openUpdateModal(selectedRowIds[0])}
                  disabled={selectedRowIds.length !== 1}
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.modify" />
                </Button>{" "}
                <Button
                  data-cy="deactivate-Button"
                  disabled={deactivateButton}
                  onClick={deleteDeactivate}
                  type="button"
                >
                  {" "}
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.deactivate" />
                </Button>{" "}
                <Button data-cy="add-Button" onClick={openAddModal}>
                  {" "}
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.add" />
                </Button>
              </>
            ) : (
              <>
                <Button
                  onClick={() => {
                    if (selectedRowIds.length === 1) {
                      const url = `${modifyButtonRedirectLink}${id}${otherParmsInLink}`;
                      window.location.href = url;
                    }
                  }}
                  disabled={modifyButton}
                  type="button"
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.modify" />
                </Button>{" "}
                <Button
                  onClick={deleteDeactivate}
                  disabled={deactivateButton}
                  type="button"
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.deactivate" />
                </Button>{" "}
                <Button
                  data-cy="add-button"
                  onClick={() => {
                    window.location.href = `${addButtonRedirectLink}`;
                  }}
                  type="button"
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.add" />
                </Button>
              </>
            )}
          </div>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: window.innerWidth < 768 ? "0.5rem" : "1rem",
              padding: window.innerWidth < 768 ? "0.5rem" : "1rem",
              flexWrap: "wrap",
            }}
          >
            <h4
              style={{
                margin: 0,
                fontSize: window.innerWidth < 768 ? "0.875rem" : "1.3rem",
                whiteSpace: "nowrap",
              }}
            >
              <FormattedMessage id="showing" /> {fromRecordCount} -{" "}
              {toRecordCount} <FormattedMessage id="of" /> {totalRecordCount}{" "}
            </h4>
            <Button
              style={{
                minWidth: window.innerWidth < 768 ? "2rem" : "2.5rem",
                minHeight: window.innerWidth < 768 ? "2rem" : "2.5rem",
                padding: "0.5rem",
                marginLeft: window.innerWidth < 768 ? "0" : "0.625rem",
              }}
              hasIconOnly={true}
              disabled={parseInt(fromRecordCount) <= 1}
              onClick={handlePreviousPage}
              renderIcon={ArrowLeft}
              iconDescription={intl.formatMessage({
                id: "organization.previous",
              })}
            />
            <Button
              style={{
                minWidth: window.innerWidth < 768 ? "2rem" : "2.5rem",
                minHeight: window.innerWidth < 768 ? "2rem" : "2.5rem",
                padding: "0.5rem",
                marginLeft: window.innerWidth < 768 ? "0" : "0.625rem",
              }}
              hasIconOnly={true}
              renderIcon={ArrowRight}
              onClick={handleNextPage}
              disabled={parseInt(toRecordCount) >= parseInt(totalRecordCount)}
              iconDescription={intl.formatMessage({
                id: "organization.next",
              })}
            />
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
