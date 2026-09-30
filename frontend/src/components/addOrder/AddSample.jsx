import React, { useEffect, useRef, useState } from "react";
import { Button, Link, Column, Grid } from "@carbon/react";
import { Add } from "@carbon/react/icons";
import { getFromOpenElisServer } from "../utils/Utils";
import SampleType from "./SampleType";
import { applySampleTypeUpdate, newSampleKey } from "./sampleTypeUpdate";
import { FormattedMessage } from "react-intl";
const AddSample = (props) => {
  const { samples, setSamples, error, domain, allowReferral } = props;
  const componentMounted = useRef(false);

  const [rejectSampleReasons, setRejectSampleReasons] = useState([]);

  const handleAddNewSample = () => {
    let updateSamples = [...samples];
    updateSamples.push({
      key: newSampleKey(),
      index: updateSamples.length + 1,
      sampleRejected: false,
      rejectionReason: "",
      requestReferralEnabled: false,
      referralItems: [],
      sampleTypeId: "",
      sampleXML: null,
      panels: [],
      tests: [],
    });
    setSamples(updateSamples);
  };

  const sampleTypeObject = (object) => {
    setSamples((currentSamples) =>
      applySampleTypeUpdate(currentSamples, object),
    );
  };

  const fetchRejectSampleReasons = (res) => {
    if (componentMounted.current) {
      setRejectSampleReasons(res);
    }
  };

  const handleRemoveSample = (e, sample) => {
    e.preventDefault();
    let filtered = samples.filter(function (element) {
      return element !== sample;
    });
    setSamples(filtered);
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/displayList/REJECTION_REASONS",
      fetchRejectSampleReasons,
    );
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  return (
    <>
      <h3>
        <FormattedMessage id="label.button.sample" />
      </h3>
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <div className="orderLegendBody">
            {samples.map((sample, i) => {
              return (
                <div className="sampleType" key={sample.key ?? i}>
                  <h4>
                    <FormattedMessage id="label.button.sample" /> {i + 1}
                    <span className="requiredlabel">*</span>
                  </h4>
                  <Link href="#" onClick={(e) => handleRemoveSample(e, sample)}>
                    {<FormattedMessage id="sample.remove.action" />}
                  </Link>
                  <SampleType
                    index={i}
                    rejectSampleReasons={rejectSampleReasons}
                    sample={sample}
                    setSample={(newSample) => {
                      let newSamples = [...samples];
                      newSamples[i] = newSample;
                      setSamples(newSamples);
                    }}
                    sampleTypeObject={sampleTypeObject}
                    error={error}
                    domain={domain}
                    allowReferral={allowReferral}
                  />
                </div>
              );
            })}

            <Button onClick={handleAddNewSample}>
              {<FormattedMessage id="sample.add.action" />}
              &nbsp; &nbsp;
              <Add size={16} />
            </Button>
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default AddSample;
