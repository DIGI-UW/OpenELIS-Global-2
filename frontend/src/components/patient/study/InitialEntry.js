import React, { useState } from 'react';
import { Grid, Column, Row, Dropdown, Select, SelectItem } from '@carbon/react';
import { FormattedMessage, useIntl } from 'react-intl';

const InitialEntry = () => {

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
            <div className="orderLegendBody">
                <Grid>
                    <Column lg={16} md={8} sm={4}>
                        <h4>
                            <FormattedMessage id="sidenav.label.study.initialentry" />
                        </h4>
                    </Column>
                    <Column lg={4} md={4} sm={4}>
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
    )
}

export default InitialEntry;