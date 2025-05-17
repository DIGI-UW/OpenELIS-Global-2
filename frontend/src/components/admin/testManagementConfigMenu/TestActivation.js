import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Pagination,
  Search,
  Select,
  SelectItem,
  Stack,
  Checkbox,
  Modal,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import CustomCheckBox from "../../common/CustomCheckBox.js";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType.js";
import {
  SortableTestList,
  SortableSampleTypeList,
} from "./sortableListComponent/SortableList.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "label.testActivate",
    link: "/MasterListsPage#TestActivation",
  },
];

function TestActivation() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  const [isLoading, setIsLoading] = useState(false);
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [testActivationData, setTestActivationData] = useState({});
  const [changedTestActivationData, setChangedTestActivationData] = useState(
    {},
  );
  const [sampleTypeIdToListMapTests, setSampleTypeIdToListMapTests] = useState(
    [],
  );
  const [testArrangementArray, setTestArrangementArray] = useState([]);
  const [sampleTypeArrangementActivate, setSampleTypeArrangementActivate] =
    useState(false);
  const [sampleTypesWithIdValueSorting, setSampleTypesWithIdValueSorting] =
    useState([]);
  const [
    sampleTypesWithIdValueActivatedSorting,
    setSampleTypesWithIdValueActivatedSorting,
  ] = useState([]);
  const [jsonChangeList, setJsonChangeList] = useState({
    activateSample: [],
    deactivateSample: [],
    activateTest: [],
    deactivateTest: [],
  });

  function handleTestActivationData(res) {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestActivationData(res);
      setChangedTestActivationData(res);
    }
  }

  const handleActiveTestListCheckboxChangeActiveTests = (
    test,
    sampleTypeId,
    isChecked,
  ) => {
    setChangedTestActivationData((prev) => {
      let activeTestList = [...prev.activeTestList];
      let inactiveTestList = [...prev.inactiveTestList];

      let movedSampleType = null;

      activeTestList = activeTestList
        .map((sample) => {
          if (sample.sampleType.id === sampleTypeId) {
            let activeTests = [...sample.activeTests];
            let inactiveTests = [...sample.inactiveTests];

            const originalState = testActivationData.activeTestList.find(
              (sample) => sample.sampleType.id === sampleTypeId,
            );

            if (activeTests.some((t) => t.id === test.id)) {
              if (isChecked === true) {
                inactiveTests = inactiveTests.filter(
                  (item) => item.id !== test.id,
                );
                if (!activeTests.some((item) => item.id === test.id)) {
                  activeTests.push(test);
                }
              } else {
                activeTests = activeTests.filter((item) => item.id !== test.id);
                if (!inactiveTests.some((item) => item.id === test.id)) {
                  inactiveTests.push(test);
                }
              }
            }

            if (activeTests.length === 0) {
              movedSampleType = {
                sampleType: sample.sampleType,
                activeTests: [],
                inactiveTests: [...inactiveTests],
              };
              return null;
            }

            return { ...sample, activeTests, inactiveTests };
          }
          return sample;
        })
        .filter(Boolean);

      if (movedSampleType) {
        let existingSample = inactiveTestList.find(
          (sample) => sample.sampleType.id === sampleTypeId,
        );

        let updatedInactiveTestList = inactiveTestList.map((sample) =>
          sample.sampleType.id === sampleTypeId
            ? {
                ...sample,
                inactiveTests: [
                  ...sample.inactiveTests,
                  ...movedSampleType.inactiveTests,
                ],
              }
            : sample,
        );

        if (!existingSample) {
          updatedInactiveTestList.push(movedSampleType);
        }

        return {
          ...prev,
          activeTestList,
          inactiveTestList: updatedInactiveTestList,
        };
      }

      return { ...prev, activeTestList };
    });

    setJsonChangeList((prev) => {
      let activateTest = [...prev.activateTest];
      let deactivateTest = [...prev.deactivateTest];
      let activateSample = Array.isArray(prev.activateSample)
        ? [...prev.activateSample]
        : [];
      let deactivateSample = Array.isArray(prev.deactivateSample)
        ? [...prev.deactivateSample]
        : [];
      const originalState = testActivationData?.activeTestList?.find(
        (sample) => String(sample.sampleType.id) === String(sampleTypeId),
      );

      const isOriginallyActive = originalState?.activeTests?.some(
        (t) => t.id === test.id,
      );

      if (isChecked === true) {
        if (!isOriginallyActive) {
          if (!activateTest.some((item) => item.id === test.id)) {
            handleActiveTestsOnChangeSetJsonChangeList(test, sampleTypeId);
          }
          if (!activateSample.some((item) => item.id === sampleTypeId)) {
            handleActiveSampleOnChangeSetJsonChangeList(sampleTypeId);
          } else {
            activateSample = activateSample.filter(
              (item) => item.id !== sampleTypeId,
            );
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      } else {
        if (isOriginallyActive || isOriginallyActive === undefined) {
          if (!deactivateTest.some((item) => item.id === test.id)) {
            deactivateTest.push({ id: Number(test.id) });
            handleActiveTestsOnChangeSetJsonChangeListRemove(test);
          }
          const sample = changedTestActivationData?.activeTestList.find(
            (s) => String(s.sampleType.id) === String(sampleTypeId),
          );
          if (sample?.activeTests?.length === 1) {
            if (!deactivateSample.some((item) => item.id === sampleTypeId)) {
              deactivateSample.push({ id: Number(sampleTypeId) });
              handleActiveSampleOnChangeSetJsonChangeListRemove(sampleTypeId);
            } else {
              deactivateSample = deactivateSample.filter(
                (item) => item.id !== sampleTypeId,
              );
            }
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      }

      return {
        ...prev,
        activateTest,
        deactivateTest,
        activateSample,
        deactivateSample,
      };
    });
  };

  const handleActiveTestListCheckboxChangeInactiveTests = (
    test,
    sampleTypeId,
    isChecked,
  ) => {
    setChangedTestActivationData((prev) => {
      let activeTestList = [...prev.activeTestList];

      activeTestList = activeTestList.map((sample) => {
        if (sample.sampleType.id === sampleTypeId) {
          let activeTests = [...sample.activeTests];
          let inactiveTests = [...sample.inactiveTests];

          if (inactiveTests.some((t) => t.id === test.id)) {
            if (isChecked === true) {
              inactiveTests = inactiveTests.filter(
                (item) => item.id !== test.id,
              );
              if (!activeTests.some((item) => item.id === test.id)) {
                activeTests.push(test);
              }
            } else {
              activeTests = activeTests.filter((item) => item.id !== test.id);
              if (!inactiveTests.some((item) => item.id === test.id)) {
                inactiveTests.push(test);
              }
            }
          }

          return { ...sample, activeTests, inactiveTests };
        }
        return sample;
      });

      return { ...prev, activeTestList };
    });

    setJsonChangeList((prev) => {
      let activateTest = [...prev.activateTest];
      let deactivateTest = [...prev.deactivateTest];
      let activateSample = Array.isArray(prev.activateSample)
        ? [...prev.activateSample]
        : [];
      let deactivateSample = Array.isArray(prev.deactivateSample)
        ? [...prev.deactivateSample]
        : [];

      const originalState = testActivationData.activeTestList.find(
        (sample) => String(sample.sampleType.id) === String(sampleTypeId),
      );

      const isOriginallyActive = originalState?.activeTests?.some(
        (t) => t.id === test.id,
      );

      if (isChecked === true) {
        if (!isOriginallyActive) {
          if (activateTest.some((item) => item.id === test.id)) {
            handleActiveTestsOnChangeSetJsonChangeListRemove(
              test,
              sampleTypeId,
            );
          } else {
            handleActiveTestsOnChangeSetJsonChangeList(test, sampleTypeId);
          }
          if (!activateSample.some((item) => item.id === sampleTypeId)) {
            handleActiveSampleOnChangeSetJsonChangeList(sampleTypeId);
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      } else {
        if (isOriginallyActive || isOriginallyActive === undefined) {
          if (!deactivateTest.some((item) => item.id === test.id)) {
            deactivateTest.push({ id: Number(test.id) });
            handleActiveTestsOnChangeSetJsonChangeListRemove(test);
          }
          const sample = changedTestActivationData?.activeTestList.find(
            (s) => String(s.sampleType.id) === String(sampleTypeId),
          );
          console.log(sample?.inactiveTests?.length);
          console.log(sample?.activeTests?.length);
          console.log(sample?.inactiveTests?.length > 0);
          console.log(sample?.activeTests?.length === 1);
          if (
            sample?.inactiveTests?.length > 0 &&
            sample?.activeTests?.length === 1
          ) {
            if (!deactivateSample.some((item) => item.id === sampleTypeId)) {
              deactivateSample.push({ id: Number(sampleTypeId) });
              handleActiveSampleOnChangeSetJsonChangeListRemove(sampleTypeId);
            } else {
              deactivateSample = deactivateSample.filter(
                (item) => item.id !== sampleTypeId,
              );
            }
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      }

      return {
        ...prev,
        activateTest,
        deactivateTest,
        activateSample,
        deactivateSample,
      };
    });
  };

  const handleInactiveTestListCheckboxChangeActiveTests = (
    test,
    sampleTypeId,
    isChecked,
  ) => {
    setChangedTestActivationData((prev) => {
      let inactiveTestList = [...prev.inactiveTestList];

      inactiveTestList = inactiveTestList.map((sample) => {
        if (sample.sampleType.id === sampleTypeId) {
          let activeTests = [...sample.activeTests];
          let inactiveTests = [...sample.inactiveTests];

          const originalState = testActivationData.inactiveTestList.find(
            (sample) => sample.sampleType.id === sampleTypeId,
          );

          if (
            activeTests.some((t) => t.id === test.id) ||
            inactiveTests.some((t) => t.id === test.id)
          ) {
            if (isChecked === false) {
              activeTests = activeTests.filter((item) => item.id !== test.id);
              if (!inactiveTests.some((item) => item.id === test.id)) {
                inactiveTests.push(test);
              }
            } else {
              inactiveTests = inactiveTests.filter(
                (item) => item.id !== test.id,
              );
              if (!activeTests.some((item) => item.id === test.id)) {
                activeTests.push(test);
              }
            }
          }

          return { ...sample, activeTests, inactiveTests };
        }
        return sample;
      });

      return { ...prev, inactiveTestList };
    });

    setJsonChangeList((prev) => {
      let activateTest = [...prev.activateTest];
      let deactivateTest = [...prev.deactivateTest];
      let activateSample = Array.isArray(prev.activateSample)
        ? [...prev.activateSample]
        : [];
      let deactivateSample = Array.isArray(prev.deactivateSample)
        ? [...prev.deactivateSample]
        : [];

      const originalState = testActivationData.inactiveTestList.find(
        (sample) => String(sample.sampleType.id) === String(sampleTypeId),
      );

      const isOriginallyInactive = originalState?.inactiveTests?.some(
        (t) => t.id === test.id,
      );

      if (isChecked === false) {
        if (!isOriginallyInactive) {
          if (!deactivateTest.some((item) => item.id === test.id)) {
            deactivateTest.push({ id: test.id });
          }
          const sample = changedTestActivationData?.inactiveTestList.find(
            (s) => String(s.sampleType.id) === String(sampleTypeId),
          );
          console.log(sample?.activeTests?.length > 0);
          console.log(sample?.inactiveTests?.length === 1); // 0 or 1
          if (sample?.activeTests?.length > 0 && sample?.inactiveTests === 1) {
            if (!deactivateSample.some((item) => item.id === sampleTypeId)) {
              deactivateSample.push({ id: sampleTypeId });
              handleActiveSampleOnChangeSetJsonChangeListRemove(sampleTypeId);
            } else {
              deactivateSample = deactivateSample.filter(
                (item) => item.id !== sampleTypeId,
              );
            }
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      } else {
        if (isOriginallyInactive || isOriginallyInactive === undefined) {
          if (!activateTest.some((item) => item.id === test.id)) {
            handleActiveTestsOnChangeSetJsonChangeList(test, sampleTypeId);
          }
          if (!activateSample.some((item) => item.id === sampleTypeId)) {
            handleActiveSampleOnChangeSetJsonChangeList(sampleTypeId);
          } else {
            activateSample = activateSample.filter(
              (item) => item.id !== sampleTypeId,
            );
          }
          // deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
          // deactivateSample = deactivateSample.filter(
          //   (item) => item.id !== sampleTypeId,
          // );
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      }

      return {
        ...prev,
        activateTest,
        deactivateTest,
        activateSample,
        deactivateSample,
      };
    });
  };

  const handleInactiveTestListCheckboxChangeInactiveTests = (
    test,
    sampleTypeId,
    isChecked,
  ) => {
    setChangedTestActivationData((prev) => {
      let inactiveTestList = [...prev.inactiveTestList];
      let activeTestList = [...prev.activeTestList];

      let updatedSample = null;

      inactiveTestList = inactiveTestList.map((sample) => {
        if (sample.sampleType.id === sampleTypeId) {
          let activeTests = [...sample.activeTests];
          let inactiveTests = [...sample.inactiveTests];

          const originalState = testActivationData.inactiveTestList.find(
            (sample) => sample.sampleType.id === sampleTypeId,
          );

          if (inactiveTests.some((t) => t.id === test.id)) {
            if (isChecked) {
              inactiveTests = inactiveTests.filter(
                (item) => item.id !== test.id,
              );
              if (!activeTests.some((item) => item.id === test.id)) {
                activeTests.push(test);
              }

              if (activeTests.length > 0) {
                updatedSample = {
                  sampleType: sample.sampleType,
                  activeTests,
                  inactiveTests,
                };
                setSampleTypeArrangementActivate(true);
              }
            } else {
              activeTests = activeTests.filter((item) => item.id !== test.id);
              if (!inactiveTests.some((item) => item.id === test.id)) {
                inactiveTests.push(test);
              }
            }
          }

          return { ...sample, activeTests, inactiveTests };
        }
        return sample;
      });

      inactiveTestList = inactiveTestList.filter(
        (sample) => sample.sampleType.id !== sampleTypeId,
      );
      if (updatedSample) {
        activeTestList.push(updatedSample);
      }

      return { ...prev, inactiveTestList, activeTestList };
    });

    setJsonChangeList((prev) => {
      let activateTest = [...prev.activateTest];
      let deactivateTest = [...prev.deactivateTest];
      let activateSample = Array.isArray(prev.activateSample)
        ? [...prev.activateSample]
        : [];
      let deactivateSample = Array.isArray(prev.deactivateSample)
        ? [...prev.deactivateSample]
        : [];

      const originalState = testActivationData.inactiveTestList.find(
        (sample) => String(sample.sampleType.id) === String(sampleTypeId),
      );

      const isOriginallyInactive = originalState?.inactiveTests?.some(
        (t) => t.id === test.id,
      );

      if (isChecked === false) {
        if (!isOriginallyInactive) {
          if (!deactivateTest.some((item) => item.id === test.id)) {
            deactivateTest.push({ id: test.id });
          }
          const sample = changedTestActivationData?.inactiveTestList.find(
            (s) => String(s.sampleType.id) === String(sampleTypeId),
          );
          console.log(sample?.activeTests?.length > 0);
          console.log(sample?.inactiveTests?.length === 1); // 0 or 1
          if (
            sample?.activateTest?.length > 0 &&
            sample?.inactiveTest?.length === 1
          ) {
            if (!deactivateSample.some((item) => item.id === sampleTypeId)) {
              deactivateSample.push({ id: sampleTypeId });
              handleActiveSampleOnChangeSetJsonChangeListRemove(sampleTypeId);
            } else {
              deactivateSample = deactivateSample.filter(
                (item) => item.id !== sampleTypeId,
              );
            }
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      } else {
        if (isOriginallyInactive) {
          if (!activateTest.some((item) => item.id === test.id)) {
            handleActiveTestsOnChangeSetJsonChangeList(test, sampleTypeId);
          }
          if (!activateSample.some((item) => item.id === sampleTypeId)) {
            handleActiveSampleOnChangeSetJsonChangeList(sampleTypeId);
          } else {
            activateSample = activateSample.filter(
              (item) => item.id !== sampleTypeId,
            );
          }
          // deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
          // deactivateSample = deactivateSample.filter(
          //   (item) => item.id !== sampleTypeId,
          // );
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      }

      return {
        ...prev,
        activateTest,
        deactivateTest,
        activateSample,
        deactivateSample,
      };
    });
  };

  function testActivationPostCall() {
    setIsLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/TestActivation`,
      JSON.stringify({
        jsonChangeList: JSON.stringify({
          activateSample: JSON.stringify(jsonChangeList.activateSample),
          deactivateSample: JSON.stringify(jsonChangeList.deactivateSample),
          activateTest: JSON.stringify(jsonChangeList.activateTest),
          deactivateTest: JSON.stringify(jsonChangeList.deactivateTest),
        }),
      }),
      (res) => testActivationPostCallback(res),
    );
  }

  function testActivationPostCallback(res) {
    if (res) {
      setIsLoading(false);
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "notification.user.post.save.success",
        }),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    }
  }

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/TestActivation`, (res) => {
      handleTestActivationData(res);
    });
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  useEffect(() => {
    if (testActivationData) {
      let sortOrder = 0;

      const activeList = testActivationData.activeTestList || [];
      const inactiveList = testActivationData.inactiveTestList || [];

      const activatedSamples = activeList.map((sample) => ({
        id: Number(sample.sampleType.id),
        value: sample.sampleType.value,
        activated: false,
        sortOrder: sortOrder++,
      }));

      setSampleTypesWithIdValueActivatedSorting(activatedSamples);

      const allSampleTypeMap = new Map();

      [...activeList, ...inactiveList].forEach((sample) => {
        const id = Number(sample.sampleType.id);
        if (!allSampleTypeMap.has(id)) {
          allSampleTypeMap.set(id, sample.sampleType);
        }
      });

      sortOrder = 0;

      const allSamples = Array.from(allSampleTypeMap.values()).map(
        (sampleType) => ({
          id: Number(sampleType.id),
          value: sampleType.value,
          activated: false,
          sortOrder: sortOrder++,
        }),
      );

      setSampleTypesWithIdValueSorting(allSamples);
    }
  }, [testActivationData]);

  const handleActiveSampleOnChangeSetJsonChangeList = (sampleTypeId) => {
    let updatedSamples = [];

    setSampleTypesWithIdValueSorting((prev) => {
      updatedSamples = prev.map((s) =>
        String(s.id) === String(sampleTypeId) ? { ...s, activated: true } : s,
      );
      return updatedSamples;
    });

    const sampleType = sampleTypesWithIdValueSorting.find(
      (item) => String(item.id) === String(sampleTypeId),
    );

    if (!sampleType) {
      window.location.reload();
      return;
    }

    const alreadyExists = sampleTypesWithIdValueActivatedSorting.some(
      (item) => String(item.id) === String(sampleTypeId),
    );

    if (!alreadyExists) {
      const updatedSampleTypesList = [
        ...sampleTypesWithIdValueActivatedSorting,
        {
          id: sampleType?.id,
          value: sampleType?.value,
          activated: true,
          sortOrder: sampleTypesWithIdValueActivatedSorting?.length, // this may need fix for sampleType?.sortOrder or sampleType?.sortOrder + 1 but working now
        },
      ];

      setSampleTypesWithIdValueActivatedSorting(updatedSampleTypesList);
      setJsonChangeList((prevList) => ({
        ...prevList,
        activateSample: updatedSampleTypesList.map(
          ({ value, ...rest }) => rest,
        ),
      }));
    } else {
      const updatedSampleType = sampleTypesWithIdValueActivatedSorting.map(
        (s) =>
          String(s.id) === String(sampleTypeId) ? { ...s, activated: true } : s,
      );

      setSampleTypesWithIdValueActivatedSorting(updatedSampleType);

      setJsonChangeList((prevList) => ({
        ...prevList,
        activateSample: updatedSampleType.map(({ value, ...rest }) => rest),
      }));
    }
  };

  const handleActiveSampleOnChangeSetJsonChangeListRemove = (sampleTypeId) => {
    // setSampleTypesWithIdValueSorting((prev) =>
    //   prev.filter((s) => String(s.id) !== String(sampleTypeId)),
    // );

    setJsonChangeList((prev) => ({
      ...prev,
      activateSample: prev.activateSample.filter(
        (s) => String(s.id) !== String(sampleTypeId),
      ),
    }));

    setSampleTypesWithIdValueActivatedSorting((prev) =>
      prev.filter((s) => String(s.id) !== String(sampleTypeId)),
    );
  };

  const handleActiveTestsOnChangeSetJsonChangeList = (test, sampleTypeId) => {
    const alreadyExists = sampleTypeIdToListMapTests.some(
      (item) => item.id === sampleTypeId,
    );

    // const alreadyExists = testArrangementArray.some((group) =>
    //   group.some((t) => String(t.id) === String(test.id)),
    // );

    if (!alreadyExists) {
      const newSampleTypeEntry = { id: sampleTypeId };
      setSampleTypeIdToListMapTests((prev) => [...prev, newSampleTypeEntry]);

      const allTestLists = [
        ...(changedTestActivationData?.activeTestList || []),
        ...(changedTestActivationData?.inactiveTestList || []),
      ];

      const foundSample = allTestLists.find(
        (sample) => Number(sample?.sampleType?.id) === Number(sampleTypeId),
      );

      if (foundSample) {
        const allTests = [
          ...(foundSample.activeTests || []),
          ...(foundSample.inactiveTests || []),
        ];

        let sortOrder = 0;

        const testGroup = allTests.map((t) => ({
          id: t.id,
          activated: String(t.id) === String(test.id),
          sortOrder: sortOrder++,
        }));

        setTestArrangementArray((prev) => [...prev, testGroup]);

        const selectedTest = testGroup.find(
          (t) => String(t.id) === String(test.id),
        );

        if (selectedTest) {
          setJsonChangeList((prev) => {
            const alreadyIncluded = prev.activateTest?.some(
              (t) => t.id === selectedTest.id,
            );

            if (!alreadyIncluded) {
              return {
                ...prev,
                activateTest: [
                  ...prev.activateTest,
                  { ...selectedTest, id: Number(selectedTest.id) },
                ],
              };
            } else {
              return {
                ...prev,
                activateTest: prev.activateTest.map((t) =>
                  t.id === Number(selectedTest.id)
                    ? { ...t, activated: true }
                    : t,
                ),
              };
            }
          });
        }
      }
    } else {
      let foundTest = null;

      const updated = testArrangementArray.map((arr) =>
        arr.map((t) => {
          const isSameId = String(t.id) === String(test.id);
          if (isSameId) {
            foundTest = { ...t, activated: true };
            return foundTest;
          }
          return t;
        }),
      );

      if (foundTest) {
        setTestArrangementArray(updated);

        setJsonChangeList((prev) => {
          const alreadyIncluded = prev.activateTest?.some(
            (t) => Number(t.id) === Number(foundTest.id),
          );
          const sortOrder = prev.activateTest?.length || 0;

          if (!alreadyIncluded) {
            return {
              ...prev,
              activateTest: [
                ...prev.activateTest,
                {
                  id: Number(foundTest.id),
                  activated: true,
                  sortOrder: sortOrder, // may be this need to be used but foundTest.sortOrder thsi is working now
                },
              ],
            };
          } else {
            return {
              ...prev,
              activateTest: prev.activateTest.map((t) =>
                Number(t.id) === Number(foundTest.id)
                  ? { ...t, activated: true }
                  : t,
              ),
            };
          }
        });
      }
    }
  };

  const handleActiveTestsOnChangeSetJsonChangeListRemove = (test) => {
    setJsonChangeList((prev) => ({
      ...prev,
      activateTest: prev.activateTest.filter((t) => t.id !== Number(test.id)),
    }));

    setTestArrangementArray((prev) =>
      prev.map((group) =>
        group.map((t) =>
          String(t.id) === String(test.id) ? { ...t, activated: false } : t,
        ),
      ),
    );
  };

  const getTestIdsWithName = (testsArray) => {
    const testIdsWithName = [];

    const allActiveTests = [
      ...(changedTestActivationData?.activeTestList?.flatMap(
        (sample) => sample.activeTests,
      ) || []),
      ...(changedTestActivationData?.activeTestList?.flatMap(
        (sample) => sample.inactiveTests,
      ) || []),
      ...(changedTestActivationData?.inactiveTestList?.flatMap(
        (sample) => sample.activeTests,
      ) || []),
      ...(changedTestActivationData?.inactiveTestList?.flatMap(
        (sample) => sample.inactiveTests,
      ) || []),
    ];

    testsArray?.forEach((t) => {
      const match = allActiveTests?.find(
        (activeTest) => String(activeTest.id) === String(t.id),
      );
      if (match) {
        testIdsWithName.push({
          id: match.id,
          value: match.value,
          activated: t.activated === true,
        });
      }
    });

    return testIdsWithName;
  };

  const getSampleTypeValueWithId = (sampleTypeIdArray) => {
    const sampleTypeValues = [];

    // [{ id: 53 },{ id: 3 }] this will be the recived this now listen i will need to traverse througe the testActivateData.activeTestList[] and inactiveTestList[] both find the sampleType object of the id which recvied hence after computing i will retnur the sampleType.valuse in the form of array which can be futher mapped to the sortableList reusable component

    const allSamples = [
      ...(testActivationData?.activeTestList || []),
      ...(testActivationData?.inactiveTestList || []),
    ];

    sampleTypeIdArray.forEach(({ id }) => {
      const foundSample = allSamples.find(
        (sample) => Number(sample?.sampleType?.id) === Number(id),
      );

      if (foundSample && foundSample.sampleType) {
        sampleTypeValues.push(foundSample.sampleType);
      }
    });

    return sampleTypeValues;
  };

  if (!isLoading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  return (
    <>
      {notificationVisible && <AlertDialog />}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="label.testActivate" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Section>
                      <Heading>
                        <FormattedMessage id="instructions.test.activation" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                disabled={
                  jsonChangeList?.activateTest?.length === 0 &&
                  jsonChangeList?.deactivateTest?.length === 0 &&
                  jsonChangeList?.activateSample?.length === 0 &&
                  jsonChangeList?.deactivateSample?.length === 0
                }
                onClick={() => {
                  setIsConfirmModalOpen(true);
                }}
                type="button"
              >
                <FormattedMessage id="label.button.submit" />
              </Button>{" "}
              <Button
                onClick={() => window.location.reload()}
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="label.button.cancel" />
              </Button>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          {changedTestActivationData?.activeTestList &&
          changedTestActivationData?.activeTestList?.length > 0 ? (
            <Grid fullWidth={true}>
              {changedTestActivationData?.activeTestList?.map((sample) => (
                <>
                  <Column lg={16} md={8} sm={4} key={sample.sampleType.id}>
                    <Section>
                      <Section>
                        <Section>
                          <Heading>{sample.sampleType.value}</Heading>
                        </Section>
                      </Section>
                    </Section>
                  </Column>
                  {sample.activeTests.map((test) => (
                    <Column
                      lg={4}
                      md={4}
                      sm={4}
                      key={`${sample.sampleType.id}-${test.id}`}
                    >
                      <Checkbox
                        id={`${sample.sampleType.id}-${test.id}`}
                        labelText={test.value}
                        checked={true}
                        onChange={(_, { checked }) => {
                          handleActiveTestListCheckboxChangeActiveTests(
                            test,
                            sample.sampleType.id,
                            checked,
                          );
                        }}
                      />
                    </Column>
                  ))}
                  {sample.inactiveTests.map((test) => (
                    <Column
                      lg={4}
                      md={4}
                      sm={4}
                      key={`${sample.sampleType.id}-${test.id}`}
                    >
                      <Checkbox
                        id={`${sample.sampleType.id}-${test.id}`}
                        labelText={test.value}
                        checked={false}
                        onChange={(_, { checked }) => {
                          handleActiveTestListCheckboxChangeInactiveTests(
                            test,
                            sample.sampleType.id,
                            checked,
                          );
                        }}
                      />
                    </Column>
                  ))}
                </>
              ))}
            </Grid>
          ) : (
            <></>
          )}
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Heading>Inactive Sample Types</Heading>
                  {/* TODO : change this to formated message */}
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          {changedTestActivationData?.inactiveTestList &&
          changedTestActivationData?.inactiveTestList?.length > 0 ? (
            <Grid fullWidth={true}>
              {changedTestActivationData.inactiveTestList.map((sample) => (
                <>
                  <Column lg={16} md={8} sm={4} key={sample.sampleType.id}>
                    <Section>
                      <Section>
                        <Section>
                          <Heading>{sample.sampleType.value}</Heading>
                        </Section>
                      </Section>
                    </Section>
                  </Column>
                  {sample.activeTests.map((test) => (
                    <Column
                      lg={4}
                      md={4}
                      sm={4}
                      key={`${sample.sampleType.id}-${test.id}`}
                    >
                      <Checkbox
                        id={`${sample.sampleType.id}-${test.id}`}
                        labelText={test.value}
                        checked={true}
                        onChange={(_, { checked }) => {
                          handleInactiveTestListCheckboxChangeActiveTests(
                            test,
                            sample.sampleType.id,
                            checked,
                          );
                        }}
                      />
                    </Column>
                  ))}
                  {sample.inactiveTests.map((test) => (
                    <Column
                      lg={4}
                      md={4}
                      sm={4}
                      key={`${sample.sampleType.id}-${test.id}`}
                    >
                      <Checkbox
                        id={`${sample.sampleType.id}-${test.id}`}
                        labelText={test.value}
                        checked={false}
                        onChange={(_, { checked }) => {
                          handleInactiveTestListCheckboxChangeInactiveTests(
                            test,
                            sample.sampleType.id,
                            checked,
                          );
                        }}
                      />
                    </Column>
                  ))}
                </>
              ))}
            </Grid>
          ) : (
            <></>
          )}
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                disabled={
                  jsonChangeList?.activateTest?.length === 0 &&
                  jsonChangeList?.deactivateTest?.length === 0 &&
                  jsonChangeList?.activateSample?.length === 0 &&
                  jsonChangeList?.deactivateSample?.length === 0
                }
                onClick={() => {
                  setIsConfirmModalOpen(true);
                }}
                type="button"
              >
                <FormattedMessage id="label.button.submit" />
              </Button>{" "}
              <Button
                onClick={() => window.location.reload()}
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="label.button.cancel" />
              </Button>
            </Column>
          </Grid>
        </div>
      </div>

      <Modal
        open={isConfirmModalOpen}
        size="md"
        modalHeading={<FormattedMessage id="label.test.order.confirm" />}
        primaryButtonText={<FormattedMessage id="column.name.accept" />}
        secondaryButtonText={<FormattedMessage id="back.action.button" />}
        onRequestSubmit={() => {
          setIsConfirmModalOpen(false);
          testActivationPostCall();
        }}
        onRequestClose={() => {
          setIsConfirmModalOpen(false);
          window.location.reload();
        }}
        preventCloseOnClickOutside={true}
        shouldSubmitOnEnter={true}
      >
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id="instructions.test.activation.sort" />
                  </Heading>
                </Section>
              </Section>
            </Section>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                marginTop: "20px",
              }}
            >
              {changedTestActivationData &&
                sampleTypeIdToListMapTests &&
                sampleTypeIdToListMapTests?.length > 0 &&
                Array.isArray(testArrangementArray) &&
                testArrangementArray.map((testsArray, index) => (
                  <SortableTestList
                    key={index}
                    sampleType={
                      getSampleTypeValueWithId([
                        sampleTypeIdToListMapTests[index],
                      ])[0]?.value
                    }
                    tests={getTestIdsWithName(testsArray)}
                    onSort={(updatedTests) => {
                      setTestArrangementArray((prev) => {
                        const newArrangement = [...prev];
                        newArrangement[index] = updatedTests;
                        return newArrangement;
                      });
                      setJsonChangeList((prev) => {
                        const updatedActivateTest = prev.activateTest.map(
                          (test) => {
                            const matchIndex = updatedTests.findIndex(
                              (t) => t.id === test.id,
                            );
                            if (matchIndex !== -1) {
                              return {
                                ...test,
                                sortOrder: matchIndex,
                              };
                            }
                            return test;
                          },
                        );
                        return {
                          ...prev,
                          activateTest: updatedActivateTest,
                        };
                      });
                    }}
                  />
                ))}
            </div>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                marginTop: "20px",
              }}
            >
              {sampleTypeArrangementActivate &&
                sampleTypesWithIdValueActivatedSorting?.length > 0 && (
                  <SortableSampleTypeList
                    tests={sampleTypesWithIdValueActivatedSorting}
                    onSort={(updatedSamples) =>
                      setJsonChangeList((prev) => ({
                        ...prev,
                        activateSample: updatedSamples,
                      }))
                    }
                  />
                )}
            </div>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id="instructions.test.activation.confirm" />
                  </Heading>
                </Section>
              </Section>
            </Section>
          </Column>
        </Grid>
        <br />
        <br />
        {jsonChangeList?.activateTest && (
          <>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="label.test.activate" />
                        </Heading>
                      </Section>
                    </Section>
                  </Section>
                </Section>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              {Array.isArray(testArrangementArray) &&
                testArrangementArray.length > 0 &&
                testArrangementArray.map((testsArray, index) => {
                  const activatedTests = getTestIdsWithName(
                    testsArray.filter((t) => t.activated === true),
                  );

                  return activatedTests.map((test) => (
                    <>
                      <Column lg={4} md={4} sm={4} key={`${index}-${test.id}`}>
                        <span>{test.value}</span>
                      </Column>
                      <br />
                    </>
                  ));
                })}
            </Grid>
          </>
        )}
        <br />
        {jsonChangeList?.activateSample && (
          <>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="label.sample.types.activate" />
                        </Heading>
                      </Section>
                    </Section>
                  </Section>
                </Section>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              {sampleTypesWithIdValueActivatedSorting
                ?.filter((sampleType) => sampleType.activated === true)
                .map((sampleType) => (
                  <Column lg={4} md={4} sm={4} key={sampleType.id}>
                    <span>{sampleType.value}</span>
                  </Column>
                ))}
            </Grid>
          </>
        )}
        <br />
        {jsonChangeList?.deactivateTest && (
          <>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="label.test.deactivate" />
                        </Heading>
                      </Section>
                    </Section>
                  </Section>
                </Section>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              {jsonChangeList?.deactivateTest?.length > 0 &&
                jsonChangeList.deactivateTest.map((test) => {
                  const testValue = getTestIdsWithName([test])[0]?.value;

                  return (
                    <Column lg={4} md={4} sm={4} key={test.id}>
                      <span>{testValue}</span>
                    </Column>
                  );
                })}
            </Grid>
          </>
        )}
        <br />
        {jsonChangeList?.deactivateSample && (
          <>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="label.sample.types.deactivate" />
                        </Heading>
                      </Section>
                    </Section>
                  </Section>
                </Section>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              {jsonChangeList?.deactivateSample?.length > 0 &&
                jsonChangeList.deactivateSample.map((sampleType) => {
                  const sampleTypeValue = getSampleTypeValueWithId([
                    { id: sampleType.id },
                  ])[0]?.value;

                  return (
                    <Column lg={4} md={4} sm={4} key={sampleType.id}>
                      <span>{sampleTypeValue}</span>
                    </Column>
                  );
                })}
            </Grid>
          </>
        )}
      </Modal>
    </>
  );
}

export default injectIntl(TestActivation);
