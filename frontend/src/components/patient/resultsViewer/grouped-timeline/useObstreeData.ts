import { useMemo, useState, useEffect } from "react";
import { assessValue, exist } from "../loadPatientTestData/helpers";
import { getFromOpenElisServer } from "../../../utils/Utils.js";

export const getName = (prefix, name) => {
  return prefix ? `${prefix}-${name}` : name;
};

const augmentObstreeData = (node, prefix) => {
  const outData = JSON.parse(JSON.stringify(node));
  outData.flatName = getName(prefix, node.display);
  outData.hasData = false;

  if (outData?.subSets?.length) {
    outData.subSets = outData.subSets.map((subNode) =>
      augmentObstreeData(subNode, getName(prefix, node?.display)),
    );
    outData.hasData = outData.subSets.some((subNode) => subNode.hasData);
  }
  if (exist(outData?.hiNormal, outData?.lowNormal)) {
    outData.range = `${outData.lowNormal} – ${outData.hiNormal}`;
  }
  if (outData?.obs?.length) {
    const assess = assessValue(outData);
    outData.obs = outData.obs.map((ob) => ({
      ...ob,
      interpretation: assess(ob.value),
    }));
    outData.hasData = true;
  }

  return { ...outData };
};

const useGetManyObstreeData = (patientUuid) => {
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchResultsTree = (resultsTree) => {
    setData(resultsTree);
    setIsLoading(false);
  };

  useEffect(() => {
    if (patientUuid) {
      getFromOpenElisServer(
        `/rest/result-tree?patientId=${patientUuid}`,
        fetchResultsTree,
      );
    }
  }, [patientUuid]);

  const result = useMemo(() => {
    return (
      data?.map((resp) => {
        if (resp) {
          const { ...rest } = resp;
          const newData = augmentObstreeData(resp, "");
          return { ...rest, loading: false, data: newData };
        } else {
          return {
            data: {},
            error: false,
            loading: false,
          };
        }
      }) || [
        {
          data: {},
          error: false,
          loading: false,
        },
      ]
    );
  }, [data]);
  const roots = result.map((item) => item.data);
  const loading = isLoading;
  return { roots, loading, error };
};

export default useGetManyObstreeData;
export { useGetManyObstreeData };
