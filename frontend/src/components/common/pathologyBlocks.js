import React, { useRef } from "react";
import {
    IconButton,
    Grid,
    Column,
    TextArea,
    Select,
    SelectItem,
    Button,
    TextInput,
} from "@carbon/react";

import { FormattedMessage, useIntl } from "react-intl";
import { Launch, Subtract, ArrowLeft, ArrowRight } from "@carbon/react/icons";


const PathologyBlocks = ({
    ThisPathologySampleInfo,
    ThisIntl,
    ThisBlocksToAdd,
    OnRemoveBlock,  
    OnRemoveBlockNumberChange,
    OnBlockLocationChange,
    onPrintBlockNumber,
    OnsetBlocksToAdd,
    OnAddBlocks
}) => {
    return (
    <Grid fullWidth={true} className="gridBoundary">
        <Column lg={16} md={8} sm={4}>
        <h5>
            <FormattedMessage id="pathology.label.blocks" />
        </h5>
        <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
        </Column>
        {ThisPathologySampleInfo.blocks &&
        ThisPathologySampleInfo.blocks.map((block, index) => {
            return (
            <>
                <Column lg={2} md={8} sm={4}>
                    <IconButton
                        label={ThisIntl.formatMessage({
                        id: "label.button.remove.block",
                        })}
                        onClick={() => {OnRemoveBlock(index);}}
                        kind="tertiary"
                        size="sm"
                    >
                        <Subtract size={18} />{" "}
                        <FormattedMessage id="pathology.label.block" />
                    </IconButton>
                </Column>
                <Column lg={3} md={2} sm={1} key={index}>
                    <TextInput
                        id="blockNumber"
                        labelText={ThisIntl.formatMessage({
                        id: "pathology.label.block.number",
                        })}
                        hideLabel={true}
                        placeholder={ThisIntl.formatMessage({
                        id: "pathology.label.block.number",
                        })}
                        value={block.blockNumber}
                        type="number"
                        onChange={(e) => {OnRemoveBlockNumberChange(e.target.value, index);}}
                    />
                </Column>
                <Column lg={3} md={2} sm={1}>
                <TextInput
                    id="location"
                    labelText={ThisIntl.formatMessage({
                    id: "pathology.label.location",
                    })}
                    hideLabel={true}
                    placeholder={ThisIntl.formatMessage({
                    id: "pathology.label.location",
                    })}
                    value={block.location}
                    onChange={(e) => {OnBlockLocationChange(e.target.value, index);}}
                />
                </Column>
                <Column lg={3} md={2} sm={2}>
                <Button
                    onClick={() => {onPrintBlockNumber(block)}}
                >
                    {" "}
                    <FormattedMessage id="pathology.label.printlabel" />
                </Button>
                </Column>
                <Column lg={5} md={2} sm={0} />
                <Column lg={16} md={8} sm={4}>
                <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>
            </>
            );
        })}
        <Column lg={2} md={8} sm={4}>
            <TextInput
                id="blocksToAdd"
                labelText={ThisIntl.formatMessage({
                id: "pathology.label.block.add.number",
                })}
                hideLabel={true}
                placeholder={ThisIntl.formatMessage({
                id: "pathology.label.block.add.number",
                })}
                value={ThisBlocksToAdd}
                type="number"
                onChange={(e) => {
                OnsetBlocksToAdd(e.target.value);
                }}
            />
        </Column>
        <Column lg={14} md={8} sm={4}>
            <Button
                onClick={() => {
                    OnAddBlocks(ThisPathologySampleInfo, ThisBlocksToAdd)
                }}
            >
                <FormattedMessage id="pathology.label.addblock" />
            </Button>
        </Column>
    </Grid>
    );
}

export default PathologyBlocks;


