import React, { useState, useEffect } from "react";
import {
  SideNav,
  SideNavItems,
  Button,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Tag,
  Loading,
} from "@carbon/react";
import { Close } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { TransactionAPI, UsageAPI } from "./InventoryService";
import AuditLogViewer from "./AuditLogViewer";
import "./LotDetailsPanel.css";

const LotDetailsPanel = ({ open, onClose, lot }) => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState([]);
  const [usage, setUsage] = useState([]);
  const [auditLogOpen, setAuditLogOpen] = useState(false);
  const [qcHistory, setQcHistory] = useState([]);

  useEffect(() => {
    if (open && lot) {
      fetchLotDetails();
    }
  }, [open, lot]);

  const fetchLotDetails = async () => {
    setLoading(true);
    try {
      const txns = await TransactionAPI.getByLot(lot.id);
      setTransactions(txns || []);

      // Filter QC transactions for QC history
      const qcTransactions = (txns || []).filter(
        (t) => t.transactionType === "QC_TEST",
      );
      setQcHistory(qcTransactions);

      const usageData = await UsageAPI.getByLot(lot.id);
      setUsage(usageData || []);
    } catch (err) {
      console.error("Error fetching lot details:", err);
    } finally {
      setLoading(false);
    }
  };

  if (!lot) return null;

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleString();
  };

  const getQCStatusTag = (status) => {
    const statusMap = {
      PASSED: { type: "green", label: "Passed" },
      FAILED: { type: "red", label: "Failed" },
      PENDING: { type: "gray", label: "Pending" },
      QUARANTINED: { type: "magenta", label: "Quarantined" },
      NOT_REQUIRED: { type: "outline", label: "Not Required" },
    };
    const config = statusMap[status] || statusMap.PENDING;
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  const extractQcResult = (notes) => {
    if (!notes) return "UNKNOWN";
    // Parse notes like "QC status changed from PENDING to PASSED"
    if (notes.includes("to PASSED")) return "PASSED";
    if (notes.includes("to FAILED")) return "FAILED";
    if (notes.includes("to QUARANTINED")) return "QUARANTINED";
    if (notes.includes("to PENDING")) return "PENDING";
    return "UNKNOWN";
  };

  return (
    <div className={`lot-details-panel ${open ? "open" : ""}`}>
      <div className="panel-header">
        <h3>
          <FormattedMessage id="lot.details.title" />
        </h3>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Close}
          iconDescription="Close"
          onClick={onClose}
          hasIconOnly
        />
      </div>

      <div className="panel-content">
        {loading ? (
          <Loading description="Loading lot details..." withOverlay={false} />
        ) : (
          <Tabs>
            <TabList aria-label="Lot details tabs">
              <Tab>
                <FormattedMessage id="lot.details.tab.info" />
              </Tab>
              <Tab>
                <FormattedMessage id="lot.details.tab.transactions" />
              </Tab>
              <Tab>
                <FormattedMessage id="lot.details.tab.usage" />
              </Tab>
              <Tab>
                <FormattedMessage id="lot.details.tab.audit" />
              </Tab>
            </TabList>

            <TabPanels>
              <TabPanel>
                <div className="panel-section">
                  <h4>
                    <FormattedMessage id="lot.details.section.basic" />
                  </h4>
                  <StructuredListWrapper>
                    <StructuredListBody>
                      <StructuredListRow>
                        <StructuredListCell>
                          <FormattedMessage id="lot.number" />
                        </StructuredListCell>
                        <StructuredListCell>
                          <strong>{lot.lotNumber}</strong>
                        </StructuredListCell>
                      </StructuredListRow>
                      {lot.barcode && (
                        <StructuredListRow>
                          <StructuredListCell>
                            <FormattedMessage id="lot.barcode" />
                          </StructuredListCell>
                          <StructuredListCell>
                            <strong>{lot.barcode}</strong>
                          </StructuredListCell>
                        </StructuredListRow>
                      )}
                      <StructuredListRow>
                        <StructuredListCell>
                          <FormattedMessage id="catalog.item.name" />
                        </StructuredListCell>
                        <StructuredListCell>
                          {lot.inventoryItem?.name}
                        </StructuredListCell>
                      </StructuredListRow>
                      <StructuredListRow>
                        <StructuredListCell>
                          <FormattedMessage id="catalog.item.type" />
                        </StructuredListCell>
                        <StructuredListCell>
                          {lot.inventoryItem?.itemType}
                        </StructuredListCell>
                      </StructuredListRow>
                      <StructuredListRow>
                        <StructuredListCell>
                          <FormattedMessage id="lot.qcStatus" />
                        </StructuredListCell>
                        <StructuredListCell>
                          {getQCStatusTag(lot.qcStatus)}
                        </StructuredListCell>
                      </StructuredListRow>
                    </StructuredListBody>
                  </StructuredListWrapper>
                </div>

                <div className="panel-section">
                  <h4>
                    <FormattedMessage id="lot.details.section.quantities" />
                  </h4>
                  <StructuredListWrapper>
                    <StructuredListBody>
                      <StructuredListRow>
                        <StructuredListCell>
                          <FormattedMessage id="lot.initialQuantity" />
                        </StructuredListCell>
                        <StructuredListCell>
                          {lot.initialQuantity} {lot.inventoryItem?.units}
                        </StructuredListCell>
                      </StructuredListRow>
                      <StructuredListRow>
                        <StructuredListCell>
                          <FormattedMessage id="lot.currentQuantity" />
                        </StructuredListCell>
                        <StructuredListCell>
                          <strong>
                            {lot.currentQuantity} {lot.inventoryItem?.units}
                          </strong>
                        </StructuredListCell>
                      </StructuredListRow>
                    </StructuredListBody>
                  </StructuredListWrapper>
                </div>

                <div className="panel-section">
                  <h4>
                    <FormattedMessage id="lot.details.section.dates" />
                  </h4>
                  <StructuredListWrapper>
                    <StructuredListBody>
                      <StructuredListRow>
                        <StructuredListCell>
                          <FormattedMessage id="lot.receiptDate" />
                        </StructuredListCell>
                        <StructuredListCell>
                          {formatDate(lot.receiptDate)}
                        </StructuredListCell>
                      </StructuredListRow>
                      <StructuredListRow>
                        <StructuredListCell>
                          <FormattedMessage id="lot.expirationDate" />
                        </StructuredListCell>
                        <StructuredListCell>
                          {formatDate(lot.expirationDate)}
                        </StructuredListCell>
                      </StructuredListRow>
                      {lot.dateOpened && (
                        <StructuredListRow>
                          <StructuredListCell>
                            <FormattedMessage id="lot.dateOpened" />
                          </StructuredListCell>
                          <StructuredListCell>
                            {formatDate(lot.dateOpened)}
                          </StructuredListCell>
                        </StructuredListRow>
                      )}
                    </StructuredListBody>
                  </StructuredListWrapper>
                </div>

                {lot.storageLocation && (
                  <div className="panel-section">
                    <h4>
                      <FormattedMessage id="lot.details.section.storage" />
                    </h4>
                    <p>{lot.storageLocation.name || lot.storageLocation}</p>
                  </div>
                )}

                {/* Type-specific Item Information */}
                {lot.inventoryItem && (
                  <div className="panel-section">
                    <h4>
                      <FormattedMessage id="lot.details.section.itemSpecific" />
                    </h4>
                    <StructuredListWrapper>
                      <StructuredListBody>
                        {/* REAGENT-specific fields */}
                        {lot.inventoryItem.itemType === "REAGENT" && (
                          <>
                            {lot.inventoryItem.stabilityAfterOpening && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.stabilityAfterOpening" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.stabilityAfterOpening}{" "}
                                  <FormattedMessage id="catalog.item.days" />
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                            {lot.inventoryItem.dilutionNotes && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.dilutionNotes" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.dilutionNotes}
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                            {lot.inventoryItem.storageRequirements && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.storageRequirements" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.storageRequirements}
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                          </>
                        )}

                        {/* CARTRIDGE-specific fields */}
                        {lot.inventoryItem.itemType === "CARTRIDGE" && (
                          <>
                            {lot.inventoryItem.compatibleAnalyzers && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.compatibleAnalyzers" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.compatibleAnalyzers}
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                            {lot.inventoryItem.calibrationRequired && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.calibrationRequired" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.calibrationRequired === "Y"
                                    ? intl.formatMessage({ id: "label.yes" })
                                    : intl.formatMessage({ id: "label.no" })}
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                          </>
                        )}

                        {/* RDT-specific fields */}
                        {lot.inventoryItem.itemType === "RDT" && (
                          <>
                            {lot.inventoryItem.testsPerKit && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.testsPerKit" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.testsPerKit}
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                            {lot.inventoryItem.individualTracking && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.individualTracking" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.individualTracking === "Y"
                                    ? intl.formatMessage({ id: "label.yes" })
                                    : intl.formatMessage({ id: "label.no" })}
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                          </>
                        )}

                        {/* HIV_KIT and SYPHILIS_KIT-specific fields */}
                        {(lot.inventoryItem.itemType === "HIV_KIT" ||
                          lot.inventoryItem.itemType === "SYPHILIS_KIT") && (
                          <>
                            {lot.inventoryItem.sourceOrganization && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.sourceOrganization" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.sourceOrganization}
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                            {lot.inventoryItem.kitTestType && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.kitTestType" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.kitTestType}
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                            {lot.inventoryItem.testsPerKit && (
                              <StructuredListRow>
                                <StructuredListCell>
                                  <FormattedMessage id="catalog.item.testsPerKit" />
                                </StructuredListCell>
                                <StructuredListCell>
                                  {lot.inventoryItem.testsPerKit}
                                </StructuredListCell>
                              </StructuredListRow>
                            )}
                          </>
                        )}
                      </StructuredListBody>
                    </StructuredListWrapper>
                  </div>
                )}

                {/* QC History Section */}
                <div className="panel-section">
                  <h4>
                    <FormattedMessage id="lot.details.section.qcHistory" />
                  </h4>
                  {qcHistory.length === 0 ? (
                    <p className="empty-state">
                      <FormattedMessage id="lot.details.qcHistory.empty" />
                    </p>
                  ) : (
                    <StructuredListWrapper>
                      <StructuredListHead>
                        <StructuredListRow head>
                          <StructuredListCell head>
                            <FormattedMessage id="lot.qcHistory.date" />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage id="lot.qcHistory.qcRunId" />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage id="lot.qcHistory.result" />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage id="lot.qcHistory.technician" />
                          </StructuredListCell>
                        </StructuredListRow>
                      </StructuredListHead>
                      <StructuredListBody>
                        {qcHistory.map((qc) => (
                          <StructuredListRow key={qc.id}>
                            <StructuredListCell>
                              {formatDate(qc.transactionDate)}
                            </StructuredListCell>
                            <StructuredListCell>
                              {qc.referenceId || (
                                <span style={{ color: "#8d8d8d" }}>N/A</span>
                              )}
                            </StructuredListCell>
                            <StructuredListCell>
                              {getQCStatusTag(extractQcResult(qc.notes))}
                            </StructuredListCell>
                            <StructuredListCell>
                              {qc.performedByUser || (
                                <span style={{ color: "#8d8d8d" }}>
                                  Unknown
                                </span>
                              )}
                            </StructuredListCell>
                          </StructuredListRow>
                        ))}
                      </StructuredListBody>
                    </StructuredListWrapper>
                  )}
                </div>
              </TabPanel>

              <TabPanel>
                <div className="panel-section">
                  <h4>
                    <FormattedMessage id="lot.details.section.transactions" />
                  </h4>
                  {transactions.length === 0 ? (
                    <p className="empty-state">
                      <FormattedMessage id="lot.details.no.transactions" />
                    </p>
                  ) : (
                    <StructuredListWrapper>
                      <StructuredListHead>
                        <StructuredListRow head>
                          <StructuredListCell head>
                            <FormattedMessage id="transaction.date" />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage id="transaction.type" />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage id="transaction.quantity" />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage id="transaction.user" />
                          </StructuredListCell>
                        </StructuredListRow>
                      </StructuredListHead>
                      <StructuredListBody>
                        {transactions.map((txn) => (
                          <StructuredListRow key={txn.id}>
                            <StructuredListCell>
                              {formatDate(txn.transactionDate)}
                            </StructuredListCell>
                            <StructuredListCell>
                              {txn.transactionType}
                            </StructuredListCell>
                            <StructuredListCell>
                              {txn.quantityChange > 0 ? "+" : ""}
                              {txn.quantityChange}
                            </StructuredListCell>
                            <StructuredListCell>
                              {txn.performedByUser || "-"}
                            </StructuredListCell>
                          </StructuredListRow>
                        ))}
                      </StructuredListBody>
                    </StructuredListWrapper>
                  )}
                </div>
              </TabPanel>

              <TabPanel>
                <div className="panel-section">
                  <h4>
                    <FormattedMessage id="lot.details.section.usage" />
                  </h4>
                  {usage.length === 0 ? (
                    <p className="empty-state">
                      <FormattedMessage id="lot.details.no.usage" />
                    </p>
                  ) : (
                    <StructuredListWrapper>
                      <StructuredListHead>
                        <StructuredListRow head>
                          <StructuredListCell head>
                            <FormattedMessage id="usage.date" />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage id="usage.quantityUsed" />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage id="usage.testResultId" />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage id="usage.analysisId" />
                          </StructuredListCell>
                        </StructuredListRow>
                      </StructuredListHead>
                      <StructuredListBody>
                        {usage.map((u) => (
                          <StructuredListRow key={u.id}>
                            <StructuredListCell>
                              {formatDate(u.usageDate)}
                            </StructuredListCell>
                            <StructuredListCell>
                              {u.quantityUsed}
                            </StructuredListCell>
                            <StructuredListCell>
                              {u.testResultId || "-"}
                            </StructuredListCell>
                            <StructuredListCell>
                              {u.analysisId || "-"}
                            </StructuredListCell>
                          </StructuredListRow>
                        ))}
                      </StructuredListBody>
                    </StructuredListWrapper>
                  )}
                </div>
              </TabPanel>

              <TabPanel>
                <div className="panel-section">
                  <h4>
                    <FormattedMessage id="lot.details.section.audit" />
                  </h4>
                  <p>
                    <FormattedMessage id="lot.details.audit.description" />
                  </p>
                  <Button
                    kind="primary"
                    size="md"
                    onClick={() => setAuditLogOpen(true)}
                  >
                    <FormattedMessage id="lot.details.viewAuditLog" />
                  </Button>
                </div>
              </TabPanel>
            </TabPanels>
          </Tabs>
        )}
      </div>

      {/* Audit Log Modal */}
      <AuditLogViewer
        open={auditLogOpen}
        onClose={() => setAuditLogOpen(false)}
        entityType="LOT"
        entityId={lot?.id}
        entityName={lot?.lotNumber}
      />
    </div>
  );
};

export default LotDetailsPanel;
