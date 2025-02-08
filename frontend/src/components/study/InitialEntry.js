import React, { useState } from 'react';
import { Grid, Column, Row, Dropdown, Select, SelectItem, TextInput, Checkbox, Button } from '@carbon/react';
import { FormattedMessage } from 'react-intl';

const InitialEntry = () => {

    const [form, setForm] = useState(null);

    const formChangeHandler = (e) => {
        console.log(e.selectedItem.text);

        setForm(e.selectedItem.text);
    };

    const items = [{
        text: 'sample.entry.project.initialARV.title'
    }, {
        text: 'sample.entry.project.followupARV.title'
    }, {
        text: 'sample.entry.project.RTN.title'
    }, {
        text: 'sample.entry.project.EID.title'
    }, {
        text: 'sample.entry.project.indeterminate.title'
    }, {
        text: 'sample.entry.project.specialRequest.title'
    }, {
        text: 'sample.entry.project.VL.title'
    }, {
        text: 'sample.entry.project.RT.title'
    }, {
        text: 'sample.entry.project.HPV.title'
    }];


    return (
        <>
            <div className="orderLegendBody">
                <Grid>
                    <Column lg={16} md={8} sm={4}>
                        <h4>
                            <FormattedMessage id="sample.label.initialentry.record" />
                        </h4>
                    </Column>
                    <Column lg={4} md={4} sm={4}>
                        <div>
                            <Dropdown id="default" titleText="Form" label="" onChange={(e) => { formChangeHandler(e) }} items={items} itemToString={item => item ? <FormattedMessage id={item.text} /> : ''} />
                        </div>
                    </Column>
                </Grid>
            </div>
            <div className="orderLegendBody">
                <div style={{ display: form === "sample.entry.project.initialARV.title" ? "block" : "none" }} id="InitialARV_Id">
                    <h3>
                        <FormattedMessage id="sample.entry.project.initialARV.title" />
                    </h3>
                    <form>
                        <Grid>
                            <Column lg={8} md={4} sm={4}>
                                <Select id="centerName" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Name</div>} required >
                                    <SelectItem value="" text="Select Center" />
                                    {/* Map organization list here */}
                                </Select>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="centerCode" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Code</div>} required >
                                    <SelectItem value="" text="Select Code" />
                                    {/* Map organization list here */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="nameOfDoctor" labelText="Doctor Name" size="lg" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="receivedDateForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required
                                    maxLength={10}
                                />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="receivedTimeForDisplay"
                                    labelText="Received Time (HH:mm)"
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date Taken (dd/mm/yyyy)</div>} required maxLength={10} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="interviewTime" labelText="Time Taken (HH:mm)" maxLength={5} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="subjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Unique Health ID Number</div>} required maxLength={7} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="siteSubjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Unique Health ID number</div>} required />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="labNoForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number (AARC)</div>} required
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="gender" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Gender</div>} required >
                                    <SelectItem value="" text="Select Gender" />
                                    <SelectItem value="1" text="Male" />
                                    <SelectItem value="2" text="Female" />

                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="dateOfBirth" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Birth Date (dd/mm/yyyy)</div>} required maxLength={10} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="age" labelText="Age (Years)" maxLength={2} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Specimens Collected</h5>
                                <Checkbox defaultChecked id="dryTubeTaken" labelText="Dry Tube Taken" />
                                <Checkbox defaultChecked id="edtaTubeTaken" labelText="EDTA Tube Taken" />
                                <h5>Dry Tube Tests</h5>
                                <Checkbox defaultChecked id="serologyHIVTest" labelText="Serology HIV Test" />
                                <Checkbox id="glycemiaTest" labelText="Glycemia Test" />
                                <Checkbox defaultChecked id="creatinineTest" labelText="Creatinine Test" />
                                <Checkbox id="transaminaseTest" labelText="Transaminase Test" />
                                <h5>EDTA Tube Tests</h5>
                                <Checkbox defaultChecked id="nfsTest" labelText="NFS Test" />
                                <Checkbox defaultChecked id="cd4cd8Test" labelText="CD4/CD8 Test" />
                                <h5>Other Tests</h5>
                                <Checkbox id="viralLoadTest" labelText="Viral Load Test" />
                                <Checkbox id="genotypingTest" labelText="Genotyping Test" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="underInvestigation" labelText="Under Investigation">
                                    <SelectItem value="" text="Select Option" />
                                    <SelectItem value="1" text="Yes" />
                                    <SelectItem value="2" text="No" />
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <TextInput
                                    id="underInvestigationComment"
                                    labelText="Investigation Comment"
                                    maxLength={1000}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Button type="submit">Submit</Button>
                            </Column>
                        </Grid>
                    </form>
                </div>

                <div style={{ display: form === "sample.entry.project.followupARV.title" ? "block" : "none" }} id="FollowUpARV_Id">
                    <h3>
                        <FormattedMessage id="sample.entry.project.followupARV.title" />
                    </h3>
                    <form>
                        <Grid>
                            <Column lg={8} md={4} sm={4}>
                                <Select id="centerName" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Name</div>} required >
                                    <SelectItem value="" text="Select Center" />
                                    {/* Map organization list here */}
                                </Select>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="centerCode" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Code</div>} required >
                                    <SelectItem value="" text="Select Code" />
                                    {/* Map organization list here */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="nameOfDoctor" labelText="Doctor Name" size="lg" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="receivedDateForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required
                                    maxLength={10}
                                />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="receivedTimeForDisplay"
                                    labelText="Received Time (HH:mm)"
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date Taken (dd/mm/yyyy)</div>} required maxLength={10} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="interviewTime" labelText="Time Taken (HH:mm)" maxLength={5} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="subjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Unique Health ID Number</div>} required maxLength={7} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="siteSubjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Unique Health ID number</div>} required />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="labNoForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number (AARC)</div>} required
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="gender" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Gender</div>} required >
                                    <SelectItem value="" text="Select Gender" />
                                    <SelectItem value="1" text="Male" />
                                    <SelectItem value="2" text="Female" />

                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="dateOfBirth" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Birth Date (dd/mm/yyyy)</div>} required maxLength={10} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="age" labelText="Age (Years)" maxLength={2} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="farv.hiv" labelText="HIV Status">
                                    <SelectItem value="" text="Select Status" />
                                    <SelectItem value="1" text="HIV-1" />
                                    <SelectItem value="2" text="HIV-1+2" />
                                    <SelectItem value="2" text="HIV-2" />
                                    <SelectItem value="2" text="Indeterminate" />
                                    <SelectItem value="2" text="Invalid" />
                                    <SelectItem value="2" text="Negative" />
                                    <SelectItem value="2" text="Positive" />
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Specimens Collected</h5>
                                <Checkbox defaultChecked id="dryTubeTaken" labelText="Dry Tube Taken" />
                                <Checkbox id="edtaTubeTaken" labelText="EDTA Tube Taken" />
                                <h5>Dry Tube Tests</h5>
                                <Checkbox id="serologyHIVTest" labelText="Serology HIV Test" />
                                <Checkbox id="glycemiaTest" labelText="Glycemia Test" />
                                <Checkbox defaultChecked id="creatinineTest" labelText="Creatinine Test" />
                                <Checkbox id="transaminaseTest" labelText="Transaminase Test" />
                                <h5>EDTA Tube Tests</h5>
                                <Checkbox id="nfsTest" labelText="NFS Test" />
                                <Checkbox id="cd4cd8Test" labelText="CD4/CD8 Test" />
                                <h5>Other Tests</h5>
                                <Checkbox id="viralLoadTest" labelText="Viral Load Test" />
                                <Checkbox id="genotypingTest" labelText="Genotyping Test" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="underInvestigation" labelText="Under Investigation">
                                    <SelectItem value="" text="Select Option" />
                                    <SelectItem value="1" text="Yes" />
                                    <SelectItem value="2" text="No" />
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <TextInput
                                    id="underInvestigationComment"
                                    labelText="Investigation Comment"
                                    maxLength={1000}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Button type="submit">Submit</Button>
                            </Column>
                        </Grid>
                    </form>
                </div>

                <div style={{ display: form === "sample.entry.project.RTN.title" ? "block" : "none" }} id="RTN_Id">
                    <h3>
                        <FormattedMessage id="sample.entry.project.RTN.title" />
                    </h3>
                    <form>
                        <Grid>
                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="receivedDateForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required
                                    maxLength={10}
                                />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="receivedTimeForDisplay"
                                    labelText="Received Time (HH:mm)"
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date Taken (dd/mm/yyyy)</div>} required maxLength={10} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="interviewTime" labelText="Time Taken (HH:mm)" maxLength={5} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="dateOfBirth" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Birth Date (dd/mm/yyyy)</div>} required maxLength={10} />
                            </Column>

                            <Column lg={1} style={{ display: "flex", alignItems: "center", justifyContent: "center" }}>
                                <h6>Age</h6>
                            </Column>

                            <Column lg={3} md={4} sm={4}>
                                <TextInput id="age.year" labelText="Year" maxLength={2} />
                            </Column>

                            <Column lg={3} md={4} sm={4}>
                                <TextInput id="age.year" labelText="Month" maxLength={2} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="gender" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Gender</div>} required >
                                    <SelectItem value="" text="Select Gender" />
                                    <SelectItem value="1" text="Male" />
                                    <SelectItem value="2" text="Female" />

                                </Select>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="labNoForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number (LRTN)</div>} required
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Specimens Collected</h5>
                                <Checkbox defaultChecked id="dryTubeTaken" labelText="Dry Tube Taken" />
                                <h5>Dry Tube Tests</h5>
                                <Checkbox defaultChecked id="serologyHIVTest" labelText="Serology HIV Test" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="underInvestigation" labelText="Under Investigation">
                                    <SelectItem value="" text="Select Option" />
                                    <SelectItem value="1" text="Yes" />
                                    <SelectItem value="2" text="No" />
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <TextInput
                                    id="underInvestigationComment"
                                    labelText="Investigation Comment"
                                    maxLength={1000}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Button type="submit">Submit</Button>
                            </Column>
                        </Grid>
                    </form>
                </div>

                <div style={{ display: form === "sample.entry.project.EID.title" ? "block" : "none" }} id="EID_Id">
                    <h3>
                        <FormattedMessage id="sample.entry.project.EID.title" />
                    </h3>
                    <form>
                        <Grid>
                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="receivedDateForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required
                                    maxLength={10}
                                />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="receivedTimeForDisplay"
                                    labelText="Received Time (HH:mm)"
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date Taken (dd/mm/yyyy)</div>} required maxLength={10} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="interviewTime" labelText="Time Taken (HH:mm)" maxLength={5} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="siteName" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Name</div>} required >
                                    <SelectItem value="" text="Select Site Name" />
                                    
                                </Select>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="siteCode" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Code</div>} required >
                                    <SelectItem value="" text="Select Site Name" />
                                    
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={2} md={2} sm={2} style={{ display: "flex", alignItems: "center", justifyContent: "center" }}>
                                <h6>DBS Infant Number</h6>
                            </Column>

                            <Column lg={1} md={1} sm={1} style={{ display: "flex", alignItems: "center", justifyContent: "end" }}>
                                <h5>DBS</h5>
                            </Column>

                            <Column lg={2} md={2} sm={2}>
                                <TextInput id="codeSiteID" labelText="" required maxLength={4} />
                            </Column>

                            <Column lg={2} md={2} sm={2}>
                                <TextInput id="infantID" labelText="" required maxLength={4} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="siteInfantNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>DBS Site Infant Number</div>} required maxLength={18} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="labNoForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number (LRTN)</div>} required
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidWhichPCR" labelText="Which PCR?" >
                                    <SelectItem value="" text="Select PCR" />
                                    <SelectItem value="1" text="1st PCR" />
                                    <SelectItem value="2" text="2nd PCR" />
                                </Select>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="reasonForPCRTest" labelText="Reason for second PCR Test" >
                                    <SelectItem value="" text="Select Reason" />
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="nameOfRequestor" labelText="Name of Requester" />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="nameOfSampler" labelText="Name of Sampler" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <h5>
                                    <FormattedMessage id="sample.entry.project.title.infantInformation" />
                                </h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="dateOfBirth" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date of Birth (dd/mm/yyyy)</div>} required maxLength={10} />
                            </Column>

                            <Column lg={1} style={{ display: "flex", alignItems: "center", justifyContent: "center" }}>
                                <h6>Age</h6>
                            </Column>

                            <Column lg={3} md={4} sm={4}>
                                <TextInput id="month" labelText="Month" maxLength={2} />
                            </Column>

                            <Column lg={3} md={4} sm={4}>
                                <TextInput id="ageWeek" labelText="Week" maxLength={2} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="gender" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Gender</div>} required >
                                    <SelectItem value="" text="Select Gender" />
                                    <SelectItem value="1" text="Male" />
                                    <SelectItem value="2" text="Female" />
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidInfantPTME" labelText="Did the infant benefit from PTME?" >
                                    <SelectItem value="" text="Select Options" />
                                    <SelectItem value="1" text="Yes" />
                                    <SelectItem value="2" text="No" />
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidTypeOfClinic" labelText="From which type of clinic did the infant come from?" >
                                    <SelectItem value="" text="Select Options" />
                                    
                                </Select>
                            </Column>

                            <Column lg={4} md={4} sm={4}>
                                <TextInput id="eidTypeOfClinicOther" labelText="Specify" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidHowChildFed" labelText="How is the child fed?" >
                                    <SelectItem value="" text="Select Options" />
                                    
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidStoppedBreastfeeding" labelText="Stopped Breast Feeding" >
                                    <SelectItem value="" text="Select Options" />
                                    
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidInfantSymptomatic" labelText="Is the child presenting signs of HIV/AIDS?" >
                                    <SelectItem value="" text="Select Options" />
                                    
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidInfantProphy" labelText="Infant's Prophylaxis ARV" >
                                    <SelectItem value="" text="Select Options" />
                                    
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidInfantCotrimoxazole" labelText="Infant taking cotrimoxazole?" >
                                    <SelectItem value="" text="Select Options" />
                                    
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <h5>
                                    <FormattedMessage id="sample.entry.project.title.mothersInformation" />
                                </h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidMothersHIVStatus" labelText="Mother's HIV Status" >
                                    <SelectItem value="" text="Select Options" />
                                    
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="eidMothersARV" labelText="Mother's ARV Treatment" >
                                    <SelectItem value="" text="Select Options" />
                                    
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <hr />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="underInvestigation" labelText="Under Investigation">
                                    <SelectItem value="" text="Select Option" />
                                    <SelectItem value="1" text="Yes" />
                                    <SelectItem value="2" text="No" />
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <TextInput
                                    id="underInvestigationComment"
                                    labelText="Investigation Comment"
                                    maxLength={1000}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Specimens Collected</h5>
                                <Checkbox id="dryTubeTaken" labelText="Dry Tube" />
                                <Checkbox defaultChecked id="dbsTaken" labelText="Dry Blood Spot" />
                                <h5>Tests</h5>
                                <Checkbox defaultChecked id="dnaPCRMessage" labelText="DNA PCR" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Button type="submit">Submit</Button>
                            </Column>
                        </Grid>
                    </form>
                </div>
            </div>
        </>
    )
}

export default InitialEntry;