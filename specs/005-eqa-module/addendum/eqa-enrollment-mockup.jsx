import { useState } from "react";
import { Search, ChevronRight, ChevronDown, Plus, MoreVertical, Clock, CheckCircle, XCircle, Edit2, Eye, Settings, Users, Microscope, TestTube2, Bug, AlertCircle, ClipboardList, BarChart3, Shield, User, Building, Bell, HelpCircle, Home, Menu, Package, FileBarChart, Check, X, PauseCircle, RotateCcw, Award, Truck, AlertTriangle, FileCheck, Activity } from "lucide-react";

const colors = {
  tealPrimary: '#00695c', tealLight: '#00897b', tealLighter: '#e0f2f1', tealDark: '#004d40',
  orange: '#e65100', orangeLight: '#ff8f00', orangeLighter: '#fff3e0',
  green: '#2e7d32', greenLight: '#e8f5e9', red: '#c62828', redLight: '#ffebee',
  yellow: '#f9a825', yellowLight: '#fffde7', blue: '#1565c0', blueLight: '#e3f2fd',
  gray50: '#fafafa', gray100: '#f5f5f5', gray200: '#eeeeee', gray300: '#e0e0e0',
  gray400: '#bdbdbd', gray500: '#9e9e9e', gray600: '#757575', gray700: '#616161',
  gray800: '#424242', gray900: '#212121', white: '#ffffff',
};

const sampleMyPrograms = [
  { id: 1, name: 'Clinical Chemistry PT', provider: 'WHO AFRO', description: 'Proficiency testing for clinical chemistry', isActive: true, labUnits: ['Chemistry'], tests: ['Glucose', 'Creatinine', 'ALT', 'AST', 'Total Bilirubin'], panels: ['Basic Metabolic Panel'] },
  { id: 2, name: 'Hematology EQA 2025', provider: 'NHRL', description: 'Quarterly hematology proficiency testing', isActive: true, labUnits: ['Hematology'], tests: ['ESR'], panels: ['CBC Panel'] },
  { id: 3, name: 'CD4 Enumeration PT', provider: 'WHO AFRO', description: 'CD4 count proficiency testing for HIV', isActive: true, labUnits: ['Immunology'], tests: ['CD4 Count', 'CD4 %'], panels: [] },
  { id: 4, name: 'AFB Smear Microscopy', provider: 'NTRL', description: 'TB smear quality assurance', isActive: false, labUnits: ['Microbiology'], tests: ['AFB Smear'], panels: [] },
  { id: 5, name: 'Malaria RDT QA', provider: 'NMCP', description: 'Malaria rapid diagnostic test QA', isActive: true, labUnits: ['Parasitology'], tests: ['Malaria RDT'], panels: [] },
];
const sampleManagedPrograms = [
  { id: 101, name: 'Regional Chemistry PT Round 3', provider: 'Central District Hospital', description: 'Regional proficiency testing round for chemistry', isActive: true, participantCount: 12 },
  { id: 102, name: 'District Hematology QA', provider: 'Central District Hospital', description: 'District-level hematology QA program', isActive: true, participantCount: 8 },
  { id: 103, name: 'TB Smear Rechecking', provider: 'Central District Hospital', description: 'Blinded rechecking program for TB smear microscopy', isActive: true, participantCount: 15 },
];
const sampleOrganizations = [
  { id: 101, name: 'Central District Hospital', code: 'CDH-001', district: 'Central', type: 'Hospital', isLocal: true },
  { id: 102, name: 'Sunrise Health Center III', code: 'SHC-003', district: 'Eastern', type: 'Health Center' },
  { id: 103, name: 'Lakeview Regional Hospital', code: 'LRH-001', district: 'Western', type: 'Hospital' },
  { id: 104, name: 'Hillside Clinic', code: 'HSC-002', district: 'Northern', type: 'Clinic' },
  { id: 105, name: 'Valley Medical Laboratory', code: 'VML-001', district: 'Southern', type: 'Laboratory' },
  { id: 106, name: 'Green Valley Health Center IV', code: 'GVH-004', district: 'Central', type: 'Health Center' },
  { id: 107, name: 'Riverside District Hospital', code: 'RDH-001', district: 'Eastern', type: 'Hospital' },
  { id: 108, name: 'Mountain View Dispensary', code: 'MVD-001', district: 'Northern', type: 'Dispensary' },
];
const sampleEnrollments = [
  { id: 1, organizationId: 101, orgName: 'Central District Hospital', orgCode: 'CDH-001', district: 'Central', enrollmentDate: '2025-01-15', status: 'Active', isLocal: true },
  { id: 2, organizationId: 102, orgName: 'Sunrise Health Center III', orgCode: 'SHC-003', district: 'Eastern', enrollmentDate: '2025-01-15', status: 'Active' },
  { id: 3, organizationId: 103, orgName: 'Lakeview Regional Hospital', orgCode: 'LRH-001', district: 'Western', enrollmentDate: '2025-01-20', status: 'Active' },
  { id: 4, organizationId: 104, orgName: 'Hillside Clinic', orgCode: 'HSC-002', district: 'Northern', enrollmentDate: '2025-02-01', status: 'Suspended' },
  { id: 5, organizationId: 105, orgName: 'Valley Medical Laboratory', orgCode: 'VML-001', district: 'Southern', enrollmentDate: '2025-01-15', status: 'Withdrawn' },
];
const sampleEqaOrders = [
  { id: 'EQA26-001', labNumber: 'DEV012600000042', program: 'Clinical Chemistry PT', provider: 'WHO AFRO', status: 'In Progress', deadline: '2026-03-15', priority: 'Standard', dateEntered: '2026-02-01', sampleId: 'PT-CHEM-2026-A' },
  { id: 'EQA26-002', labNumber: 'DEV012600000058', program: 'Hematology EQA 2025', provider: 'NHRL', status: 'Completed', deadline: '2026-02-28', priority: 'Urgent', dateEntered: '2026-01-15', sampleId: 'PT-HEM-2026-B' },
  { id: 'EQA26-003', labNumber: 'DEV012600000063', program: 'CD4 Enumeration PT', provider: 'WHO AFRO', status: 'Pending', deadline: '2026-03-30', priority: 'Standard', dateEntered: '2026-02-10', sampleId: 'PT-CD4-2026-A' },
  { id: 'EQA26-004', labNumber: 'DEV012600000071', program: 'Malaria RDT QA', provider: 'NMCP', status: 'Overdue', deadline: '2026-02-10', priority: 'Critical', dateEntered: '2026-01-20', sampleId: 'PT-MAL-2026-C' },
  { id: 'EQA26-005', labNumber: 'DEV012600000085', program: 'Clinical Chemistry PT', provider: 'WHO AFRO', status: 'Pending', deadline: '2026-04-10', priority: 'Standard', dateEntered: '2026-02-12', sampleId: 'PT-CHEM-2026-B' },
  { id: 'EQA26-006', labNumber: 'DEV012600000091', program: 'AFB Smear Microscopy', provider: 'NTRL', status: 'Completed', deadline: '2026-01-31', priority: 'Standard', dateEntered: '2025-12-15', sampleId: 'PT-TB-2026-A' },
];
const sampleAlerts = [
  { id: 1, type: 'EQA Deadline', message: 'Malaria RDT QA results overdue — deadline was 2026-02-10', severity: 'critical', time: '2 hours ago', acknowledged: false },
  { id: 2, type: 'STAT Order', message: 'STAT order DEV012600000102 — CBC, BMP for ER patient', severity: 'critical', time: '15 min ago', acknowledged: false },
  { id: 3, type: 'Critical Result', message: 'Potassium 6.8 mEq/L (critical high) — DEV012600000088', severity: 'critical', time: '30 min ago', acknowledged: false },
  { id: 4, type: 'EQA Deadline', message: 'Clinical Chemistry PT deadline approaching — due 2026-03-15', severity: 'warning', time: '1 day ago', acknowledged: false },
  { id: 5, type: 'QC Failure', message: 'Chemistry analyzer QC failed for Glucose — Lot 2024-5891', severity: 'warning', time: '4 hours ago', acknowledged: false },
  { id: 6, type: 'Sample Expiry', message: '3 samples expiring within 24 hours in Hematology', severity: 'warning', time: '3 hours ago', acknowledged: true },
  { id: 7, type: 'EQA Deadline', message: 'CD4 Enumeration PT deadline in 45 days', severity: 'info', time: '1 day ago', acknowledged: true },
];
const labUnitsOptions = ['Chemistry', 'Hematology', 'Microbiology', 'Immunology', 'Parasitology', 'Urinalysis', 'Blood Bank', 'Serology'];
const testsOptions = ['Glucose', 'Creatinine', 'ALT', 'AST', 'Total Bilirubin', 'Albumin', 'Total Protein', 'BUN', 'Sodium', 'Potassium', 'Chloride', 'CO2', 'Calcium', 'WBC', 'RBC', 'Hemoglobin', 'Hematocrit', 'Platelet Count', 'ESR', 'CD4 Count', 'CD4 %', 'AFB Smear', 'Malaria RDT', 'Urinalysis', 'HbA1c', 'Cholesterol', 'Triglycerides', 'HDL', 'LDL'];
const panelsOptions = ['Basic Metabolic Panel', 'Comprehensive Metabolic Panel', 'CBC Panel', 'Lipid Panel', 'Liver Function Panel', 'Renal Function Panel', 'Thyroid Panel', 'Coagulation Panel'];

// === REUSABLE COMPONENTS ===
const Badge = ({ variant = 'default', children, size = 'md' }) => {
  const v = { success: { bg: colors.greenLight, c: colors.green, b: colors.green }, error: { bg: colors.redLight, c: colors.red, b: colors.red }, warning: { bg: colors.yellowLight, c: '#b8860b', b: colors.yellow }, info: { bg: colors.blueLight, c: colors.blue, b: colors.blue }, default: { bg: colors.gray100, c: colors.gray700, b: colors.gray400 }, teal: { bg: colors.tealLighter, c: colors.tealPrimary, b: colors.tealPrimary }, orange: { bg: colors.orangeLighter, c: colors.orange, b: colors.orange } }[variant] || { bg: colors.gray100, c: colors.gray700, b: colors.gray400 };
  return <span style={{ display: 'inline-flex', alignItems: 'center', padding: size === 'sm' ? '2px 6px' : '4px 10px', borderRadius: '4px', fontSize: size === 'sm' ? '11px' : '12px', fontWeight: 600, backgroundColor: v.bg, color: v.c, border: `1px solid ${v.b}`, whiteSpace: 'nowrap' }}>{children}</span>;
};
const Btn = ({ variant = 'primary', size = 'md', children, icon: Icon, onClick, disabled }) => {
  const v = { primary: { bg: colors.tealPrimary, c: 'white', b: 'none' }, secondary: { bg: 'white', c: colors.gray700, b: `1px solid ${colors.gray300}` }, danger: { bg: colors.red, c: 'white', b: 'none' }, ghost: { bg: 'transparent', c: colors.tealPrimary, b: 'none' } }[variant] || { bg: colors.tealPrimary, c: 'white', b: 'none' };
  const s = size === 'sm' ? { p: '6px 12px', f: '12px' } : { p: '8px 16px', f: '14px' };
  return <button onClick={onClick} disabled={disabled} style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '6px', padding: s.p, fontSize: s.f, fontWeight: 500, backgroundColor: v.bg, color: v.c, border: v.b, borderRadius: '4px', cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.5 : 1 }}>{Icon && <Icon size={size === 'sm' ? 14 : 16} />}{children}</button>;
};
const Card = ({ children, title, subtitle, noPadding }) => (
  <div style={{ backgroundColor: 'white', borderRadius: '8px', border: `1px solid ${colors.gray200}`, boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}>
    {title && <div style={{ padding: '16px 20px', borderBottom: `1px solid ${colors.gray200}` }}><h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600, color: colors.gray900 }}>{title}</h3>{subtitle && <div style={{ fontSize: '13px', color: colors.gray500, marginTop: '4px' }}>{subtitle}</div>}</div>}
    <div style={{ padding: noPadding ? 0 : '20px' }}>{children}</div>
  </div>
);
const Tile = ({ title, value, subtitle, icon: Icon, color }) => (
  <div style={{ backgroundColor: 'white', borderRadius: '8px', border: `1px solid ${colors.gray200}`, padding: '20px', borderLeft: `4px solid ${color}` }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
      <div><div style={{ fontSize: '12px', fontWeight: 500, color: colors.gray500, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{title}</div><div style={{ fontSize: '28px', fontWeight: 700, color: colors.gray900, marginTop: '4px' }}>{value}</div>{subtitle && <div style={{ fontSize: '12px', color: colors.gray500, marginTop: '4px' }}>{subtitle}</div>}</div>
      {Icon && <div style={{ padding: '8px', borderRadius: '8px', backgroundColor: `${color}15` }}><Icon size={20} color={color} /></div>}
    </div>
  </div>
);
const Overflow = ({ items, onSelect }) => {
  const [open, setOpen] = useState(false);
  return (<div style={{ position: 'relative' }}>
    <button onClick={() => setOpen(!open)} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '6px', borderRadius: '4px', color: colors.gray600, display: 'flex' }}><MoreVertical size={16} /></button>
    {open && <><div style={{ position: 'fixed', inset: 0, zIndex: 99 }} onClick={() => setOpen(false)} /><div style={{ position: 'absolute', right: 0, top: '100%', zIndex: 100, backgroundColor: colors.white, border: `1px solid ${colors.gray200}`, borderRadius: '4px', boxShadow: '0 4px 16px rgba(0,0,0,0.12)', minWidth: '190px' }}>{items.map((item, i) => <button key={i} onClick={() => { onSelect(item.action); setOpen(false); }} disabled={item.disabled} style={{ display: 'flex', alignItems: 'center', gap: '8px', width: '100%', padding: '10px 16px', border: 'none', background: 'none', fontSize: '13px', color: item.danger ? colors.red : item.disabled ? colors.gray400 : colors.gray800, cursor: item.disabled ? 'not-allowed' : 'pointer', textAlign: 'left', opacity: item.disabled ? 0.5 : 1 }}>{item.icon && <item.icon size={14} />}{item.label}</button>)}</div></>}
  </div>);
};
const TagList = ({ items, max = 3, color = colors.tealPrimary }) => {
  const show = items.slice(0, max); const more = items.length - max;
  return <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>{show.map(t => <span key={t} style={{ display: 'inline-block', padding: '2px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: 500, backgroundColor: `${color}15`, color, border: `1px solid ${color}30` }}>{t}</span>)}{more > 0 && <span style={{ fontSize: '11px', color: colors.gray500, alignSelf: 'center' }}>+{more} more</span>}</div>;
};
const MultiSelect = ({ label, options, selected, onChange, placeholder }) => {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const filtered = options.filter(o => o.toLowerCase().includes(search.toLowerCase()));
  const toggle = (item) => onChange(selected.includes(item) ? selected.filter(s => s !== item) : [...selected, item]);
  return (<div style={{ marginBottom: '8px' }}>
    {label && <label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: colors.gray700, marginBottom: '4px' }}>{label}</label>}
    <div onClick={() => setOpen(!open)} style={{ minHeight: '34px', padding: '4px 10px', borderRadius: '4px', border: `1px solid ${open ? colors.tealPrimary : colors.gray300}`, cursor: 'pointer', display: 'flex', flexWrap: 'wrap', gap: '4px', alignItems: 'center', backgroundColor: colors.white }}>
      {selected.length === 0 && <span style={{ color: colors.gray500, fontSize: '12px' }}>{placeholder || 'Select...'}</span>}
      {selected.map(s => <span key={s} style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', padding: '1px 6px', borderRadius: '3px', fontSize: '11px', backgroundColor: colors.tealLighter, color: colors.tealPrimary, border: `1px solid ${colors.tealPrimary}30` }}>{s}<X size={10} style={{ cursor: 'pointer' }} onClick={e => { e.stopPropagation(); toggle(s); }} /></span>)}
    </div>
    {open && <div style={{ border: `1px solid ${colors.gray200}`, borderRadius: '0 0 4px 4px', boxShadow: '0 4px 8px rgba(0,0,0,0.1)', maxHeight: '150px', overflow: 'auto', marginTop: '-1px', backgroundColor: colors.white, position: 'relative', zIndex: 10 }}>
      <div style={{ padding: '6px 8px', borderBottom: `1px solid ${colors.gray200}`, position: 'sticky', top: 0, backgroundColor: colors.white }}><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Type to filter..." autoFocus style={{ width: '100%', border: 'none', outline: 'none', fontSize: '12px', boxSizing: 'border-box' }} /></div>
      {filtered.map(o => <div key={o} onClick={() => toggle(o)} style={{ padding: '5px 10px', fontSize: '12px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px', backgroundColor: selected.includes(o) ? colors.tealLighter : 'transparent' }}><div style={{ width: '14px', height: '14px', borderRadius: '3px', border: `2px solid ${selected.includes(o) ? colors.tealPrimary : colors.gray300}`, backgroundColor: selected.includes(o) ? colors.tealPrimary : 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>{selected.includes(o) && <Check size={8} color="white" />}</div>{o}</div>)}
    </div>}
  </div>);
};
const th = { padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: colors.gray600, fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', borderBottom: `2px solid ${colors.gray200}` };

// === HEADER ===
const Header = ({ onMenuClick }) => (
  <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 16px', height: '48px', backgroundColor: colors.tealPrimary, color: 'white' }}>
    <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
      <button onClick={onMenuClick} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '4px', display: 'flex' }}><Menu size={24} color="white" /></button>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><Microscope size={24} /><span style={{ fontWeight: 700, fontSize: '16px' }}>OpenELIS Global</span></div>
    </div>
    <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
      <div style={{ fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px' }}><Building size={16} />Central Hospital Laboratory</div>
      <button style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '4px', display: 'flex', position: 'relative' }}><Bell size={20} color="white" /><span style={{ position: 'absolute', top: 0, right: 0, width: '8px', height: '8px', backgroundColor: colors.orange, borderRadius: '50%' }} /></button>
      <button style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '4px', display: 'flex' }}><HelpCircle size={20} color="white" /></button>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><div style={{ width: '32px', height: '32px', borderRadius: '50%', backgroundColor: 'rgba(255,255,255,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><User size={18} /></div><span style={{ fontSize: '13px' }}>J. Williams</span><ChevronDown size={16} /></div>
    </div>
  </header>
);

// === SIDEBAR ===
const Sidebar = ({ isOpen, currentView, onNavigate, alertCount }) => {
  const [expanded, setExpanded] = useState(['eqa-tests', 'eqa-mgmt']);
  const toggle = (id) => setExpanded(p => p.includes(id) ? p.filter(m => m !== id) : [...p, id]);
  const items = [
    { id: 'home', label: 'Home', icon: Home },
    { id: 'order', label: 'Order', icon: ClipboardList, children: [{ id: 'order-entry', label: 'Order Entry' }, { id: 'order-search', label: 'Order Search' }, { id: 'batch-order', label: 'Batch Order Entry' }, { id: 'electronic-orders', label: 'Electronic Orders' }] },
    { id: 'results', label: 'Results', icon: FileBarChart, children: [{ id: 'results-entry', label: 'Results Entry' }, { id: 'results-validation', label: 'Validation' }, { id: 'results-search', label: 'Result Search' }] },
    { id: 'microbiology', label: 'Microbiology', icon: Bug, children: [{ id: 'micro-dashboard', label: 'Dashboard' }, { id: 'micro-pending', label: 'Pending Cultures' }, { id: 'micro-ast', label: 'AST Worklist' }] },
    { id: 'patient', label: 'Patient', icon: Users },
    { id: 'sample', label: 'Sample', icon: TestTube2 },
    { id: 'eqa-tests', label: 'EQA Tests', icon: FileCheck, children: [{ id: 'eqa-orders', label: 'Orders' }, { id: 'eqa-my-programs', label: 'My Programs' }] },
    { id: 'eqa-mgmt', label: 'EQA Management', icon: Award, children: [{ id: 'mgmt-programs', label: 'Programs' }, { id: 'mgmt-participants', label: 'Participants' }, { id: 'mgmt-distributions', label: 'Distributions' }, { id: 'mgmt-results', label: 'Results & Analysis' }] },
    { id: 'inventory', label: 'Inventory', icon: Package },
    { id: 'quality-control', label: 'Quality Control', icon: Shield },
    { id: 'alerts', label: 'Alerts', icon: AlertCircle, badge: alertCount },
    { id: 'reports', label: 'Reports', icon: BarChart3, children: [{ id: 'routine-reports', label: 'Routine Reports' }, { id: 'study-reports', label: 'Study Reports' }, { id: 'whonet-export', label: 'WHONET Export' }, { id: 'antibiogram', label: 'Antibiogram' }] },
    { id: 'admin', label: 'Administration', icon: Settings, children: [{ id: 'test-management', label: 'Test Management' }, { id: 'amr-config', label: 'AMR Configuration' }, { id: 'site-info', label: 'Site Information' }, { id: 'user-management', label: 'User Management' }, { id: 'dictionaries', label: 'Dictionaries' }] },
  ];
  if (!isOpen) return null;
  return (<aside style={{ width: '260px', minWidth: '260px', backgroundColor: 'white', borderRight: `1px solid ${colors.gray200}`, height: 'calc(100vh - 48px)', overflowY: 'auto' }}><nav style={{ padding: '8px 0' }}>{items.map(item => {
    const active = currentView === item.id || (item.children && item.children.some(c => c.id === currentView));
    return (<div key={item.id}>
      <div onClick={() => item.children ? toggle(item.id) : onNavigate(item.id)} style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 16px', cursor: 'pointer', backgroundColor: active && !item.children ? colors.tealLighter : 'transparent', color: active && !item.children ? colors.tealPrimary : colors.gray800 }}>
        <item.icon size={18} /><span style={{ flex: 1, fontSize: '14px', fontWeight: active && !item.children ? 600 : 400 }}>{item.label}</span>
        {item.badge > 0 && <span style={{ minWidth: '20px', height: '20px', borderRadius: '10px', backgroundColor: colors.red, color: 'white', fontSize: '11px', fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0 5px' }}>{item.badge}</span>}
        {item.children && (expanded.includes(item.id) ? <ChevronDown size={16} /> : <ChevronRight size={16} />)}
      </div>
      {item.children && expanded.includes(item.id) && <div style={{ backgroundColor: colors.gray50 }}>{item.children.map(child => <div key={child.id} onClick={() => onNavigate(child.id)} style={{ padding: '10px 16px 10px 46px', fontSize: '13px', cursor: 'pointer', backgroundColor: currentView === child.id ? colors.tealLighter : 'transparent', color: currentView === child.id ? colors.tealPrimary : colors.gray700, fontWeight: currentView === child.id ? 600 : 400 }}>{child.label}</div>)}</div>}
    </div>);
  })}</nav></aside>);
};

// === INLINE ENROLLMENT FORM (My Programs - with test mapping) ===
const InlineEnrollmentForm = ({ program, onSave, onCancel }) => {
  const isEdit = !!program;
  const [name, setName] = useState(program?.name || '');
  const [provider, setProvider] = useState(program?.provider || '');
  const [description, setDescription] = useState(program?.description || '');
  const [isActive, setIsActive] = useState(program?.isActive ?? true);
  const [labUnits, setLabUnits] = useState(program?.labUnits || []);
  const [tests, setTests] = useState(program?.tests || []);
  const [panels, setPanels] = useState(program?.panels || []);
  const [showSugg, setShowSugg] = useState(false);
  const allProviders = [...new Set(sampleMyPrograms.map(p => p.provider))];
  const sugg = allProviders.filter(p => p.toLowerCase().includes(provider.toLowerCase()) && p !== provider);
  return (
    <div style={{ padding: '20px 24px', backgroundColor: colors.gray50, borderTop: `2px solid ${colors.tealPrimary}` }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: colors.tealPrimary }}>{isEdit ? `Editing: ${program.name}` : 'New EQA Program Enrollment'}</h4>
        <button onClick={onCancel} style={{ background: 'none', border: 'none', cursor: 'pointer', color: colors.gray500, display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px' }}><X size={14} /> Cancel</button>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px', marginBottom: '16px' }}>
        <div><label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: colors.gray700, marginBottom: '4px' }}>Program Name <span style={{ color: colors.red }}>*</span></label><input value={name} onChange={e => setName(e.target.value)} placeholder="e.g., Clinical Chemistry PT" style={{ width: '100%', padding: '8px 10px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px', boxSizing: 'border-box' }} /></div>
        <div style={{ position: 'relative' }}>
          <label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: colors.gray700, marginBottom: '4px' }}>Provider <span style={{ color: colors.red }}>*</span></label>
          <input value={provider} onChange={e => { setProvider(e.target.value); setShowSugg(true); }} onFocus={() => setShowSugg(true)} onBlur={() => setTimeout(() => setShowSugg(false), 200)} placeholder="e.g., WHO AFRO" style={{ width: '100%', padding: '8px 10px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px', boxSizing: 'border-box' }} />
          {showSugg && sugg.length > 0 && <div style={{ position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 20, backgroundColor: colors.white, border: `1px solid ${colors.gray200}`, borderRadius: '0 0 4px 4px', boxShadow: '0 4px 8px rgba(0,0,0,0.1)' }}>{sugg.map(s => <button key={s} onMouseDown={() => { setProvider(s); setShowSugg(false); }} style={{ display: 'block', width: '100%', padding: '6px 10px', border: 'none', background: 'none', textAlign: 'left', fontSize: '12px', cursor: 'pointer' }}>{s}</button>)}</div>}
        </div>
        <div><label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: colors.gray700, marginBottom: '4px' }}>Description</label><input value={description} onChange={e => setDescription(e.target.value)} placeholder="Optional notes..." style={{ width: '100%', padding: '8px 10px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px', boxSizing: 'border-box' }} /></div>
      </div>
      <div style={{ padding: '12px 16px', backgroundColor: colors.white, borderRadius: '6px', border: `1px solid ${colors.gray200}`, marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
          <div style={{ fontSize: '12px', fontWeight: 600, color: colors.gray600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>Test Mapping (Optional)</div>
          <div style={{ fontSize: '11px', color: colors.gray500 }}>Pre-populates Order Entry — users can override</div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px' }}>
          <MultiSelect label="Lab Unit(s)" options={labUnitsOptions} selected={labUnits} onChange={setLabUnits} placeholder="Select lab units..." />
          <MultiSelect label="Tests" options={testsOptions} selected={tests} onChange={setTests} placeholder="Select tests..." />
          <MultiSelect label="Panels" options={panelsOptions} selected={panels} onChange={setPanels} placeholder="Select panels..." />
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label style={{ fontSize: '12px', fontWeight: 500, color: colors.gray700 }}>Status:</label>
          <button onClick={() => setIsActive(!isActive)} style={{ width: '38px', height: '20px', borderRadius: '10px', border: 'none', backgroundColor: isActive ? colors.green : colors.gray400, cursor: 'pointer', position: 'relative' }}><div style={{ width: '16px', height: '16px', borderRadius: '50%', backgroundColor: colors.white, position: 'absolute', top: '2px', left: isActive ? '20px' : '2px', transition: 'left 0.2s' }} /></button>
          <span style={{ fontSize: '12px', color: isActive ? colors.green : colors.gray500 }}>{isActive ? 'Active' : 'Inactive'}</span>
        </div>
        <div style={{ display: 'flex', gap: '8px' }}><Btn variant="secondary" size="sm" onClick={onCancel}>Cancel</Btn><Btn size="sm" disabled={!name || !provider} onClick={() => onSave()}>{isEdit ? 'Save Changes' : 'Save Enrollment'}</Btn></div>
      </div>
    </div>
  );
};

// === EQA TESTS → ORDERS ===
const EQAOrdersPage = ({ onEnterTest }) => {
  const [search, setSearch] = useState(''); const [statusF, setStatusF] = useState('all'); const [programF, setProgramF] = useState('all');
  const orders = sampleEqaOrders;
  const filtered = orders.filter(o => { const ms = !search || o.labNumber.toLowerCase().includes(search.toLowerCase()) || o.program.toLowerCase().includes(search.toLowerCase()); const mst = statusF === 'all' || o.status.toLowerCase().replace(' ', '') === statusF; const mp = programF === 'all' || o.program === programF; return ms && mst && mp; });
  const counts = { pending: orders.filter(o => o.status === 'Pending').length, inProgress: orders.filter(o => o.status === 'In Progress').length, overdue: orders.filter(o => o.status === 'Overdue').length, completed: orders.filter(o => o.status === 'Completed').length };
  return (<div style={{ padding: '24px' }}>
    <div style={{ fontSize: '12px', color: colors.gray500, marginBottom: '4px' }}>Home / EQA Tests / Orders</div>
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
      <div><h2 style={{ margin: 0, fontSize: '20px', fontWeight: 600, color: colors.gray900 }}>EQA Orders</h2><p style={{ margin: '4px 0 0', fontSize: '13px', color: colors.gray500 }}>Proficiency testing orders for this laboratory</p></div>
      <Btn icon={Plus} onClick={() => onEnterTest(null)}>Enter New EQA Test</Btn>
    </div>
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginBottom: '24px' }}>
      <Tile title="Pending" value={counts.pending} icon={Clock} color={colors.blue} /><Tile title="In Progress" value={counts.inProgress} icon={Activity} color={colors.tealPrimary} /><Tile title="Overdue" value={counts.overdue} icon={AlertTriangle} color={colors.red} /><Tile title="Completed This Month" value={counts.completed} icon={CheckCircle} color={colors.green} />
    </div>
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px', flexWrap: 'wrap' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '0 12px', height: '36px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, backgroundColor: colors.white, flex: 1, maxWidth: '360px' }}><Search size={16} color={colors.gray500} /><input placeholder="Search by lab number or program..." value={search} onChange={e => setSearch(e.target.value)} style={{ border: 'none', outline: 'none', fontSize: '13px', flex: 1, backgroundColor: 'transparent' }} /></div>
      <select value={statusF} onChange={e => setStatusF(e.target.value)} style={{ height: '36px', padding: '0 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px' }}><option value="all">All Status</option><option value="pending">Pending</option><option value="inprogress">In Progress</option><option value="completed">Completed</option><option value="overdue">Overdue</option></select>
      <select value={programF} onChange={e => setProgramF(e.target.value)} style={{ height: '36px', padding: '0 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px' }}><option value="all">All Programs</option>{[...new Set(orders.map(o => o.program))].map(p => <option key={p} value={p}>{p}</option>)}</select>
    </div>
    <Card noPadding><table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}><thead><tr style={{ backgroundColor: colors.gray100 }}>{['Lab Number', 'EQA Program', 'Provider', 'Status', 'Deadline', 'Priority', 'Date Entered', 'Actions'].map(h => <th key={h} style={th}>{h}</th>)}</tr></thead><tbody>{filtered.map((o, i) => {
      const ov = o.status === 'Overdue';
      return (<tr key={o.id} style={{ borderBottom: `1px solid ${colors.gray200}`, backgroundColor: ov ? colors.redLight : i % 2 === 0 ? colors.white : colors.gray50 }}>
        <td style={{ padding: '12px 16px' }}><div style={{ fontFamily: 'monospace', fontSize: '12px', fontWeight: 500, color: colors.tealPrimary, cursor: 'pointer' }}>{o.labNumber}</div><div style={{ fontSize: '11px', color: colors.gray500, marginTop: '2px' }}>{o.id}</div></td>
        <td style={{ padding: '12px 16px', fontWeight: 500, color: colors.gray900 }}>{o.program}</td>
        <td style={{ padding: '12px 16px', color: colors.gray600 }}>{o.provider}</td>
        <td style={{ padding: '12px 16px' }}><Badge size="sm" variant={o.status === 'Completed' ? 'success' : o.status === 'Overdue' ? 'error' : o.status === 'In Progress' ? 'info' : 'warning'}>{o.status}</Badge></td>
        <td style={{ padding: '12px 16px' }}><div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: ov ? colors.red : colors.gray700, fontWeight: ov ? 600 : 400 }}>{ov && <AlertTriangle size={14} />}{o.deadline}</div></td>
        <td style={{ padding: '12px 16px' }}><Badge size="sm" variant={o.priority === 'Critical' ? 'error' : o.priority === 'Urgent' ? 'orange' : 'default'}>{o.priority}</Badge></td>
        <td style={{ padding: '12px 16px', color: colors.gray600 }}>{o.dateEntered}</td>
        <td style={{ padding: '12px 16px' }}><Overflow items={[{ label: 'View Order', icon: Eye, action: 'view' }, { label: 'Enter Results', icon: Edit2, action: 'enter', disabled: o.status === 'Completed' }, { label: 'View Results', icon: FileBarChart, action: 'results', disabled: o.status !== 'Completed' }]} onSelect={() => {}} /></td>
      </tr>);
    })}</tbody></table>
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 16px', borderTop: `1px solid ${colors.gray200}`, fontSize: '12px', color: colors.gray500 }}><span>{filtered.length} of {orders.length} orders</span><span>Items per page: 25</span></div></Card>
  </div>);
};

// === EQA TESTS → MY PROGRAMS (inline row expand) ===
const MyProgramsPage = () => {
  const [search, setSearch] = useState('');
  const [expandedId, setExpandedId] = useState(null);
  const programs = sampleMyPrograms;
  const filtered = programs.filter(p => !search || p.name.toLowerCase().includes(search.toLowerCase()) || p.provider.toLowerCase().includes(search.toLowerCase()));
  const close = () => setExpandedId(null);
  return (<div style={{ padding: '24px' }}>
    <div style={{ fontSize: '12px', color: colors.gray500, marginBottom: '4px' }}>Home / EQA Tests / My Programs</div>
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
      <div><h2 style={{ margin: 0, fontSize: '20px', fontWeight: 600, color: colors.gray900 }}>My EQA Programs</h2><p style={{ margin: '4px 0 0', fontSize: '13px', color: colors.gray500 }}>Programs this laboratory participates in — populates EQA dropdown in Order Entry</p></div>
      <Btn icon={Plus} onClick={() => setExpandedId('new')} disabled={expandedId === 'new'}>Enroll in Program</Btn>
    </div>
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}><div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '0 12px', height: '36px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, backgroundColor: colors.white, flex: 1, maxWidth: '360px' }}><Search size={16} color={colors.gray500} /><input placeholder="Search by program name or provider..." value={search} onChange={e => setSearch(e.target.value)} style={{ border: 'none', outline: 'none', fontSize: '13px', flex: 1, backgroundColor: 'transparent' }} /></div></div>
    <Card noPadding><table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}><thead><tr style={{ backgroundColor: colors.gray100 }}>{['Program Name', 'Provider', 'Lab Units', 'Tests / Panels', 'Status', 'Actions'].map(h => <th key={h} style={th}>{h}</th>)}</tr></thead><tbody>
      {expandedId === 'new' && <tr><td colSpan={6} style={{ padding: 0 }}><InlineEnrollmentForm program={null} onSave={close} onCancel={close} /></td></tr>}
      {filtered.map((p, i) => (<>
        <tr key={p.id} style={{ borderBottom: expandedId === p.id ? 'none' : `1px solid ${colors.gray200}`, backgroundColor: expandedId === p.id ? colors.tealLighter : i % 2 === 0 ? colors.white : colors.gray50, cursor: 'pointer' }} onClick={() => expandedId === p.id ? close() : setExpandedId(p.id)}>
          <td style={{ padding: '12px 16px' }}><div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><ChevronDown size={14} color={colors.gray400} style={{ transform: expandedId === p.id ? 'rotate(0deg)' : 'rotate(-90deg)', transition: 'transform 0.15s' }} /><div><div style={{ fontWeight: 500, color: colors.gray900 }}>{p.name}</div>{p.description && <div style={{ fontSize: '12px', color: colors.gray500, marginTop: '2px' }}>{p.description}</div>}</div></div></td>
          <td style={{ padding: '12px 16px', color: colors.gray700 }}>{p.provider}</td>
          <td style={{ padding: '12px 16px' }}>{p.labUnits.length > 0 ? <TagList items={p.labUnits} color={colors.blue} /> : <span style={{ color: colors.gray400, fontSize: '12px' }}>—</span>}</td>
          <td style={{ padding: '12px 16px' }}>{(p.tests.length + p.panels.length) > 0 ? <TagList items={[...p.panels.map(pa => `📋 ${pa}`), ...p.tests]} max={4} /> : <span style={{ color: colors.gray400, fontSize: '12px' }}>—</span>}</td>
          <td style={{ padding: '12px 16px' }}><Badge size="sm" variant={p.isActive ? 'success' : 'default'}>{p.isActive ? 'Active' : 'Inactive'}</Badge></td>
          <td style={{ padding: '12px 16px' }} onClick={e => e.stopPropagation()}><Overflow items={[{ label: 'Edit', icon: Edit2, action: 'edit' }, { label: p.isActive ? 'Deactivate' : 'Reactivate', icon: p.isActive ? XCircle : CheckCircle, action: 'toggle' }]} onSelect={a => { if (a === 'edit') setExpandedId(p.id); }} /></td>
        </tr>
        {expandedId === p.id && <tr key={`${p.id}-form`}><td colSpan={6} style={{ padding: 0 }}><InlineEnrollmentForm program={p} onSave={close} onCancel={close} /></td></tr>}
      </>))}
    </tbody></table>
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 16px', borderTop: `1px solid ${colors.gray200}`, fontSize: '12px', color: colors.gray500 }}><span>{filtered.length} of {programs.length} programs</span><span>Items per page: 25</span></div></Card>
  </div>);
};

// === INLINE MANAGED PROGRAM FORM (simpler) ===
const InlineMgmtForm = ({ program, onSave, onCancel }) => {
  const isEdit = !!program;
  const [name, setName] = useState(program?.name || ''); const [provider, setProvider] = useState(program?.provider || ''); const [desc, setDesc] = useState(program?.description || ''); const [active, setActive] = useState(program?.isActive ?? true);
  return (<div style={{ padding: '20px 24px', backgroundColor: colors.gray50, borderTop: `2px solid ${colors.tealPrimary}` }}>
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}><h4 style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: colors.tealPrimary }}>{isEdit ? `Editing: ${program.name}` : 'New EQA Program'}</h4><button onClick={onCancel} style={{ background: 'none', border: 'none', cursor: 'pointer', color: colors.gray500, display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px' }}><X size={14} /> Cancel</button></div>
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 2fr', gap: '16px', marginBottom: '16px' }}>
      <div><label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: colors.gray700, marginBottom: '4px' }}>Program Name <span style={{ color: colors.red }}>*</span></label><input value={name} onChange={e => setName(e.target.value)} placeholder="e.g., Regional Chemistry PT" style={{ width: '100%', padding: '8px 10px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px', boxSizing: 'border-box' }} /></div>
      <div><label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: colors.gray700, marginBottom: '4px' }}>Provider <span style={{ color: colors.red }}>*</span></label><input value={provider} onChange={e => setProvider(e.target.value)} placeholder="e.g., Central District Hospital" style={{ width: '100%', padding: '8px 10px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px', boxSizing: 'border-box' }} /></div>
      <div><label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: colors.gray700, marginBottom: '4px' }}>Description</label><input value={desc} onChange={e => setDesc(e.target.value)} placeholder="Optional description..." style={{ width: '100%', padding: '8px 10px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px', boxSizing: 'border-box' }} /></div>
    </div>
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><label style={{ fontSize: '12px', fontWeight: 500, color: colors.gray700 }}>Status:</label><button onClick={() => setActive(!active)} style={{ width: '38px', height: '20px', borderRadius: '10px', border: 'none', backgroundColor: active ? colors.green : colors.gray400, cursor: 'pointer', position: 'relative' }}><div style={{ width: '16px', height: '16px', borderRadius: '50%', backgroundColor: colors.white, position: 'absolute', top: '2px', left: active ? '20px' : '2px', transition: 'left 0.2s' }} /></button><span style={{ fontSize: '12px', color: active ? colors.green : colors.gray500 }}>{active ? 'Active' : 'Inactive'}</span></div>
      <div style={{ display: 'flex', gap: '8px' }}><Btn variant="secondary" size="sm" onClick={onCancel}>Cancel</Btn><Btn size="sm" disabled={!name || !provider} onClick={() => onSave()}>{isEdit ? 'Save Changes' : 'Save Program'}</Btn></div>
    </div>
  </div>);
};

// === EQA MANAGEMENT → PROGRAMS (inline row expand) ===
const MgmtProgramsPage = ({ onEnterTest }) => {
  const [search, setSearch] = useState(''); const [expandedId, setExpandedId] = useState(null);
  const programs = sampleManagedPrograms;
  const filtered = programs.filter(p => !search || p.name.toLowerCase().includes(search.toLowerCase()));
  const close = () => setExpandedId(null);
  return (<div style={{ padding: '24px' }}>
    <div style={{ fontSize: '12px', color: colors.gray500, marginBottom: '4px' }}>Home / EQA Management / Programs</div>
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
      <div><h2 style={{ margin: 0, fontSize: '20px', fontWeight: 600, color: colors.gray900 }}>EQA Programs</h2><p style={{ margin: '4px 0 0', fontSize: '13px', color: colors.gray500 }}>Manage programs distributed by this laboratory</p></div>
      <Btn icon={Plus} onClick={() => setExpandedId('new')} disabled={expandedId === 'new'}>Add Program</Btn>
    </div>
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}><div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '0 12px', height: '36px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, backgroundColor: colors.white, flex: 1, maxWidth: '360px' }}><Search size={16} color={colors.gray500} /><input placeholder="Search programs..." value={search} onChange={e => setSearch(e.target.value)} style={{ border: 'none', outline: 'none', fontSize: '13px', flex: 1, backgroundColor: 'transparent' }} /></div></div>
    <Card noPadding><table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}><thead><tr style={{ backgroundColor: colors.gray100 }}>{['Program Name', 'Provider', 'Enrolled Participants', 'Status', 'Actions'].map(h => <th key={h} style={th}>{h}</th>)}</tr></thead><tbody>
      {expandedId === 'new' && <tr><td colSpan={5} style={{ padding: 0 }}><InlineMgmtForm program={null} onSave={close} onCancel={close} /></td></tr>}
      {filtered.map((p, i) => (<>
        <tr key={p.id} style={{ borderBottom: expandedId === p.id ? 'none' : `1px solid ${colors.gray200}`, backgroundColor: expandedId === p.id ? colors.tealLighter : i % 2 === 0 ? colors.white : colors.gray50, cursor: 'pointer' }} onClick={() => expandedId === p.id ? close() : setExpandedId(p.id)}>
          <td style={{ padding: '12px 16px' }}><div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><ChevronDown size={14} color={colors.gray400} style={{ transform: expandedId === p.id ? 'rotate(0deg)' : 'rotate(-90deg)', transition: 'transform 0.15s' }} /><div><div style={{ fontWeight: 500, color: colors.gray900 }}>{p.name}</div>{p.description && <div style={{ fontSize: '12px', color: colors.gray500, marginTop: '2px' }}>{p.description}</div>}</div></div></td>
          <td style={{ padding: '12px 16px', color: colors.gray700 }}>{p.provider}</td>
          <td style={{ padding: '12px 16px' }}><span style={{ fontWeight: 500, color: colors.tealPrimary }}>{p.participantCount} facilities</span></td>
          <td style={{ padding: '12px 16px' }}><Badge size="sm" variant={p.isActive ? 'success' : 'default'}>{p.isActive ? 'Active' : 'Inactive'}</Badge></td>
          <td style={{ padding: '12px 16px' }} onClick={e => e.stopPropagation()}><Overflow items={[{ label: 'Edit Program', icon: Edit2, action: 'edit' }, { label: 'Enter New EQA Test', icon: ClipboardList, action: 'test' }, { label: p.isActive ? 'Deactivate' : 'Activate', icon: p.isActive ? XCircle : CheckCircle, action: 'toggle' }]} onSelect={a => { if (a === 'edit') setExpandedId(p.id); if (a === 'test') onEnterTest(p); }} /></td>
        </tr>
        {expandedId === p.id && <tr key={`${p.id}-form`}><td colSpan={5} style={{ padding: 0 }}><InlineMgmtForm program={p} onSave={close} onCancel={close} /></td></tr>}
      </>))}
    </tbody></table></Card>
  </div>);
};

// === EQA MANAGEMENT → PARTICIPANTS (modals kept for multi-select org picker + withdraw confirm) ===
const MgmtParticipantsPage = () => {
  const [selProg, setSelProg] = useState(null); const [search, setSearch] = useState(''); const [statusF, setStatusF] = useState('all'); const [showEnroll, setShowEnroll] = useState(false); const [showWithdraw, setShowWithdraw] = useState(null);
  const enrollments = sampleEnrollments;
  const filtered = enrollments.filter(e => { const ms = !search || e.orgName.toLowerCase().includes(search.toLowerCase()); const mst = statusF === 'all' || e.status.toLowerCase() === statusF; return ms && mst; });
  return (<div style={{ padding: '24px' }}>
    <div style={{ fontSize: '12px', color: colors.gray500, marginBottom: '4px' }}>Home / EQA Management / Participants</div>
    <h2 style={{ margin: '0 0 4px', fontSize: '20px', fontWeight: 600, color: colors.gray900 }}>Participants</h2>
    <p style={{ margin: '0 0 20px', fontSize: '13px', color: colors.gray500 }}>Manage organization enrollment in EQA programs</p>
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px', padding: '12px 16px', backgroundColor: colors.tealLighter, borderRadius: '8px', border: `1px solid ${colors.tealPrimary}30` }}><Award size={16} color={colors.tealPrimary} /><label style={{ fontSize: '13px', fontWeight: 500, color: colors.gray700 }}>Select EQA Program:</label>
      <select value={selProg?.id || ''} onChange={e => setSelProg(sampleManagedPrograms.find(p => p.id === parseInt(e.target.value)))} style={{ height: '36px', padding: '0 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px', backgroundColor: colors.white, minWidth: '300px' }}><option value="">-- Select Program --</option>{sampleManagedPrograms.filter(p => p.isActive).map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select>
    </div>
    {!selProg ? <Card><div style={{ textAlign: 'center', padding: '40px' }}><Users size={40} color={colors.gray300} /><div style={{ fontSize: '15px', fontWeight: 500, color: colors.gray600, marginTop: '12px' }}>Select a program to manage participants</div></div></Card> : <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '0 12px', height: '36px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, backgroundColor: colors.white, flex: 1, maxWidth: '320px' }}><Search size={16} color={colors.gray500} /><input placeholder="Search organizations..." value={search} onChange={e => setSearch(e.target.value)} style={{ border: 'none', outline: 'none', fontSize: '13px', flex: 1, backgroundColor: 'transparent' }} /></div>
        <select value={statusF} onChange={e => setStatusF(e.target.value)} style={{ height: '36px', padding: '0 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px' }}><option value="all">All Status</option><option value="active">Active</option><option value="suspended">Suspended</option><option value="withdrawn">Withdrawn</option></select>
        <div style={{ flex: 1 }} /><Btn icon={Plus} onClick={() => setShowEnroll(true)}>Enroll Participant</Btn>
      </div>
      <Card noPadding><table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}><thead><tr style={{ backgroundColor: colors.gray100 }}>{['Organization Name', 'Code', 'District', 'Enrollment Date', 'Status', 'Actions'].map(h => <th key={h} style={th}>{h}</th>)}</tr></thead><tbody>{filtered.map((e, i) => (
        <tr key={e.id} style={{ borderBottom: `1px solid ${colors.gray200}`, backgroundColor: i % 2 === 0 ? colors.white : colors.gray50 }}>
          <td style={{ padding: '12px 16px' }}><div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><span style={{ fontWeight: 500, color: colors.gray900 }}>{e.orgName}</span>{e.isLocal && <Badge variant="teal" size="sm">This Lab</Badge>}</div></td>
          <td style={{ padding: '12px 16px', fontFamily: 'monospace', fontSize: '12px', color: colors.gray600 }}>{e.orgCode}</td>
          <td style={{ padding: '12px 16px', color: colors.gray700 }}>{e.district}</td>
          <td style={{ padding: '12px 16px', color: colors.gray700 }}>{e.enrollmentDate}</td>
          <td style={{ padding: '12px 16px' }}><Badge size="sm" variant={e.status === 'Active' ? 'success' : e.status === 'Suspended' ? 'warning' : 'error'}>{e.status}</Badge></td>
          <td style={{ padding: '12px 16px' }}><Overflow items={[{ label: 'Suspend', icon: PauseCircle, action: 'suspend', disabled: e.status !== 'Active' }, { label: 'Withdraw', icon: XCircle, action: 'withdraw', danger: true, disabled: e.status === 'Withdrawn' }, { label: 'Reactivate', icon: RotateCcw, action: 'reactivate', disabled: e.status !== 'Suspended' }]} onSelect={a => { if (a === 'withdraw') setShowWithdraw(e); }} /></td>
        </tr>))}</tbody></table></Card>
      {showEnroll && <EnrollOrgModal enrollments={enrollments} onClose={() => setShowEnroll(false)} />}
      {showWithdraw && <WithdrawModal enrollment={showWithdraw} onClose={() => setShowWithdraw(null)} />}
    </>}
  </div>);
};
const EnrollOrgModal = ({ enrollments, onClose }) => {
  const [search, setSearch] = useState(''); const [sel, setSel] = useState(new Set());
  const enrolled = new Set(enrollments.filter(e => e.status === 'Active').map(e => e.organizationId));
  const filtered = sampleOrganizations.filter(o => !search || o.name.toLowerCase().includes(search.toLowerCase()) || o.code.toLowerCase().includes(search.toLowerCase()));
  const toggle = (id) => { if (enrolled.has(id)) return; const n = new Set(sel); if (n.has(id)) n.delete(id); else n.add(id); setSel(n); };
  return (<div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
    <div style={{ backgroundColor: colors.white, borderRadius: '8px', width: '720px', maxHeight: '80vh', display: 'flex', flexDirection: 'column', boxShadow: '0 8px 32px rgba(0,0,0,0.2)' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 24px', borderBottom: `1px solid ${colors.gray200}` }}><h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>Enroll Participant</h3><button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} color={colors.gray600} /></button></div>
      <div style={{ padding: '16px 24px 0' }}><div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '0 12px', height: '36px', borderRadius: '4px', border: `1px solid ${colors.gray300}` }}><Search size={16} color={colors.gray500} /><input placeholder="Search organizations..." value={search} onChange={e => setSearch(e.target.value)} style={{ border: 'none', outline: 'none', fontSize: '13px', flex: 1 }} /></div>{sel.size > 0 && <div style={{ fontSize: '12px', color: colors.tealPrimary, fontWeight: 500, marginTop: '6px' }}>{sel.size} selected</div>}</div>
      <div style={{ flex: 1, overflow: 'auto', padding: '12px 24px' }}><table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}><thead><tr><th style={{ width: '40px', padding: '8px', borderBottom: `1px solid ${colors.gray200}` }}></th>{['Organization Name', 'Code', 'District', 'Type'].map(h => <th key={h} style={{ padding: '8px 12px', textAlign: 'left', borderBottom: `1px solid ${colors.gray200}`, fontSize: '12px', fontWeight: 600, color: colors.gray600 }}>{h}</th>)}</tr></thead><tbody>{filtered.map(o => { const isE = enrolled.has(o.id); const isS = sel.has(o.id); return (
        <tr key={o.id} onClick={() => toggle(o.id)} style={{ cursor: isE ? 'not-allowed' : 'pointer', backgroundColor: isS ? colors.tealLighter : 'transparent', opacity: isE ? 0.5 : 1, borderBottom: `1px solid ${colors.gray200}` }}>
          <td style={{ padding: '8px 12px' }}><div style={{ width: '18px', height: '18px', borderRadius: '3px', border: `2px solid ${isS ? colors.tealPrimary : colors.gray300}`, backgroundColor: isS ? colors.tealPrimary : 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{isS && <Check size={12} color="white" />}</div></td>
          <td style={{ padding: '8px 12px' }}><div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><span style={{ fontWeight: 500 }}>{o.name}</span>{o.isLocal && <Badge variant="teal" size="sm">This Lab</Badge>}{isE && <Badge variant="default" size="sm">Already Enrolled</Badge>}</div></td>
          <td style={{ padding: '8px 12px', fontFamily: 'monospace', fontSize: '12px', color: colors.gray600 }}>{o.code}</td><td style={{ padding: '8px 12px', color: colors.gray700 }}>{o.district}</td><td style={{ padding: '8px 12px', color: colors.gray700 }}>{o.type}</td>
        </tr>); })}</tbody></table></div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', padding: '16px 24px', borderTop: `1px solid ${colors.gray200}` }}><Btn variant="secondary" onClick={onClose}>Cancel</Btn><Btn disabled={sel.size === 0}>Enroll Selected ({sel.size})</Btn></div>
    </div>
  </div>);
};
const WithdrawModal = ({ enrollment, onClose }) => {
  const [reason, setReason] = useState('');
  return (<div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1001 }}>
    <div style={{ backgroundColor: colors.white, borderRadius: '8px', width: '440px', boxShadow: '0 8px 32px rgba(0,0,0,0.2)' }}>
      <div style={{ padding: '16px 24px', borderBottom: `1px solid ${colors.gray200}` }}><h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600, color: colors.red }}>Withdraw Participant</h3></div>
      <div style={{ padding: '24px' }}><div style={{ padding: '12px', backgroundColor: colors.redLight, borderRadius: '4px', fontSize: '13px', marginBottom: '16px', borderLeft: `3px solid ${colors.red}` }}>Are you sure you want to withdraw this organization?<div style={{ marginTop: '8px', fontWeight: 600 }}>{enrollment.orgName} ({enrollment.orgCode})</div></div><label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: colors.gray700, marginBottom: '6px' }}>Reason for Withdrawal</label><textarea value={reason} onChange={e => setReason(e.target.value)} placeholder="Optional reason..." rows={3} style={{ width: '100%', padding: '10px 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '14px', resize: 'vertical', fontFamily: 'inherit', boxSizing: 'border-box' }} /></div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', padding: '16px 24px', borderTop: `1px solid ${colors.gray200}` }}><Btn variant="secondary" onClick={onClose}>Cancel</Btn><Btn variant="danger" onClick={onClose}>Withdraw</Btn></div>
    </div>
  </div>);
};

// === ALERTS PAGE ===
const AlertsPage = () => {
  const [filter, setFilter] = useState('all');
  const alerts = sampleAlerts;
  const filtered = filter === 'all' ? alerts : filter === 'unack' ? alerts.filter(a => !a.acknowledged) : alerts.filter(a => a.type.toLowerCase().replace(/\s/g, '') === filter);
  const crit = alerts.filter(a => a.severity === 'critical' && !a.acknowledged).length;
  const warn = alerts.filter(a => a.severity === 'warning' && !a.acknowledged).length;
  return (<div style={{ padding: '24px' }}>
    <div style={{ fontSize: '12px', color: colors.gray500, marginBottom: '4px' }}>Home / Alerts</div>
    <h2 style={{ margin: '0 0 4px', fontSize: '20px', fontWeight: 600, color: colors.gray900 }}>Alerts</h2>
    <p style={{ margin: '0 0 24px', fontSize: '13px', color: colors.gray500 }}>Laboratory-wide alerts and notifications</p>
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '24px' }}><Tile title="Critical" value={crit} icon={AlertTriangle} color={colors.red} subtitle="Require immediate action" /><Tile title="Warnings" value={warn} icon={AlertCircle} color={colors.yellow} subtitle="Approaching deadlines" /><Tile title="Total Active" value={alerts.filter(a => !a.acknowledged).length} icon={Bell} color={colors.blue} subtitle="Unacknowledged" /></div>
    <select value={filter} onChange={e => setFilter(e.target.value)} style={{ height: '36px', padding: '0 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '13px', marginBottom: '16px' }}><option value="all">All Alerts</option><option value="unack">Unacknowledged</option><option value="eqadeadline">EQA Deadlines</option><option value="statorder">STAT Orders</option><option value="criticalresult">Critical Results</option><option value="qcfailure">QC Failures</option><option value="sampleexpiry">Sample Expiry</option></select>
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>{filtered.map(a => (
      <div key={a.id} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', padding: '16px', backgroundColor: colors.white, borderRadius: '8px', border: `1px solid ${colors.gray200}`, borderLeft: `4px solid ${a.severity === 'critical' ? colors.red : a.severity === 'warning' ? colors.yellow : colors.blue}`, opacity: a.acknowledged ? 0.6 : 1 }}>
        {a.severity === 'critical' ? <AlertTriangle size={18} color={colors.red} /> : a.severity === 'warning' ? <AlertCircle size={18} color="#b8860b" /> : <Bell size={18} color={colors.blue} />}
        <div style={{ flex: 1 }}><div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}><Badge size="sm" variant={a.severity === 'critical' ? 'error' : a.severity === 'warning' ? 'warning' : 'info'}>{a.type}</Badge>{a.acknowledged && <Badge size="sm" variant="default">Acknowledged</Badge>}<span style={{ fontSize: '12px', color: colors.gray500, marginLeft: 'auto' }}>{a.time}</span></div><div style={{ fontSize: '13px', color: colors.gray800 }}>{a.message}</div></div>
        {!a.acknowledged && <Btn variant="ghost" size="sm" icon={Check}>Acknowledge</Btn>}
      </div>
    ))}</div>
  </div>);
};

// === ORDER ENTRY (EQA) ===
const OrderEntryEQA = ({ preSelectedProgram, onBack }) => {
  const [step, setStep] = useState(0);
  const steps = ['Patient Info', 'Program Selection', 'Add Sample', 'Add Order'];
  return (<div style={{ padding: '24px' }}>
    <button onClick={onBack} style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', cursor: 'pointer', fontSize: '13px', color: colors.tealPrimary, marginBottom: '16px' }}>← Back</button>
    <h2 style={{ fontSize: '20px', fontWeight: 600, margin: '0 0 24px', color: colors.gray900 }}>Order Entry — New EQA Test</h2>
    <div style={{ display: 'flex', alignItems: 'center', gap: '4px', marginBottom: '24px', padding: '16px', backgroundColor: colors.gray100, borderRadius: '8px' }}>{steps.map((s, i) => <div key={s} style={{ display: 'flex', alignItems: 'center', flex: 1 }}><div style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }} onClick={() => setStep(i)}><div style={{ width: '28px', height: '28px', borderRadius: '50%', backgroundColor: i < step ? colors.tealPrimary : i === step ? colors.blue : colors.gray400, color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '12px', fontWeight: 600 }}>{i < step ? <Check size={14} /> : i + 1}</div><span style={{ fontSize: '13px', fontWeight: i === step ? 600 : 400, color: i <= step ? colors.gray900 : colors.gray500 }}>{s}</span></div>{i < 3 && <div style={{ flex: 1, height: '2px', marginLeft: '12px', backgroundColor: i < step ? colors.tealPrimary : colors.gray300 }} />}</div>)}</div>
    {step === 0 && <Card title="Step 1: Patient Information"><div style={{ padding: '12px 16px', backgroundColor: colors.tealLighter, borderRadius: '8px', border: `1px solid ${colors.tealPrimary}40`, marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '12px' }}><div style={{ width: '20px', height: '20px', borderRadius: '3px', backgroundColor: colors.tealPrimary, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Check size={14} color="white" /></div><div><span style={{ fontSize: '14px', fontWeight: 600, color: colors.gray900 }}>This is an EQA Order</span><div style={{ fontSize: '12px', color: colors.gray600, marginTop: '2px' }}>Pre-selected from EQA Tests. Patient demographics set to N/A.</div></div></div><div style={{ opacity: 0.5, pointerEvents: 'none' }}><div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>{['Last Name', 'First Name', 'Date of Birth', 'Gender'].map(f => <div key={f}><label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: colors.gray500, marginBottom: '6px' }}>{f}</label><input disabled value="N/A" style={{ width: '100%', padding: '10px 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '14px', backgroundColor: colors.gray100, color: colors.gray500, boxSizing: 'border-box' }} /></div>)}</div></div><div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '20px' }}><Btn onClick={() => setStep(1)}>Next →</Btn></div></Card>}
    {step === 1 && <Card title="Step 2: Program Selection"><div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}><div><label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: colors.gray700, marginBottom: '6px' }}>EQA Program <span style={{ color: colors.red }}>*</span></label><select defaultValue={preSelectedProgram?.id || ''} style={{ width: '100%', padding: '10px 12px', borderRadius: '4px', fontSize: '14px', boxSizing: 'border-box', border: preSelectedProgram ? `2px solid ${colors.tealPrimary}` : `1px solid ${colors.gray300}`, backgroundColor: preSelectedProgram ? colors.tealLighter : colors.white }}><option value="">Select EQA Program...</option>{sampleMyPrograms.filter(p => p.isActive).map(p => <option key={p.id} value={p.id}>{p.name} ({p.provider})</option>)}</select>{preSelectedProgram && <div style={{ fontSize: '12px', color: colors.tealPrimary, marginTop: '4px', fontWeight: 500 }}>Pre-selected from program context</div>}</div><div><label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: colors.gray700, marginBottom: '6px' }}>Provider Sample ID</label><input placeholder="Enter provider's sample ID..." style={{ width: '100%', padding: '10px 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '14px', boxSizing: 'border-box' }} /></div><div><label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: colors.gray700, marginBottom: '6px' }}>Testing Deadline <span style={{ color: colors.red }}>*</span></label><input type="date" style={{ width: '100%', padding: '10px 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '14px', boxSizing: 'border-box' }} /></div><div><label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: colors.gray700, marginBottom: '6px' }}>Priority</label><select defaultValue="standard" style={{ width: '100%', padding: '10px 12px', borderRadius: '4px', border: `1px solid ${colors.gray300}`, fontSize: '14px', boxSizing: 'border-box' }}><option>Standard</option><option>Urgent</option><option>Critical</option></select></div></div><div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '20px' }}><Btn variant="secondary" onClick={() => setStep(0)}>← Back</Btn><Btn onClick={() => setStep(2)}>Next →</Btn></div></Card>}
    {step >= 2 && <Card title={`Step ${step + 1}: ${steps[step]}`}><div style={{ textAlign: 'center', padding: '40px' }}><ClipboardList size={40} color={colors.gray300} /><div style={{ fontSize: '14px', color: colors.gray600, marginTop: '12px' }}>Standard order entry workflow continues here{preSelectedProgram && <div style={{ marginTop: '8px', color: colors.tealPrimary, fontWeight: 500 }}>Pre-populated tests/panels from "{preSelectedProgram.name}" enrollment would appear here</div>}</div></div><div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '20px' }}><Btn variant="secondary" onClick={() => setStep(step - 1)}>← Back</Btn>{step < 3 ? <Btn onClick={() => setStep(step + 1)}>Next →</Btn> : <Btn onClick={onBack} icon={Check}>Save Order</Btn>}</div></Card>}
  </div>);
};

// === PLACEHOLDER ===
const Placeholder = ({ title, breadcrumb, description, icon: Icon }) => (<div style={{ padding: '24px' }}><div style={{ fontSize: '12px', color: colors.gray500, marginBottom: '4px' }}>{breadcrumb}</div><h2 style={{ margin: '0 0 4px', fontSize: '20px', fontWeight: 600, color: colors.gray900 }}>{title}</h2><p style={{ margin: '0 0 24px', fontSize: '13px', color: colors.gray500 }}>{description}</p><Card><div style={{ textAlign: 'center', padding: '60px' }}>{Icon && <Icon size={48} color={colors.gray300} />}<div style={{ fontSize: '15px', fontWeight: 500, color: colors.gray600, marginTop: '16px' }}>{title}</div><div style={{ fontSize: '13px', color: colors.gray400, marginTop: '4px' }}>Covered in parent EQA FRS</div></div></Card></div>);

// === MAIN ===
export default function EQAModule() {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [view, setView] = useState('eqa-my-programs');
  const [showOE, setShowOE] = useState(false);
  const [oeProg, setOeProg] = useState(null);
  const alertCount = sampleAlerts.filter(a => a.severity === 'critical' && !a.acknowledged).length;
  const nav = (v) => { setView(v); setShowOE(false); };
  const enterTest = (prog) => { setOeProg(prog || null); setShowOE(true); };
  const content = () => {
    if (showOE) return <OrderEntryEQA preSelectedProgram={oeProg} onBack={() => setShowOE(false)} />;
    switch (view) {
      case 'eqa-orders': return <EQAOrdersPage onEnterTest={enterTest} />;
      case 'eqa-my-programs': return <MyProgramsPage />;
      case 'mgmt-programs': return <MgmtProgramsPage onEnterTest={enterTest} />;
      case 'mgmt-participants': return <MgmtParticipantsPage />;
      case 'mgmt-distributions': return <Placeholder title="Distributions" breadcrumb="Home / EQA Management / Distributions" description="Create, track, and manage EQA sample distributions" icon={Truck} />;
      case 'mgmt-results': return <Placeholder title="Results & Analysis" breadcrumb="Home / EQA Management / Results & Analysis" description="Collect participant results and perform statistical analysis" icon={BarChart3} />;
      case 'alerts': return <AlertsPage />;
      default: return <div style={{ padding: '24px' }}><Card><div style={{ textAlign: 'center', padding: '60px' }}><Microscope size={48} color={colors.gray300} /><h2 style={{ marginTop: '16px', color: colors.gray700 }}>Welcome to OpenELIS Global</h2><p style={{ color: colors.gray500 }}>Navigate using the sidebar</p></div></Card></div>;
    }
  };
  return (<div style={{ display: 'flex', flexDirection: 'column', height: '100vh', fontFamily: "'IBM Plex Sans', -apple-system, BlinkMacSystemFont, sans-serif" }}>
    <Header onMenuClick={() => setSidebarOpen(!sidebarOpen)} />
    <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}><Sidebar isOpen={sidebarOpen} currentView={view} onNavigate={nav} alertCount={alertCount} /><main style={{ flex: 1, overflow: 'auto', backgroundColor: colors.gray50 }}>{content()}</main></div>
  </div>);
}
