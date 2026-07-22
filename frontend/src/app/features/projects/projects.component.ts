import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../../core/services/project.service';
import { AuthService } from '../../core/services/auth.service';
import { SiteFilterService } from '../../core/services/site-filter.service';
import { ProjectRequest, ProjectResponse, ProjectStatus } from '../../core/models/project.model';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
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
    DropdownModule,
    TagModule,
    ToastModule,
    ConfirmDialogModule
  ],
  providers: [MessageService, ConfirmationService],
  template: `
    <div class="space-y-6 pb-8">
      <p-toast></p-toast>
      <p-confirmDialog header="Delete Confirmation" icon="pi pi-exclamation-triangle" appendTo="body"></p-confirmDialog>

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
          [globalFilterFields]="['id', 'name', 'description', 'ownerName', 'status', 'siteId']"
          #dt
          styleClass="p-datatable-striped">
          
          <ng-template pTemplate="caption">
            <div class="flex items-center justify-between pb-3">
              <span class="text-sm font-bold text-[#155A8A]">Active Portfolio Projects ({{ projects().length }})</span>
              <div class="relative w-72">
                <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs pointer-events-none"></i>
                <input pInputText type="text" (input)="dt.filterGlobal($any($event.target).value, 'contains')" placeholder="Filter all columns..." class="w-full pl-9 pr-3 py-1.5 bg-gray-50 text-xs rounded-xl border border-gray-200 focus:bg-white focus:border-[#0076C8]" />
              </div>
            </div>
          </ng-template>

          <ng-template pTemplate="header">
            <tr>
              <th pSortableColumn="name">Project Name <p-sortIcon field="name"></p-sortIcon></th>
              <th pSortableColumn="siteId">Industrial Site <p-sortIcon field="siteId"></p-sortIcon></th>
              <th>Description</th>
              <th pSortableColumn="ownerName">Owner <p-sortIcon field="ownerName"></p-sortIcon></th>
              <th pSortableColumn="status">Status <p-sortIcon field="status"></p-sortIcon></th>
              <th pSortableColumn="startDate">Start Date <p-sortIcon field="startDate"></p-sortIcon></th>
              <th class="text-right">Actions</th>
            </tr>
          </ng-template>

          <ng-template pTemplate="body" let-project>
            <tr>
              <td class="font-bold text-[#155A8A]">{{ project.name }}</td>
              <td>
                <span class="inline-flex items-center px-2 py-0.5 rounded-xl text-xs font-black bg-[#155A8A]/10 text-[#155A8A] border border-[#155A8A]/20">
                  🏭 {{ getSiteLabel(project.siteId) }}
                </span>
              </td>
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
              <td colspan="8" class="text-center p-12 text-gray-400 text-sm font-medium">
                No projects found. Click "New Project" to register a plant project.
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <!-- Create / Edit Modal Dialog -->
      <p-dialog [(visible)]="displayDialog" [header]="isEditMode ? 'Edit Project' : 'Create New Project'" [modal]="true" appendTo="body" styleClass="w-full max-w-lg p-fluid rounded-3xl">
        <ng-template pTemplate="content">
          <div class="space-y-4 pt-2">
            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Project Name *</label>
              <input pInputText [(ngModel)]="currentRequest.name" placeholder="e.g. Line 2 Optimization" required />
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Industrial Site Scope *</label>
              <p-dropdown [options]="getAuthorizedSiteOptions()" 
                          optionLabel="label" 
                          optionValue="value" 
                          [(ngModel)]="currentRequest.siteId" 
                          placeholder="Select Plant Site"
                          appendTo="body"
                          styleClass="w-full">
              </p-dropdown>
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Description</label>
              <textarea pInputText [(ngModel)]="currentRequest.description" rows="3" placeholder="Objective, expected OEE target..."></textarea>
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Owner User ID *</label>
              <input pInputText type="number" [(ngModel)]="currentRequest.ownerId" required min="1" />
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
  authService = inject(AuthService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  siteFilterService = inject(SiteFilterService);

  projects = signal<ProjectResponse[]>([]);
  loading = signal<boolean>(true);

  displayDialog = false;
  isEditMode = false;
  selectedProjectId: number | null = null;

  siteOptions = [
    { label: 'AVOCarbon Luxembourg (Head Office)', value: 'luxembourg' },
    { label: 'AVOCarbon Tunisia', value: 'tunisia' },
    { label: 'AVOCarbon France - Poitiers', value: 'france-poitiers' },
    { label: 'AVOCarbon France - Amiens', value: 'france-amiens' },
    { label: 'AVOCarbon Germany', value: 'germany' },
    { label: 'AVOCarbon India', value: 'india' },
    { label: 'AVOCarbon China - Tianjin', value: 'china-tianjin' },
    { label: 'AVOCarbon China - Kunshan', value: 'china-kunshan' },
    { label: 'AVOCarbon Korea', value: 'korea' },
    { label: 'AVOCarbon Mexico', value: 'mexico' }
  ];

  currentRequest: ProjectRequest = {
    name: '',
    description: '',
    ownerId: 1,
    startDate: new Date().toISOString(),
    siteId: ''
  };

  constructor() {
    effect(() => {
      // Reload projects automatically whenever the active filter site changes
      this.siteFilterService.selectedSiteId();
      this.loadProjects();
    });
  }

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading.set(true);
    this.projectService.getAllProjects().subscribe({
      next: (data) => {
        const activeSite = this.siteFilterService.selectedSiteId();
        let filtered = data;
        
        // Scope filter
        if (activeSite) {
          filtered = data.filter(p => p.siteId === activeSite);
        }
        
        // User authorization filter
        filtered = filtered.filter(p => this.authService.isSiteAuthorized(p.siteId));

        this.projects.set(filtered);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to load projects' });
      }
    });
  }

  getAuthorizedSiteOptions() {
    return this.siteOptions.filter(opt => this.authService.isSiteAuthorized(opt.value));
  }

  openCreateDialog(): void {
    this.isEditMode = false;
    this.selectedProjectId = null;
    const currentUser = this.authService.currentUserSignal();
    const authorizedSites = this.getAuthorizedSiteOptions();
    
    this.currentRequest = {
      name: '',
      description: '',
      ownerId: currentUser?.id || 1,
      startDate: new Date().toISOString(),
      siteId: authorizedSites.length > 0 ? authorizedSites[0].value : ''
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
      startDate: project.startDate,
      siteId: project.siteId
    };
    this.displayDialog = true;
  }

  saveProject(): void {
    if (!this.currentRequest.name || this.currentRequest.name.trim() === '') {
      this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: 'Project name is required' });
      return;
    }

    if (!this.currentRequest.siteId || this.currentRequest.siteId.trim() === '') {
      this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: 'Site selection is required' });
      return;
    }

    if (!this.authService.isSiteAuthorized(this.currentRequest.siteId)) {
      this.messageService.add({ severity: 'error', summary: 'Unauthorized Site', detail: 'You do not have access permissions for this site' });
      return;
    }

    if (!this.currentRequest.ownerId || this.currentRequest.ownerId < 1) {
      this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: 'Valid Owner User ID is required' });
      return;
    }

    if (this.isEditMode && this.selectedProjectId) {
      this.projectService.updateProject(this.selectedProjectId, this.currentRequest).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Project Updated', detail: 'Project updated successfully' });
          this.displayDialog = false;
          this.loadProjects();
        },
        error: (err) => this.messageService.add({ severity: 'error', summary: 'Update Error', detail: err.error?.message || 'Failed to update project' })
      });
    } else {
      this.projectService.createProject(this.currentRequest).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Project Created', detail: 'Project created successfully' });
          this.displayDialog = false;
          this.loadProjects();
        },
        error: (err) => this.messageService.add({ severity: 'error', summary: 'Creation Error', detail: err.error?.message || 'Failed to create project' })
      });
    }
  }

  confirmDelete(project: ProjectResponse): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete project '${project.name}'?`,
      accept: () => {
        this.projectService.deleteProject(project.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Project Deleted', detail: 'Project deleted successfully' });
            this.loadProjects();
          },
          error: (err) => this.messageService.add({ severity: 'error', summary: 'Delete Error', detail: err.error?.message || 'Failed to delete project' })
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

  getSiteLabel(siteId: string): string {
    const site = this.siteOptions.find(s => s.value === siteId);
    return site ? site.label.replace('AVOCarbon ', '').replace(' (Head Office)', '') : siteId;
  }
}
