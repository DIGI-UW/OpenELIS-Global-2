import React, { useEffect } from "react";
import { confirmAlert } from "react-confirm-alert";
import config from "../../config.json";

const PROMPT_MESSAGE = `To enhance our understanding of the geographical reach and scale of OpenELIS usage, we kindly request your consent to collect anonymous location information. Please be assured that this data will be used solely for statistical analysis to improve our services. We want to emphasize that no clinical or personal information will be collected or stored. Your participation is entirely voluntary and will greatly contribute to optimizing the effectiveness of OpenELIS. If you agree to share this anonymous location data, please opt in by choosing Yes (default) or No.`;

function LocationReportingPrompt() {
  useEffect(() => {
    const checkAndPrompt = async () => {
      try {
        const resp = await fetch(config.serverBaseUrl + "/rest/locationreporting", {
          credentials: "include",
        });
        if (resp.status === 200) {
          const json = await resp.json();
          // if opt-in was not explicitly configured, prompt the user
          if (json && json.configured === false) {
            const options = {
              title: "Share anonymous location data?",
              message: PROMPT_MESSAGE,
              buttons: [
                {
                  label: "Yes",
                  onClick: async () => {
                    await fetch(config.serverBaseUrl + "/rest/locationreporting", {
                      method: "POST",
                      credentials: "include",
                      headers: {
                        "Content-Type": "application/json",
                        "X-CSRF-Token": localStorage.getItem("CSRF"),
                      },
                      body: JSON.stringify({ optIn: true }),
                    });
                  },
                },
                {
                  label: "No",
                  onClick: async () => {
                    await fetch(config.serverBaseUrl + "/rest/locationreporting", {
                      method: "POST",
                      credentials: "include",
                      headers: {
                        "Content-Type": "application/json",
                        "X-CSRF-Token": localStorage.getItem("CSRF"),
                      },
                      body: JSON.stringify({ optIn: false }),
                    });
                  },
                },
              ],
              closeOnEscape: true,
              closeOnClickOutside: false,
            };

            // show prompt (Yes is the first button, effectively the default)
            confirmAlert(options);
          }
        }
      } catch (e) {
        console.error("LocationReportingPrompt error", e);
      }
    };

    checkAndPrompt();
  }, []);

  return null; // invisible component
}

export default LocationReportingPrompt;
