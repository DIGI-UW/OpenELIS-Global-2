const BASE = "/rest/dashboards/management";

export const summaryUrl = (scope) => `${BASE}/summary?scope=${scope}`;

export const kpisUrl = (scope) => `${BASE}/kpis?scope=${scope}`;
export const workloadUrl = (scope) => `${BASE}/workload?scope=${scope}`;
export const equipmentUrl = (scope) => `${BASE}/equipment?scope=${scope}`;
export const antibiogramUrl = (scope) => `${BASE}/antibiogram?scope=${scope}`;
export const stockUrl = () => `${BASE}/stock`;
export const qualityWeightsUrl = () => `${BASE}/quality-weights`;

export const qualityUrl = (scope) => `${BASE}/quality?scope=${scope}`;
