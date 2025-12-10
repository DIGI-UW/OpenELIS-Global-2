import React, { useState, useContext } from "react";
import {
  Column,
  Form,
  Grid,
  Heading,
  Section,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationKinds, AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";

import InitialARV from "./Forms/InitialARV";
import FollowUpARV from "./Forms/FollowUpARV";
import RTN from "./Forms/RTN";
import EID from "./Forms/EID";
import Indeterminate from "./Forms/Interdeterminate";
import SpecialRequest from "./Forms/SpecialRequest";
import ARVViralOverload from "./Forms/ARVViralOverload";
import RecencyTesting from "./Forms/RecencyTesting";
import HPVTesting from "./Forms/HPVTesting";
import PageBreadCrumb from "../common/PageBreadCrumb";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sampleEntryByProject.page.title", link: "/SampleEntryByProject" },
];

const initialFormData = {
  type: "",
  value: "",
  description: "",
  date: null,
  comments: "",
  status: "",
};

const selectOptions = [
  { value: "", text: "" },
  { value: "initialARV", text: "Initial ARV" },
  { value: "follow-upARV", text: "Follow-up ARV" },
  { value: "RTN", text: "RTN" },
  { value: "EID", text: "EID" },
  { value: "interdeterminate", text: "interdeterminate" },
  { value: "specialRequest", text: "Special Request" },
  { value: "ARV-viralLoad", text: "ARV - Viral Load" },
  { value: "recencyTesting", text: "Recency Testing" },
  { value: "HPVTesting", text: "HPV Testing" },
];

const SampleEntryByProjectForm = () => {
  const [formData, setFormData] = useState(initialFormData);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();

  const [selectedForm, SetSelectedForm] = useState();

  const handleSubmit = (e) => {
    e.preventDefault();
    // Handle form submission
    setNotificationVisible(true);
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: "record.save.success" }),
    });
  };

  const handleCancel = () => {
    setFormData(initialFormData);
  };

  const handleFormChange = (value) => {
    switch (value) {
      case "":
        SetSelectedForm();
        break;
      case "initialARV":
        SetSelectedForm(<InitialARV />);
        break;
      case "follow-upARV":
        SetSelectedForm(<FollowUpARV />);
        break;
      case "RTN":
        SetSelectedForm(<RTN />);
        break;
      case "EID":
        SetSelectedForm(<EID />);
        break;
      case "interdeterminate":
        SetSelectedForm(<Indeterminate />);
        break;
      case "specialRequest":
        SetSelectedForm(<SpecialRequest />);
        break;
      case "ARV-viralLoad":
        SetSelectedForm(<ARVViralOverload />);
        break;
      case "recencyTesting":
        SetSelectedForm(<RecencyTesting />);
        break;
      case "HPVTesting":
        SetSelectedForm(<HPVTesting />);
        break;
    }
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      {notificationVisible && <AlertDialog />}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="sampleEntryByProject.edit.record.title" />
            </Heading>
          </Section>
        </Column>
      </Grid>
      <br />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Form onSubmit={handleSubmit}>
            <Select
              id="type"
              labelText={intl.formatMessage({
                id: "sampleEntryByProject.label.form",
              })}
              value={formData.type}
              onChange={(e) => {
                setFormData({
                  ...formData,
                  type: e.target.value,
                });
                handleFormChange(e.target.value);
              }}
            >
              {selectOptions.map((option) => (
                <SelectItem
                  key={option.value}
                  value={option.value}
                  text={option.text}
                />
              ))}
            </Select>
          </Form>
          <br />
          <hr />
          <br />
          {selectedForm}
        </Column>
      </Grid>
    </>
  );
};

export default SampleEntryByProjectForm;
