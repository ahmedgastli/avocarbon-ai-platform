import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataSourceService } from '../../core/services/datasource.service';
import { ProjectService } from '../../core/services/project.service';
import { AuthService } from '../../core/services/auth.service';
import { SiteFilterService } from '../../core/services/site-filter.service';
import { DataSourceRequest, DataSourceResponse, DataSourceType, SyncFrequency } from '../../core/models/datasource.model';
import { ProjectResponse } from '../../core/models/project.model';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';

@Component({
  selector: 'app-datasources',
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
    TooltipModule,
    ConfirmDialogModule
  ],
  providers: [MessageService, ConfirmationService],
  template: `
    <div class="space-y-6 pb-8">
      <p-toast></p-toast>
      <p-confirmDialog header="Delete Confirmation" icon="pi pi-exclamation-triangle" appendTo="body"></p-confirmDialog>

      <!-- Header Toolbar Card -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <div>
          <h1 class="text-2xl font-black text-[#155A8A] tracking-tight">API Data Source Connectors</h1>
          <p class="text-xs text-[#666666] mt-1">Manage external REST API integrations for automated OEE & scrap metrics</p>
        </div>

        <div class="flex items-center space-x-3">
          <label class="text-xs font-bold text-[#155A8A] uppercase tracking-wider hidden sm:inline">Project Scope:</label>
          <p-dropdown 
            [options]="projects()" 
            optionLabel="name" 
            optionValue="id" 
            [(ngModel)]="selectedProjectId" 
            (onChange)="loadDataSources()"
            placeholder="Select Project"
            appendTo="body"
            styleClass="w-64 p-inputtext-sm rounded-xl"
            [disabled]="projects().length === 0">
          </p-dropdown>
          <button pButton label="Add Data Source" icon="pi pi-plus" class="p-button-accent" (click)="openCreateDialog()" [disabled]="projects().length === 0"></button>
        </div>
      </div>

      <!-- Data Source Table Card -->
      <div class="bg-white rounded-3xl p-6 shadow-sm border border-gray-100">
        <p-table 
          [value]="dataSources()" 
          [paginator]="true" 
          [rows]="10" 
          [loading]="loading()"
          [globalFilterFields]="['name', 'url', 'type', 'siteId']"
          #dt
          styleClass="p-datatable-striped">
          
          <ng-template pTemplate="caption">
            <div class="flex items-center justify-between pb-3">
              <span class="text-sm font-bold text-[#155A8A]">Active API Connectors ({{ dataSources().length }})</span>
              <div class="relative w-72">
                <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs pointer-events-none"></i>
                <input pInputText type="text" (input)="dt.filterGlobal($any($event.target).value, 'contains')" placeholder="Search connectors..." class="w-full pl-9 pr-3 py-1.5 bg-gray-50 text-xs rounded-xl border border-gray-200 focus:bg-white focus:border-[#0076C8]" />
              </div>
            </div>
          </ng-template>

          <ng-template pTemplate="header">
            <tr>
              <th pSortableColumn="name">Connector Name <p-sortIcon field="name"></p-sortIcon></th>
              <th pSortableColumn="siteId">Industrial Site <p-sortIcon field="siteId"></p-sortIcon></th>
              <th pSortableColumn="type">API Type <p-sortIcon field="type"></p-sortIcon></th>
              <th>Endpoint URL</th>
              <th>Connection Status</th>
              <th>Last Synchronization</th>
              <th pSortableColumn="createdAt">Created Date <p-sortIcon field="createdAt"></p-sortIcon></th>
              <th class="text-right">Actions</th>
            </tr>
          </ng-template>

          <ng-template pTemplate="body" let-ds>
            <tr>
              <td class="font-bold text-[#155A8A]">{{ ds.name }}</td>
              <td>
                <span class="inline-flex items-center px-2 py-0.5 rounded-xl text-xs font-black bg-[#155A8A]/10 text-[#155A8A] border border-[#155A8A]/20">
                  🏭 {{ getSiteLabel(ds.siteId) }}
                </span>
              </td>
              <td>
                <p-tag [value]="ds.type" [severity]="getTypeSeverity(ds.type)"></p-tag>
              </td>
              <td>
                <span class="text-xs font-mono text-gray-600 bg-gray-50 px-2 py-1 rounded-lg border border-gray-200 block max-w-xs truncate" [title]="ds.url">
                  {{ ds.url }}
                </span>
              </td>
              <td>
                <div class="flex items-center space-x-1.5 text-xs text-emerald-600 font-black">
                  <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                  <span>Active</span>
                </div>
              </td>
              <td class="text-xs text-gray-600 font-medium">
                {{ ds.updatedAt ? (ds.updatedAt | date:'medium') : 'Never' }}
              </td>
              <td class="text-xs text-gray-500 font-medium">
                {{ ds.createdAt | date:'mediumDate' }}
              </td>
              <td class="text-right space-x-2 whitespace-nowrap">
                <button pButton 
                        icon="pi pi-pencil" 
                        class="p-button-rounded p-button-text p-button-info p-button-sm"
                        pTooltip="Edit Connector" 
                        tooltipPosition="top"
                        (click)="openEditDialog(ds)"
                        [disabled]="deletingId() === ds.id || syncingId() === ds.id"></button>

                <button pButton 
                        [icon]="syncingId() === ds.id ? 'pi pi-spin pi-spinner' : 'pi pi-sync'" 
                        class="p-button-rounded p-button-text p-button-success p-button-sm"
                        pTooltip="Sync Connector" 
                        tooltipPosition="top"
                        (click)="triggerSync(ds)"
                        [disabled]="deletingId() === ds.id || syncingId() === ds.id"></button>

                <button pButton 
                        [icon]="deletingId() === ds.id ? 'pi pi-spin pi-spinner' : 'pi pi-trash'" 
                        class="p-button-rounded p-button-text p-button-danger p-button-sm"
                        pTooltip="Delete Connector" 
                        tooltipPosition="top"
                        (click)="confirmDelete(ds)"
                        [disabled]="deletingId() === ds.id || syncingId() === ds.id"></button>
              </td>
            </tr>
          </ng-template>

          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="8" class="text-center p-12 text-gray-400 text-sm font-medium">
                <i class="pi pi-database text-4xl mb-3 text-gray-300 block"></i>
                <span>No API Data Source Connectors registered for this project.</span>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <!-- Add / Edit Modal Dialog -->
      <p-dialog [(visible)]="displayDialog" [header]="isEditMode ? 'Edit API Data Source' : 'Add API Data Source'" [modal]="true" appendTo="body" styleClass="w-full max-w-lg p-fluid rounded-3xl">
        <ng-template pTemplate="content">
          <div class="space-y-4 pt-2">
            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Connector Name *</label>
              <input pInputText [(ngModel)]="currentRequest.name" placeholder="e.g. Line 1 Production API" required />
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
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">API Endpoint URL *</label>
              <input pInputText [(ngModel)]="currentRequest.url" placeholder="http://mock-api.avocarbon.com/prod/line1" required />
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Bearer Token *</label>
              <input pInputText [(ngModel)]="currentRequest.token" placeholder="secret-api-token" required />
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">API Type *</label>
              <p-dropdown [options]="typeOptions" optionLabel="label" optionValue="value" [(ngModel)]="currentRequest.type" placeholder="Select Type" appendTo="body" styleClass="w-full"></p-dropdown>
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Sync Frequency *</label>
              <p-dropdown [options]="frequencyOptions" optionLabel="label" optionValue="value" [(ngModel)]="currentRequest.syncFrequency" placeholder="Select Frequency" appendTo="body" styleClass="w-full"></p-dropdown>
            </div>
          </div>
        </ng-template>

        <ng-template pTemplate="footer">
          <button pButton label="Cancel" icon="pi pi-times" class="p-button-text" (click)="displayDialog = false"></button>
          <button pButton [label]="isEditMode ? 'Save Connector' : 'Add Connector'" icon="pi pi-check" class="p-button-accent" (click)="saveDataSource()"></button>
        </ng-template>
      </p-dialog>
    </div>
  `
})
export class DataSourcesComponent implements OnInit {
  private dataSourceService = inject(DataSourceService);
  private projectService = inject(ProjectService);
  authService = inject(AuthService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  siteFilterService = inject(SiteFilterService);

  projects = signal<ProjectResponse[]>([]);
  selectedProjectId = 0;
  dataSources = signal<DataSourceResponse[]>([]);
  syncingId = signal<number | null>(null);
  deletingId = signal<number | null>(null);
  loading = signal<boolean>(false);

  displayDialog = false;
  isEditMode = false;
  selectedDataSourceId: number | null = null;

  typeOptions = [
    { label: 'PRODUCTION', value: 'PRODUCTION' },
    { label: 'QUALITY', value: 'QUALITY' },
    { label: 'CUSTOMER', value: 'CUSTOMER' },
    { label: 'MAINTENANCE', value: 'MAINTENANCE' }
  ];

  frequencyOptions = [
    { label: 'DAILY', value: 'DAILY' },
    { label: 'WEEKLY', value: 'WEEKLY' },
    { label: 'MONTHLY', value: 'MONTHLY' },
    { label: 'MANUAL', value: 'MANUAL' }
  ];

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

  currentRequest: DataSourceRequest = {
    name: '',
    url: '',
    token: '',
    type: 'PRODUCTION',
    syncFrequency: 'DAILY',
    siteId: ''
  };

  constructor() {
    effect(() => {
      // Reload projects list to match the active site filter!
      this.siteFilterService.selectedSiteId();
      this.reloadProjects();
    });
  }

  ngOnInit(): void {
    this.reloadProjects();
  }

  reloadProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projs) => {
        const activeSite = this.siteFilterService.selectedSiteId();
        let filtered = projs;
        if (activeSite) {
          filtered = projs.filter(p => p.siteId === activeSite);
        }
        filtered = filtered.filter(p => this.authService.isSiteAuthorized(p.siteId));

        this.projects.set(filtered);
        if (filtered.length > 0) {
          if (!filtered.some(p => p.id === this.selectedProjectId)) {
            this.selectedProjectId = filtered[0].id;
          }
          this.loadDataSources();
        } else {
          this.selectedProjectId = 0;
          this.dataSources.set([]);
        }
      }
    });
  }

  loadDataSources(): void {
    if (this.selectedProjectId === 0) {
      this.dataSources.set([]);
      return;
    }
    this.loading.set(true);
    this.dataSourceService.getDataSourcesByProject(this.selectedProjectId).subscribe({
      next: (dsList) => {
        const activeSite = this.siteFilterService.selectedSiteId();
        let filtered = dsList;
        if (activeSite) {
          filtered = dsList.filter(ds => ds.siteId === activeSite);
        }
        filtered = filtered.filter(ds => this.authService.isSiteAuthorized(ds.siteId));
        this.dataSources.set(filtered);
        this.loading.set(false);
      },
      error: () => {
        this.dataSources.set([]);
        this.loading.set(false);
      }
    });
  }

  getAuthorizedSiteOptions() {
    return this.siteOptions.filter(opt => this.authService.isSiteAuthorized(opt.value));
  }

  openCreateDialog(): void {
    this.isEditMode = false;
    this.selectedDataSourceId = null;
    const selectedProj = this.projects().find(p => p.id === this.selectedProjectId);
    const authorizedSites = this.getAuthorizedSiteOptions();
    
    this.currentRequest = {
      name: '',
      url: 'http://mock-api.avocarbon.com/prod',
      token: 'mock-token-secret',
      type: 'PRODUCTION',
      syncFrequency: 'DAILY',
      siteId: selectedProj ? selectedProj.siteId : (authorizedSites.length > 0 ? authorizedSites[0].value : '')
    };
    this.displayDialog = true;
  }

  openEditDialog(ds: DataSourceResponse): void {
    this.isEditMode = true;
    this.selectedDataSourceId = ds.id;
    this.currentRequest = {
      name: ds.name,
      url: ds.url,
      token: ds.token,
      type: ds.type,
      syncFrequency: ds.syncFrequency,
      siteId: ds.siteId
    };
    this.displayDialog = true;
  }

  saveDataSource(): void {
    if (!this.currentRequest.name || this.currentRequest.name.trim() === '') {
      this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: 'Connector name is required' });
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

    if (!this.currentRequest.url || !this.currentRequest.url.startsWith('http')) {
      this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: 'Valid API URL starting with http:// or https:// is required' });
      return;
    }

    if (this.isEditMode && this.selectedDataSourceId) {
      this.dataSourceService.updateDataSource(this.selectedDataSourceId, this.currentRequest).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Connector Saved', detail: 'API Data source connector updated successfully' });
          this.displayDialog = false;
          this.loadDataSources();
        },
        error: (err) => this.messageService.add({ severity: 'error', summary: 'Save Error', detail: err.error?.message || 'Failed to update data source' })
      });
    } else {
      this.dataSourceService.createDataSource(this.selectedProjectId, this.currentRequest).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Connector Added', detail: 'API Data source connector added successfully' });
          this.displayDialog = false;
          this.loadDataSources();
        },
        error: (err) => this.messageService.add({ severity: 'error', summary: 'Creation Error', detail: err.error?.message || 'Failed to add data source' })
      });
    }
  }

  confirmDelete(ds: DataSourceResponse): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this API Data Source Connector? This action cannot be undone.',
      header: 'Delete Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.deletingId.set(ds.id);
        this.dataSourceService.deleteDataSource(ds.id).subscribe({
          next: () => {
            this.deletingId.set(null);
            this.messageService.add({
              severity: 'success',
              summary: 'Deleted Successfully',
              detail: 'API Data Source Connector deleted successfully.'
            });
            this.loadDataSources();
          },
          error: (err) => {
            this.deletingId.set(null);
            this.messageService.add({
              severity: 'error',
              summary: 'Delete Failed',
              detail: err.error?.message || 'Failed to delete data source connector'
            });
          }
        });
      }
    });
  }

  triggerSync(ds: DataSourceResponse): void {
    this.syncingId.set(ds.id);
    this.dataSourceService.syncDataSource(ds.id).subscribe({
      next: (count) => {
        this.syncingId.set(null);
        this.messageService.add({ 
          severity: 'success', 
          summary: 'Synchronization Complete', 
          detail: `Successfully imported ${count} data points from ${ds.name}` 
        });
        this.loadDataSources();
      },
      error: (err) => {
        this.syncingId.set(null);
        this.messageService.add({ severity: 'error', summary: 'Sync Failed', detail: err.error?.message || 'Synchronization error' });
      }
    });
  }

  getTypeSeverity(type: DataSourceType): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (type) {
      case 'PRODUCTION': return 'info';
      case 'QUALITY': return 'warn';
      case 'CUSTOMER': return 'success';
      case 'MAINTENANCE': return 'danger';
      default: return 'secondary';
    }
  }

  getSiteLabel(siteId: string): string {
    const site = this.siteOptions.find(s => s.value === siteId);
    return site ? site.label.replace('AVOCarbon ', '').replace(' (Head Office)', '') : siteId;
  }
}
