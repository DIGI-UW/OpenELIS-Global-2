import React, { useState } from "react";
import { useParams, useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import BreadcrumbNav from "../components/BreadcrumbNav";
import LocationPickerPage from "../LocationPicker/LocationPickerPage";
import { LEVEL_ORDER } from "../LocationPicker/useLocationPicker";
import useSampleStorage from "../hooks/useSampleStorage";

/**
 * ManageLocationPage — /Storage/sample-items/:id/manage-location.
 *
 * Wraps <LocationPickerPage> in a data-loading shell. The sample data
 * is passed via router state from SampleItemsPage (no separate GET
 * endpoint — the list already fetched it). On deep-link we fall back
 * to a minimal sample object using the URL id.
 *
 * On save: POST /rest/storage/sample-items/{assign|move}, then
 * navigate back to /Storage/sample-items?t={now} so the list refetches
 * and shows the updated row.
 */
export default function ManageLocationPage() {
  const { id } = useParams();
  const history = useHistory();
  const location = useLocation();
  const intl = useIntl();
  const [error, setError] = useState(null);
  const { assignSampleItem, moveSampleItem } = useSampleStorage();

  // Sample data comes from the SampleItemsPage via router state
  // (history.push({pathname, state: {sample}})). On a deep-link refresh,
  // state is lost — fall back to a minimal object so the picker still
  // renders.
  const sample = location.state?.sample || {
    id,
    sampleItemId: id,
    sampleAccessionNumber: "",
    type: "",
    status: "Active",
    location: "",
  };

  const currentLocation = (() => {
    const hasAnyLevel = LEVEL_ORDER.some((lvl) => sample[`${lvl}Id`]);
    const locationPath = sample.location || sample.hierarchicalPath || "";
    if (!hasAnyLevel && !locationPath) return null;
    const selection = {};
    LEVEL_ORDER.forEach((lvl) => {
      if (sample[`${lvl}Id`]) {
        selection[lvl] = {
          id: sample[`${lvl}Id`],
          name: sample[`${lvl}Name`] || "",
        };
      }
    });
    return {
      selection,
      hierarchicalPath: locationPath,
      position: sample.positionCoordinate
        ? { mode: "text", value: sample.positionCoordinate }
        : null,
    };
  })();

  const navigateBack = () => {
    history.push(`/Storage/sample-items?t=${Date.now()}`);
  };

  const handleSave = async ({ selection, position, reason, notes }) => {
    // Deepest selected level wins — the backend assign/move endpoints
    // expect one locationId + locationType pair (not a hierarchy).
    let deepest = null;
    for (const lvl of LEVEL_ORDER) {
      if (selection[lvl]) deepest = { type: lvl, value: selection[lvl] };
    }
    if (!deepest || deepest.type === "room") {
      setError("Select a device, shelf, rack, or box before saving");
      return;
    }

    let positionCoordinate = null;
    if (position) {
      if (position.mode === "text") {
        positionCoordinate = (position.value || "").trim() || null;
      } else if (position.mode === "grid") {
        const row = (position.row || "").toString().trim();
        const col = (position.column || "").toString().trim();
        positionCoordinate = row + col || null;
      }
    }

    const payload = {
      sampleItemId: sample.sampleItemId || sample.id || id,
      locationId: String(deepest.value.id),
      locationType: deepest.type,
      positionCoordinate,
      notes: notes || null,
    };

    try {
      if (currentLocation) {
        await moveSampleItem({ ...payload, reason: reason || null });
      } else {
        await assignSampleItem(payload);
      }
      navigateBack();
    } catch (e) {
      setError(e.message || "Save failed");
    }
  };

  const crumbs = [
    {
      label: intl.formatMessage({
        id: "storage.breadcrumb.storage",
        defaultMessage: "Storage",
      }),
      href: "/Storage",
    },
    {
      label: intl.formatMessage({
        id: "storage.breadcrumb.sampleitems",
        defaultMessage: "Sample Items",
      }),
      href: "/Storage/sample-items",
    },
    {
      label: sample.sampleAccessionNumber || sample.sampleItemId || id,
      href: `/Storage/sample-items/${id}/manage-location`,
    },
  ];

  return (
    <>
      {error && (
        <div role="alert" style={{ padding: "1rem", background: "#fce5e5" }}>
          {error}
        </div>
      )}
      <LocationPickerPage
        sample={{
          id: sample.sampleItemId || sample.id || id,
          sampleAccessionNumber: sample.sampleAccessionNumber || "",
          sampleType: sample.type || sample.sampleType || "",
          status: sample.status || "Active",
        }}
        currentLocation={currentLocation}
        breadcrumb={<BreadcrumbNav crumbs={crumbs} />}
        onSave={handleSave}
        onCancel={navigateBack}
      />
    </>
  );
}
