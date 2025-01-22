import { Button, DatePicker, Form, FormGroup, MultiSelect, RadioButton, RadioButtonGroup, TextInput } from '@carbon/react';
import React, { useState, useEffect } from 'react';

const PatientEditByProjectForm = ({ formData }) => {
  const [data, setData] = useState({});

  useEffect(() => {
    if (formData) {
      setData(formData);
    }
  }, [formData]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const handleDateChange = (name, date) => {
    setData((prevData) => ({
      ...prevData,
      [name]: date,
    }));
  };

  const handleMultiSelectChange = (name, selectedItems) => {
    setData((prevData) => ({
      ...prevData,
      observations: {
        ...prevData.observations,
        [name]: selectedItems,
      },
    }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    console.log('Form submitted:', data);
  };

  if (!formData) {
    return <div>No data available</div>;
  }

  return (
    <Form onSubmit={handleSubmit}>
      {/* Patient Information */}
      <FormGroup legendText="Patient Information">
        <TextInput
          id="firstName"
          labelText="First Name"
          name="firstName"
          value={data.firstName || ""}
          onChange={handleChange}
        />
        <TextInput
          id="lastName"
          labelText="Last Name"
          name="lastName"
          value={data.lastName || ""}
          onChange={handleChange}
        />
        <RadioButtonGroup
          name="gender"
          valueSelected={data.gender || ""}
          onChange={handleChange}
          legendText="Gender"
        >
          <RadioButton labelText="Male" value="male" id="male" />
          <RadioButton labelText="Female" value="female" id="female" />
        </RadioButtonGroup>
        <DatePicker
          id="birthDate"
          dateFormat="m/d/Y"
          name="birthDateForDisplay"
          labelText="Date of Birth"
          value={data.birthDateForDisplay || ""}
          onChange={(date) => handleDateChange('birthDateForDisplay', date)}
        />
      </FormGroup>

      {/* Interview Details */}
      <FormGroup legendText="Interview Details">
        <DatePicker
          id="interviewDate"
          dateFormat="m/d/Y"
          name="interviewDate"
          labelText="Interview Date"
          value={data.interviewDate || ""}
          onChange={(date) => handleDateChange('interviewDate', date)}
        />
        <TextInput
          id="interviewTime"
          labelText="Interview Time"
          name="interviewTime"
          value={data.interviewTime || ""}
          onChange={handleChange}
        />
      </FormGroup>

      {/* Received Details */}
      <FormGroup legendText="Received Details">
        <DatePicker
          id="receivedDate"
          dateFormat="m/d/Y"
          name="receivedDateForDisplay"
          labelText="Received Date"
          value={data.receivedDateForDisplay || ""}
          onChange={(date) => handleDateChange('receivedDateForDisplay', date)}
        />
        <TextInput
          id="receivedTime"
          labelText="Received Time"
          name="receivedTimeForDisplay"
          value={data.receivedTimeForDisplay || ""}
          onChange={handleChange}
        />
      </FormGroup>

      {/* Observations Section */}
      <FormGroup legendText="Observations">
        <MultiSelect
          id="priorDiseasesList"
          label="Prior Diseases"
          name="priorDiseasesList"
          items={data.observations?.priorDiseasesList || []}
          itemToString={(item) => item.label}
          onChange={(selectedItems) =>
            handleMultiSelectChange('priorDiseasesList', selectedItems)
          }
        />
        <MultiSelect
          id="currentDiseasesList"
          label="Current Diseases"
          name="currentDiseasesList"
          items={data.observations?.currentDiseasesList || []}
          itemToString={(item) => item.label}
          onChange={(selectedItems) =>
            handleMultiSelectChange('currentDiseasesList', selectedItems)
          }
        />
        <MultiSelect
          id="rtnPriorDiseasesList"
          label="Return Prior Diseases"
          name="rtnPriorDiseasesList"
          items={data.observations?.rtnPriorDiseasesList || []}
          itemToString={(item) => item.label}
          onChange={(selectedItems) =>
            handleMultiSelectChange('rtnPriorDiseasesList', selectedItems)
          }
        />
        <MultiSelect
          id="rtnCurrentDiseasesList"
          label="Return Current Diseases"
          name="rtnCurrentDiseasesList"
          items={data.observations?.rtnCurrentDiseasesList || []}
          itemToString={(item) => item.label}
          onChange={(selectedItems) =>
            handleMultiSelectChange('rtnCurrentDiseasesList', selectedItems)
          }
        />
      </FormGroup>

      {/* Submit and Cancel Buttons */}
      <Button type="submit" kind="primary">
        Submit
      </Button>
      <Button type="button" kind="secondary" onClick={() => alert('Cancel action')}>
        Cancel
      </Button>
    </Form>
  );
};

export default PatientEditByProjectForm;
