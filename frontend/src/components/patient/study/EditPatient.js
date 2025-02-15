import React, { useState } from "react";
import SearchPatientForm from "../SearchPatientForm";
import { FormattedMessage, useIntl } from "react-intl";
import { Grid, Column, Section, Heading, Select, SelectItem } from "@carbon/react";
import PageBreadCrumb from "../../common/PageBreadCrumb";


function EditPatient() {
    const breadcrumbs = [
        { label: "home.label", link: "/" },
        { label: "patient.label.edit", link: "/EditPatient" },
    ];
    const [form, setForm] = useState(null);

    const formChangeHandler = (e) => {
        console.log(e.selectedItem.text);

        setForm(e.selectedItem.text);
    };

    const intl = useIntl();
    const items = [{
        value: 'InitialARV_Id',
        text: 'sample.entry.project.initialARV.title'
    }, {
        value: 'FollowUpARV_Id',
        text: 'sample.entry.project.followupARV.title'
    }, {
        value: 'RTN_Id',
        text: 'sample.entry.project.RTN.title'
    }, {
        value: 'EID_Id',
        text: 'sample.entry.project.EID.title'
    }, {
        value: 'VL_Id',
        text: 'sample.entry.project.VL.title'
    }, {
        value: 'Recency_Id',
        text: 'sample.entry.project.RT.title'
    }];
    return (
        <>
            <PageBreadCrumb breadcrumbs={breadcrumbs} />
            <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                    <Section>
                        <Section>
                            <Heading>
                                <FormattedMessage id="patient.label.edit" />
                            </Heading>
                        </Section>
                    </Section>
                </Column>
            </Grid>
            <div className="orderLegendBody">
                <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                        <FormattedMessage
                            id="search.patient.label"
                            defaultMessage="Search for Patient"
                        />
                    </Column>
                </Grid>
                <br></br>
                <SearchPatientForm />
                <br></br>
                <Grid>
                    <Column md={4} sm={4}>
                        <Select id="studyFormsID" labelText="" >
                            <SelectItem value='0' selectedItem />
                            {items.map((item, index) => {
                                return (
                                    <SelectItem key={index} value={item.value} text={intl.formatMessage({ id: item.text })} />
                                )
                            })}
                        </Select>
                    </Column>
                </Grid>
            </div>
        </>
    );
}


export default EditPatient;
