import React from "react";
import PropTypes from "prop-types";
import { Button, Grid, Column, Section } from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { ArrowLeft, ArrowRight } from "@carbon/icons-react";

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
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div>
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
          <div style={{ display: "flex", alignItems: "center" }}>
            <h4 style={{ marginRight: "10px" }}>
              <FormattedMessage id="showing" /> {fromRecordCount} -{" "}
              {toRecordCount} <FormattedMessage id="of" /> {totalRecordCount}{" "}
            </h4>
            <Button
              style={{ marginLeft: "10px" }}
              hasIconOnly={true}
              disabled={parseInt(fromRecordCount) <= 1}
              onClick={handlePreviousPage}
              renderIcon={ArrowLeft}
              iconDescription={intl.formatMessage({
                id: "organization.previous",
              })}
            />{" "}
            <Button
              style={{ marginLeft: "10px" }}
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
