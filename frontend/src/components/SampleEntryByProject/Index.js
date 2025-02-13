import { React, useRef, useState } from "react";
import SampleEntryByProjectForm from "./SampleEntryByProjectForm";

export { default as SampleEntryByProjectSearch } from "./SampleEntryByProjectForm";

const SampleEntryByProjectPage = () => {
  const sampleEntryByProjectRef = useRef(null);
  const [sampleEntryByProject, setSampleEntryByProject] = useState([]);
  return (
    <>
      <SampleEntryByProjectForm />
    </>
  );
};

export default SampleEntryByProjectPage;
