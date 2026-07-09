import GenericSampleOrder from "../genericSample/GenericSampleOrder";
import NotebookSampleOrder from "../notebook/NotebookSampleOrder";
import SampleBatchEntry from "../batchOrderEntry/SampleBatchEntry";
import PathologyCaseView from "../pathology/PathologyCaseView";
import ImmunohistochemistryCaseView from "../immunohistochemistry/ImmunohistochemistryCaseView";
import CytologyCaseView from "../cytology/CytologyCaseView";

describe("OGC-284 workflow rollout targets", () => {
  test.each([
    ["GenericSampleOrder", GenericSampleOrder],
    ["NotebookSampleOrder", NotebookSampleOrder],
    ["SampleBatchEntry", SampleBatchEntry],
    ["PathologyCaseView", PathologyCaseView],
    ["ImmunohistochemistryCaseView", ImmunohistochemistryCaseView],
    ["CytologyCaseView", CytologyCaseView],
  ])("%s exports a workflow component", (_name, component) => {
    expect(component).toBeTruthy();
  });
});
