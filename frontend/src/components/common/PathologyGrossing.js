import React from "react";
import { Grid, Column, TextArea, Select, SelectItem} from "@carbon/react";
import { FormattedMessage } from "react-intl";

const PathologyGrossing = ({
    intl,
    grossing,
    microscopyExam,
    onGrossingChange,
    onMicroscopyExamChange,
    statusId,
    statusName,
    statusValue,
    ThisStatuses = [],
    onStatusChange,
    technicianId,
    technicianName,
    technicianValue,
    technicianUsers = [],
    pathologistUsers = [],
    assignedPathologist,
    pathologistValue,
}) => {
    return (
        <Grid fullWidth={true} className="gridBoundary">
            <Column lg={4} md={2} sm={2}>
                <Select
                    id= {statusId}
                    name={statusName}
                    labelText={intl.formatMessage({id: "label.button.select.status"})}
                    value={statusValue}
                    onChange={(event) => {onStatusChange(event.target.value)}}
                >
                    <SelectItem disabled value="placeholder" text="Status" />

                    {ThisStatuses.map((status, index) => {
                    return (
                        <SelectItem key={index} text={status.value} value={status.id} />
                    );
                    })}
                </Select>
            </Column>

            <Column lg={4} md={2} sm={2}>
                <Select
                    id={technicianId}
                    name={technicianName}
                    labelText={intl.formatMessage({
                    id: "label.button.select.technician",
                    })}
                    value={technicianValue}
                    onChange={(event) => {onTechnicianChange(event.target.value)}}
                >
                <SelectItem />
                    {technicianUsers.map((user, index) => {
                    return (
                        <SelectItem key={index} text={user.value} value={user.id} />
                    );
                    })}
                </Select>
            </Column>


            <Column lg={4} md={2} sm={2}>
                <Select
                    id={assignedPathologist}
                    name={assignedPathologist}
                    labelText={
                    <FormattedMessage id="label.button.select.pathologist" />
                    }
                    value={pathologistValue}
                    onChange={(event) => {onPathologistChange(event.target.value)}}
                >
                    <SelectItem />
                    {pathologistUsers.map((user, index) => {
                    return (
                        <SelectItem key={index} text={user.value} value={user.id} />
                    );
                    })}
                </Select>
            </Column>

            <Column lg={16} md={8} sm={4}>
                <TextArea
                    labelText={
                    <FormattedMessage id="pathology.label.grossexam" />
                    }
                    value={grossing}
                    onChange={(e) => {onGrossingChange(e.target.value)}}
                />
            </Column>
            <Column lg={16} md={8} sm={4}>
                <TextArea
                    labelText={
                    <FormattedMessage id="pathology.label.microexam" />
                    }
                    value={microscopyExam}
                    onChange={(e) => {onMicroscopyExamChange(e.target.value)}}
                />
            </Column>
        </Grid>
    )
}

export default PathologyGrossing;
