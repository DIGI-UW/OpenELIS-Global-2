import React, { useContext, useState, useEffect } from "react";
import { useHistory } from "react-router-dom";
import { useIntl } from "react-intl";
import AddSample from "../../addOrder/AddSample";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";

/**
 * OrderCollect - Step 2: Collect Sample
 *
 * Wraps existing AddSample component.
 * Handles sample collection and test selection.
 */

const OrderCollect = () => {
  const intl = useIntl();
  const history = useHistory();
  const { orderData, samples, setSamples, saveOrder, setCurrentStep } =
    useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [errors, setErrors] = useState([]);

  const elementError = (path) => {
    if (errors?.errors?.length > 0) {
      const error = errors.inner?.find((e) => e.path === path);
      return error ? error.message : null;
    }
    return null;
  };

  // Validate that at least one sample with tests is selected
  const canProceed =
    samples &&
    samples.length > 0 &&
    samples.some((sample) => sample.tests && sample.tests.length > 0);

  const handleSave = async () => {
    try {
      await saveOrder();
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleSaveAndNext = async () => {
    try {
      await saveOrder();
      setCurrentStep(2);
      history.push("/order/label");
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  return (
    <OrderWorkflowLayout
      currentStep={1}
      title="order.step.collect"
      canProceed={canProceed}
      onSave={handleSave}
      onSaveAndNext={handleSaveAndNext}
    >
      {notificationVisible && <AlertDialog />}

      <AddSample
        error={elementError}
        setSamples={setSamples}
        samples={samples}
      />
    </OrderWorkflowLayout>
  );
};

export default OrderCollect;
