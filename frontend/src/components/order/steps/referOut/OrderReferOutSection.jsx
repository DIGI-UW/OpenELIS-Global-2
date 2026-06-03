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
import SubcontractStatusTag from "./subcontractStatusTag";
import OrderReferOutForm from "./OrderReferOutForm";

const OrderReferOutSection = () => {
  const intl = useIntl();
  const { samples, setSamples, orderData, saveOrder, loadOrder, labNumber } =
    useOrderContext();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [expandedSampleId, setExpandedSampleId] = useState(null);
  const [savingSampleId, setSavingSampleId] = useState(null);

  const referralOrganizations = orderData?.referralOrganizations || [];

  const closeForm = () => setExpandedSampleId(null);

  const handleSaveReferral = async (sampleIndex, formValues) => {
    const sampleItemId = samples[sampleIndex]?.sampleItemId;
    setSavingSampleId(sampleItemId);
    try {
      // Stash the referralItems on the sample, then trigger an order save.
      // The backend ReferralSetService creates one Referral per (sample,test)
      // pair from the expanded payload built in OrderContext.buildReferralItems.
      const existing = samples[sampleIndex]?.referralItems?.[0] || {};
      // Edits already know the server-assigned referralId, so the optimistic
      // setSamples is canonical and a backend reload would race the async
      // FHIR-listener write (clobbering the typed values with stale data).
      // Creates still need the reload to discover the new referralId.
      const isEdit = !!existing.referralId;
      const updatedSamples = samples.map((s, idx) =>
        idx === sampleIndex
          ? {
              ...s,
              referralItems: [
                {
                  ...existing,
                  ...formValues,
                },
              ],
            }
          : s,
      );
      setSamples(updatedSamples);
      await saveOrder(false, false, updatedSamples, isEdit);
      if (!isEdit && labNumber) {
        await loadOrder(labNumber, false);
      }
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "label.referOut.save.success",
          defaultMessage: "Referral saved.",
        }),
      });
      setNotificationVisible(true);
      closeForm();
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          error?.message ||
          intl.formatMessage({
            id: "label.referOut.save.error",
            defaultMessage: "Failed to save referral.",
          }),
      });
      setNotificationVisible(true);
    } finally {
      setSavingSampleId(null);
    }
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
    .filter((s) => s.sampleItemId)
    .map((sample, index) => {
      const referral = sample.referralItems?.[0] || null;
      return {
        id: String(sample.sampleItemId || index),
        sampleIndex: index,
        sampleId: sample.sampleItemId || `${index + 1}`,
        sampleType: sample.sampleTypeName || sample.name || "---",
        tests: sample.tests || [],
        referral,
      };
    });

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
        defaultMessage: "Referring Lab",
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
                        <SubcontractStatusTag
                          status={referral?.subcontractStatus}
                        />
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
                          <OverflowMenu size="sm" flipped ariaLabel="actions">
                            <OverflowMenuItem
                              itemText={intl.formatMessage({
                                id: "label.referOut.action.edit",
                                defaultMessage: "Edit",
                              })}
                              onClick={() => setExpandedSampleId(row.id)}
                            />
                            {referral.subcontractStatus === "DRAFT" && (
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
