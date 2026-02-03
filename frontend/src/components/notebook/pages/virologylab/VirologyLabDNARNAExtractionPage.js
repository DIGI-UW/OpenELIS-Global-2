import {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  useRef,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Modal,
  TextInput,
  Dropdown,
  DatePickerInput,
  Grid,
  Column,
  Tile,
  TextArea,
  Tag,
  Loading,
} from "@carbon/react";
import { Chemistry, Renew, CheckmarkFilled } from "@carbon/react/icons";
import useVirologyLabPermissions from "../../../../hooks/useVirologyLabPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import { NotificationContext } from "../../../layout/Layout";
import {
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import { NotificationKinds } from "../../../../components/common/CustomNotification";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * VirologyLabDNARNAExtractionPage - Page 2: DNA/RNA Extraction
 *
 * Manages the extraction of nucleic acids from samples using VirologyLab design pattern:
 * - Section-based layout (not tabs)
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Records extraction method/kit, lot numbers, operator, date, and notes
 * - Tracks sample progression to QC Assessment (Page 3)
 */
export const VirologyLabDNARNAExtractionPage = ({
  samples = [],
  pageData = {},
  onSampleUpdate,
  onSampleStatusChange,
  isLoading = false,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { getPagePermissionLevel, canSaveData, canAccessDNARNAExtraction } =
    useVirologyLabPermissions();
  const { hasAnyRole } = usePermissions();

  const allowedRoles = [
    "VirologyLab Lab Technician",
    "VirologyLab Manager",
    "VirologyLab PI",
  ];

  const canAccessPage = canAccessDNARNAExtraction() || hasAnyRole(allowedRoles);
  const pagePermissionLevel = getPagePermissionLevel("DNA/RNA Extraction");
  const canProcessSamples = canSaveData(pagePermissionLevel);

  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [isExtractionModalOpen, setIsExtractionModalOpen] = useState(false);

  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        message={intl.formatMessage({
          id: "notebook.virologylab.extraction.accessDenied",
          defaultMessage:
            "You do not have permission to access VirologyLab DNA/RNA Extraction. Required roles: VirologyLab Lab Technician, VirologyLab Manager, or VirologyLab PI.",
        })}
        allowedRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="notebook-page-content virologylab-dna-extraction">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header-section">
            <div className="page-title-row">
              <h4>
                <FormattedMessage
                  id="notebook.virologylab.extraction.title"
                  defaultMessage="VirologyLab DNA/RNA Extraction"
                />
              </h4>
              <div className="page-status-indicators">
                <Tag type="blue" size="sm">
                  <FormattedMessage
                    id="notebook.virologylab.extraction.stage"
                    defaultMessage="Stage 2"
                  />
                </Tag>
                <Tag type="outline" size="sm">
                  {samples.length}{" "}
                  <FormattedMessage
                    id="label.samples"
                    defaultMessage="samples"
                  />
                </Tag>
              </div>
            </div>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="action-section">
        <Column lg={16} md={8} sm={4}>
          <div className="action-buttons">
            <Button
              kind="primary"
              size="sm"
              renderIcon={Chemistry}
              onClick={() => setIsExtractionModalOpen(true)}
              disabled={!canProcessSamples || selectedSampleIds.length === 0}
            >
              <FormattedMessage
                id="notebook.virologylab.extraction.processExtraction"
                defaultMessage="Process Extraction"
              />
            </Button>

            <Button
              kind="ghost"
              size="sm"
              renderIcon={Renew}
              disabled={isLoading}
            >
              <FormattedMessage
                id="label.refresh"
                defaultMessage="Refresh"
              />
            </Button>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth className="samples-grid-section">
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            samples={samples}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            isLoading={isLoading}
            pageContext="virologylab-extraction"
            onSampleUpdate={onSampleUpdate}
          />
        </Column>
      </Grid>
    </div>
  );
};

export default VirologyLabDNARNAExtractionPage;