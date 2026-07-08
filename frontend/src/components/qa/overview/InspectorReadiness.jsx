import React, { useState } from "react";
import { Accordion, AccordionItem } from "@carbon/react";
import { useIntl } from "react-intl";
import ComingSoon from "./ComingSoon";

const STORAGE_KEY = "qa.overview.inspectorOpen";

const QUESTIONS = [
  { titleKey: "qa.overview.inspector.q1", ticket: "OGC-694" },
  { titleKey: "qa.overview.inspector.q2", ticket: "OGC-721" },
  { titleKey: "qa.overview.inspector.q3", ticket: "OGC-695" },
  { titleKey: "qa.overview.inspector.q4", ticket: "OGC-699" },
  { titleKey: "qa.overview.inspector.q5", ticket: "OGC-716" },
];

const InspectorReadiness = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.inspector" });
  // Collapsed by default; open state is sticky per browser session (OGC-694).
  const [open, setOpen] = useState(
    () => sessionStorage.getItem(STORAGE_KEY) === "1",
  );
  const handleHeadingClick = ({ isOpen }) => {
    setOpen(isOpen);
    sessionStorage.setItem(STORAGE_KEY, isOpen ? "1" : "0");
  };

  return (
    <section className="qa-overview-section" aria-label={title}>
      <Accordion>
        <AccordionItem
          title={title}
          open={open}
          onHeadingClick={handleHeadingClick}
        >
          <div className="qa-cs-rows">
            {QUESTIONS.map((question) => (
              <ComingSoon key={question.titleKey} variant="row" {...question} />
            ))}
          </div>
        </AccordionItem>
      </Accordion>
    </section>
  );
};

export default InspectorReadiness;
