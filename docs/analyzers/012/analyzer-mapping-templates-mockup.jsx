import { useState, useCallback, useRef, useEffect } from "react";

// ═══════════════════════════════════════════════════════════════════
// i18n SIMULATION — all user-facing strings use localization tags
// ═══════════════════════════════════════════════════════════════════
const i18n = {
  "app.name": "OpenELIS Global", "app.version": "Version: 3.2.1.2",
  "nav.home": "Home", "nav.genericSample": "Generic Sample", "nav.order": "Order",
  "nav.patient": "Patient", "nav.storage": "Storage", "nav.analyzers": "Analyzers",
  "nav.analyzers.list": "Analyzers List", "nav.analyzers.errorDashboard": "Error Dashboard",
  "nav.analyzers.profileLibrary": "Profile Library",
  "nav.nonConform": "Non-Conform", "nav.workplan": "Workplan", "nav.pathology": "Pathology",
  "nav.immunohistochemistry": "Immunohistochemistry", "nav.cytology": "Cytology",
  "nav.results": "Results", "nav.validation": "Validation", "nav.reports": "Reports",
  "nav.admin": "Admin", "nav.billing": "Billing", "nav.aliquot": "Aliquot",
  "nav.notebook": "NoteBook", "nav.inventory": "Inventory", "nav.help": "Help",
  // Analyzer List
  "analyzer.list.title": "Analyzer List",
  "analyzer.list.subtitle": "Manage laboratory analyzers and field mappings",
  "analyzer.list.addAnalyzer": "Add Analyzer",
  "analyzer.list.totalAnalyzers": "Total Analyzers",
  "analyzer.list.active": "Active", "analyzer.list.inactive": "Inactive",
  "analyzer.list.pluginWarnings": "Plugin Warnings",
  "analyzer.list.search": "Search analyzers...",
  "analyzer.list.status": "Status", "analyzer.list.allStatuses": "All Statuses",
  "analyzer.list.col.name": "Name", "analyzer.list.col.profile": "Profile",
  "analyzer.list.col.labUnits": "Lab Units", "analyzer.list.col.connection": "Connection",
  "analyzer.list.col.testCodes": "Test Codes", "analyzer.list.col.status": "Status",
  "analyzer.list.col.lastModified": "Last Modified", "analyzer.list.col.actions": "Actions",
  // Overflow
  "analyzer.action.fieldMappings": "Field Mappings",
  "analyzer.action.testConnection": "Test Connection",
  "analyzer.action.copyMappings": "Copy Mappings",
  "analyzer.action.exportProfile": "Export Profile",
  "analyzer.action.edit": "Edit", "analyzer.action.delete": "Delete",
  // Modal
  "analyzer.modal.addTitle": "Add New Analyzer",
  "analyzer.modal.editTitle": "Edit Analyzer",
  "analyzer.modal.name": "Analyzer Name",
  "analyzer.modal.namePlaceholder": "e.g., Hematology Analyzer 1",
  "analyzer.modal.status": "Status",
  "analyzer.modal.profile": "Analyzer Profile",
  "analyzer.modal.profile.none": "None (Start from Scratch)",
  "analyzer.modal.profile.builtIn": "BUILT-IN",
  "analyzer.modal.profile.siteLibrary": "SITE LIBRARY",
  "analyzer.modal.profile.importFile": "Import from File...",
  "analyzer.modal.profile.search": "Search profiles...",
  "analyzer.modal.profile.info": "Profile Info",
  "analyzer.modal.profile.applied": "Profile will be applied on save",
  "analyzer.modal.labUnits": "Lab Units",
  "analyzer.modal.labUnits.placeholder": "Select lab units...",
  "analyzer.modal.labUnits.help": "Lab units this analyzer serves. Configured in Admin → Lab Units.",
  "analyzer.modal.pluginType": "Plugin Type",
  "analyzer.modal.protocolVersion": "Protocol Version",
  "analyzer.modal.connectionRole": "Connection Role",
  "analyzer.modal.serverMode": "Server (OpenELIS Listens)",
  "analyzer.modal.clientMode": "Client (OpenELIS Connects)",
  "analyzer.modal.listenPort": "Listen Port",
  "analyzer.modal.ipAddress": "IP Address",
  "analyzer.modal.portNumber": "Port Number",
  "analyzer.modal.connectionTimeout": "Connection Timeout (s)",
  "analyzer.modal.retryCount": "NAK Retry Count",
  "analyzer.modal.testConnection": "Test Connection",
  "analyzer.modal.cancel": "Cancel", "analyzer.modal.save": "Save",
  // Export
  "export.title": "Export Analyzer Profile",
  "export.profileName": "Profile Name",
  "export.description": "Description",
  "export.author": "Author",
  "export.tags": "Tags (comma-separated)",
  "export.download": "Download Profile",
  // Lab Unit filter
  "analyzer.list.labUnitFilter": "Lab Unit",
  "analyzer.list.allLabUnits": "All Lab Units",
  // Field Mappings (carried from v2)
  "mapping.title": "Field Mappings",
  "mapping.subtitle": "Configure field mappings between analyzer and OpenELIS",
  "mapping.warning.required": "Required mappings are missing",
  "mapping.stat.totalMappings": "Total Mappings",
  "mapping.stat.requiredMappings": "Required Mappings",
  "mapping.stat.unmappedFields": "Unmapped Fields",
  "mapping.btn.queryAnalyzer": "Query Analyzer",
  "mapping.btn.testMapping": "Test Mapping",
  "mapping.btn.saveMappings": "Save Mappings",
  "mapping.tab.testCodes": "Test Codes", "mapping.tab.qcRules": "QC Rules",
  "mapping.tab.simulator": "Message Simulator",
  "mapping.tab.fieldExtraction": "Field Extraction",
  "mapping.tab.advanced": "Advanced",
  "mapping.testCodes.title": "Analyzer Fields (All)",
  "mapping.testCodes.search": "Search...",
  "mapping.testCodes.addMapping": "Add Mapping",
  "mapping.testCodes.bulkAdd": "Bulk Add",
  "mapping.testCodes.importCsv": "Import CSV",
  "mapping.testCodes.autoMatch": "Auto-Match",
  "mapping.testCodes.createNewTest": "Create New Test",
  "mapping.summary.title": "Mappings Summary",
  "mapping.summary.empty": "Select a field from the left panel to view or create mappings.",
  "mapping.summary.analyzerCode": "Analyzer Code",
  "mapping.summary.openelisTest": "OpenELIS Test",
  "mapping.summary.transform": "Transformation",
  "mapping.summary.analyzerUnit": "Analyzer Unit",
  "mapping.summary.openelisUnit": "OpenELIS Unit",
  "transform.passThrough": "Pass-through", "transform.greaterLessFlag": "Greater/Less Flag",
  "transform.valueMap": "Value Map", "transform.thresholdClassify": "Threshold Classify",
  "transform.codedLookup": "Coded Lookup",
  "qc.title": "QC Sample Identification",
  "qc.description": "A sample is identified as QC if ANY of these rules match:",
  "qc.addRule": "Add QC Rule", "qc.testRules": "Test QC Rules",
  "simulator.title": "Message Simulator", "simulator.clear": "Clear",
  "simulator.parse": "Parse",
  "simulator.empty": "Paste an ASTM message above and click Parse to validate your configuration.",
  "delete.title": "Delete Analyzer", "delete.cancel": "Cancel", "delete.delete": "Delete",
  "delete.confirm": "Are you sure you want to delete {name}? This action cannot be undone.",
};
const t = (key) => i18n[key] || key;

// ═══════════════════════════════════════════════════════════════════
// DESIGN TOKENS
// ═══════════════════════════════════════════════════════════════════
const tk = {
  navBg: "#1a2a3a", navText: "#ccd6e0", navTextActive: "#ffffff",
  navHoverBg: "#243444", navActiveBg: "#0f62fe22", headerBg: "#0d1b2a",
  blue: "#0f62fe", blueHover: "#0043ce", blueLight: "#d0e2ff", blueBg: "#edf5ff",
  green: "#198038", greenBg: "#defbe6", red: "#da1e28", redBg: "#fff1f1",
  yellow: "#f1c21b", yellowBg: "#fcf4d6",
  g900: "#161616", g800: "#262626", g700: "#393939", g600: "#525252",
  g500: "#6f6f6f", g400: "#8d8d8d", g300: "#a8a8a8", g200: "#c6c6c6",
  g100: "#e0e0e0", g50: "#f4f4f4", g10: "#fafafa", white: "#ffffff",
  font: "'IBM Plex Sans', -apple-system, BlinkMacSystemFont, sans-serif",
  mono: "'IBM Plex Mono', 'Menlo', monospace",
};

// ═══════════════════════════════════════════════════════════════════
// MOCK DATA
// ═══════════════════════════════════════════════════════════════════
const PROFILES = [
  { id: "indiko-plus-chemistry-v1", name: "Thermo Fisher Indiko Plus — Chemistry Panel", manufacturer: "Thermo Fisher", model: "Indiko Plus", protocol: "ASTM LIS2-A2", version: "1.0.0", author: "OpenELIS Community", testCount: 48, qcRules: 3, source: "BUILT_IN", description: "Standard chemistry profile for Indiko Plus with 48 test codes. Based on LIS Interface Manual v3.2.", defaultPort: 5000, connectionRole: "SERVER" },
  { id: "maldi-biotyper-astm-v1", name: "Bruker MALDI Biotyper sirius — Organism ID", manufacturer: "Bruker", model: "MALDI Biotyper sirius", protocol: "ASTM LIS2-A2", version: "1.0.0", author: "OpenELIS Community", testCount: 2, qcRules: 1, source: "BUILT_IN", description: "MALDI-TOF organism identification with confidence scoring and threshold classification.", defaultPort: 5001, connectionRole: "SERVER" },
  { id: "vidas-immunoassay-v1", name: "bioMérieux VIDAS — Immunoassay", manufacturer: "bioMérieux", model: "VIDAS", protocol: "ASTM LIS2-A2", version: "1.0.0", author: "OpenELIS Community", testCount: 12, qcRules: 2, source: "BUILT_IN", description: "Immunoassay panel for VIDAS platform.", defaultPort: 5002, connectionRole: "SERVER" },
  { id: "stago-st4-coag-v1", name: "Stago ST4 — Coagulation", manufacturer: "Stago", model: "ST4/STA-R", protocol: "ASTM LIS2-A2", version: "1.0.0", author: "OpenELIS Community", testCount: 8, qcRules: 2, source: "BUILT_IN", description: "Coagulation panel for Stago ST4 and STA-R series.", defaultPort: 5003, connectionRole: "SERVER" },
  { id: "sysmex-xn-hema-v1", name: "Sysmex XN Series — Hematology", manufacturer: "Sysmex", model: "XN Series", protocol: "ASTM LIS2-A2", version: "1.0.0", author: "OpenELIS Community", testCount: 30, qcRules: 2, source: "BUILT_IN", description: "Complete CBC with differential for Sysmex XN-1000/2000/3000/9000.", defaultPort: 5004, connectionRole: "SERVER" },
  { id: "mindray-bs-chem-v1", name: "Mindray BS-200/300/400 — Chemistry", manufacturer: "Mindray", model: "BS-200/300/400", protocol: "HL7 v2.3.1", version: "1.0.0", author: "OpenELIS Community", testCount: 40, qcRules: 2, source: "BUILT_IN", description: "Chemistry panel for Mindray BS-series using HL7 over MLLP.", defaultPort: 9100, connectionRole: "SERVER" },
  { id: "mindray-bc5380-hema-v1", name: "Mindray BC-5380 — Hematology", manufacturer: "Mindray", model: "BC-5380", protocol: "HL7 v2.3.1", version: "1.0.0", author: "OpenELIS Community", testCount: 26, qcRules: 1, source: "BUILT_IN", description: "Hematology panel for Mindray BC-5380 using HL7 over MLLP.", defaultPort: 9101, connectionRole: "SERVER" },
  { id: "custom-indiko-modified", name: "Custom Indiko (modified QC rules)", manufacturer: "Thermo Fisher", model: "Indiko Plus", protocol: "ASTM LIS2-A2", version: "1.1.0", author: "Lab Admin", testCount: 48, qcRules: 5, source: "SITE_LIBRARY", description: "Modified Indiko profile with additional QC prefix rules for site-specific QC naming.", defaultPort: 5000, connectionRole: "SERVER" },
];

const LAB_UNITS = [
  { id: 1, name: "Hematology" }, { id: 2, name: "Clinical Chemistry" },
  { id: 3, name: "Microbiology" }, { id: 4, name: "Immunology / Serology" },
  { id: 5, name: "Parasitology" }, { id: 6, name: "Molecular Biology" },
  { id: 7, name: "Blood Bank" }, { id: 8, name: "Urinalysis" },
  { id: 9, name: "Coagulation" }, { id: 10, name: "TB Laboratory" },
];

const ANALYZERS = [
  { id: "a1", name: "Sysmex XN-1000", profileId: "sysmex-xn-hema-v1", labUnits: [1], connection: "Server :5004", testCodes: 30, status: "Active", lastModified: "2026-02-20" },
  { id: "a2", name: "Indiko Plus #1", profileId: "indiko-plus-chemistry-v1", labUnits: [2], connection: "Server :5000", testCodes: 48, status: "Active", lastModified: "2026-02-18" },
  { id: "a3", name: "MALDI Biotyper", profileId: "maldi-biotyper-astm-v1", labUnits: [3], connection: "Server :5001", testCodes: 2, status: "Setup", lastModified: "2026-02-15" },
  { id: "a4", name: "Mindray BS-380", profileId: "mindray-bs-chem-v1", labUnits: [2, 4], connection: "Server :9100", testCodes: 40, status: "Active", lastModified: "2026-02-14" },
  { id: "a5", name: "Mindray BC-5380", profileId: "mindray-bc5380-hema-v1", labUnits: [1], connection: "Server :9101", testCodes: 26, status: "Setup", lastModified: "2026-02-12" },
  { id: "a6", name: "VIDAS Unit 1", profileId: "vidas-immunoassay-v1", labUnits: [4], connection: "Server :5002", testCodes: 12, status: "Inactive", lastModified: "2026-01-30" },
  { id: "a7", name: "Stago ST4", profileId: "stago-st4-coag-v1", labUnits: [9], connection: "Server :5003", testCodes: 8, status: "Setup", lastModified: "2026-02-22" },
  { id: "a8", name: "Custom Analyzer", profileId: null, labUnits: [], connection: "-", testCodes: 0, status: "Setup", lastModified: "-" },
];

const OE_TESTS = [
  { id: "t01", name: "Glucose", code: "GLU", resultType: "NUMERIC", unit: "mg/dL" },
  { id: "t02", name: "Blood Urea Nitrogen", code: "BUN", resultType: "NUMERIC", unit: "mg/dL" },
  { id: "t03", name: "Creatinine", code: "CREA", resultType: "NUMERIC", unit: "mg/dL" },
  { id: "t04", name: "ALT (SGPT)", code: "ALT", resultType: "NUMERIC", unit: "U/L" },
  { id: "t05", name: "AST (SGOT)", code: "AST", resultType: "NUMERIC", unit: "U/L" },
  { id: "t06", name: "Sodium", code: "Na", resultType: "NUMERIC", unit: "mmol/L" },
  { id: "t07", name: "Potassium", code: "K", resultType: "NUMERIC", unit: "mmol/L" },
  { id: "t08", name: "Total Bilirubin", code: "TBIL", resultType: "NUMERIC", unit: "mg/dL" },
  { id: "t09", name: "WBC", code: "WBC", resultType: "NUMERIC", unit: "10^3/uL" },
  { id: "t10", name: "Hemoglobin", code: "HGB", resultType: "NUMERIC", unit: "g/dL" },
];

const INIT_MAPPINGS = [
  { id: "f1", analyzerCode: "GLU", displayName: "Glucose", astmRef: "R.3", type: "NUMERIC", unit: "mg/dL", oeTestId: "t01", oeTestName: "Glucose", oeUnit: "mg/dL", transform: "PASSTHROUGH", status: "mapped" },
  { id: "f2", analyzerCode: "BUN", displayName: "Blood Urea Nitrogen", astmRef: "R.3", type: "NUMERIC", unit: "mg/dL", oeTestId: "t02", oeTestName: "Blood Urea Nitrogen", oeUnit: "mg/dL", transform: "PASSTHROUGH", status: "mapped" },
  { id: "f3", analyzerCode: "CREA", displayName: "Creatinine", astmRef: "R.3", type: "NUMERIC", unit: "mg/dL", oeTestId: "t03", oeTestName: "Creatinine", oeUnit: "mg/dL", transform: "PASSTHROUGH", status: "mapped" },
  { id: "f4", analyzerCode: "ALT", displayName: "ALT", astmRef: "R.3", type: "NUMERIC", unit: "U/L", oeTestId: "t04", oeTestName: "ALT (SGPT)", oeUnit: "U/L", transform: "PASSTHROUGH", status: "mapped" },
  { id: "f5", analyzerCode: "CHOL", displayName: "Cholesterol", astmRef: "R.3", type: "NUMERIC", unit: "mg/dL", oeTestId: null, oeTestName: "", oeUnit: "", transform: "PASSTHROUGH", status: "unmapped" },
  { id: "f6", analyzerCode: "TBIL", displayName: "Total Bilirubin", astmRef: "R.3", type: "NUMERIC", unit: "mg/dL", oeTestId: "t08", oeTestName: "Total Bilirubin", oeUnit: "mg/dL", transform: "GREATER_LESS_FLAG", status: "mapped" },
];

// ═══════════════════════════════════════════════════════════════════
// REUSABLE COMPONENTS
// ═══════════════════════════════════════════════════════════════════
const OverflowMenu = ({ items, onSelect }) => {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useEffect(() => { const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); }; document.addEventListener("mousedown", h); return () => document.removeEventListener("mousedown", h); }, []);
  return (
    <div ref={ref} style={{ position: "relative", display: "inline-block" }}>
      <button style={{ background: "none", border: "none", cursor: "pointer", padding: "4px 8px", fontSize: 18, color: tk.g600, lineHeight: 1 }} onClick={() => setOpen(!open)}>⋮</button>
      {open && (
        <div style={{ position: "absolute", right: 0, top: "100%", zIndex: 100, background: tk.navBg, minWidth: 180, boxShadow: "0 8px 24px rgba(0,0,0,.25)" }}>
          {items.map((item, i) => (
            <div key={i} style={{ padding: "10px 16px", cursor: "pointer", fontSize: 13, color: item.danger ? "#ff8389" : tk.white, borderBottom: `1px solid rgba(255,255,255,.06)` }}
              onMouseEnter={(e) => { if (!item.danger) e.currentTarget.style.background = tk.blue; }}
              onMouseLeave={(e) => e.currentTarget.style.background = "transparent"}
              onClick={() => { onSelect(item.key); setOpen(false); }}>{item.label}</div>
          ))}
        </div>
      )}
    </div>
  );
};

const Tag = ({ children, onRemove, color = tk.blue }) => (
  <span style={{ display: "inline-flex", alignItems: "center", gap: 4, padding: "2px 8px", fontSize: 11, fontWeight: 500, background: `${color}18`, color: color, marginRight: 4, marginBottom: 2 }}>
    {children}
    {onRemove && <span style={{ cursor: "pointer", fontSize: 13, lineHeight: 1, opacity: 0.7 }} onClick={onRemove}>×</span>}
  </span>
);

const TestComboBox = ({ value, onChange, onCreateNew }) => {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState(value ? (OE_TESTS.find(t => t.id === value)?.name || "") : "");
  const ref = useRef(null);
  const filtered = OE_TESTS.filter(t => !search || t.name.toLowerCase().includes(search.toLowerCase()) || t.code.toLowerCase().includes(search.toLowerCase()));
  useEffect(() => { const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); }; document.addEventListener("mousedown", h); return () => document.removeEventListener("mousedown", h); }, []);
  useEffect(() => { if (value) { const test = OE_TESTS.find(t => t.id === value); if (test) setSearch(test.name); } }, [value]);
  return (
    <div ref={ref} style={{ position: "relative" }}>
      <input style={{ width: "100%", padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}`, background: tk.white, color: tk.g900, boxSizing: "border-box" }} placeholder="Search tests..." value={search}
        onChange={(e) => { setSearch(e.target.value); if (value) onChange(null); setOpen(true); }} onFocus={() => setOpen(true)} />
      {open && (
        <div style={{ position: "absolute", top: "100%", left: 0, right: 0, zIndex: 50, background: tk.white, border: `1px solid ${tk.g200}`, boxShadow: "0 4px 16px rgba(0,0,0,.1)", maxHeight: 240, overflowY: "auto" }}>
          {filtered.slice(0, 8).map(test => (
            <div key={test.id} style={{ padding: "8px 12px", cursor: "pointer", fontSize: 13, borderBottom: `1px solid ${tk.g50}` }}
              onMouseEnter={(e) => e.currentTarget.style.background = tk.blueBg}
              onMouseLeave={(e) => e.currentTarget.style.background = "transparent"}
              onClick={() => { onChange(test.id); setSearch(test.name); setOpen(false); }}>
              <div style={{ fontWeight: 500 }}>{test.name}</div>
              <div style={{ fontSize: 11, color: tk.g500 }}>{test.code} · {test.resultType} · {test.unit}</div>
            </div>
          ))}
          <div style={{ padding: "8px 12px", cursor: "pointer", fontSize: 13, color: tk.blue, fontWeight: 600, borderTop: `1px solid ${tk.g100}`, background: tk.g50 }}
            onClick={() => { onCreateNew(search); setOpen(false); }}>+ {t("mapping.testCodes.createNewTest")}</div>
        </div>
      )}
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════
// PROFILE SELECTOR COMBOBOX (FR-24)
// ═══════════════════════════════════════════════════════════════════
const ProfileSelector = ({ value, onChange, onImportFile }) => {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const ref = useRef(null);
  useEffect(() => { const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); }; document.addEventListener("mousedown", h); return () => document.removeEventListener("mousedown", h); }, []);

  const builtIn = PROFILES.filter(p => p.source === "BUILT_IN" && (!search || p.name.toLowerCase().includes(search.toLowerCase())));
  const siteLib = PROFILES.filter(p => p.source === "SITE_LIBRARY" && (!search || p.name.toLowerCase().includes(search.toLowerCase())));
  const selected = value ? PROFILES.find(p => p.id === value) : null;

  return (
    <div ref={ref} style={{ position: "relative" }}>
      <div style={{ display: "flex", alignItems: "center", border: `1px solid ${tk.g200}`, background: tk.white, cursor: "pointer" }} onClick={() => setOpen(!open)}>
        <div style={{ flex: 1, padding: "10px 12px", fontSize: 14, color: selected ? tk.g900 : tk.g400, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
          {selected ? selected.name : t("analyzer.modal.profile.none")}
        </div>
        <div style={{ padding: "10px 12px", color: tk.g400, fontSize: 10 }}>▼</div>
      </div>
      {open && (
        <div style={{ position: "absolute", top: "100%", left: 0, right: 0, zIndex: 60, background: tk.white, border: `1px solid ${tk.g200}`, boxShadow: "0 8px 24px rgba(0,0,0,.12)", maxHeight: 400, overflowY: "auto" }}>
          <div style={{ padding: 8, borderBottom: `1px solid ${tk.g100}` }}>
            <input style={{ width: "100%", padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }}
              placeholder={t("analyzer.modal.profile.search")} value={search} onChange={(e) => setSearch(e.target.value)} autoFocus />
          </div>
          {/* None */}
          <div style={{ padding: "10px 16px", cursor: "pointer", fontSize: 13, fontWeight: !value ? 600 : 400, background: !value ? tk.blueBg : "transparent", color: !value ? tk.blue : tk.g900 }}
            onMouseEnter={(e) => { if (value) e.currentTarget.style.background = tk.g50; }}
            onMouseLeave={(e) => { if (value) e.currentTarget.style.background = "transparent"; }}
            onClick={() => { onChange(null); setOpen(false); setSearch(""); }}>
            {t("analyzer.modal.profile.none")}
          </div>
          {/* Built-in */}
          {builtIn.length > 0 && (
            <>
              <div style={{ padding: "6px 16px", fontSize: 11, fontWeight: 700, color: tk.g500, background: tk.g50, textTransform: "uppercase", letterSpacing: ".5px" }}>{t("analyzer.modal.profile.builtIn")}</div>
              {builtIn.map(p => (
                <div key={p.id} style={{ padding: "10px 16px", cursor: "pointer", fontSize: 13, background: value === p.id ? tk.blueBg : "transparent", borderBottom: `1px solid ${tk.g50}` }}
                  onMouseEnter={(e) => { if (value !== p.id) e.currentTarget.style.background = tk.g10; }}
                  onMouseLeave={(e) => { if (value !== p.id) e.currentTarget.style.background = "transparent"; }}
                  onClick={() => { onChange(p.id); setOpen(false); setSearch(""); }}>
                  <div style={{ fontWeight: 500, color: value === p.id ? tk.blue : tk.g900 }}>{p.name}</div>
                  <div style={{ fontSize: 11, color: tk.g500, marginTop: 2 }}>{p.protocol} · {p.testCount} tests · v{p.version}</div>
                </div>
              ))}
            </>
          )}
          {/* Site Library */}
          {siteLib.length > 0 && (
            <>
              <div style={{ padding: "6px 16px", fontSize: 11, fontWeight: 700, color: tk.g500, background: tk.g50, textTransform: "uppercase", letterSpacing: ".5px" }}>{t("analyzer.modal.profile.siteLibrary")}</div>
              {siteLib.map(p => (
                <div key={p.id} style={{ padding: "10px 16px", cursor: "pointer", fontSize: 13, background: value === p.id ? tk.blueBg : "transparent", borderBottom: `1px solid ${tk.g50}` }}
                  onMouseEnter={(e) => { if (value !== p.id) e.currentTarget.style.background = tk.g10; }}
                  onMouseLeave={(e) => { if (value !== p.id) e.currentTarget.style.background = "transparent"; }}
                  onClick={() => { onChange(p.id); setOpen(false); setSearch(""); }}>
                  <div style={{ fontWeight: 500, color: value === p.id ? tk.blue : tk.g900 }}>{p.name}</div>
                  <div style={{ fontSize: 11, color: tk.g500, marginTop: 2 }}>{p.protocol} · {p.testCount} tests · v{p.version}</div>
                </div>
              ))}
            </>
          )}
          {/* Import */}
          <div style={{ padding: "10px 16px", cursor: "pointer", fontSize: 13, color: tk.blue, fontWeight: 600, borderTop: `1px solid ${tk.g100}`, background: tk.g50, display: "flex", alignItems: "center", gap: 6 }}
            onClick={() => { onImportFile(); setOpen(false); }}>
            📁 {t("analyzer.modal.profile.importFile")}
          </div>
        </div>
      )}
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════
// LAB UNIT MULTI-SELECT (FR-25)
// ═══════════════════════════════════════════════════════════════════
const LabUnitMultiSelect = ({ selected, onChange }) => {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const ref = useRef(null);
  useEffect(() => { const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); }; document.addEventListener("mousedown", h); return () => document.removeEventListener("mousedown", h); }, []);
  const filtered = LAB_UNITS.filter(u => !search || u.name.toLowerCase().includes(search.toLowerCase()));
  const toggle = (id) => onChange(selected.includes(id) ? selected.filter(x => x !== id) : [...selected, id]);
  return (
    <div ref={ref} style={{ position: "relative" }}>
      <div style={{ minHeight: 42, border: `1px solid ${tk.g200}`, background: tk.white, padding: "6px 8px", cursor: "pointer", display: "flex", flexWrap: "wrap", alignItems: "center", gap: 2 }} onClick={() => setOpen(!open)}>
        {selected.length === 0 && <span style={{ color: tk.g400, fontSize: 14, padding: "2px 4px" }}>{t("analyzer.modal.labUnits.placeholder")}</span>}
        {selected.map(id => {
          const u = LAB_UNITS.find(x => x.id === id);
          return u ? <Tag key={id} onRemove={(e) => { e.stopPropagation(); toggle(id); }}>{u.name}</Tag> : null;
        })}
      </div>
      {open && (
        <div style={{ position: "absolute", top: "100%", left: 0, right: 0, zIndex: 55, background: tk.white, border: `1px solid ${tk.g200}`, boxShadow: "0 4px 16px rgba(0,0,0,.1)", maxHeight: 260, overflowY: "auto" }}>
          <div style={{ padding: 8, borderBottom: `1px solid ${tk.g100}` }}>
            <input style={{ width: "100%", padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder="Filter lab units..." value={search} onChange={(e) => setSearch(e.target.value)} autoFocus />
          </div>
          {filtered.map(u => (
            <div key={u.id} style={{ padding: "8px 16px", cursor: "pointer", fontSize: 13, display: "flex", alignItems: "center", gap: 8, background: selected.includes(u.id) ? tk.blueBg : "transparent" }}
              onMouseEnter={(e) => { if (!selected.includes(u.id)) e.currentTarget.style.background = tk.g10; }}
              onMouseLeave={(e) => { if (!selected.includes(u.id)) e.currentTarget.style.background = selected.includes(u.id) ? tk.blueBg : "transparent"; }}
              onClick={() => toggle(u.id)}>
              <div style={{ width: 16, height: 16, border: `2px solid ${selected.includes(u.id) ? tk.blue : tk.g300}`, background: selected.includes(u.id) ? tk.blue : "transparent", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 10, color: tk.white, flexShrink: 0 }}>{selected.includes(u.id) ? "✓" : ""}</div>
              {u.name}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════
// MAIN APPLICATION
// ═══════════════════════════════════════════════════════════════════
export default function AnalyzerMappingApp() {
  const [view, setView] = useState("list");
  const [selectedAnalyzer, setSelectedAnalyzer] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(null);
  const [showExportModal, setShowExportModal] = useState(null);
  const [toast, setToast] = useState(null);
  const [expandedNav, setExpandedNav] = useState("analyzers");

  // Modal form state
  const [modalProfile, setModalProfile] = useState(null);
  const [modalLabUnits, setModalLabUnits] = useState([]);
  const [connectionRole, setConnectionRole] = useState("server");

  // Mappings page state
  const [activeTab, setActiveTab] = useState("testcodes");
  const [selectedField, setSelectedField] = useState(null);
  const [mappings, setMappings] = useState(INIT_MAPPINGS);
  const [showBulkAdd, setShowBulkAdd] = useState(false);
  const [bulkText, setBulkText] = useState("");
  const [showInlineCreate, setShowInlineCreate] = useState(null);
  const [newTestForm, setNewTestForm] = useState({ name: "", code: "", resultType: "NUMERIC", unit: "", sampleType: "Serum" });
  const [qcRules, setQcRules] = useState([{ id: "qc1", type: "specimenIdPrefix", field: "O.3", value: "QC-" }, { id: "qc2", type: "fieldEquals", field: "O.16", value: "QC" }]);
  const [simMessage, setSimMessage] = useState("");
  const [simResult, setSimResult] = useState(null);

  // List filters
  const [listLabUnitFilter, setListLabUnitFilter] = useState(null);

  const showToastMsg = useCallback((msg) => { setToast(msg); setTimeout(() => setToast(null), 3000); }, []);
  const openMappings = (a) => { setSelectedAnalyzer(a); setView("mappings"); setActiveTab("testcodes"); setSelectedField(null); };

  const openAddModal = (editAnalyzer) => {
    if (editAnalyzer && typeof editAnalyzer === "object") {
      setModalProfile(editAnalyzer.profileId);
      setModalLabUnits(editAnalyzer.labUnits || []);
    } else { setModalProfile(null); setModalLabUnits([]); }
    setShowAddModal(editAnalyzer || true);
  };

  // Profile selection effects
  const selectedProfile = modalProfile ? PROFILES.find(p => p.id === modalProfile) : null;
  const onProfileChange = (profileId) => {
    setModalProfile(profileId);
    if (profileId) {
      const p = PROFILES.find(x => x.id === profileId);
      if (p) setConnectionRole(p.connectionRole === "SERVER" ? "server" : "client");
    }
  };

  const addMapping = () => setMappings(prev => [...prev, { id: `f${Date.now()}`, analyzerCode: "", displayName: "", astmRef: "R.3", type: "NUMERIC", unit: "", oeTestId: null, oeTestName: "", oeUnit: "", transform: "PASSTHROUGH", status: "draft" }]);
  const updateMapping = (id, field, value) => setMappings(prev => prev.map(m => { if (m.id !== id) return m; const u = { ...m, [field]: value }; if (field === "oeTestId" && value) { const test = OE_TESTS.find(t => t.id === value); if (test) { u.oeTestName = test.name; u.oeUnit = test.unit; u.status = "mapped"; } } return u; }));
  const deleteMapping = (id) => setMappings(prev => prev.filter(m => m.id !== id));
  const doBulkAdd = () => { const codes = bulkText.split("\n").map(l => l.trim()).filter(Boolean); const existing = new Set(mappings.map(m => m.analyzerCode)); const newRows = [...new Set(codes)].filter(c => !existing.has(c)).map(code => ({ id: `f${Date.now()}_${code}`, analyzerCode: code, displayName: code, astmRef: "R.3", type: "NUMERIC", unit: "", oeTestId: null, oeTestName: "", oeUnit: "", transform: "PASSTHROUGH", status: "unmapped" })); setMappings(prev => [...prev, ...newRows]); setShowBulkAdd(false); setBulkText(""); showToastMsg(`Added ${newRows.length} test codes`); };
  const doCreateTest = () => { const newTest = { id: `t_new_${Date.now()}`, name: newTestForm.name, code: newTestForm.code, resultType: newTestForm.resultType, unit: newTestForm.unit }; OE_TESTS.push(newTest); updateMapping(showInlineCreate, "oeTestId", newTest.id); setShowInlineCreate(null); showToastMsg(`Test "${newTest.name}" created and mapped`); };
  const runSimulator = () => { if (!simMessage.trim()) return; const lines = simMessage.split("\n"); const header = lines.find(l => l.startsWith("H|")); const results = lines.filter(l => l.startsWith("R|")); const order = lines.find(l => l.startsWith("O|")); const instrument = header ? header.split("|")[4]?.split("^")[0] || "Unknown" : "Unknown"; const specimen = order ? order.split("|")[2] || "Unknown" : "Unknown"; const parsed = results.map(r => { const f = r.split("|"); const testId = f[2]?.replace(/\^/g, "") || "?"; const val = f[3] || ""; const units = f[4] || ""; const flag = f[6] || ""; const match = mappings.find(m => m.analyzerCode === testId); return { testId, value: val, units, flag, oeName: match?.oeTestName || "UNMAPPED", status: match ? "matched" : "unmatched" }; }); setSimResult({ instrument, specimen, results: parsed, warnings: parsed.filter(p => p.status === "unmatched").length }); };

  const mappedCount = mappings.filter(m => m.status === "mapped").length;
  const unmappedCount = mappings.filter(m => m.status !== "mapped").length;
  const filteredAnalyzers = listLabUnitFilter ? ANALYZERS.filter(a => a.labUnits.includes(listLabUnitFilter)) : ANALYZERS;

  // ═══════════════════════════════════════════════════════════════
  // SIDEBAR
  // ═══════════════════════════════════════════════════════════════
  const NAV = [
    { key: "home", label: t("nav.home") },
    { key: "order", label: t("nav.order"), exp: true },
    { key: "patient", label: t("nav.patient"), exp: true },
    { key: "analyzers", label: t("nav.analyzers"), exp: true, children: [
      { key: "analyzersList", label: t("nav.analyzers.list") },
      { key: "profileLibrary", label: t("nav.analyzers.profileLibrary") },
      { key: "errorDashboard", label: t("nav.analyzers.errorDashboard") },
    ]},
    { key: "results", label: t("nav.results"), exp: true },
    { key: "validation", label: t("nav.validation"), exp: true },
    { key: "reports", label: t("nav.reports"), exp: true },
    { key: "admin", label: t("nav.admin") },
  ];

  const Sidebar = () => (
    <div style={{ width: 224, background: tk.navBg, color: tk.navText, overflowY: "auto", flexShrink: 0, display: "flex", flexDirection: "column" }}>
      <div style={{ padding: "12px 16px", display: "flex", alignItems: "center", gap: 10, background: tk.headerBg, borderBottom: "1px solid rgba(255,255,255,.06)" }}>
        <div style={{ width: 36, height: 36, borderRadius: 6, background: "#2a5a8a", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11, color: tk.white, fontWeight: 700 }}>OE</div>
        <div><div style={{ fontSize: 14, fontWeight: 600, color: tk.white }}>Test LIMS</div><div style={{ fontSize: 11, color: tk.g400 }}>{t("app.version")}</div></div>
      </div>
      <div style={{ flex: 1, paddingTop: 8 }}>
        {NAV.map(item => (
          <div key={item.key}>
            <div style={{ padding: "8px 12px 8px 16px", fontSize: 13, color: item.key === "analyzers" ? tk.navTextActive : tk.navText, background: item.key === "analyzers" ? tk.navActiveBg : "transparent", cursor: "pointer", display: "flex", justifyContent: "space-between", alignItems: "center", fontWeight: item.key === "analyzers" ? 600 : 400, borderLeft: item.key === "analyzers" ? `3px solid ${tk.blue}` : "3px solid transparent" }}
              onClick={() => item.exp && setExpandedNav(expandedNav === item.key ? null : item.key)}>
              <span>{item.label}</span>{item.exp && <span style={{ fontSize: 10, color: tk.g400, transform: expandedNav === item.key ? "rotate(180deg)" : "rotate(0)", transition: "transform .2s" }}>▼</span>}
            </div>
            {item.children && expandedNav === item.key && item.children.map(child => (
              <div key={child.key} style={{ padding: "8px 16px 8px 40px", fontSize: 13, color: child.key === "analyzersList" ? tk.navTextActive : tk.navText, fontWeight: child.key === "analyzersList" ? 600 : 400, cursor: "pointer" }}>{child.label}</div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );

  // ═══════════════════════════════════════════════════════════════
  // ANALYZER LIST VIEW
  // ═══════════════════════════════════════════════════════════════
  const ListView = () => (
    <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
      <div style={{ padding: "20px 28px", background: tk.white, borderBottom: `1px solid ${tk.g100}`, display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 15, fontWeight: 600, marginBottom: 4 }}><span>{t("nav.analyzers")}</span><span style={{ color: tk.g400, fontWeight: 400 }}>{">"}</span><span>{t("analyzer.list.title")}</span></div>
          <div style={{ fontSize: 13, color: tk.g500 }}>{t("analyzer.list.subtitle")}</div>
        </div>
        <button style={{ padding: "10px 20px", fontSize: 13, fontWeight: 500, background: tk.blue, color: tk.white, border: "none", cursor: "pointer" }} onClick={() => openAddModal(true)}>{t("analyzer.list.addAnalyzer")} +</button>
      </div>
      {/* Stats */}
      <div style={{ display: "flex", margin: "0 28px", borderBottom: `1px solid ${tk.g100}`, background: tk.white }}>
        {[[t("analyzer.list.totalAnalyzers"), ANALYZERS.length], [t("analyzer.list.active"), ANALYZERS.filter(a => a.status === "Active").length], [t("analyzer.list.inactive"), ANALYZERS.filter(a => a.status === "Inactive").length]].map(([label, val], i) => (
          <div key={i} style={{ flex: 1, padding: "16px 24px", borderRight: i < 2 ? `1px solid ${tk.g100}` : "none" }}>
            <div style={{ fontSize: 12, color: tk.g500, marginBottom: 4 }}>{label}</div>
            <div style={{ fontSize: 28, fontWeight: 300, color: tk.g900 }}>{val}</div>
          </div>
        ))}
      </div>
      {/* Filters */}
      <div style={{ margin: "0 28px", background: tk.white }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "12px 16px", borderBottom: `1px solid ${tk.g100}` }}>
          <span style={{ color: tk.g400 }}>🔍</span>
          <input style={{ border: "none", outline: "none", fontSize: 14, flex: 1, background: "transparent" }} placeholder={t("analyzer.list.search")} />
        </div>
        <div style={{ padding: "12px 16px", borderBottom: `1px solid ${tk.g100}`, display: "flex", gap: 16 }}>
          <div>
            <div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.list.status")}</div>
            <select style={{ width: 160, padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}` }}><option>{t("analyzer.list.allStatuses")}</option><option>Setup</option><option>Active</option><option>Inactive</option></select>
          </div>
          <div>
            <div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.list.labUnitFilter")}</div>
            <select style={{ width: 200, padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}` }} value={listLabUnitFilter || ""} onChange={(e) => setListLabUnitFilter(e.target.value ? Number(e.target.value) : null)}>
              <option value="">{t("analyzer.list.allLabUnits")}</option>
              {LAB_UNITS.map(u => <option key={u.id} value={u.id}>{u.name}</option>)}
            </select>
          </div>
        </div>
        {/* Table */}
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
          <thead><tr>
            {["col.name","col.profile","col.labUnits","col.testCodes","col.status","col.actions"].map(k => (
              <th key={k} style={{ textAlign: "left", padding: "10px 16px", background: tk.g50, borderBottom: `2px solid ${tk.g100}`, fontWeight: 600 }}>{t(`analyzer.list.${k}`)}</th>
            ))}
          </tr></thead>
          <tbody>
            {filteredAnalyzers.map((a, i) => {
              const prof = a.profileId ? PROFILES.find(p => p.id === a.profileId) : null;
              return (
                <tr key={a.id} style={{ background: i % 2 === 1 ? tk.g10 : tk.white }}>
                  <td style={{ padding: "12px 16px", borderBottom: `1px solid ${tk.g100}` }}>
                    <div style={{ fontWeight: 500 }}>{a.name}</div>
                    <div style={{ fontSize: 11, color: tk.g500 }}>{a.connection}</div>
                  </td>
                  <td style={{ padding: "12px 16px", borderBottom: `1px solid ${tk.g100}` }}>
                    {prof ? <span style={{ fontSize: 12 }}>{prof.manufacturer} {prof.model}</span> : <span style={{ color: tk.g400, fontSize: 12 }}>—</span>}
                  </td>
                  <td style={{ padding: "12px 16px", borderBottom: `1px solid ${tk.g100}` }}>
                    {a.labUnits.length > 0 ? a.labUnits.map(id => { const u = LAB_UNITS.find(x => x.id === id); return u ? <Tag key={id}>{u.name}</Tag> : null; }) : <span style={{ color: tk.g400, fontSize: 12 }}>—</span>}
                  </td>
                  <td style={{ padding: "12px 16px", borderBottom: `1px solid ${tk.g100}` }}>{a.testCodes}</td>
                  <td style={{ padding: "12px 16px", borderBottom: `1px solid ${tk.g100}` }}>
                    <span style={{ padding: "4px 12px", fontSize: 12, fontWeight: 500, background: a.status === "Active" ? tk.greenBg : a.status === "Inactive" ? tk.redBg : tk.g50, color: a.status === "Active" ? tk.green : a.status === "Inactive" ? tk.red : tk.g700 }}>{a.status}</span>
                  </td>
                  <td style={{ padding: "12px 16px", borderBottom: `1px solid ${tk.g100}` }}>
                    <OverflowMenu items={[
                      { key: "fieldMappings", label: t("analyzer.action.fieldMappings") },
                      { key: "testConnection", label: t("analyzer.action.testConnection") },
                      { key: "exportProfile", label: t("analyzer.action.exportProfile") },
                      { key: "edit", label: t("analyzer.action.edit") },
                      { key: "delete", label: t("analyzer.action.delete"), danger: true },
                    ]} onSelect={(key) => {
                      if (key === "fieldMappings") openMappings(a);
                      else if (key === "delete") setShowDeleteModal(a);
                      else if (key === "edit") openAddModal(a);
                      else if (key === "exportProfile") setShowExportModal(a);
                    }} />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );

  // ═══════════════════════════════════════════════════════════════
  // FIELD MAPPINGS VIEW (carried from v2 — test codes, QC, sim, etc.)
  // ═══════════════════════════════════════════════════════════════
  const MappingsView = () => (
    <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
      <div style={{ padding: "20px 28px", background: tk.white, borderBottom: `1px solid ${tk.g100}` }}>
        <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 15, fontWeight: 600, marginBottom: 4 }}>
          <span style={{ cursor: "pointer", fontSize: 16 }} onClick={() => setView("list")}>←</span>
          <span style={{ cursor: "pointer" }} onClick={() => setView("list")}>{t("nav.analyzers")}</span>
          <span style={{ color: tk.g400, fontWeight: 400 }}>{">"}</span><span>{t("mapping.title")}</span>
          <span style={{ color: tk.g400, fontWeight: 400 }}>{">"}</span><span>{selectedAnalyzer?.name}</span>
        </div>
      </div>
      {unmappedCount > 0 && <div style={{ margin: "0 28px", padding: "12px 16px", background: tk.yellowBg, borderLeft: `3px solid ${tk.yellow}`, display: "flex", alignItems: "center", gap: 10, fontSize: 13 }}>
        <div style={{ width: 20, height: 20, borderRadius: 10, background: tk.yellow, color: tk.white, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 700, flexShrink: 0 }}>!</div>
        <div><strong>{t("mapping.warning.required")}</strong></div>
      </div>}
      <div style={{ display: "flex", margin: "0 28px", borderBottom: `1px solid ${tk.g100}`, background: tk.white }}>
        {[[t("mapping.stat.totalMappings"), mappings.length], [t("mapping.stat.requiredMappings"), mappedCount], [t("mapping.stat.unmappedFields"), unmappedCount, unmappedCount > 0 ? tk.red : null]].map(([label, val, color], i) => (
          <div key={i} style={{ flex: 1, padding: "16px 24px", borderRight: i < 2 ? `1px solid ${tk.g100}` : "none" }}><div style={{ fontSize: 12, color: tk.g500, marginBottom: 4 }}>{label}</div><div style={{ fontSize: 28, fontWeight: 300, color: color || tk.g900 }}>{val}</div></div>
        ))}
      </div>
      <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, padding: "12px 28px" }}>
        <button style={{ padding: "10px 20px", fontSize: 13, background: tk.white, color: tk.blue, border: `1px solid ${tk.blue}`, cursor: "pointer" }}>{t("mapping.btn.queryAnalyzer")}</button>
        <button style={{ padding: "10px 20px", fontSize: 13, background: tk.white, color: tk.blue, border: `1px solid ${tk.blue}`, cursor: "pointer" }}>{t("mapping.btn.testMapping")}</button>
        <button style={{ padding: "10px 20px", fontSize: 13, background: tk.blue, color: tk.white, border: "none", cursor: "pointer" }}>{t("mapping.btn.saveMappings")}</button>
      </div>
      <div style={{ display: "flex", borderBottom: `2px solid ${tk.g100}`, margin: "0 28px", background: tk.white }}>
        {[["testcodes", t("mapping.tab.testCodes")], ["qcrules", t("mapping.tab.qcRules")], ["simulator", t("mapping.tab.simulator")], ["extraction", t("mapping.tab.fieldExtraction")], ["advanced", t("mapping.tab.advanced")]].map(([key, label]) => (
          <button key={key} style={{ padding: "12px 20px", fontSize: 13, fontWeight: activeTab === key ? 600 : 400, color: activeTab === key ? tk.blue : tk.g600, background: "transparent", border: "none", borderBottom: activeTab === key ? `2px solid ${tk.blue}` : "2px solid transparent", cursor: "pointer", marginBottom: -2 }} onClick={() => setActiveTab(key)}>{label}</button>
        ))}
      </div>
      <div style={{ margin: "0 28px", background: tk.white, flex: 1, overflowY: "auto" }}>
        {/* TEST CODES TAB */}
        {activeTab === "testcodes" && (
          <div style={{ display: "flex" }}>
            <div style={{ flex: "0 0 58%", borderRight: `1px solid ${tk.g100}` }}>
              <div style={{ padding: "14px 16px", borderBottom: `1px solid ${tk.g100}`, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontSize: 15, fontWeight: 600 }}>{t("mapping.testCodes.title")}</span>
                <div style={{ display: "flex", gap: 4 }}>
                  {[t("mapping.testCodes.bulkAdd"), t("mapping.testCodes.importCsv"), t("mapping.testCodes.autoMatch")].map((label, i) => (
                    <button key={i} style={{ padding: "4px 8px", fontSize: 12, background: "transparent", color: tk.blue, border: "none", cursor: "pointer", fontWeight: 500 }} onClick={() => i === 0 ? setShowBulkAdd(true) : i === 2 ? showToastMsg("Auto-match found 2 matches") : null}>{label}</button>
                  ))}
                </div>
              </div>
              <div style={{ overflowY: "auto", maxHeight: 420 }}>
                <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
                  <thead><tr>{["Field Name", "ASTM Ref", "Type", "Status"].map((h, i) => <th key={i} style={{ textAlign: "left", padding: "10px 16px", background: tk.g50, borderBottom: `2px solid ${tk.g100}`, fontWeight: 600 }}>{h}</th>)}</tr></thead>
                  <tbody>
                    {mappings.map(m => (
                      <tr key={m.id} style={{ background: selectedField === m.id ? tk.blueBg : m.status === "unmapped" ? `${tk.redBg}44` : tk.white, cursor: "pointer" }} onClick={() => setSelectedField(m.id)}>
                        <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}` }}><div style={{ fontWeight: 500 }}>{m.displayName || m.analyzerCode || "(new)"}</div><div style={{ fontSize: 11, color: tk.g500, fontFamily: tk.mono }}>{m.analyzerCode}</div></td>
                        <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}`, fontFamily: tk.mono, fontSize: 12 }}>{m.astmRef}</td>
                        <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}`, fontSize: 12 }}>{m.type}</td>
                        <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}` }}><span style={{ padding: "3px 10px", fontSize: 11, fontWeight: 500, background: m.status === "mapped" ? tk.greenBg : m.status === "unmapped" ? tk.redBg : tk.yellowBg, color: tk.g700 }}>{m.status === "mapped" ? "✓ Mapped" : m.status === "draft" ? "Draft" : "Unmapped"}</span></td>
                      </tr>
                    ))}
                    <tr><td colSpan={4} style={{ padding: "10px 16px", textAlign: "center", borderBottom: `1px solid ${tk.g100}` }}><button style={{ background: "transparent", color: tk.blue, border: "none", cursor: "pointer", fontSize: 13, fontWeight: 500 }} onClick={addMapping}>+ {t("mapping.testCodes.addMapping")}</button></td></tr>
                  </tbody>
                </table>
              </div>
            </div>
            {/* Right panel */}
            <div style={{ flex: 1 }}>
              <div style={{ padding: "14px 16px", borderBottom: `1px solid ${tk.g100}`, fontSize: 15, fontWeight: 600 }}>{t("mapping.summary.title")}</div>
              {!selectedField ? <div style={{ padding: 40, textAlign: "center", color: tk.g500, fontSize: 13 }}><div style={{ fontSize: 28, marginBottom: 8, opacity: 0.3 }}>↙</div>{t("mapping.summary.empty")}</div> : (() => {
                const m = mappings.find(x => x.id === selectedField); if (!m) return null;
                return (
                  <div style={{ padding: 20 }}>
                    <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("mapping.summary.analyzerCode")}</div><input style={{ width: "100%", padding: "8px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box", fontFamily: tk.mono }} value={m.analyzerCode} onChange={(e) => { updateMapping(m.id, "analyzerCode", e.target.value); updateMapping(m.id, "displayName", e.target.value); }} /></div>
                    <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("mapping.summary.openelisTest")}</div>
                      {showInlineCreate === m.id ? (
                        <div style={{ border: `1px solid ${tk.blue}`, background: tk.blueBg, padding: 16 }}>
                          <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 12, color: tk.blue }}>+ {t("mapping.testCodes.createNewTest")}</div>
                          <div style={{ display: "flex", gap: 12, marginBottom: 10 }}>
                            <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>Test Name *</div><input style={{ width: "100%", padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} autoFocus value={newTestForm.name} onChange={(e) => setNewTestForm(f => ({ ...f, name: e.target.value, code: e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 10) }))} /></div>
                            <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>Test Code</div><input style={{ width: "100%", padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}`, boxSizing: "border-box", fontFamily: tk.mono }} value={newTestForm.code} onChange={(e) => setNewTestForm(f => ({ ...f, code: e.target.value }))} /></div>
                          </div>
                          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}><button style={{ padding: "8px 12px", fontSize: 13, background: "transparent", color: tk.blue, border: "none", cursor: "pointer" }} onClick={() => setShowInlineCreate(null)}>Cancel</button><button style={{ padding: "8px 16px", fontSize: 13, background: tk.blue, color: tk.white, border: "none", cursor: "pointer", opacity: newTestForm.name ? 1 : .4 }} disabled={!newTestForm.name} onClick={doCreateTest}>Create & Map</button></div>
                        </div>
                      ) : <TestComboBox value={m.oeTestId} onChange={(id) => updateMapping(m.id, "oeTestId", id)} onCreateNew={(search) => { setShowInlineCreate(m.id); setNewTestForm({ name: search || "", code: (search || "").toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 10), resultType: "NUMERIC", unit: "", sampleType: "Serum" }); }} />}
                    </div>
                    <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("mapping.summary.transform")}</div><select style={{ width: "100%", padding: "8px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} value={m.transform} onChange={(e) => updateMapping(m.id, "transform", e.target.value)}><option value="PASSTHROUGH">{t("transform.passThrough")}</option><option value="GREATER_LESS_FLAG">{t("transform.greaterLessFlag")}</option><option value="VALUE_MAP">{t("transform.valueMap")}</option><option value="THRESHOLD_CLASSIFY">{t("transform.thresholdClassify")}</option><option value="CODED_LOOKUP">{t("transform.codedLookup")}</option></select></div>
                    <div style={{ display: "flex", gap: 16, marginBottom: 16 }}>
                      <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("mapping.summary.analyzerUnit")}</div><input style={{ width: "100%", padding: "8px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} value={m.unit} onChange={(e) => updateMapping(m.id, "unit", e.target.value)} /></div>
                      <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("mapping.summary.openelisUnit")}</div><input style={{ width: "100%", padding: "8px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, background: tk.g50, boxSizing: "border-box" }} value={m.oeUnit} readOnly /></div>
                    </div>
                    <div style={{ display: "flex", justifyContent: "flex-end" }}><button style={{ padding: "8px 12px", fontSize: 13, background: "transparent", color: tk.red, border: "none", cursor: "pointer", fontWeight: 500 }} onClick={() => { deleteMapping(m.id); setSelectedField(null); }}>Delete Mapping</button></div>
                  </div>
                );
              })()}
            </div>
          </div>
        )}
        {/* QC RULES */}
        {activeTab === "qcrules" && <div style={{ padding: 24 }}>
          <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 4 }}>{t("qc.title")}</div>
          <div style={{ fontSize: 13, color: tk.g500, marginBottom: 16 }}>{t("qc.description")}</div>
          {qcRules.map((rule, i) => (
            <div key={rule.id} style={{ border: `1px solid ${tk.g200}`, padding: 16, marginBottom: 8 }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}><span style={{ fontSize: 13, fontWeight: 600 }}>Rule {i + 1}</span><button style={{ background: "transparent", color: tk.red, border: "none", cursor: "pointer", fontSize: 12 }} onClick={() => setQcRules(p => p.filter(r => r.id !== rule.id))}>Remove</button></div>
              <div style={{ display: "flex", gap: 16 }}>
                <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>Type</div><select style={{ width: "100%", padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} value={rule.type} onChange={(e) => setQcRules(p => p.map(r => r.id === rule.id ? { ...r, type: e.target.value } : r))}><option value="fieldEquals">Field Equals</option><option value="specimenIdPrefix">Specimen ID Prefix</option><option value="specimenIdRegex">Specimen ID Pattern</option><option value="fieldContains">Field Contains</option></select></div>
                <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>Field</div><input style={{ width: "100%", padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}`, boxSizing: "border-box", fontFamily: tk.mono }} value={rule.field} onChange={(e) => setQcRules(p => p.map(r => r.id === rule.id ? { ...r, field: e.target.value } : r))} /></div>
                <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>Value</div><input style={{ width: "100%", padding: "8px 12px", fontSize: 13, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} value={rule.value} onChange={(e) => setQcRules(p => p.map(r => r.id === rule.id ? { ...r, value: e.target.value } : r))} /></div>
              </div>
            </div>
          ))}
          <div style={{ display: "flex", gap: 12, marginTop: 12 }}>
            <button style={{ background: "transparent", color: tk.blue, border: "none", cursor: "pointer", fontSize: 13, fontWeight: 500 }} onClick={() => setQcRules(p => [...p, { id: `qc${Date.now()}`, type: "fieldEquals", field: "", value: "" }])}>+ {t("qc.addRule")}</button>
            <button style={{ padding: "8px 16px", fontSize: 13, background: tk.white, color: tk.blue, border: `1px solid ${tk.blue}`, cursor: "pointer" }}>{t("qc.testRules")}</button>
          </div>
        </div>}
        {/* SIMULATOR */}
        {activeTab === "simulator" && <div style={{ padding: 24 }}>
          <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 12 }}>{t("simulator.title")}</div>
          <textarea style={{ width: "100%", minHeight: 140, padding: 12, fontSize: 13, fontFamily: tk.mono, border: `1px solid ${tk.g200}`, resize: "vertical", boxSizing: "border-box" }} value={simMessage} onChange={(e) => setSimMessage(e.target.value)} placeholder={"H|\\^&|||Indiko Plus^Thermo|||||||\nO|1|LAB-2025-001||^^^GLU\\^^^BUN|\nR|1|^^^GLU|95|mg/dL|70-100|N|F\nR|2|^^^BUN|14|mg/dL|7-20|N|F\nL|1|N"} />
          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", margin: "12px 0" }}>
            <button style={{ background: "transparent", color: tk.blue, border: "none", cursor: "pointer", fontSize: 13, fontWeight: 500 }} onClick={() => { setSimMessage(""); setSimResult(null); }}>{t("simulator.clear")}</button>
            <button style={{ padding: "10px 20px", fontSize: 13, background: tk.blue, color: tk.white, border: "none", cursor: "pointer" }} onClick={runSimulator}>{t("simulator.parse")} ▶</button>
          </div>
          {!simResult ? <div style={{ padding: 40, textAlign: "center", color: tk.g500, fontSize: 13 }}>{t("simulator.empty")}</div> : <div>
            <div style={{ padding: "12px 16px", background: simResult.warnings > 0 ? tk.yellowBg : tk.greenBg, borderLeft: `3px solid ${simResult.warnings > 0 ? tk.yellow : tk.green}`, marginBottom: 16, fontSize: 13 }}>{simResult.warnings > 0 ? `⚠ ${simResult.warnings} unmatched` : "✓ All matched"}</div>
            <div style={{ fontSize: 13, marginBottom: 12 }}><strong>Instrument:</strong> {simResult.instrument} · <strong>Specimen:</strong> {simResult.specimen}</div>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
              <thead><tr>{["Analyzer Code","OpenELIS Test","Value","Units","Flag","Status"].map(h => <th key={h} style={{ textAlign: "left", padding: "10px 16px", background: tk.g50, borderBottom: `2px solid ${tk.g100}`, fontWeight: 600 }}>{h}</th>)}</tr></thead>
              <tbody>{simResult.results.map((r, i) => (
                <tr key={i} style={{ background: r.status === "unmatched" ? tk.yellowBg : tk.white }}>
                  <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}`, fontFamily: tk.mono, fontWeight: 600 }}>{r.testId}</td>
                  <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}` }}>{r.oeName}</td>
                  <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}`, fontFamily: tk.mono }}>{r.value}</td>
                  <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}` }}>{r.units}</td>
                  <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}` }}>{r.flag || "—"}</td>
                  <td style={{ padding: "10px 16px", borderBottom: `1px solid ${tk.g100}` }}><span style={{ padding: "3px 10px", fontSize: 11, fontWeight: 500, background: r.status === "matched" ? tk.greenBg : tk.redBg }}>{r.status === "matched" ? "✓" : "⚠"}</span></td>
                </tr>
              ))}</tbody>
            </table>
          </div>}
        </div>}
        {/* FIELD EXTRACTION */}
        {activeTab === "extraction" && <div style={{ padding: 24 }}>
          <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 4 }}>Advanced Field Extraction</div>
          <div style={{ padding: "10px 14px", background: tk.blueBg, borderLeft: `3px solid ${tk.blue}`, fontSize: 13, marginBottom: 20 }}>ℹ These defaults work for most ASTM analyzers. Only change if your instrument uses non-standard field positions.</div>
          {[["Specimen ID Field", "O.3"], ["Test ID Field", "R.3"], ["Test ID Component", "4"], ["Result Value Field", "R.4"], ["Result Units Field", "R.5"], ["Abnormal Flag Field", "R.7"], ["Result Status Field", "R.8"], ["Result Timestamp Field", "R.13"], ["Sender ID Field", "H.5"]].map(([label, dflt]) => (
            <div key={label} style={{ display: "flex", gap: 16, marginBottom: 12, alignItems: "center" }}><div style={{ width: 220, fontSize: 13, fontWeight: 500 }}>{label}</div><input style={{ flex: 1, padding: "8px 12px", fontSize: 13, fontFamily: tk.mono, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder={dflt} /></div>
          ))}
        </div>}
        {/* ADVANCED */}
        {activeTab === "advanced" && <div style={{ padding: 24 }}>
          <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 16 }}>Connection Settings</div>
          <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.connectionRole")}</div><select style={{ width: "100%", padding: "8px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} value={connectionRole} onChange={(e) => setConnectionRole(e.target.value)}><option value="server">{t("analyzer.modal.serverMode")}</option><option value="client">{t("analyzer.modal.clientMode")}</option></select></div>
          {connectionRole === "server" ? <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.listenPort")}</div><input style={{ width: 160, padding: "8px 12px", fontSize: 14, border: `1px solid ${tk.g200}` }} placeholder="5000" /></div> : <div style={{ display: "flex", gap: 16, marginBottom: 16 }}><div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.ipAddress")}</div><input style={{ width: "100%", padding: "8px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder="192.168.1.10" /></div><div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.portNumber")}</div><input style={{ width: "100%", padding: "8px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder="5000" /></div></div>}
          <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 16, marginTop: 28 }}>Result Aggregation</div>
          <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>Aggregation Mode</div><select style={{ width: 300, padding: "8px 12px", fontSize: 14, border: `1px solid ${tk.g200}` }}><option>Per Message</option><option>By Specimen</option><option>By Session</option></select></div>
          <div style={{ fontWeight: 600, fontSize: 16, marginBottom: 16, marginTop: 28 }}>Abnormal Flag Mapping</div>
          <table style={{ borderCollapse: "collapse", fontSize: 13, maxWidth: 400 }}>
            <thead><tr><th style={{ textAlign: "left", padding: "8px 16px", background: tk.g50, borderBottom: `2px solid ${tk.g100}`, fontWeight: 600 }}>Analyzer Flag</th><th style={{ textAlign: "left", padding: "8px 16px", background: tk.g50, borderBottom: `2px solid ${tk.g100}`, fontWeight: 600 }}>OpenELIS Interpretation</th></tr></thead>
            <tbody>{[["N","Normal"],["L","Low"],["H","High"],["LL","Critical Low"],["HH","Critical High"],["A","Abnormal"],["(empty)","Normal"]].map(([f,i]) => <tr key={f}><td style={{ padding: "8px 16px", borderBottom: `1px solid ${tk.g100}`, fontFamily: tk.mono, fontWeight: 600 }}>{f}</td><td style={{ padding: "8px 16px", borderBottom: `1px solid ${tk.g100}` }}>{i}</td></tr>)}</tbody>
          </table>
        </div>}
      </div>
    </div>
  );

  // ═══════════════════════════════════════════════════════════════
  // MODALS
  // ═══════════════════════════════════════════════════════════════
  const Modal = ({ title, onClose, children, footer, wide }) => (
    <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(22,22,22,0.55)", zIndex: 800, display: "flex", alignItems: "center", justifyContent: "center" }} onClick={onClose}>
      <div style={{ background: tk.white, width: wide ? 640 : 560, maxHeight: "85vh", display: "flex", flexDirection: "column", boxShadow: "0 16px 48px rgba(0,0,0,.2)" }} onClick={e => e.stopPropagation()}>
        <div style={{ padding: "20px 24px", display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}><div style={{ fontSize: 18, fontWeight: 600 }}>{title}</div><button style={{ background: "none", border: "none", cursor: "pointer", fontSize: 20, color: tk.g600 }} onClick={onClose}>✕</button></div>
        <div style={{ padding: "0 24px 24px", flex: 1, overflowY: "auto" }}>{children}</div>
        <div style={{ display: "flex" }}>{footer}</div>
      </div>
    </div>
  );
  const FooterBtn = ({ label, onClick, kind = "primary" }) => <button style={{ flex: 1, padding: "16px 24px", fontSize: 14, fontWeight: 500, background: kind === "cancel" ? tk.g700 : kind === "danger" ? tk.red : tk.blue, color: tk.white, border: "none", cursor: "pointer", textAlign: "left" }} onClick={onClick}>{label}</button>;

  return (
    <div style={{ display: "flex", fontFamily: tk.font, fontSize: 14, color: tk.g900, minHeight: "100vh", background: tk.g50 }}>
      <Sidebar />
      <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "auto" }}>
        <div style={{ background: tk.headerBg, height: 48, display: "flex", alignItems: "center", justifyContent: "flex-end", padding: "0 20px", gap: 16 }}>
          {["🔍","🔔","👤"].map((ic,i) => <span key={i} style={{ color: tk.g300, cursor: "pointer", fontSize: 16 }}>{ic}</span>)}
        </div>
        {view === "list" ? <ListView /> : <MappingsView />}
      </div>

      {/* ── ADD/EDIT ANALYZER MODAL (FR-24) ── */}
      {showAddModal && (
        <Modal wide title={typeof showAddModal === "object" ? t("analyzer.modal.editTitle") : t("analyzer.modal.addTitle")} onClose={() => setShowAddModal(false)}
          footer={<><FooterBtn label={t("analyzer.modal.cancel")} kind="cancel" onClick={() => setShowAddModal(false)} /><FooterBtn label={t("analyzer.modal.save")} onClick={() => { setShowAddModal(false); showToastMsg(selectedProfile ? `Analyzer saved — profile "${selectedProfile.name}" will be applied` : "Analyzer saved"); }} /></>}>
          
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.name")} *</div>
            <input style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder={t("analyzer.modal.namePlaceholder")} defaultValue={typeof showAddModal === "object" ? showAddModal.name : ""} />
          </div>

          {/* ANALYZER PROFILE (FR-24) */}
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.profile")}</div>
            <ProfileSelector value={modalProfile} onChange={onProfileChange} onImportFile={() => showToastMsg("File picker opened (simulated)")} />
            {selectedProfile && (
              <div style={{ marginTop: 8, padding: "12px 16px", background: tk.blueBg, borderLeft: `3px solid ${tk.blue}`, fontSize: 12 }}>
                <div style={{ fontWeight: 600, marginBottom: 4, color: tk.blue }}>{t("analyzer.modal.profile.info")}</div>
                <div style={{ display: "grid", gridTemplateColumns: "auto 1fr", gap: "2px 12px", color: tk.g700 }}>
                  <span style={{ fontWeight: 500 }}>Manufacturer:</span><span>{selectedProfile.manufacturer}</span>
                  <span style={{ fontWeight: 500 }}>Model:</span><span>{selectedProfile.model}</span>
                  <span style={{ fontWeight: 500 }}>Protocol:</span><span>{selectedProfile.protocol}</span>
                  <span style={{ fontWeight: 500 }}>Test Codes:</span><span>{selectedProfile.testCount}</span>
                  <span style={{ fontWeight: 500 }}>QC Rules:</span><span>{selectedProfile.qcRules}</span>
                  <span style={{ fontWeight: 500 }}>Version:</span><span>v{selectedProfile.version} by {selectedProfile.author}</span>
                </div>
                <div style={{ marginTop: 6, fontSize: 11, color: tk.g500, fontStyle: "italic" }}>{selectedProfile.description}</div>
                <div style={{ marginTop: 8, fontSize: 11, color: tk.blue, fontWeight: 500 }}>{t("analyzer.modal.profile.applied")}</div>
              </div>
            )}
          </div>

          {/* LAB UNITS (FR-25) */}
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.labUnits")}</div>
            <LabUnitMultiSelect selected={modalLabUnits} onChange={setModalLabUnits} />
            <div style={{ fontSize: 11, color: tk.g500, marginTop: 4 }}>{t("analyzer.modal.labUnits.help")}</div>
          </div>

          <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.status")}</div><select style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }}><option>Setup</option><option>Active</option><option>Inactive</option><option>Validation</option></select></div>

          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.pluginType")}</div>
            <select style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }}>
              <option>Select plugin type...</option><option>Generic ASTM</option><option>Generic HL7</option><option>Flat File Import</option>
            </select>
          </div>

          <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.protocolVersion")}</div><select style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} defaultValue={selectedProfile?.protocol || "ASTM LIS2-A2"}><option>ASTM LIS2-A2</option><option>HL7 v2.3.1</option></select></div>

          <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.connectionRole")}</div><select style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} value={connectionRole} onChange={(e) => setConnectionRole(e.target.value)}><option value="server">{t("analyzer.modal.serverMode")}</option><option value="client">{t("analyzer.modal.clientMode")}</option></select></div>

          {connectionRole === "server" ? (
            <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.listenPort")}</div><input style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder="5000" defaultValue={selectedProfile?.defaultPort || ""} /></div>
          ) : (
            <div style={{ display: "flex", gap: 16, marginBottom: 16 }}>
              <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.ipAddress")}</div><input style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder="192.168.1.10" /></div>
              <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("analyzer.modal.portNumber")}</div><input style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder="5000" defaultValue={selectedProfile?.defaultPort || ""} /></div>
            </div>
          )}
          <button style={{ padding: "10px 20px", fontSize: 13, background: tk.white, color: tk.blue, border: `1px solid ${tk.blue}`, cursor: "pointer" }}>{t("analyzer.modal.testConnection")}</button>
        </Modal>
      )}

      {/* DELETE MODAL */}
      {showDeleteModal && <Modal title={t("delete.title")} onClose={() => setShowDeleteModal(null)} footer={<><FooterBtn label={t("delete.cancel")} kind="cancel" onClick={() => setShowDeleteModal(null)} /><FooterBtn label={t("delete.delete")} kind="danger" onClick={() => { setShowDeleteModal(null); showToastMsg("Analyzer deleted"); }} /></>}><p style={{ fontSize: 14, color: tk.g700, lineHeight: 1.5 }}>{t("delete.confirm").replace("{name}", showDeleteModal.name)}</p></Modal>}

      {/* EXPORT PROFILE MODAL (FR-22.4) */}
      {showExportModal && (
        <Modal title={t("export.title")} onClose={() => setShowExportModal(null)} footer={<><FooterBtn label={t("analyzer.modal.cancel")} kind="cancel" onClick={() => setShowExportModal(null)} /><FooterBtn label={t("export.download")} onClick={() => { setShowExportModal(null); showToastMsg("Profile downloaded as JSON"); }} /></>}>
          <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("export.profileName")} *</div><input style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} defaultValue={`${showExportModal.name} Profile`} /></div>
          <div style={{ marginBottom: 16 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("export.description")}</div><textarea style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box", minHeight: 60, resize: "vertical" }} placeholder="Describe what this profile includes..." /></div>
          <div style={{ display: "flex", gap: 16, marginBottom: 16 }}>
            <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("export.author")}</div><input style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder="Your name" /></div>
            <div style={{ flex: 1 }}><div style={{ fontSize: 12, color: tk.g600, marginBottom: 4 }}>{t("export.tags")}</div><input style={{ width: "100%", padding: "10px 12px", fontSize: 14, border: `1px solid ${tk.g200}`, boxSizing: "border-box" }} placeholder="chemistry, ASTM" /></div>
          </div>
          <div style={{ padding: "10px 14px", background: tk.g50, borderLeft: `3px solid ${tk.g200}`, fontSize: 12, color: tk.g600 }}>
            The exported profile will include: test code mappings, QC rules, value transformations, field extraction config, and abnormal flag mappings. Site-specific data (name, IP, test ID bindings) will be stripped.
          </div>
        </Modal>
      )}

      {/* BULK ADD MODAL */}
      {showBulkAdd && <Modal title={t("mapping.testCodes.bulkAdd")} onClose={() => setShowBulkAdd(false)} footer={<><FooterBtn label={t("analyzer.modal.cancel")} kind="cancel" onClick={() => setShowBulkAdd(false)} /><FooterBtn label="Add Codes" onClick={doBulkAdd} /></>}>
        <div style={{ fontSize: 13, color: tk.g600, marginBottom: 8 }}>Paste analyzer test codes (one per line):</div>
        <textarea style={{ width: "100%", minHeight: 140, padding: 12, fontSize: 13, fontFamily: tk.mono, border: `1px solid ${tk.g200}`, resize: "vertical", boxSizing: "border-box" }} placeholder={"GLU\nBUN\nCREA\nAST\nALT"} value={bulkText} onChange={(e) => setBulkText(e.target.value)} autoFocus />
        {bulkText.trim() && <div style={{ marginTop: 8, fontSize: 12, color: tk.g600 }}>{bulkText.split("\n").filter(l => l.trim()).length} codes will be added.</div>}
      </Modal>}

      {/* TOAST */}
      {toast && <div style={{ position: "fixed", bottom: 24, right: 24, background: tk.green, color: tk.white, padding: "12px 20px", fontSize: 13, fontWeight: 500, zIndex: 999, boxShadow: "0 4px 12px rgba(0,0,0,.15)" }}>✓ {toast}</div>}
    </div>
  );
}
