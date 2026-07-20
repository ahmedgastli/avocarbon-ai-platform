import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { KpiAggregationResponse } from '../models/analytics.model';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/analytics';

  getKpiSummary(projectId: number): Observable<KpiAggregationResponse> {
    return this.http.get<KpiAggregationResponse>(`${this.apiUrl}/projects/${projectId}/summary`);
  }

  getKpiHistory(projectId: number): Observable<KpiAggregationResponse[]> {
    return this.http.get<KpiAggregationResponse[]>(`${this.apiUrl}/projects/${projectId}/history`);
  }
}
