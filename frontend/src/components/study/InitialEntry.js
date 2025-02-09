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

                <div style={{ display: form === "sample.entry.project.indeterminate.title" ? "block" : "none" }} id="Indeterminate_Id">
                    <h3>
                        <FormattedMessage id="sample.entry.project.indeterminate.title" />
                    </h3>
                    <form>
                        <Grid>
                            <Column lg={8} md={4} sm={4}><TextInput id="receivedDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="receivedTime" labelText="Received Time (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Interview Date (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="interviewTime" labelText="Interview Time (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="siteName" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Name</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="address" labelText="Address" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={5} md={4} sm={4}><TextInput id="phoneNumber" labelText="Phone Number" /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="faxNumber" labelText="Fax Number" /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="email" labelText="Email" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={5} md={4} sm={4}><TextInput id="subjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Unique Health ID number</div>} required maxLength={7} /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="siteSubjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Unique Health ID number</div>} required /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="labNo" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number</div>} required maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="gender" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Gender</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Gender options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="birthDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date of Birth (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="age" labelText="Age" maxLength={2} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>First Test</h5>
                            </Column>

                            <Column lg={5} md={4} sm={4}><TextInput id="firstTestDate" labelText="Date" maxLength={10} /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="firstTestName" labelText="Test Name" /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="firstTestResult" labelText="Result" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Second Test</h5>
                            </Column>

                            <Column lg={5} md={4} sm={4}><TextInput id="secondTestDate" labelText="Date" maxLength={10} /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="secondTestName" labelText="Test Name" /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="secondTestResult" labelText="Result" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />
                                <br />
                                {" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="siteFinalResult" labelText="Final Result of Site" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Specimen</h5>
                            </Column>

                            <Column lg={8} md={4} sm={4}><Checkbox defaultChecked id="dryTubeTaken" labelText="ARV Dry Tube Taken" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Dry Tube Tests</h5>
                            </Column>

                            <Column lg={8} md={4} sm={4}><Checkbox defaultChecked id="serologyHIVTest" labelText="Serology HIV Test" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="underInvestigation" labelText="Under Investigation">
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="underInvestigationComment" labelText="Investigation Comment" maxLength={1000} /></Column>

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

                <div style={{ display: form === "sample.entry.project.specialRequest.title" ? "block" : "none" }} id="SpecialRequest_Id">
                    <h3>
                        <FormattedMessage id="sample.entry.project.specialRequest.title" />
                    </h3>
                    <form>
                        <Grid>
                            <Column lg={8} md={4} sm={4}><TextInput id="receivedDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="receivedTime" labelText="Received Time (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Interview Date (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="interviewTime" labelText="Interview Time (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="siteName" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Name</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={5} md={4} sm={4}><TextInput id="subjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Unique Health ID number</div>} required maxLength={7} /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="siteSubjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Unique Health ID number</div>} required /></Column>
                            <Column lg={5} md={4} sm={4}><TextInput id="labNo" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number (LSPE)</div>} required maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="gender" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Gender</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Gender options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="birthDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date of Birth (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="age" labelText="Age (year)" maxLength={2} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="reasonForRequest" labelText="Reason for Request" >
                                    <SelectItem value="" text="Select" />
                                    {/* Gender options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Specimens Collected</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox id="dryTubeTaken" labelText="Dry Tube" />
                                <Checkbox id="edtaTubeTaken" labelText="EDTA tube" />
                                <Checkbox id="dbsTaken" labelText="Dry Blood Spot" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Dry Tube Tests</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox id="murexTest" labelText="Murex Combinaison" />
                                <Checkbox id="genscreenTest" labelText="Genscreen" />
                                <Checkbox id="vironostikaTest" labelText="Vironostika" />
                                <Checkbox id="innoliaTest" labelText="Innolia" />
                                <Checkbox id="glycemiaTest" labelText="Glycemia Test" />
                                <Checkbox id="creatinineTest" labelText="Creatinine Test" />
                                <Checkbox id="transaminaseTest" labelText="Transaminase Test" />
                                <Checkbox id="transaminaseALTLTest" labelText="Transaminase ALTL" />
                                <Checkbox id="transaminaseASTLTest" labelText="Transaminase ASTL" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>EDTA Tube Tests</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox id="nfsTest" labelText="NFS Test" />
                                <Checkbox id="gbTest" labelText="GB" />
                                <Checkbox id="lymphTest" labelText="Lymph %" />
                                <Checkbox id="monoTest" labelText="Mono" />
                                <Checkbox id="eoTest" labelText="Eo %" />
                                <Checkbox id="basoTest" labelText="Baso %" />
                                <Checkbox id="grTest" labelText="GR" />
                                <Checkbox id="hbTest" labelText="HB" />
                                <Checkbox id="hctTest" labelText="HCT" />
                                <Checkbox id="vgmTest" labelText="VGM" />
                                <Checkbox id="tcmhTest" labelText="TCMH" />
                                <Checkbox id="ccmhTest" labelText="CCMH" />
                                <Checkbox id="plqTest" labelText="PLQ" />
                                <Checkbox id="cd4cd8Test" labelText="CD4/CD8 Test" />
                                <Checkbox id="cd3CountTest" labelText="CD3 Count" />
                                <Checkbox id="cd4CountTest" labelText="CD4 Count" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Other Tests</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox id="dnaPCR" labelText="DNA PCR" />
                                <Checkbox id="viralLoadTest" labelText="Viral Load Test" />
                                <Checkbox id="genotypingTest" labelText="Genotyping" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="underInvestigation" labelText="Under Investigation">
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="underInvestigationComment" labelText="Investigation Comment" maxLength={1000} /></Column>

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

                <div style={{ display: form === "sample.entry.project.VL.title" ? "block" : "none" }} id="VL_Id">
                    <h3>
                        <FormattedMessage id="sample.entry.project.VL.title" />
                    </h3>
                    <form>
                        <Grid>
                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="centerName" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Name</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="centerCode" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Code</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="nameOfDoctor" labelText="Name of clinician" /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="nameOfSampler" labelText="Name of Sampler" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="receivedDateForDisplay" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="receivedTimeForDisplay" labelText="Received Time (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date Taken (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="interviewTime" labelText="Time Taken (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="subjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Unique Health ID number</div>} required maxLength={7} />
                            </Column>


                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="siteSubjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Unique Health ID number</div>} required maxLength={18} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="labNoForDisplay" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number (AARC)</div>} required maxLength={5} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="birthDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date of Birth (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="age" labelText="Age (year)" maxLength={2} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="gender" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Gender</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Gender options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="vlPregnancy" labelText="Pregnant" >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="vlSuckle" labelText="Breastfeeding" >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="hivStatus" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>HIV Type</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
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

                            <Column lg={5} md={3} sm={2}>
                                <Select id="currentARVTreatment" labelText="Is the patient currently receiving ARV treatment ?" >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="arvTreatmentInitDate" labelText="If yes, Date ARV treatment initiation" required maxLength={10} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={3} sm={2}>
                                <Select id="arvTreatmentRegime" labelText="Therapeutic line" >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <hr />{" "}
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="vlReasonForRequest" labelText="Reason of viral load request" >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="vlOtherReasonForRequest" labelText="	Specify" maxLength={50} />
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

                            <Column lg={16} md={8} sm={4}>
                                <h5>At treatment initiation</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="initcd4Count" labelText="CD4 Count" maxLength={4} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="initcd4Percent" labelText="CD4 Percentage Count" maxLength={10} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="initcd4Date" labelText="Date" maxLength={10} />
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
                                <h5>Viral Load request</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="demandcd4Count" labelText="CD4 Count" maxLength={4} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="demandcd4Percent" labelText="CD4 Percentage Count" maxLength={10} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="demandcd4Date" labelText="Date" maxLength={10} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <hr />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={2}>
                                <Select id="vlBenefit" labelText="Prior Viral Load Request made ?" >
                                    <SelectItem value="" text="Select" />
                                    {/* options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="priorVLLab" labelText="If yes, specify the laboratory" maxLength={10} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="priorVLValue" labelText="Value" maxLength={10} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="priorVLDate" labelText="Date" maxLength={10} />
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

                            {/* Patient record status TO BE implemented. */}

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
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="underInvestigationComment" labelText="Investigation Comment" maxLength={1000} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Specimens Collected</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox id="edtaTubeTaken" labelText="EDTA tube" />
                                <Checkbox id="dbsvlTaken" labelText="Dry Blood Spot" />
                                <Checkbox id="pscvlTaken" labelText="PSC (?)" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Tests</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox defaultChecked id="viralLoadTest" labelText="Viral Load Test" />
                                
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

                <div style={{ display: form === "sample.entry.project.RT.title" ? "block" : "none" }} id="RT_Id">
                    <h3>
                        <FormattedMessage id="sample.entry.project.RT.title" />
                    </h3>
                    <form>
                        <Grid>
                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Facility</h5>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="centerCode" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Code</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Patient Informations</h5>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="labNoForDisplay" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number (RTRI)</div>} required maxLength={5} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="siteSubjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Recency ID No</div>} required maxLength={18} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="birthDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date of Birth (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="age" labelText="Age (year)" maxLength={2} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="gender" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Gender</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Gender options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="vlPregnancy" labelText="Pregnant" >
                                    <SelectItem value="" text="Select" />
                                    {/* Gender options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="vlSuckle" labelText="Breastfeeding" >
                                    <SelectItem value="" text="Select" />
                                    {/* Gender options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Sample information</h5>
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="nameOfDoctor" labelText="Name of clinician" /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="nameOfSampler" labelText="Name of Sampler" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="receivedDateForDisplay" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="receivedTimeForDisplay" labelText="Received Time (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date Taken (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="interviewTime" labelText="Time Taken (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Sample Type</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox defaultChecked id="plasmataken" labelText="Plasma" />
                                <Checkbox id="serumTaken" labelText="Serum" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Tests</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox defaultChecked id="asanteTest" labelText="Asante HIV-1 Rapid Recency" />
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

                <div style={{ display: form === "sample.entry.project.HPV.title" ? "block" : "none" }} id="HPV_Id">
                    <h3>
                        <FormattedMessage id="sample.entry.project.HPV.title" />
                    </h3>
                    <form>
                        <Grid>
                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Facility</h5>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="centerCode" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Code</div>} required >
                                    <SelectItem value="" text="Select" />
                                    {/* Options dynamically populated */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Patient Informations</h5>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="labNoForDisplay" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number (RTRI)</div>} required maxLength={5} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="siteSubjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Patient identification</div>} required maxLength={18} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="hivStatus" labelText="HIV Status" >
                                    <SelectItem value="" text="Select" />

                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="birthDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date of Birth (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="age" labelText="Age (year)" maxLength={2} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Sample information</h5>
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="nameOfDoctor" labelText="Name of clinician" /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="receivedDateForDisplay" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="receivedTimeForDisplay" labelText="Received Time (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}><TextInput id="interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date Taken (dd/mm/yyyy)</div>} required maxLength={10} /></Column>
                            <Column lg={8} md={4} sm={4}><TextInput id="interviewTime" labelText="Time Taken (HH:mm)" maxLength={5} /></Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={5} md={3} sm={2}>
                                <Select id="hpvSamplingMethod" labelText="Sample Type" >
                                    <SelectItem value="" text="Select" />

                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Specimens Collected</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox defaultChecked id="preservCytTaken" labelText="PreservCyt (Cervico-vaginal sample)" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Tests</h5>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <Checkbox defaultChecked id="hpvTest" labelText="test HPV HR" />
                                <Checkbox defaultChecked id="abbottOrRocheAnalysis" labelText="Analysis on Abbott or Roche equipment" />
                                <Checkbox id="geneXpertAnalysis" labelText="Analysis on GeneXpert" />
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