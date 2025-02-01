import React from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  Modal,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, injectIntl } from "react-intl";
import PropTypes from "prop-types";

const RenameModelBox = ({
  data,
  isModalOpen,
  openModel,
  closeModel,
  onSubmit,
  onInputChangeEn,
  onInputChangeFr,
  isLoading,
  modalHeading,
  heading,
  mainLabel,
  confirmationStep,
  inputError,
  lang,
  langPost,
  selectedItem,
  hasFrench,
}) => {
  return (
    <>
      {data ? (
        <Grid fullWidth={true}>
          {data.map((item, index) => (
            <Column key={index} lg={5} md={5} sm={5}>
              <Button
                id={`button-${index}`}
                kind="ghost"
                type="button"
                onClick={() => {
                  openModel(item);
                }}
                style={{ color: "#000000" }}
              >
                {item.value}
              </Button>
            </Column>
          ))}
        </Grid>
      ) : (
        <>
          <Loading active={isLoading} />
        </>
      )}

      <Modal
        open={isModalOpen}
        size="sm"
        modalHeading={`${modalHeading} : ${selectedItem?.value}`}
        primaryButtonText={
          confirmationStep ? (
            <>
              <FormattedMessage id="column.name.accept" />
            </>
          ) : (
            <>
              <FormattedMessage id="column.name.save" />
            </>
          )
        }
        secondaryButtonText={
          confirmationStep ? (
            <>
              <FormattedMessage id="header.reject" />
            </>
          ) : (
            <>
              <FormattedMessage id="label.button.cancel" />
            </>
          )
        }
        onRequestSubmit={onSubmit}
        onRequestClose={closeModel}
      >
        {lang && lang?.name && langPost && langPost?.name ? (
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              {/* Edit or Confirmation */}
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id={heading} />
                  </Heading>
                </Section>
              </Section>
              <br />
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      {/* main blue lable */}
                      <FormattedMessage id={mainLabel} />
                    </Heading>
                  </Section>
                </Section>
              </Section>
              <br />
              <>
                <FormattedMessage id="english.current" /> :{" "}
                {lang?.name?.english}
              </>
              <br />
              <br />
              <TextInput
                id={`eng`}
                labelText=""
                hideLabel
                value={`${langPost?.name?.english}` || ""}
                onChange={(e) => {
                  onInputChangeEn(e);
                }}
                required
                invalid={inputError}
                invalidText={<FormattedMessage id="required.invalidtext" />}
              />
              <br />
              {hasFrench && (
                <>
                  <>
                    <FormattedMessage id="french.current" /> :{" "}
                    {lang?.name?.french}
                  </>
                  <br />
                  <br />
                  <TextInput
                    id={`fr`}
                    labelText=""
                    hideLabel
                    value={`${langPost?.name?.french}` || ""}
                    onChange={(e) => {
                      onInputChangeFr(e);
                    }}
                    required
                    invalid={inputError}
                    invalidText={<FormattedMessage id="required.invalidtext" />}
                  />
                </>
              )}
            </Column>
          </Grid>
        ) : (
          <>
            <div>
              <Loading />
            </div>
          </>
        )}
        <br />
        {confirmationStep && (
          <>
            <Section>
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id="confirmation.rename" />
                  </Heading>
                </Section>
              </Section>
            </Section>
          </>
        )}
      </Modal>
    </>
  );
};

RenameModelBox.propTypes = {
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
  isModalOpen: PropTypes.bool.isRequired,
  closeModel: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
  onInputChangeEn: PropTypes.func.isRequired,
  onInputChangeFr: PropTypes.func.isRequired,
  isLoading: PropTypes.bool.isRequired,
  modalHeading: PropTypes.string.isRequired,
  heading: PropTypes.string.isRequired,
  mainLabel: PropTypes.string.isRequired,
  confirmationStep: PropTypes.bool.isRequired,
  inputError: PropTypes.bool.isRequired,
  lang: PropTypes.object.isRequired,
  langPost: PropTypes.object.isRequired,
  selectedItem: PropTypes.object.isRequired,
};

export default injectIntl(RenameModelBox);
