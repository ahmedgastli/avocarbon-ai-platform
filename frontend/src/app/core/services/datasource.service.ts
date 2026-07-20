import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DataSourceRequest, DataSourceResponse } from '../models/datasource.model';

@Injectable({
  providedIn: 'root'
})
export class DataSourceService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api';

  getDataSourcesByProject(projectId: number): Observable<DataSourceResponse[]> {
    return this.http.get<DataSourceResponse[]>(`${this.apiUrl}/projects/${projectId}/datasources`);
  }

  getDataSourceById(id: number): Observable<DataSourceResponse> {
    return this.http.get<DataSourceResponse>(`${this.apiUrl}/datasources/${id}`);
  }

  createDataSource(projectId: number, request: DataSourceRequest): Observable<DataSourceResponse> {
    return this.http.post<DataSourceResponse>(`${this.apiUrl}/projects/${projectId}/datasources`, request);
  }

  updateDataSource(id: number, request: DataSourceRequest): Observable<DataSourceResponse> {
    return this.http.put<DataSourceResponse>(`${this.apiUrl}/datasources/${id}`, request);
  }

  deleteDataSource(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/datasources/${id}`);
  }

  syncDataSource(id: number): Observable<number> {
    return this.http.post<number>(`${this.apiUrl}/datasources/${id}/sync`, {});
  }
}
