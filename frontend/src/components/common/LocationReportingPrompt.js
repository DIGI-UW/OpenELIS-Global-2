import React, { useEffect } from "react";
import { confirmAlert } from "react-confirm-alert";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";

const PROMPT_MESSAGE = `To enhance our understanding of the geographical reach and scale of OpenELIS usage, we kindly request your consent to collect anonymous location information. Please be assured that this data will be used solely for statistical analysis to improve our services. We want to emphasize that no clinical or personal information will be collected or stored. Your participation is entirely voluntary and will greatly contribute to optimizing the effectiveness of OpenELIS. If you agree to share this anonymous location data, please opt in by choosing Yes (default) or No.`;

function LocationReportingPrompt() {
  useEffect(() => {
    const checkAndPrompt = () => {
      // use shared helper so CSRF and credentials handling is consistent
      try {
        getFromOpenElisServer("/rest/locationreporting", (json) => {
          if (json && json.configured === false) {
            const options = {
              title: "Share anonymous location data?",
              message: PROMPT_MESSAGE,
              buttons: [
                {
                  label: "Yes",
                  onClick: () => {
                    postToOpenElisServer(
                      "/rest/locationreporting",
                      JSON.stringify({ optIn: true }),
                      () => {},
                    );
                  },
                },
                {
                  label: "No",
                  onClick: () => {
                    postToOpenElisServer(
                      "/rest/locationreporting",
                      JSON.stringify({ optIn: false }),
                      () => {},
                    );
                  },
                },
              ],
              closeOnEscape: true,
              closeOnClickOutside: false,
            };

            // show prompt (Yes is the first button, effectively the default)
            confirmAlert(options);
          }
        });
      } catch (e) {
        console.error("LocationReportingPrompt error", e);
      }
    };

    checkAndPrompt();
  }, []);

  return null; // invisible component
}

export default LocationReportingPrompt;
