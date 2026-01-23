import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Grid, Column, Dropdown, Button } from "@carbon/react";
import { ZoomIn, ZoomOut, Renew, Download } from "@carbon/icons-react";
import { LineChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";

import "./HistoricalTrends.scss";
import { fetchHistoricalReadings } from "./api";

const TIME_RANGE_OPTIONS = [
  "Last 24 Hours",
  "Last 7 Days",
  "Last 30 Days",
  "All Time",
];

const RANGE_TO_DURATION = {
  "Last 24 Hours": 24 * 60 * 60 * 1000,
  "Last 7 Days": 7 * 24 * 60 * 60 * 1000,
  "Last 30 Days": 30 * 24 * 60 * 60 * 1000,
  "All Time": 90 * 24 * 60 * 60 * 1000,
};

const MAX_SERIES = 5;

const getRangeBoundaries = (timeRange) => {
  const duration =
    RANGE_TO_DURATION[timeRange] ?? RANGE_TO_DURATION["Last 24 Hours"];
  const end = new Date();
  const start = new Date(end.getTime() - duration);
  return { start: start.toISOString(), end: end.toISOString() };
};

const formatTrendLabel = (value) => {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

export default function HistoricalTrends({
  devices = [],
  initialSelectedFreezerId = null,
  onFreezerSelected,
}) {
  const [selectedFreezer, setSelectedFreezer] = useState(
    initialSelectedFreezerId || "All Freezers",
  );
  const [timeRange, setTimeRange] = useState("Last 24 Hours");
  const [chartData, setChartData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [zoomLevel, setZoomLevel] = useState(1);

  const freezerOptions = useMemo(() => {
    const uniqueDevices = devices.filter(
      (device, index, self) =>
        device.id && self.findIndex((d) => d.id === device.id) === index,
    );
    const names = uniqueDevices.map(
      (device) => device.unitName || `Freezer ${device.id}`,
    );
    return ["All Freezers", ...names];
  }, [devices]);

  const freezerNameToIdMap = useMemo(() => {
    const map = {};
    devices.forEach((device) => {
      if (device.id) {
        const name = device.unitName || `Freezer ${device.id}`;
        map[name] = device.id;
      }
    });
    return map;
  }, [devices]);

  const loadReadings = useCallback(async () => {
    if (!devices.length) {
      setChartData([]);
      return;
    }
    const ids =
      selectedFreezer === "All Freezers"
        ? devices
            .map((device) => device.id)
            .filter(Boolean)
            .slice(0, MAX_SERIES)
        : [freezerNameToIdMap[selectedFreezer] || devices[0]?.id].filter(
            Boolean,
          );

    if (!ids.length) {
      setChartData([]);
      return;
    }

    setLoading(true);
    setError(null);
    const { start, end } = getRangeBoundaries(timeRange);

    try {
      const responses = await Promise.all(
        ids.map((id) =>
          fetchHistoricalReadings(id, start, end).then((data) => ({
            freezerId: id,
            readings: data || [],
          })),
        ),
      );

      const normalized = responses.flatMap(({ freezerId, readings }) => {
        const device = devices.find((d) => d.id === freezerId);
        const freezerName = device?.unitName || `Freezer ${freezerId}`;
        return readings
          .filter((reading) => reading.temperatureCelsius != null)
          .map((reading) => ({
            group: freezerName,
            key: formatTrendLabel(reading.recordedAt),
            value: reading.temperatureCelsius,
          }));
      });

      setChartData(normalized);
    } catch (apiError) {
      setError(apiError.message || "Unable to load historical data.");
      setChartData([]);
    } finally {
      setLoading(false);
    }
  }, [devices, selectedFreezer, timeRange, freezerNameToIdMap]);

  useEffect(() => {
    loadReadings();
  }, [loadReadings]);

  useEffect(() => {
    if (
      initialSelectedFreezerId &&
      devices.some((device) => device.id === initialSelectedFreezerId)
    ) {
      const device = devices.find((d) => d.id === initialSelectedFreezerId);
      const freezerName =
        device?.unitName || `Freezer ${initialSelectedFreezerId}`;
      setSelectedFreezer(freezerName);
      if (onFreezerSelected) {
        onFreezerSelected(initialSelectedFreezerId);
      }
    }
  }, [initialSelectedFreezerId, devices, onFreezerSelected]);

  useEffect(() => {
    if (
      selectedFreezer !== "All Freezers" &&
      !freezerNameToIdMap[selectedFreezer]
    ) {
      setSelectedFreezer("All Freezers");
    }
  }, [devices, selectedFreezer, freezerNameToIdMap]);

  const stats = useMemo(() => {
    if (!chartData.length) {
      return { avg: "-", min: "-", max: "-", count: 0 };
    }
    const values = chartData.map((d) => d.value);
    const sum = values.reduce((acc, v) => acc + v, 0);
    const avg = sum / values.length;
    const min = Math.min(...values);
    const max = Math.max(...values);
    return {
      avg: avg.toFixed(1),
      min: min.toFixed(1),
      max: max.toFixed(1),
      count: values.length,
    };
  }, [chartData]);

  const chartOptions = useMemo(
    () => ({
      title: "",
      axes: {
        bottom: {
          title: "",
          mapsTo: "key",
          scaleType: "labels",
        },
        left: {
          title: "Temperature (°C)",
          mapsTo: "value",
          scaleType: "linear",
        },
      },
      legend: {
        position: "bottom",
      },
      height: `${400 * zoomLevel}px`,
      tooltip: {
        showTotal: false,
      },
    }),
    [zoomLevel],
  );

  const handleZoomIn = useCallback(() => {
    setZoomLevel((prev) => Math.min(prev + 0.25, 3));
  }, []);

  const handleZoomOut = useCallback(() => {
    setZoomLevel((prev) => Math.max(prev - 0.25, 0.25));
  }, []);

  const handleReset = useCallback(() => {
    setZoomLevel(1);
    loadReadings();
  }, [loadReadings]);

  const handleExportCsv = useCallback(() => {
    if (!chartData.length) {
      return;
    }

    // Create CSV content
    const headers = ["Freezer", "Timestamp", "Temperature (°C)"];
    const rows = chartData.map((item) => [item.group, item.key, item.value]);

    const csvContent = [
      headers.join(","),
      ...rows.map((row) => row.join(",")),
    ].join("\n");

    // Create blob and download
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);

    const timestamp = new Date().toISOString().split("T")[0];
    const filename = `freezer-temperature-data-${timestamp}.csv`;

    link.setAttribute("href", url);
    link.setAttribute("download", filename);
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [chartData]);

  return (
    <div className="hist-trends-page">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h3 className="hist-title">Historical Temperature Trends</h3>
        </Column>
      </Grid>

      <Grid fullWidth className="hist-filter-row">
        <Column lg={6} md={4} sm={4}>
          <Dropdown
            id="freezer-filter"
            titleText="Freezer"
            label={selectedFreezer}
            items={freezerOptions}
            selectedItem={selectedFreezer}
            onChange={({ selectedItem }) => {
              setSelectedFreezer(selectedItem);
              if (onFreezerSelected) {
                onFreezerSelected(selectedItem);
              }
            }}
          />
        </Column>
        <Column lg={4} md={3} sm={4}>
          <Dropdown
            id="time-range-filter"
            titleText="Time Range"
            label={timeRange}
            items={TIME_RANGE_OPTIONS}
            selectedItem={timeRange}
            onChange={({ selectedItem }) => setTimeRange(selectedItem)}
          />
        </Column>

        <Column lg={6} md={5} sm={4} className="hist-toolbar">
          <Button
            kind="ghost"
            size="sm"
            renderIcon={ZoomIn}
            onClick={handleZoomIn}
            disabled={zoomLevel >= 3}
          >
            Zoom In
          </Button>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={ZoomOut}
            onClick={handleZoomOut}
            disabled={zoomLevel <= 0.25}
          >
            Zoom Out
          </Button>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Renew}
            onClick={handleReset}
          >
            Reset
          </Button>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Download}
            onClick={handleExportCsv}
            disabled={!chartData.length}
          >
            Export CSV
          </Button>
        </Column>
      </Grid>

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="hist-chart-card">
            {error && <p className="hist-error">{error}</p>}
            {loading ? (
              <p className="hist-placeholder">Loading readings…</p>
            ) : chartData.length === 0 ? (
              <p className="hist-placeholder">
                No readings available for the selected filters.
              </p>
            ) : (
              <LineChart data={chartData} options={chartOptions} />
            )}
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="hist-kpis">
        <Column lg={4} md={4} sm={4}>
          <div className="hist-kpi-card">
            <p className="hist-kpi-label">Average Temperature</p>
            <p className="hist-kpi-value">
              {stats.avg === "-" ? "-" : `${stats.avg}°C`}
            </p>
          </div>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <div className="hist-kpi-card">
            <p className="hist-kpi-label">Min Temperature</p>
            <p className="hist-kpi-value hist-kpi-min">
              {stats.min === "-" ? "-" : `${stats.min}°C`}
            </p>
          </div>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <div className="hist-kpi-card">
            <p className="hist-kpi-label">Max Temperature</p>
            <p className="hist-kpi-value hist-kpi-max">
              {stats.max === "-" ? "-" : `${stats.max}°C`}
            </p>
          </div>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <div className="hist-kpi-card">
            <p className="hist-kpi-label">Data Points</p>
            <p className="hist-kpi-value">{stats.count.toLocaleString()}</p>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <p className="hist-footer">
            Cold Storage Monitoring v2.1.0 | Compliant with CAP, CLIA, FDA, and
            WHO guidelines | HIPAA Compliant Data Handling
          </p>
        </Column>
      </Grid>
    </div>
  );
}
