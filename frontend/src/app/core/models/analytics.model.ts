export interface KpiAggregationResponse {
  id: number;
  projectId: number;
  projectName: string;
  calculationTimestamp: string;
  aggregationPeriod: string;
  scrapRate: number;
  availability: number;
  performance: number;
  quality: number;
  oee: number;
  customerSatisfaction: number;
  maintenanceDowntime: number;
}
