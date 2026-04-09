import React, { useEffect } from "react";
import { Button, Row } from "@carbon/react";
import config from "../../config.json";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { sampleObject } from "./Index";
import { FormattedMessage } from "react-intl";

const OrderSuccessMessage = (props) => {
  const { orderFormValues, setOrderFormValues, setSamples, setPage } = props;

  const handlePrintBarCode = () => {
    let barcodesPdf =
      config.serverBaseUrl +
      `/LabelMakerServlet?labNo=${orderFormValues.sampleOrderItems.labNo}`;
    window.open(barcodesPdf);
  };

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
          <Button
            data-cy="printBarCode"
            className=""
            onClick={handlePrintBarCode}
          >
            <FormattedMessage id="print.barcode" />
          </Button>
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
