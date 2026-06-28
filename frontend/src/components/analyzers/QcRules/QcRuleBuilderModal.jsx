import React, { useState, useEffect, useCallback } from "react";
import { Button, ButtonSet, InlineNotification, Loading } from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory, useParams } from "react-router-dom";
import QcRuleRow from "./QcRuleRow";
import PageTitle from "../../common/PageTitle/PageTitle";
import {
  getAnalyzer,
  getQcRules,
  createQcRule,
  updateQcRule,
  deleteQcRule,
} from "../../../services/analyzerService";

const EMPTY_RULE = {
  ruleType: "FIELD_EQUALS",
  targetField: "",
  operand: "",
  isActive: true,
};

const QcRulePage = () => {
  const intl = useIntl();
  const history = useHistory();
  const { id: analyzerId } = useParams();
  const [analyzer, setAnalyzer] = useState(null);
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notification, setNotification] = useState(null);
  const [originalRules, setOriginalRules] = useState([]);

  // Fetch analyzer metadata for breadcrumb display name
  useEffect(() => {
    if (analyzerId) {
      getAnalyzer(analyzerId, (data) => {
        const a = data?.analyzers?.[0] || data;
        setAnalyzer(a);
      });
    }
  }, [analyzerId]);

  const loadRules = useCallback(() => {
    if (!analyzerId) return;
    setLoading(true);
    getQcRules(analyzerId, (response) => {
      setLoading(false);
      if (Array.isArray(response)) {
        setRules(response);
        setOriginalRules(JSON.parse(JSON.stringify(response)));
      } else {
        setNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "analyzer.qcRules.error.loadFailed",
          }),
        });
      }
    });
  }, [analyzerId, intl]);

  useEffect(() => {
    setNotification(null);
    loadRules();
  }, [loadRules]);

  const handleRuleChange = (index, updatedRule) => {
    setRules((prev) => {
      const next = [...prev];
      next[index] = updatedRule;
      return next;
    });
  };

  const handleAddRule = () => {
    setRules((prev) => [
      ...prev,
      { ...EMPTY_RULE, displayOrder: prev.length + 1 },
    ]);
  };

  const handleDeleteRule = (index) => {
    const rule = rules[index];
    if (rule.id) {
      setSaving(true);
      deleteQcRule(analyzer.id, rule.id, (success, error) => {
        setSaving(false);
        if (success) {
          setRules((prev) => prev.filter((_, i) => i !== index));
        } else {
          setNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "analyzer.qcRules.error.deleteFailed",
            }),
            subtitle: error?.error,
          });
        }
      });
    } else {
      setRules((prev) => prev.filter((_, i) => i !== index));
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setNotification(null);

    try {
      for (let i = 0; i < rules.length; i++) {
        const rule = rules[i];
        const payload = {
          ruleType: rule.ruleType,
          targetField: rule.targetField || null,
          operand: rule.operand,
          isActive: rule.isActive !== false,
          displayOrder: i + 1,
          description: rule.description || null,
        };

        if (rule.id) {
          const original = originalRules.find((r) => r.id === rule.id);
          const changed =
            !original ||
            original.ruleType !== payload.ruleType ||
            original.targetField !== payload.targetField ||
            original.operand !== payload.operand ||
            original.isActive !== payload.isActive ||
            original.displayOrder !== payload.displayOrder;

          if (changed) {
            await new Promise((resolve, reject) => {
              updateQcRule(analyzer.id, rule.id, payload, (response) => {
                if (response?.error) {
                  reject(new Error(response.error));
                } else {
                  resolve(response);
                }
              });
            });
          }
        } else {
          await new Promise((resolve, reject) => {
            createQcRule(analyzer.id, payload, (response) => {
              if (response?.error) {
                reject(new Error(response.error));
              } else {
                resolve(response);
              }
            });
          });
        }
      }

      setSaving(false);
      history.push("/analyzers");
    } catch (error) {
      setSaving(false);
      setNotification({
        kind: "error",
        title: intl.formatMessage({ id: "analyzer.qcRules.error.saveFailed" }),
        subtitle: error.message,
      });
    }
  };

  return (
    <div data-testid="qc-rule-page" className="qc-rule-page">
      <PageTitle
        breadcrumbs={[
          {
            label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
            link: "/analyzers",
          },
          {
            label: analyzer?.name || "...",
            link: `/analyzers/${analyzerId}/edit`,
          },
          {
            label: intl.formatMessage({ id: "analyzer.qcRules.modal.title" }),
          },
        ]}
      />
      <div className="qc-rule-content">
        {loading && <Loading withOverlay={false} small />}

        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onClose={() => setNotification(null)}
            style={{ marginBottom: "1rem" }}
          />
        )}

        <InlineNotification
          kind="info"
          title={intl.formatMessage({ id: "analyzer.qcRules.orSemantics" })}
          hideCloseButton
          lowContrast
          style={{ marginBottom: "1rem" }}
        />

        {!loading && rules.length === 0 && (
          <p
            style={{ color: "var(--cds-text-secondary)", marginBottom: "1rem" }}
          >
            {intl.formatMessage({ id: "analyzer.qcRules.noRules" })}
          </p>
        )}

        {rules.map((rule, index) => (
          <QcRuleRow
            key={rule.id || `new-${index}`}
            rule={rule}
            index={index}
            onChange={handleRuleChange}
            onDelete={handleDeleteRule}
            disabled={saving}
          />
        ))}

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Add}
          onClick={handleAddRule}
          disabled={saving || rules.length >= 50}
          data-testid="qc-rule-add-btn"
        >
          {intl.formatMessage({ id: "analyzer.qcRules.addRule" })}
        </Button>
      </div>
      <ButtonSet className="qc-rule-actions">
        <Button
          kind="secondary"
          onClick={() => history.push("/analyzers")}
          disabled={saving}
        >
          {intl.formatMessage({ id: "label.button.cancel" })}
        </Button>
        <Button
          kind="primary"
          onClick={handleSave}
          disabled={saving || loading}
          data-testid="qc-rule-save-btn"
        >
          {intl.formatMessage({ id: "label.button.save" })}
        </Button>
      </ButtonSet>
    </div>
  );
};

export default QcRulePage;
