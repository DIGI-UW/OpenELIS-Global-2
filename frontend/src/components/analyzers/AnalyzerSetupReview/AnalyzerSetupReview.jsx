import React, { useEffect, useState } from "react";
import {
  Button,
  ButtonSet,
  InlineNotification,
  Loading,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
} from "@carbon/react";
import { ArrowLeft, Checkmark } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory, useLocation, useParams } from "react-router-dom";
import * as analyzerService from "../../../services/analyzerService";
import PageHeader from "../../common/PageHeader/PageHeader";
import AnalyzerSetupProgress from "../AnalyzerSetupProgress/AnalyzerSetupProgress";
import { blockerMessageIds } from "../FieldMapping/SetupVerificationPanel";
import {
  buildAnalyzerSetupUrl,
  resolveAnalyzerReturnTo,
} from "../analyzerRoutes";
import "./AnalyzerSetupReview.css";

const AnalyzerSetupReview = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { id: analyzerId } = useParams();
  const [analyzer, setAnalyzer] = useState(null);
  const [verification, setVerification] = useState(null);
  const [loading, setLoading] = useState(true);
  const params = new URLSearchParams(location.search || "");
  const profileId = params.get("profile") || undefined;
  const returnTo = resolveAnalyzerReturnTo(params.get("returnTo"));

  useEffect(() => {
    let analyzerLoaded = false;
    let verificationLoaded = false;
    const finish = () => {
      if (analyzerLoaded && verificationLoaded) {
        setLoading(false);
      }
    };

    analyzerService.getAnalyzer(analyzerId, (response) => {
      setAnalyzer(response?.analyzers?.[0] || response || null);
      analyzerLoaded = true;
      finish();
    });
    analyzerService.getSetupVerification(analyzerId, (response) => {
      setVerification(response || null);
      verificationLoaded = true;
      finish();
    });
  }, [analyzerId]);

  if (loading) {
    return <Loading withOverlay={false} />;
  }

  const ready = verification?.readyForActivation === true;
  const active = analyzer?.status === "ACTIVE";
  const reviewState = active ? "active" : ready ? "ready" : "blocked";
  const summaryRows = [
    [
      intl.formatMessage({ id: "analyzer.review.instrument" }),
      analyzer?.name || "-",
    ],
    [
      intl.formatMessage({ id: "analyzer.review.protocol" }),
      analyzer?.protocolVersion || analyzer?.analyzerType || "-",
    ],
    [
      intl.formatMessage({ id: "analyzer.review.connection" }),
      analyzer?.ipAddress && analyzer?.port
        ? `${analyzer.ipAddress}:${analyzer.port}`
        : analyzer?.importDirectory || "-",
    ],
    [
      intl.formatMessage({ id: "analyzer.review.status" }),
      analyzer?.status || "SETUP",
    ],
  ];

  return (
    <main className="analyzer-setup-review" data-testid="analyzer-setup-review">
      <PageHeader
        breadcrumbs={[
          {
            label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
            link: "/analyzers",
          },
          {
            label: analyzer?.name || "-",
            link: buildAnalyzerSetupUrl("verify", {
              analyzerId,
              profileId,
              returnTo,
            }),
          },
          {
            label: intl.formatMessage({ id: "analyzer.setup.step.review" }),
          },
        ]}
        subtitle={intl.formatMessage({
          id: active
            ? "analyzer.review.active.subtitle"
            : "analyzer.review.subtitle",
        })}
      />
      <AnalyzerSetupProgress currentStep="review" />

      <section aria-labelledby="analyzer-review-summary">
        <div className="analyzer-setup-review__heading">
          <h2 id="analyzer-review-summary">
            {intl.formatMessage({ id: "analyzer.review.summary" })}
          </h2>
          <Tag type={ready ? "green" : "warm-gray"}>
            {intl.formatMessage({
              id: ready
                ? "analyzer.setupReadiness.ready"
                : "analyzer.setupReadiness.required",
            })}
          </Tag>
        </div>
        <StructuredListWrapper>
          <StructuredListBody>
            {summaryRows.map(([label, value]) => (
              <StructuredListRow key={label}>
                <StructuredListCell>{label}</StructuredListCell>
                <StructuredListCell>{value}</StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      </section>

      <InlineNotification
        kind={active || ready ? "success" : "warning"}
        title={intl.formatMessage({
          id: `analyzer.review.${reviewState}`,
        })}
        subtitle={intl.formatMessage({
          id: `analyzer.review.${reviewState}.detail`,
        })}
        lowContrast
        hideCloseButton
      />

      {verification?.blockers?.length > 0 && (
        <ul className="analyzer-setup-review__blockers">
          {verification.blockers.map((blocker) => (
            <li key={blocker}>
              {intl.formatMessage({
                id:
                  blockerMessageIds[blocker] ||
                  "analyzer.setupVerification.blocker.unknown",
              })}
            </li>
          ))}
        </ul>
      )}

      {verification?.verifiedBy && verification?.verifiedAt && (
        <p>
          {intl.formatMessage(
            { id: "analyzer.setupVerification.audit" },
            {
              actor: verification.verifiedBy,
              time: intl.formatDate(new Date(verification.verifiedAt), {
                dateStyle: "medium",
                timeStyle: "short",
              }),
            },
          )}
        </p>
      )}

      <ButtonSet className="analyzer-setup-review__actions">
        <Button
          kind="secondary"
          renderIcon={ArrowLeft}
          data-testid="analyzer-review-back"
          onClick={() =>
            history.push(
              buildAnalyzerSetupUrl("connect", {
                analyzerId,
                profileId,
                returnTo,
              }),
            )
          }
        >
          {intl.formatMessage({ id: "analyzer.review.back" })}
        </Button>
        <Button
          kind="primary"
          renderIcon={Checkmark}
          data-testid="analyzer-review-finish"
          onClick={() => history.push(returnTo)}
        >
          {intl.formatMessage({ id: "analyzer.review.finish" })}
        </Button>
      </ButtonSet>
    </main>
  );
};

export default AnalyzerSetupReview;
