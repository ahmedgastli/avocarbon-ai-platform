import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProjectRequest, ProjectResponse } from '../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/projects';

  getAllProjects(): Observable<ProjectResponse[]> {
    return this.http.get<ProjectResponse[]>(this.apiUrl);
  }

  getProjectById(id: number): Observable<ProjectResponse> {
    return this.http.get<ProjectResponse>(`${this.apiUrl}/${id}`);
  }

  createProject(request: ProjectRequest): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.apiUrl, request);
  }

  updateProject(id: number, request: ProjectRequest): Observable<ProjectResponse> {
    return this.http.put<ProjectResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
