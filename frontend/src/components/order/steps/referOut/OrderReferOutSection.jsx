import React, { useContext, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  Button,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  InlineNotification,
} from "@carbon/react";
import { useOrderContext } from "../../OrderContext";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import ReferralStatusTag from "./referralStatusTag";
import OrderReferOutForm from "./OrderReferOutForm";

const OrderReferOutSection = () => {
  const intl = useIntl();
  const { samples, setSamples, orderData, loadOrder, labNumber } =
    useOrderContext();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [expandedSampleId, setExpandedSampleId] = useState(null);
  const [savingSampleId, setSavingSampleId] = useState(null);
  const [bulkOpen, setBulkOpen] = useState(false);

  const referralOrganizations = orderData?.referralOrganizations || [];
  const referralReasons = orderData?.referralReasons || [];

  const closeForm = () => setExpandedSampleId(null);

  // A referral entered here is staged on its sample and saved with the step's
  // Save, as one transaction with everything else on the step (FR-A5, FR-E5).
  const stageReferral = (sample, formValues) => ({
    ...sample,
    referralItems: [
      {
        ...(sample.referralItems?.[0] || {}),
        ...formValues,
        pendingSave: true,
      },
    ],
  });

  const notifyStaged = (count) => {
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        { id: "label.referOut.staged.success" },
        { count },
      ),
    });
    setNotificationVisible(true);
  };

  const handleSaveReferral = (sampleIndex, formValues) => {
    setSamples(
      samples.map((s, idx) =>
        idx === sampleIndex ? stageReferral(s, formValues) : s,
      ),
    );
    notifyStaged(1);
    closeForm();
  };

  const isInHouse = (sample) =>
    Boolean(sample.sampleItemId) && !sample.referralItems?.[0];

  const handleBulkReferral = (formValues) => {
    const count = samples.filter(isInHouse).length;
    setSamples(
      samples.map((s) => (isInHouse(s) ? stageReferral(s, formValues) : s)),
    );
    notifyStaged(count);
    setBulkOpen(false);
  };

  const handleDispatch = (sample) => {
    const referral = sample.referralItems?.[0];
    if (!referral?.referralId) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "label.referOut.dispatch.notSaved",
          defaultMessage: "Save the referral before dispatching.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    if (!referral.handoffDatetime) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "error.referOut.handoffRequiredForDispatch",
          defaultMessage: "Handoff Date/Time is required to dispatch.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    setSavingSampleId(sample.sampleItemId);
    postToOpenElisServerJsonResponse(
      `/rest/referrals/${encodeURIComponent(referral.referralId)}/dispatch-subcontract`,
      JSON.stringify({
        handoffDatetime: referral.handoffDatetime,
        notes: referral.subcontractNotes || "",
      }),
      (response) => {
        setSavingSampleId(null);
        if (response && !response.error) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "label.referOut.dispatch.success",
              defaultMessage: "Referral dispatched.",
            }),
          });
          setNotificationVisible(true);
          if (labNumber) {
            loadOrder(labNumber, false);
          }
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              response?.error ||
              intl.formatMessage({
                id: "label.referOut.dispatch.error",
                defaultMessage: "Failed to dispatch referral.",
              }),
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const rows = samples
    .map((sample, index) => ({ sample, index }))
    .filter(({ sample }) => sample.sampleItemId)
    .map(({ sample, index }) => ({
      id: String(sample.sampleItemId),
      sampleIndex: index,
      sampleId: `${labNumber || ""}-${index + 1}`,
      sampleType: sample.sampleTypeName || sample.name || "---",
      tests: sample.tests || [],
      referral: sample.referralItems?.[0] || null,
    }));

  const headers = [
    {
      key: "sampleId",
      header: intl.formatMessage({
        id: "label.referOut.column.sampleId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "label.referOut.column.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "tests",
      header: intl.formatMessage({
        id: "label.referOut.column.tests",
        defaultMessage: "Tests",
      }),
    },
    {
      key: "referringLab",
      header: intl.formatMessage({
        id: "label.referOut.column.referringLab",
        defaultMessage: "Reference lab",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "label.referOut.column.status",
        defaultMessage: "Subcontract Status",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.referOut.column.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  const renderTests = (tests) => {
    if (!tests || tests.length === 0) {
      return <span className="cds--type-helper-text-01">—</span>;
    }
    return (
      <div className="refer-out-test-chips">
        {tests.map((t) => (
          <Tag key={t.id} type="cool-gray" size="sm">
            {t.name}
          </Tag>
        ))}
      </div>
    );
  };

  const renderReferringLab = (referral) => {
    if (!referral || !referral.referredInstituteId) return "—";
    if (referral.referredInstituteName) return referral.referredInstituteName;
    const match = referralOrganizations.find(
      (o) => o.id === referral.referredInstituteId,
    );
    return match ? match.value : referral.referredInstituteId;
  };

  if (rows.length === 0) {
    return (
      <Tile className="order-section refer-out-section">
        <h4>
          <FormattedMessage
            id="label.referOut.section.title"
            defaultMessage="Refer Out / Subcontract"
          />
        </h4>
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.referOut.empty.title",
            defaultMessage: "No samples available for referral",
          })}
          subtitle={intl.formatMessage({
            id: "label.referOut.empty.subtitle",
            defaultMessage:
              "Samples appear here once they are saved. Save the order first to refer specimens out.",
          })}
        />
      </Tile>
    );
  }

  return (
    <Tile className="order-section refer-out-section">
      <h4>
        <FormattedMessage
          id="label.referOut.section.title"
          defaultMessage="Refer Out / Subcontract"
        />
      </h4>
      <p className="section-description">
        <FormattedMessage
          id="label.referOut.section.description"
          defaultMessage="Refer specimens to an external lab. Each sample carries its full chain-of-custody metadata."
        />
      </p>

      <div className="refer-out-bulk">
        <Button
          kind="tertiary"
          size="sm"
          data-testid="refer-out-all"
          disabled={!samples.some(isInHouse) || bulkOpen}
          onClick={() => {
            setExpandedSampleId(null);
            setBulkOpen(true);
          }}
        >
          <FormattedMessage
            id="label.referOut.action.referOutAll"
            defaultMessage="Refer out all in-house samples"
          />
        </Button>
        {bulkOpen && (
          <div
            className="refer-out-bulk__form"
            data-testid="refer-out-bulk-form"
          >
            <OrderReferOutForm
              referralOrganizations={referralOrganizations}
              referralReasons={referralReasons}
              onSave={handleBulkReferral}
              onCancel={() => setBulkOpen(false)}
            />
          </div>
        )}
      </div>

      <DataTable rows={rows} headers={headers}>
        {({ rows: dtRows, headers: dtHeaders, getTableProps, getRowProps }) => (
          <Table {...getTableProps()} size="md">
            <TableHead>
              <TableRow>
                <TableExpandHeader />
                {dtHeaders.map((header) => (
                  <TableHeader key={header.key}>{header.header}</TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {dtRows.map((row) => {
                const data = rows.find((r) => r.id === row.id);
                const referral = data?.referral;
                const isExpanded = expandedSampleId === row.id;
                const isSaving = savingSampleId === row.id;
                return (
                  <React.Fragment key={row.id}>
                    <TableExpandRow
                      {...getRowProps({ row })}
                      isExpanded={isExpanded}
                      onExpand={() =>
                        setExpandedSampleId(isExpanded ? null : row.id)
                      }
                    >
                      <TableCell>{data.sampleId}</TableCell>
                      <TableCell>{data.sampleType}</TableCell>
                      <TableCell>{renderTests(data.tests)}</TableCell>
                      <TableCell>{renderReferringLab(referral)}</TableCell>
                      <TableCell>
                        {referral?.pendingSave ? (
                          <Tag
                            type="blue"
                            size="sm"
                            data-testid="refer-out-pending"
                          >
                            <FormattedMessage
                              id="label.referOut.status.pendingSave"
                              defaultMessage="Pending save"
                            />
                          </Tag>
                        ) : (
                          <ReferralStatusTag
                            status={referral?.referralStatus}
                          />
                        )}
                      </TableCell>
                      <TableCell>
                        {!referral && (
                          <Button
                            kind="tertiary"
                            size="sm"
                            onClick={() => setExpandedSampleId(row.id)}
                          >
                            <FormattedMessage
                              id="label.referOut.action.referOut"
                              defaultMessage="Refer Out"
                            />
                          </Button>
                        )}
                        {referral && (
                          <OverflowMenu
                            size="sm"
                            flipped
                            aria-label={intl.formatMessage({
                              id: "label.referOut.column.actions",
                            })}
                          >
                            <OverflowMenuItem
                              itemText={intl.formatMessage({
                                id: "label.referOut.action.edit",
                                defaultMessage: "Edit",
                              })}
                              onClick={() => setExpandedSampleId(row.id)}
                            />
                            {referral.referralStatus === "DRAFT" && (
                              <OverflowMenuItem
                                itemText={intl.formatMessage({
                                  id: "label.referOut.action.dispatch",
                                  defaultMessage: "Dispatch",
                                })}
                                disabled={isSaving}
                                onClick={() =>
                                  handleDispatch(samples[data.sampleIndex])
                                }
                              />
                            )}
                          </OverflowMenu>
                        )}
                      </TableCell>
                    </TableExpandRow>
                    {isExpanded && (
                      <TableExpandedRow colSpan={dtHeaders.length + 1}>
                        <OrderReferOutForm
                          initialValues={referral || {}}
                          referralOrganizations={referralOrganizations}
                          referralReasons={referralReasons}
                          isSaving={isSaving}
                          onSave={(values) =>
                            handleSaveReferral(data.sampleIndex, values)
                          }
                          onCancel={closeForm}
                        />
                      </TableExpandedRow>
                    )}
                  </React.Fragment>
                );
              })}
            </TableBody>
          </Table>
        )}
      </DataTable>
    </Tile>
  );
};

export default OrderReferOutSection;
