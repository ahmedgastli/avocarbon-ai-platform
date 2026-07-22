export type Role = 'ADMIN' | 'PRODUCTION_MANAGER' | 'QUALITY_MANAGER' | 'DIRECTION';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  enabled?: boolean;
  assignedSites?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginRequest {
  email: string;
  password?: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface UserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password?: string;
  role: Role;
  assignedSites?: string[];
}

export interface UserResponse extends User {}
