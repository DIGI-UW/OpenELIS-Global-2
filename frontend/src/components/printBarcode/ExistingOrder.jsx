import { React, useState, useEffect, useMemo, useRef, useContext } from "react";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
import {
  Grid,
  Column,
  Form,
  Button,
  NumberInput,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
} from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { getFromOpenElisServer } from "../utils/Utils";
import config from "../../config.json";

const FALLBACK_MAX = 10;

const positiveInt = (value, fallback) => {
  const parsed = parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
};

const clampQuantity = (value, max) => {
  const parsed = parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed < 1) {
    return 1;
  }
  return Math.min(parsed, max);
};

/**
 * The labels an existing order can print, one row each (OGC-1169, FR-I1,
 * FR-I5, FR-I7). An order saved with label requests (order entry v4) lists
 * those: the preset by display name and size, the tube it belongs to, the
 * saved quantity, and the preset's maximum for that scope; it prints through
 * the snapshot endpoint at the chosen quantity. An older order falls back to
 * one Order label and one Specimen label per tube on the label servlet, with
 * the laboratory's configured defaults and maxima.
 */
export function buildLabelRows({
  intl,
  accessionNumber,
  existingTests,
  savedRows,
  presets,
  settings,
}) {
  const tests = (existingTests || []).filter((t) => t && t.accessionNumber);
  const saved = Array.isArray(savedRows) ? savedRows : [];
  if (saved.length > 0) {
    const orderId = saved[0].parent_sample_id ?? saved[0].parentSampleId;
    const base = `${config.serverBaseUrl}/api/orders/${encodeURIComponent(orderId)}/labels/pdf`;
    const rows = saved.map((row, index) => {
      const presetId = row.preset_id ?? row.presetId;
      const sampleItemId = row.sample_item_id ?? row.sampleItemId;
      const snapshot = row.preset_snapshot ?? row.presetSnapshot ?? {};
      const snapPreset = snapshot.preset || {};
      const preset = (presets || []).find(
        (p) => String(p.id) === String(presetId),
      );
      const scope = sampleItemId ? "sample" : "order";
      const tube = sampleItemId
        ? tests.find((t) => String(t.sampleItemId) === String(sampleItemId))
        : null;
      const name = snapPreset.name || preset?.name || `#${presetId}`;
      const height = snapPreset.heightMm ?? preset?.heightMm;
      const width = snapPreset.widthMm ?? preset?.widthMm;
      const size =
        height && width
          ? intl.formatMessage({ id: "barcode.print.size" }, { height, width })
          : "";
      const max = positiveInt(
        scope === "order" ? preset?.maxPerOrder : preset?.maxPerSample,
        FALLBACK_MAX,
      );
      return {
        key: `saved-${row.id ?? index}`,
        name: size ? `${name} (${size})` : name,
        accession: tube?.accessionNumber || accessionNumber,
        info: tube?.sampleType || "",
        scope,
        qty: clampQuantity(row.qty, max),
        max,
        printUrl: (quantity) =>
          `${base}?presetId=${encodeURIComponent(presetId)}&scope=${scope}` +
          (sampleItemId
            ? `&sampleItemId=${encodeURIComponent(sampleItemId)}`
            : "") +
          `&quantity=${quantity}`,
      };
    });
    return {
      rows,
      printAllUrl: base,
      printAllText: intl.formatMessage({ id: "barcode.print.all.saved" }),
    };
  }

  const orderDefault = positiveInt(settings?.DEFAULT_ORDER_LABEL_PRINTED, 1);
  const specimenDefault = positiveInt(
    settings?.DEFAULT_SPECIMEN_LABEL_PRINTED,
    1,
  );
  const orderMax = positiveInt(settings?.MAX_ORDER_LABEL_PRINTED, FALLBACK_MAX);
  const specimenMax = positiveInt(
    settings?.MAX_SPECIMEN_LABEL_PRINTED,
    FALLBACK_MAX,
  );
  const rows = [
    {
      key: "order",
      name: intl.formatMessage({ id: "barcode.label.order" }),
      accession: accessionNumber,
      info: "",
      scope: "order",
      qty: clampQuantity(orderDefault, orderMax),
      max: orderMax,
      printUrl: (quantity) =>
        `/LabelMakerServlet?labNo=${encodeURIComponent(accessionNumber)}&type=order&quantity=${quantity}`,
    },
    ...tests.map((test, index) => ({
      key: `specimen-${test.sampleItemId || index}`,
      name: intl.formatMessage({ id: "barcode.label.specimen" }),
      accession: test.accessionNumber,
      info: test.sampleType || "",
      scope: "sample",
      qty: clampQuantity(specimenDefault, specimenMax),
      max: specimenMax,
      printUrl: (quantity) =>
        `/LabelMakerServlet?labNo=${encodeURIComponent(test.accessionNumber)}&type=specimen&quantity=${quantity}`,
    })),
  ];
  return {
    rows,
    printAllUrl: `/LabelMakerServlet?labNo=${encodeURIComponent(accessionNumber)}&type=default&quantity=`,
    printAllText: intl.formatMessage(
      { id: "barcode.print.all.legacy" },
      { orderCount: orderDefault, specimenCount: specimenDefault },
    ),
  };
}

const ExistingOrder = () => {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const [accessionNumber, setAccessionNumber] = useState("");
  const [patientSearchResults, setPatientSearchResults] = useState(null);
  const [orderResults, setOrderResults] = useState(null);
  const [savedRows, setSavedRows] = useState(null);
  const [presets, setPresets] = useState([]);
  const [quantities, setQuantities] = useState({});
  const [source, setSource] = useState("about:blank");
  const [renderBarcode, setRenderBarcode] = useState(false);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext) || {};

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/api/labelPresets", (data) => {
      if (componentMounted.current) {
        setPresets(Array.isArray(data) ? data : (data && data.content) || []);
      }
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const fetchPatientData = (res) => {
    if (componentMounted.current) {
      let patientsResults = (res && res.patientSearchResults) || [];
      if (patientsResults.length > 0) {
        setPatientSearchResults(patientsResults[0]);
      } else {
        setPatientSearchResults(null);
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({ id: "patient.search.nopatient" }),
          kind: NotificationKinds.warning,
        });
        setNotificationVisible(true);
      }
    }
  };

  // The saved labels are asked for only once the order is known to exist, so
  // an unknown accession raises the patient warning alone and no 404.
  const fetchOrderData = (res, searched) => {
    if (!componentMounted.current) {
      return;
    }
    const tests = (res && res.existingTests) || [];
    setOrderResults(tests);
    if (tests.length > 0) {
      getFromOpenElisServer(
        `/api/orders/by-accession/${encodeURIComponent(searched)}/labels`,
        fetchSavedLabels,
      );
    } else {
      setSavedRows([]);
    }
  };

  const fetchSavedLabels = (res) => {
    if (componentMounted.current) {
      setSavedRows(Array.isArray(res) ? res : []);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setSavedRows(null);
    setOrderResults(null);
    setQuantities({});
    getFromOpenElisServer(
      `/rest/patient-search-results?labNumber=${encodeURIComponent(accessionNumber)}`,
      fetchPatientData,
    );
    const searched = accessionNumber;
    getFromOpenElisServer(
      `/rest/SampleEdit?accessionNumber=${encodeURIComponent(searched)}`,
      (res) => fetchOrderData(res, searched),
    );
  };

  const ready =
    patientSearchResults !== null &&
    orderResults !== null &&
    savedRows !== null;

  const labels = useMemo(
    () =>
      ready
        ? buildLabelRows({
            intl,
            accessionNumber,
            existingTests: orderResults,
            savedRows,
            presets,
            settings: configurationProperties,
          })
        : null,
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [
      ready,
      accessionNumber,
      orderResults,
      savedRows,
      presets,
      configurationProperties,
    ],
  );

  // Seed a row's quantity once, when the row first appears; a re-render of the
  // layout (its configuration context is a new object each time) must not put
  // a typed quantity back to the default.
  const rowSignature = labels
    ? labels.rows.map((row) => `${row.key}:${row.qty}`).join("|")
    : "";
  useEffect(() => {
    if (!labels) {
      return;
    }
    setQuantities((previous) => {
      const next = {};
      labels.rows.forEach((row) => {
        next[row.key] =
          previous[row.key] === undefined ? row.qty : previous[row.key];
      });
      return next;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rowSignature]);

  const quantityFor = (row) =>
    quantities[row.key] === undefined ? row.qty : quantities[row.key];

  const print = (url) => {
    setSource(url);
    setRenderBarcode(true);
  };

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "barcode.print.col.label" }),
    },
    {
      key: "accession",
      header: intl.formatMessage({ id: "barcode.print.col.accession" }),
    },
    {
      key: "info",
      header: intl.formatMessage({ id: "barcode.print.col.info" }),
    },
    {
      key: "quantity",
      header: intl.formatMessage({ id: "barcode.print.col.quantity" }),
    },
    { key: "action", header: "" },
  ];

  const tableRows = labels
    ? labels.rows.map((row) => ({
        id: row.key,
        name: row.name,
        accession: row.accession,
        info: row.info,
        quantity: (
          <NumberInput
            id={`print-qty-${row.key}`}
            min={1}
            max={row.max}
            value={quantityFor(row)}
            hideLabel
            label={intl.formatMessage(
              { id: "barcode.print.quantity.for" },
              { label: `${row.name} ${row.accession}` },
            )}
            helperText={intl.formatMessage(
              { id: "barcode.print.max.hint" },
              { max: row.max },
            )}
            onChange={(_event, state) =>
              setQuantities((prev) => ({
                ...prev,
                [row.key]: clampQuantity(
                  state && state.value !== undefined
                    ? state.value
                    : _event?.target?.value,
                  row.max,
                ),
              }))
            }
            className="inputText"
            data-testid={`print-qty-${row.key}`}
          />
        ),
        action: (
          <Button
            size="sm"
            renderIcon={Printer}
            onClick={() => print(row.printUrl(quantityFor(row)))}
            data-testid={`print-row-${row.key}`}
            iconDescription={intl.formatMessage(
              { id: "barcode.print.row.button" },
              { label: `${row.name} ${row.accession}` },
            )}
          >
            <FormattedMessage id="barcode.print.button" />
          </Button>
        ),
      }))
    : [];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="orderLegendBody">
        <Form onSubmit={handleSearch}>
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <h4>
                <FormattedMessage id="sample.entry.search.barcode" />
              </h4>
            </Column>
            <Column lg={8} md={8} sm={4}>
              <CustomLabNumberInput
                placeholder={"Enter Lab No"}
                id="labNumber"
                name="labNumber"
                value={accessionNumber}
                onChange={(e, rawVal) => {
                  setOrderResults(null);
                  setSavedRows(null);
                  setAccessionNumber(rawVal ? rawVal : e?.target?.value);
                }}
                labelText={<FormattedMessage id="search.label.accession" />}
              />
            </Column>
            <div className="tabsLayout">
              <Column lg={16} md={8} sm={4}>
                <Button data-cy="submitButton" type="submit" className="btn">
                  <FormattedMessage id="label.button.submit" />
                </Button>
              </Column>
            </div>
          </Grid>
        </Form>
        {patientSearchResults !== null && orderResults !== null && (
          <Grid>
            <Column lg={4}>
              <h4>
                <FormattedMessage id="patient.label.name" />
              </h4>
            </Column>
            <Column lg={4}>
              <h4>
                <FormattedMessage id="patient.dob" />
              </h4>
            </Column>
            <Column lg={4}>
              <h4>
                <FormattedMessage id="patient.gender" />
              </h4>
            </Column>
            <Column lg={4}>
              <h4>
                <FormattedMessage id="patient.natioanalid" />
              </h4>
            </Column>
            <Column lg={4}>
              {patientSearchResults.firstName +
                " " +
                patientSearchResults.lastName}
            </Column>
            <Column lg={4}>{patientSearchResults.birthdate}</Column>
            <Column lg={4}>{patientSearchResults.gender}</Column>
            <Column lg={4}>{patientSearchResults.nationalId}</Column>
          </Grid>
        )}
      </div>
      {labels && (
        <div className="orderLegendBody" data-testid="order-labels">
          <DataTable rows={tableRows} headers={headers}>
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer
                title={intl.formatMessage({ id: "barcode.print.labels.title" })}
                description={labels.printAllText}
              >
                <TableToolbar>
                  <TableToolbarContent>
                    <Button
                      renderIcon={Printer}
                      onClick={() => print(labels.printAllUrl)}
                      data-testid="print-all-labels"
                    >
                      <FormattedMessage id="barcode.print.all.button" />
                    </Button>
                  </TableToolbarContent>
                </TableToolbar>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow
                        key={row.id}
                        data-testid={`label-row-${row.id}`}
                      >
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </div>
      )}
      {renderBarcode && (
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <h4>
                <FormattedMessage id="barcode.header" />
              </h4>
            </Column>
          </Grid>
          <iframe
            title={intl.formatMessage({ id: "barcode.header" })}
            src={source}
            width="100%"
            height="500px"
          />
        </div>
      )}
    </>
  );
};
export default injectIntl(ExistingOrder);
