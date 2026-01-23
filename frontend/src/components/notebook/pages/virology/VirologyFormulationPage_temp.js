notes = formulationNotes.trim();
    }

    console.log("Sending formulation data:", payload);
    postToOpenElisServerJsonResponse(
      "/rest/virology/formulation",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);
        console.log("Response data:", response);

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.formulation.success.saved",
                defaultMessage:
                  "Formulation data saved for {count} sample(s).",
              },
              { count: response.samplesUpdated || selectedSampleIds.length },
            ),
          });

          setModalOpen(false);
          resetForm();
          setSelectedSampleIds([]);
          loadPageSamples();

          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            subtitle: intl.formatMessage({
              id: "virology.formulation.error.save",
              defaultMessage:
                "Failed to save formulation data. Please try again.",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    batchNumber,
    stabilizers,
    preservatives,
    virusConcentration,
    bufferComposition,
    formulationNotes,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
    resetForm,
  ]);

  const handleCompleteFormulation = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.formulation.error.noSelectionComplete",
          defaultMessage: "Please select at least one sample to complete.",
        }),
      });
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        subtitle: intl.formatMessage({
          id: "virology.formulation.error.noPageComplete",
          defaultMessage:
            "Cannot complete samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setLoading(true);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (response) => {
        setLoading(false);

        if (response && (response.success || response === 200)) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.formulation.success.completed",
                defaultMessage:
                  "Completed formulation for