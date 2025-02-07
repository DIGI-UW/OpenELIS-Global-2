import React from 'react';
import { Grid, Column, Dropdown } from '@carbon/react';
import { FormattedMessage } from 'react-intl';

const InitialEntry = () => {


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
                    <Column>
                        <div style={{ width: 400 }}>
                            <Dropdown id="default" titleText="Form" label=""  items={items} itemToString={item => item ? <FormattedMessage id={item.text} /> : ''} />
                        </div>
                    </Column>
                </Grid>
            </div>
        </>
    )
}

export default InitialEntry;