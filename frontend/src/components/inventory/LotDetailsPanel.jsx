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
import "./LotDetailsPanel.css";

const LotDetailsPanel = ({ open, onClose, lot }) => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState([]);
  const [usage, setUsage] = useState([]);

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
      NOT_REQUIRED: { type: "outline", label: "Not Required" },
    };
    const config = statusMap[status] || statusMap.PENDING;
    return <Tag type={config.type}>{config.label}</Tag>;
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
            </TabPanels>
          </Tabs>
        )}
      </div>
    </div>
  );
};

export default LotDetailsPanel;
