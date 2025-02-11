import { React, useRef, useState } from "react";
import StudyEOrderSearch from "./StudyEOrderSearch";
import StudyEOrder from "./StudyEOrder";

export { default as StudyEOrderSearch } from "./StudyEOrderSearch";
export { default as StudyEOrder } from "./StudyEOrder";

const EOrderPage = () => {
  const eOrderRef = useRef(null);
  const [eOrders, setEOrders] = useState([]);
  return (
    <>
      <StudyEOrderSearch setEOrders={setEOrders} eOrderRef={eOrderRef} />
      <StudyEOrder
        eOrderRef={eOrderRef}
        eOrders={eOrders}
        setEOrders={setEOrders}
      />
    </>
  );
};

export default EOrderPage;
