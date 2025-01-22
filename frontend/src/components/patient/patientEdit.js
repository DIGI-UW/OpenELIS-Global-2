import { Button, Column, DatePicker, DatePickerInput, Form, FormGroup, Grid, MultiSelect, RadioButton, RadioButtonGroup, TextInput } from '@carbon/react';
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
  
    const handleDateChange = (e) => {
      const {name,value} = e.target
      setData((prevData) => ({
        ...prevData,
        [name]:value, // Flatpickr returns an array, we pick the first date
      }));
    };
  
    const handleMultiSelectChange = (name, { selectedItems }) => {
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
      setData({})
    };
  
    if (!formData) {
      return <div>No data available</div>;
    }
  
    // Function to format observations
    const formattedObservations = (diseaseList) => {
      return diseaseList?.map((item) => {
        const key = Object.keys(item)[0];
        return { id: key, label: item[key] };
      }) || [];
    };
  
    // Ensure `observations` exists to prevent errors
    const observations = data.observations || {};
    const formattedPriorDiseasesList = formattedObservations(observations.priorDiseasesList);
    const formattedCurrentDiseasesList = formattedObservations(observations.currentDiseasesList);
    const formattedRtnPriorDiseasesList = formattedObservations(observations.rtnPriorDiseasesList);
    const formattedRtnCurrentDiseasesList = formattedObservations(observations.rtnCurrentDiseasesList);
  
    return (
      <Form onSubmit={handleSubmit}>
        {/* Patient Information */}
        
         
        
        <FormGroup legendText="Patient Information">
        <Grid>
        <Column  lg={8} md={4} sm={4}>
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
          </Column>
           <Column   lg={8} md={4} sm={4} >
          <RadioButtonGroup
            name="gender"
            valueSelected={data.gender || ""}
            onChange={handleChange}
            legendText="Gender"
          >
            <RadioButton labelText="Male" value="male" id="male" />
            <RadioButton labelText="Female" value="female" id="female" />
          </RadioButtonGroup>
          
            
            
            <TextInput
            id="birthDate"
            type='date'
             dateFormat="m/d/Y"
            name="birthDateForDisplay"
            labelText="Date of Birth"
            value={data.birthDateForDisplay || ""}
            onChange={handleDateChange}
            />
          
          </Column>
          </Grid>
        </FormGroup>
        
        {/* Interview Details */}
        <Grid>
        <Column lg={8} md={4} sm={4} >
        <FormGroup legendText="Interview Details">

            <TextInput
            id="interviewDate"
            type='date'
            dateFormat="m/d/Y"
            name="interviewDate"
            labelText="Interview Date"
            value={data.interviewDate || ""}
            onChange={ handleDateChange}
            />
            
          <TextInput
            id="interviewTime"
            labelText="Interview Time"
            type="time"
            name="interviewTime"
            value={data.interviewTime || ""}
            onChange={handleChange}
          />
        </FormGroup>
        </Column>
        <Column  lg={8} md={4} sm={4} >
        {/* Received Details */}
        <FormGroup legendText="Received Details">
          
            <TextInput
            id="receivedDate"
            type='date'
            dateFormat="m/d/Y"
            name="receivedDateForDisplay"
            labelText="Received Date"
            value={data.receivedDateForDisplay || ""}
            onChange={handleDateChange}

            />
        

          <TextInput
            id="receivedTime"
            type="time"
            labelText="Received Time"
            name="receivedTimeForDisplay"
            value={data.receivedTimeForDisplay || ""}
            onChange={handleChange}
          />
        </FormGroup>
        </Column>
        </Grid>
        {/* Observations Section */}
        <FormGroup legendText="Observations">
        <Grid>
        <Column  lg={8} md={4} sm={4} >
          <MultiSelect
            id="priorDiseasesList"
            label="Prior Diseases"
            name="priorDiseasesList"
            items={formattedPriorDiseasesList}
            itemToString={(item) => (item ? item.label : '')}
            onChange={(selectedItems) => handleMultiSelectChange('priorDiseasesList', selectedItems)}
            initialSelectedItems={observations.priorDiseasesList || []}
          />
          <MultiSelect
            id="currentDiseasesList"
            label="Current Diseases"
            name="currentDiseasesList"
            items={formattedCurrentDiseasesList}
            itemToString={(item) => (item ? item.label : '')}
            onChange={(selectedItems) => handleMultiSelectChange('currentDiseasesList', selectedItems)}
            initialSelectedItems={observations.currentDiseasesList || []}
          />
          </Column>
          <Column lg={8} md={4} sm={4} >
          <MultiSelect
            id="rtnPriorDiseasesList"
            label="Return Prior Diseases"
            name="rtnPriorDiseasesList"
            items={formattedRtnPriorDiseasesList}
            itemToString={(item) => (item ? item.label : '')}
            onChange={(selectedItems) => handleMultiSelectChange('rtnPriorDiseasesList', selectedItems)}
            initialSelectedItems={observations.rtnPriorDiseasesList || []}
          />
          <MultiSelect
            id="rtnCurrentDiseasesList"
            label="Return Current Diseases"
            name="rtnCurrentDiseasesList"
            items={formattedRtnCurrentDiseasesList}
            itemToString={(item) => (item ? item.label : '')}
            onChange={(selectedItems) => handleMultiSelectChange('rtnCurrentDiseasesList', selectedItems)}
            initialSelectedItems={observations.rtnCurrentDiseasesList || []}
          />
          </Column>
          </Grid>
        </FormGroup>
  
        {/* Submit and Cancel Buttons */}
        <div  style={{margin:"10px"}}>
        <Button type="submit" kind="primary">
          Submit
        </Button>
        <Button type="clear" kind="secondary" >
          Cancel
        </Button>
        </div>
      
      </Form>
    );
  };
  
  export default PatientEditByProjectForm;
  
   