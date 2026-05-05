import {
  ArrowLeft,
  BatchJob,
  BootVolumeAlt,
  Bullhorn,
  Calendar,
  Catalog,
  CharacterWholeNumber,
  ChartBubble,
  CicsSystemGroup,
  ConnectionSignal,
  ContainerSoftware,
  ListDropdown,
  Microscope,
  Popup,
  QrCode,
  Report,
  ResultNew,
  Search,
  Settings,
  TableOfContents,
  TrashCan,
  User,
} from "@carbon/icons-react";
import { SideNavLink, SideNavMenu, SideNavMenuItem } from "@carbon/react";
import { useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import config from "../../config.json";
import { getFromOpenElisServer } from "../utils/Utils";

const ICONS = {
  menu_admin_reflex_config: Microscope,
  menu_admin_analyzer_test_name: ListDropdown,
  menu_admin_lab_number: CharacterWholeNumber,
  menu_admin_program: ChartBubble,
  menu_admin_provider_menu: CicsSystemGroup,
  menu_admin_barcode_configuration: QrCode,
  menu_admin_plugin_file: BootVolumeAlt,
  menu_admin_organization_management: ContainerSoftware,
  menu_admin_result_reporting_configuration: Report,
  menu_admin_user_management: User,
  menu_admin_batch_test_reassignment: BatchJob,
  menu_admin_test_management_config: ResultNew,
  menu_admin_menu_config_group: TableOfContents,
  menu_admin_form_entry_group: ListDropdown,
  menu_admin_common_properties: Settings,
  menu_admin_test_notification_config: Popup,
  menu_admin_dictionary: CharacterWholeNumber,
  menu_admin_notify_user: Bullhorn,
  menu_admin_search_index_management: Search,
  menu_admin_logging_management: Settings,
  menu_admin_database_cleaning: TrashCan,
  menu_admin_localization_group: TableOfContents,
  menu_admin_external_connections: ConnectionSignal,
  menu_admin_calendar_management: Calendar,
  menu_admin_master_lists: Catalog,
};

const DATA_CY = {
  menu_admin_reflex_config: "reflexTestsConfig",
  menu_admin_reflex: "reflex",
  menu_admin_calculated_value: "calculatedValue",
  menu_admin_lab_number: "labNumberMgmnt",
  menu_admin_program: "programEntry",
  menu_admin_provider_menu: "providerMgmnt",
  menu_admin_barcode_configuration: "barcodeConfig",
  menu_admin_plugin_file: "pluginFile",
  menu_admin_organization_management: "orgMgmnt",
  menu_admin_result_reporting_configuration: "resultReportingConfiguration",
  menu_admin_user_management: "userMgmnt",
  menu_admin_batch_test_reassignment: "batchTestReassignment",
  menu_admin_test_management_config: "testManagementConfigMenu",
  menu_admin_global_menu_management: "globalMenuMgmnt",
  menu_admin_admin_menu_management: "adminMenuMgmnt",
  menu_admin_billing_menu_management: "billingMenuMgmnt",
  menu_admin_nonconformity_menu_management: "nonConformMenuMgmnt",
  menu_admin_patient_menu_management: "patientMenuMgmnt",
  menu_admin_study_menu_management: "studyMenuMgmnt",
  menu_admin_form_entry_nonconformity: "nonConformConfig",
  menu_admin_form_entry_menu_statement: "menuStatementConfig",
  menu_admin_form_entry_workplan: "workPlanConfig",
  menu_admin_form_entry_site_info: "siteInfoMenu",
  menu_admin_form_entry_site_branding: "siteBrandingMenu",
  menu_admin_form_entry_result_config: "resultConfigMenu",
  menu_admin_form_entry_patient_config: "patientConfigMenu",
  menu_admin_form_entry_printed_reports: "printedReportsConfigMenu",
  menu_admin_form_entry_sample_entry: "sampleEntryConfigMenu",
  menu_admin_form_entry_validation: "validationConfigMenu",
  menu_admin_dictionary: "dictMenu",
  menu_admin_notify_user: "notifyUser",
  menu_admin_language_management: "languageManagement",
  menu_admin_translation_management: "translationManagement",
  menu_admin_calendar_management: "calendarMgmnt",
};

const dataCy = (elementId) => DATA_CY[elementId] || elementId;

const normalizePath = (url) => {
  if (!url) return "";
  const pathOnly = url.split(/[?#]/)[0] || "";
  if (pathOnly.length > 1 && pathOnly.endsWith("/")) {
    return pathOnly.slice(0, -1);
  }
  return pathOnly;
};

const isActivePath = (itemPath, currentPath) => {
  if (!itemPath) return false;
  const normalized = normalizePath(itemPath);
  if (currentPath === normalized) return true;
  return normalized.length > 1 && currentPath.startsWith(normalized + "/");
};

const groupContainsActive = (group, currentPath) =>
  (group.childMenus || []).some((child) =>
    isActivePath(child.menu?.actionURL, currentPath),
  );

/**
 * Renders the admin sidenav inline inside the global SideNav (Header.jsx swaps
 * its contents on /admin* and /MasterListsPage* routes for admin users). Tree
 * comes from /rest/admin-menu — clinlims.menu rows where nav_scope='admin'.
 *
 * Top-level rows render as either a plain SideNavLink (when they have an
 * action_url and no children) or a collapsible SideNavMenu (when they have
 * children). Same shape and same component as the lab nav, just a different
 * data source.
 */
export default function AdminContextSideNav() {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const currentPath = normalizePath(location.pathname);
  const [items, setItems] = useState([]);

  useEffect(() => {
    getFromOpenElisServer("/rest/admin-menu", (response) => {
      if (Array.isArray(response)) {
        setItems(response);
      }
    });
  }, []);

  const navigate = (path) => (e) => {
    e.preventDefault();
    e.stopPropagation();
    history.push(path);
  };

  const backToLab = (e) => {
    e.preventDefault();
    e.stopPropagation();
    history.push("/");
  };

  const renderLeaf = (menuItem, opts = {}) => {
    const m = menuItem.menu || {};
    const Icon = opts.renderIcon;
    const isActive = isActivePath(m.actionURL, currentPath);

    if (m.openInNewWindow) {
      return (
        <SideNavLink
          key={m.elementId}
          renderIcon={Icon}
          target="_blank"
          rel="noopener noreferrer"
          href={config.serverBaseUrl + m.actionURL}
          data-cy={dataCy(m.elementId)}
        >
          <FormattedMessage id={m.displayKey} />
        </SideNavLink>
      );
    }

    return (
      <SideNavLink
        key={m.elementId}
        renderIcon={Icon}
        href={m.actionURL}
        onClick={navigate(m.actionURL)}
        isActive={isActive}
        aria-current={isActive ? "page" : undefined}
        data-cy={dataCy(m.elementId)}
      >
        <FormattedMessage id={m.displayKey} />
      </SideNavLink>
    );
  };

  const renderMenuItem = (menuItem) => {
    const m = menuItem.menu || {};
    const isActive = isActivePath(m.actionURL, currentPath);

    if (m.openInNewWindow) {
      return (
        <SideNavMenuItem
          key={m.elementId}
          target="_blank"
          rel="noopener noreferrer"
          href={config.serverBaseUrl + m.actionURL}
          data-cy={dataCy(m.elementId)}
        >
          <FormattedMessage id={m.displayKey} />
        </SideNavMenuItem>
      );
    }

    return (
      <SideNavMenuItem
        key={m.elementId}
        href={m.actionURL}
        onClick={navigate(m.actionURL)}
        isActive={isActive}
        aria-current={isActive ? "page" : undefined}
        data-cy={dataCy(m.elementId)}
      >
        <FormattedMessage id={m.displayKey} />
      </SideNavMenuItem>
    );
  };

  return (
    <>
      <div className="admin-back-to-lab-wrap">
        <SideNavLink
          renderIcon={ArrowLeft}
          href="/"
          onClick={backToLab}
          data-cy="adminBackToLab"
        >
          <FormattedMessage
            id="admin.nav.backToLab"
            defaultMessage="Back to Lab"
          />
        </SideNavLink>
      </div>

      {items.map((item) => {
        const m = item.menu || {};
        // Honor the DB visibility flag — operators toggle is_active via the
        // Admin Menu Configuration page (POST /rest/admin-menu) and expect
        // deactivated rows to disappear from the sidenav. Header.jsx applies
        // the same filter for the lab nav.
        if (!m.isActive) {
          return null;
        }
        const Icon = ICONS[m.elementId];
        const activeChildren = (item.childMenus || []).filter(
          (c) => c.menu?.isActive,
        );
        const hasChildren = activeChildren.length > 0;

        if (!hasChildren) {
          return renderLeaf(item, { renderIcon: Icon });
        }

        const expanded = groupContainsActive(
          { ...item, childMenus: activeChildren },
          currentPath,
        );
        const title = intl.formatMessage({ id: m.displayKey });
        return (
          <SideNavMenu
            key={`${m.elementId}-${expanded ? "open" : "closed"}`}
            title={title}
            renderIcon={Icon}
            defaultExpanded={expanded}
            data-cy={dataCy(m.elementId)}
          >
            {activeChildren.map((child) => renderMenuItem(child))}
          </SideNavMenu>
        );
      })}
    </>
  );
}
