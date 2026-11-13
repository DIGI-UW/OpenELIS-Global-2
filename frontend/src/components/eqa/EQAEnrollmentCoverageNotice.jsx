import React, { useEffect, useState } from "react";
import { InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const idSet = (rows) => new Set((rows || []).map((row) => String(row.id)));

const panelTestIds = (panel) =>
  String(panel.testIds || "")
    .split(",")
    .map((id) => id.trim())
    .filter(Boolean);

/**
 * What the laboratory has picked for this EQA order that its enrolment in the
 * programme does not cover. A warning only: an order outside the enrolment is
 * still a valid order, and nothing here refuses one.
 */
export const testsOutsideEnrollment = (samples, enrollment) => {
  const mappedTests = idSet(enrollment.tests);
  const mappedPanels = idSet(enrollment.panels);

  // A panel the enrolment covers brings its own tests with it, so those tests
  // are covered too even when the enrolment does not name them one by one.
  const coveredByPanel = new Set();
  const outsidePanels = [];
  for (const sample of samples || []) {
    for (const panel of sample.panels || []) {
      if (mappedPanels.has(String(panel.id))) {
        panelTestIds(panel).forEach((id) => coveredByPanel.add(id));
      } else {
        outsidePanels.push(panel.name);
      }
    }
  }

  const outsideTests = [];
  for (const sample of samples || []) {
    for (const test of sample.tests || []) {
      const id = String(test.id);
      if (!mappedTests.has(id) && !coveredByPanel.has(id)) {
        outsideTests.push(test.name);
      }
    }
  }

  return [...new Set([...outsidePanels, ...outsideTests])];
};

const EQAEnrollmentCoverageNotice = ({ enrollmentId, samples }) => {
  const intl = useIntl();
  const [enrollment, setEnrollment] = useState(null);

  useEffect(() => {
    if (!enrollmentId) {
      setEnrollment(null);
      return undefined;
    }
    let mounted = true;
    getFromOpenElisServer(
      `/rest/eqa/my-programs/${enrollmentId}`,
      (response) => {
        if (mounted) {
          setEnrollment(response && response.id ? response : null);
        }
      },
    );
    return () => {
      mounted = false;
    };
  }, [enrollmentId]);

  if (!enrollment) {
    return null;
  }

  const outside = testsOutsideEnrollment(samples, enrollment);
  if (outside.length === 0) {
    return null;
  }

  return (
    <InlineNotification
      kind="warning"
      lowContrast
      hideCloseButton
      title={intl.formatMessage({ id: "eqa.order.coverage.title" })}
      subtitle={intl.formatMessage(
        { id: "eqa.order.coverage.subtitle" },
        { tests: outside.join(", ") },
      )}
    />
  );
};

export default EQAEnrollmentCoverageNotice;
