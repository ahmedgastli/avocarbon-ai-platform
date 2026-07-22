export type ProjectStatus = 'CREATED' | 'IN_PROGRESS' | 'GENERATED' | 'DEPLOYED' | 'FAILED';

export interface ProjectRequest {
  name: string;
  description?: string;
  ownerId: number;
  startDate: string;
  siteId: string;
}

export interface ProjectResponse {
  id: number;
  name: string;
  description: string;
  status: ProjectStatus;
  ownerId: number;
  ownerName: string;
  startDate: string;
  siteId: string;
  createdAt: string;
  updatedAt: string;
}
