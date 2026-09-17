import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { useOrderContext } from "./OrderContext";

/**
 * True when the URL addresses an existing order belonging to this workflow.
 *
 * Every navigation that opens an order carries the lab number as a query
 * parameter — the stepper and the dashboard's continue/accept/fix actions use
 * `?order=`, the barcode scanner uses `?labNumber=`. Only "New Order" and the
 * side navigation arrive without one, so the parameter is what distinguishes
 * "continue this order" from "start a new one".
 */
export const addressesExistingOrder = (location, workflowPrefix) => {
  const params = new URLSearchParams(location.search || "");
  const orderParam = params.get("order") || params.get("labNumber");
  return Boolean(orderParam) && location.pathname.startsWith(workflowPrefix);
};

/**
 * Clears the order context on mount unless the URL addresses an order in this
 * workflow, and reports whether this mount is a new order.
 *
 * The reset is decided by where the user navigated, never by context state.
 * Keying it on `isEditMode` left the previous order's lab number and patient on
 * the form whenever a user pressed Edit and then re-entered the step from the
 * side navigation, because edit mode outlives the step that set it.
 */
export const useNewOrderReset = (workflowPrefix) => {
  const location = useLocation();
  const { resetOrder } = useOrderContext();
  // Frozen at mount: later edits to the URL (the context strips a foreign
  // ?order= after loading it) must not turn a continued order into a new one.
  const [isNewOrder] = useState(
    () => !addressesExistingOrder(location, workflowPrefix),
  );

  useEffect(() => {
    if (isNewOrder) {
      resetOrder();
    }
  }, []);

  return isNewOrder;
};

export default useNewOrderReset;
