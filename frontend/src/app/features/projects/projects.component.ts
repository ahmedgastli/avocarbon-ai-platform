import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../../core/services/project.service';
import { AuthService } from '../../core/services/auth.service';
import { ProjectRequest, ProjectResponse, ProjectStatus } from '../../core/models/project.model';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { MessageService, ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DialogModule,
    TagModule,
    ToastModule,
    ConfirmDialogModule
  ],
  providers: [MessageService, ConfirmationService],
  template: `
    <div class="space-y-6 pb-8">
      <p-toast></p-toast>
      <p-confirmDialog header="Confirmation" icon="pi pi-exclamation-triangle"></p-confirmDialog>

      <!-- Header Toolbar -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <div>
          <h1 class="text-2xl font-black text-[#155A8A] tracking-tight">Project Portfolio Management</h1>
          <p class="text-xs text-[#666666] mt-1">Manage industrial carbon tracking and scrap reduction projects</p>
        </div>

        <button pButton label="New Project" icon="pi pi-plus" class="p-button-accent" (click)="openCreateDialog()"></button>
      </div>

      <!-- Projects Card Table -->
      <div class="bg-white rounded-3xl p-6 shadow-sm border border-gray-100">
        <p-table 
          [value]="projects()" 
          [paginator]="true" 
          [rows]="10" 
          [loading]="loading()"
          [globalFilterFields]="['name', 'description', 'ownerName', 'status']"
          #dt
          styleClass="p-datatable-striped">
          
          <ng-template pTemplate="caption">
            <div class="flex items-center justify-between pb-3">
              <span class="text-sm font-bold text-[#155A8A]">Active Portfolio Projects ({{ projects().length }})</span>
              <div class="p-inputgroup w-72">
                <span class="p-inputgroup-addon bg-gray-50 text-gray-400"><i class="pi pi-search"></i></span>
                <input pInputText type="text" (input)="dt.filterGlobal($any($event.target).value, 'contains')" placeholder="Filter projects..." class="p-inputtext-sm text-xs" />
              </div>
            </div>
          </ng-template>

          <ng-template pTemplate="header">
            <tr>
              <th pSortableColumn="id">ID <p-sortIcon field="id"></p-sortIcon></th>
              <th pSortableColumn="name">Project Name <p-sortIcon field="name"></p-sortIcon></th>
              <th>Description</th>
              <th pSortableColumn="ownerName">Owner <p-sortIcon field="ownerName"></p-sortIcon></th>
              <th pSortableColumn="status">Status <p-sortIcon field="status"></p-sortIcon></th>
              <th pSortableColumn="startDate">Start Date <p-sortIcon field="startDate"></p-sortIcon></th>
              <th class="text-right">Actions</th>
            </tr>
          </ng-template>

          <ng-template pTemplate="body" let-project>
            <tr>
              <td class="font-bold text-gray-400">#{{ project.id }}</td>
              <td class="font-bold text-[#155A8A]">{{ project.name }}</td>
              <td class="text-xs text-gray-600 max-w-xs truncate">{{ project.description || 'N/A' }}</td>
              <td class="text-xs font-semibold text-gray-700">
                <i class="pi pi-user mr-1 text-[#0076C8]"></i> {{ project.ownerName || 'Admin' }}
              </td>
              <td>
                <p-tag [value]="project.status" [severity]="getStatusSeverity(project.status)"></p-tag>
              </td>
              <td class="text-xs text-gray-500 font-medium">{{ project.startDate | date:'mediumDate' }}</td>
              <td class="text-right space-x-2">
                <button pButton icon="pi pi-pencil" class="p-button-rounded p-button-text p-button-info p-button-sm" (click)="openEditDialog(project)"></button>
                <button pButton icon="pi pi-trash" class="p-button-rounded p-button-text p-button-danger p-button-sm" (click)="confirmDelete(project)"></button>
              </td>
            </tr>
          </ng-template>

          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="7" class="text-center p-12 text-gray-400 text-sm font-medium">
                No projects found. Click "New Project" to register a plant project.
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <!-- Create / Edit Modal Dialog -->
      <p-dialog [(visible)]="displayDialog" [header]="isEditMode ? 'Edit Project' : 'Create New Project'" [modal]="true" styleClass="w-full max-w-lg p-fluid rounded-3xl">
        <ng-template pTemplate="content">
          <div class="space-y-4 pt-2">
            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Project Name *</label>
              <input pInputText [(ngModel)]="currentRequest.name" placeholder="e.g. Line 2 Optimization" required />
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Description</label>
              <textarea pInputText [(ngModel)]="currentRequest.description" rows="3" placeholder="Objective, expected OEE target..."></textarea>
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Owner User ID *</label>
              <input pInputText type="number" [(ngModel)]="currentRequest.ownerId" required />
            </div>
          </div>
        </ng-template>

        <ng-template pTemplate="footer">
          <button pButton label="Cancel" icon="pi pi-times" class="p-button-text" (click)="displayDialog = false"></button>
          <button pButton [label]="isEditMode ? 'Save Changes' : 'Create Project'" icon="pi pi-check" class="p-button-accent" (click)="saveProject()"></button>
        </ng-template>
      </p-dialog>
    </div>
  `
})
export class ProjectsComponent implements OnInit {
  private projectService = inject(ProjectService);
  private authService = inject(AuthService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  projects = signal<ProjectResponse[]>([]);
  loading = signal<boolean>(true);

  displayDialog = false;
  isEditMode = false;
  selectedProjectId: number | null = null;

  currentRequest: ProjectRequest = {
    name: '',
    description: '',
    ownerId: 1,
    startDate: new Date().toISOString()
  };

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading.set(true);
    this.projectService.getAllProjects().subscribe({
      next: (data) => {
        this.projects.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openCreateDialog(): void {
    this.isEditMode = false;
    this.selectedProjectId = null;
    const currentUser = this.authService.currentUserSignal();
    this.currentRequest = {
      name: '',
      description: '',
      ownerId: currentUser?.id || 1,
      startDate: new Date().toISOString()
    };
    this.displayDialog = true;
  }

  openEditDialog(project: ProjectResponse): void {
    this.isEditMode = true;
    this.selectedProjectId = project.id;
    this.currentRequest = {
      name: project.name,
      description: project.description,
      ownerId: project.ownerId,
      startDate: project.startDate
    };
    this.displayDialog = true;
  }

  saveProject(): void {
    if (!this.currentRequest.name) {
      this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: 'Project name is required' });
      return;
    }

    if (this.isEditMode && this.selectedProjectId) {
      this.projectService.updateProject(this.selectedProjectId, this.currentRequest).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Project updated successfully' });
          this.displayDialog = false;
          this.loadProjects();
        },
        error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to update project' })
      });
    } else {
      this.projectService.createProject(this.currentRequest).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Project created successfully' });
          this.displayDialog = false;
          this.loadProjects();
        },
        error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to create project' })
      });
    }
  }

  confirmDelete(project: ProjectResponse): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete project '${project.name}'?`,
      accept: () => {
        this.projectService.deleteProject(project.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Deleted', detail: 'Project deleted successfully' });
            this.loadProjects();
          },
          error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to delete project' })
        });
      }
    });
  }

  getStatusSeverity(status: ProjectStatus): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (status) {
      case 'DEPLOYED': return 'success';
      case 'IN_PROGRESS': return 'info';
      case 'CREATED': return 'secondary';
      case 'GENERATED': return 'warn';
      case 'FAILED': return 'danger';
      default: return 'info';
    }
  }
}
