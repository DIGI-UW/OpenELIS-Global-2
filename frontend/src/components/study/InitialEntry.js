import React, { useState } from 'react';
import { Grid, Column, Dropdown, Select, SelectItem, TextInput, Checkbox, Button } from '@carbon/react';
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
                            <Dropdown id="default" titleText="Form" label="" onChange={(e) => {formChangeHandler(e)}} items={items} itemToString={item => item ? <FormattedMessage id={item.text} /> : ''} />
                        </div>
                    </Column>
                </Grid>
            </div>
            <div className="orderLegendBody">
                <div style={{ display: form === "sample.entry.project.initialARV.title"?"block":"none" }} id="InitialARV_Id">
                    <h3>Initial ARV Entry</h3>
                    <form>
                        <Grid>
                            <Column lg={8} md={4} sm={4}>
                                <Select id="iarv.centerName" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Name</div>} required >
                                    <SelectItem value="" text="Select Center" />
                                    {/* Map organization list here */}
                                </Select>
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="iarv.centerCode" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Center Code</div>} required >
                                    <SelectItem value="" text="Select Code" />
                                    {/* Map organization list here */}
                                </Select>
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="iarv.nameOfDoctor" labelText="Doctor Name" size="lg" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="iarv.receivedDateForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Received Date (dd/mm/yyyy)</div>} required 
                                    maxLength={10}
                                />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="iarv.receivedTimeForDisplay"
                                    labelText="Received Time (HH:mm)"
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="iarv.interviewDate" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Date Taken (dd/mm/yyyy)</div>} required maxLength={10} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="iarv.interviewTime" labelText="Time Taken (HH:mm)" maxLength={5} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="iarv.subjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Unique Health ID Number</div>} required  maxLength={7} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="iarv.siteSubjectNumber" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Site Unique Health ID number</div>} required  />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput
                                    id="iarv.labNoForDisplay"
                                    labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Lab Number (AARC)</div>} required 
                                    maxLength={5}
                                />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="iarv.gender" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Gender</div>} required >
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
                                <TextInput id="iarv.dateOfBirth" labelText={<div><span style={{ color: "red", fontSize: 18 }}>*</span>Birth Date (dd/mm/yyyy)</div>} required  maxLength={10} />
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <TextInput id="iarv.age" labelText="Age (Years)" maxLength={2} />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                <h5>Specimens Collected</h5>
                                <Checkbox defaultChecked id="iarv.dryTubeTaken" labelText="Dry Tube Taken" />
                                <Checkbox defaultChecked id="iarv.edtaTubeTaken" labelText="EDTA Tube Taken" />
                                <h5>Dry Tube Tests</h5>
                                <Checkbox defaultChecked id="iarv.serologyHIVTest" labelText="Serology HIV Test" />
                                <Checkbox id="iarv.glycemiaTest" labelText="Glycemia Test" />
                                <Checkbox defaultChecked id="iarv.creatinineTest" labelText="Creatinine Test" />
                                <Checkbox id="iarv.transaminaseTest" labelText="Transaminase Test" />
                                <h5>EDTA Tube Tests</h5>
                                <Checkbox defaultChecked id="iarv.nfsTest" labelText="NFS Test" />
                                <Checkbox defaultChecked id="iarv.cd4cd8Test" labelText="CD4/CD8 Test" />
                                <h5>Other Tests</h5>
                                <Checkbox id="iarv.viralLoadTest" labelText="Viral Load Test" />
                                <Checkbox id="iarv.genotypingTest" labelText="Genotyping Test" />
                            </Column>

                            <Column lg={16} md={8} sm={4}>
                                {" "}
                                <br />{" "}
                            </Column>

                            <Column lg={8} md={4} sm={4}>
                                <Select id="iarv.underInvestigation" labelText="Under Investigation">
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
                                    id="iarv.underInvestigationComment"
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
            </div>
        </>
    )
}

export default InitialEntry;