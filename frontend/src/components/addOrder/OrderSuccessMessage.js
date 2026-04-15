import React, { useEffect } from "react";
import { Button, Row } from "@carbon/react";
import config from "../../config.json";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { sampleObject } from "./Index";
import { FormattedMessage } from "react-intl";
import PostSavePrintDialog from "../barcodeWorkflow/PostSavePrintDialog";

const OrderSuccessMessage = (props) => {
  const {
    orderFormValues,
    setOrderFormValues,
    setSamples,
    setPage,
    saveResponse,
  } = props;

  const dialogModel = saveResponse?.postSavePrintDialog;
  const accessionNumber =
    dialogModel?.accessionNumber || orderFormValues.sampleOrderItems.labNo;

  // Use the backend-provided printUrl directly. The backend correctly maps
  // "specimen" → "specimenOrder" (prints all specimens for the order) and
  // "block"/"slide" → "blockOrder"/"slideOrder". Rebuilding the URL here
  // with a bare accession number causes a 500 on type=specimen.
  const printableLabels = (() => {
    const types =
      dialogModel?.printableLabelTypes &&
      dialogModel.printableLabelTypes.length > 0
        ? dialogModel.printableLabelTypes
        : [
            {
              labelType: "order",
              quantity: 1,
              printUrl:
                config.serverBaseUrl +
                `/LabelMakerServlet?labNo=${accessionNumber}&type=order`,
            },
          ];

    return types.map((item) => {
      if (typeof item === "string") {
        // Fallback for plain string entries (legacy / unexpected shape)
        return {
          labelType: item,
          quantity: 1,
          printUrl:
            config.serverBaseUrl +
            `/LabelMakerServlet?labNo=${accessionNumber}&type=${item}`,
        };
      }
      return {
        labelType: item.labelType,
        quantity: item.quantity ?? 1,
        dimensionsMm: item.dimensionsMm || "",
        // Prepend serverBaseUrl to the relative path the backend returns
        printUrl: item.printUrl
          ? config.serverBaseUrl + item.printUrl
          : config.serverBaseUrl +
            `/LabelMakerServlet?labNo=${accessionNumber}&type=${item.labelType}`,
      };
    });
  })();

  const handleAnotherSiteOrder = () => {
    const siteId = orderFormValues.sampleOrderItems.referringSiteId;
    const siteName = orderFormValues.sampleOrderItems.referringSiteName;
    const providerId = orderFormValues.sampleOrderItems.providerId;
    const providerFirstName =
      orderFormValues.sampleOrderItems.providerFirstName;
    const providerLastName = orderFormValues.sampleOrderItems.providerLastName;
    const providerWorkPhone =
      orderFormValues.sampleOrderItems.providerWorkPhone;
    const providerFax = orderFormValues.sampleOrderItems.providerFax;
    const providerEmail = orderFormValues.sampleOrderItems.providerEmail;

    setOrderFormValues(SampleOrderFormValues);

    setOrderFormValues({
      ...SampleOrderFormValues,
      rememberSiteAndRequester: true,
      sampleOrderItems: {
        ...SampleOrderFormValues.sampleOrderItems,
        referringSiteId: siteId,
        referringSiteName: siteName,
        providerId: providerId,
        providerFirstName: providerFirstName,
        providerLastName: providerLastName,
        providerWorkPhone: providerWorkPhone,
        providerFax: providerFax,
        providerEmail: providerEmail,
      },
    });
    setPage(0);
  };

  useEffect(() => {
    if (!orderFormValues.rememberSiteAndRequester) {
      setOrderFormValues(SampleOrderFormValues);
    }
    setSamples([sampleObject]);
  }, []);

  return (
    <div className="orderLegendBody">
      <div className="orderEntrySuccessMsg">
        <img
          src={`images/success-icon.png`}
          alt="Order Entry saved successfully"
          width="120"
          height="120"
        />
        <h4>
          <FormattedMessage id="save.success" />
        </h4>
        <Row>
          <PostSavePrintDialog
            accessionNumber={accessionNumber}
            printableLabelTypes={printableLabels}
          />
        </Row>
        <Row>
          {orderFormValues.rememberSiteAndRequester && (
            <Button
              className="placeAnotherOrderBtn"
              kind="tertiary"
              onClick={handleAnotherSiteOrder}
            >
              <FormattedMessage id="request.samesite.order" />
            </Button>
          )}
        </Row>
      </div>
    </div>
  );
};

export default OrderSuccessMessage;
