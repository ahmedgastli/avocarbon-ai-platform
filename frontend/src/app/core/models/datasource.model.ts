export type DataSourceType = 'PRODUCTION' | 'QUALITY' | 'CUSTOMER' | 'MAINTENANCE';
export type SyncFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'MANUAL';

export interface DataSourceRequest {
  name: string;
  url: string;
  token: string;
  type: DataSourceType;
  syncFrequency: SyncFrequency;
  siteId: string;
}

export interface DataSourceResponse {
  id: number;
  name: string;
  url: string;
  token: string;
  type: DataSourceType;
  syncFrequency: SyncFrequency;
  projectId: number;
  projectName: string;
  siteId: string;
  createdAt: string;
  updatedAt: string;
}
