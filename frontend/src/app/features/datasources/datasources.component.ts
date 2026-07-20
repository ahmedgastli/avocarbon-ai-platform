import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataSourceService } from '../../core/services/datasource.service';
import { ProjectService } from '../../core/services/project.service';
import { DataSourceRequest, DataSourceResponse, DataSourceType, SyncFrequency } from '../../core/models/datasource.model';
import { ProjectResponse } from '../../core/models/project.model';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-datasources',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    DialogModule,
    DropdownModule,
    TagModule,
    ToastModule
  ],
  providers: [MessageService],
  template: `
    <div class="space-y-6 pb-8">
      <p-toast></p-toast>

      <!-- Header Toolbar Card -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <div>
          <h1 class="text-2xl font-black text-[#155A8A] tracking-tight">API Data Source Connectors</h1>
          <p class="text-xs text-[#666666] mt-1">Manage external REST API integrations for automated OEE & scrap metrics</p>
        </div>

        <div class="flex items-center space-x-3">
          <p-dropdown 
            [options]="projects()" 
            optionLabel="name" 
            optionValue="id" 
            [(ngModel)]="selectedProjectId" 
            (onChange)="loadDataSources()"
            placeholder="Select Project"
            styleClass="w-64 p-inputtext-sm rounded-xl">
          </p-dropdown>
          <button pButton label="Add Data Source" icon="pi pi-plus" class="p-button-accent" (click)="openCreateDialog()"></button>
        </div>
      </div>

      <!-- Data Source Cards Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        @for (ds of dataSources(); track ds.id) {
          <div class="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 flex flex-col justify-between hover:shadow-xl hover:-translate-y-1 transition-all duration-300 relative group">
            <div>
              <div class="flex items-center justify-between mb-4">
                <p-tag [value]="ds.type" [severity]="getTypeSeverity(ds.type)"></p-tag>
                <span class="text-[11px] font-bold text-gray-400 uppercase flex items-center bg-gray-50 px-2 py-0.5 rounded-md border border-gray-100">
                  <i class="pi pi-sync mr-1 text-[10px]"></i> {{ ds.syncFrequency }}
                </span>
              </div>

              <h3 class="text-lg font-black text-[#155A8A] mb-1 group-hover:text-[#0076C8] transition-colors">{{ ds.name }}</h3>
              <p class="text-xs text-gray-500 font-mono bg-gray-50 p-2.5 rounded-xl border border-gray-200 truncate mb-4">{{ ds.url }}</p>
            </div>

            <!-- Card Actions -->
            <div class="pt-4 border-t border-gray-100 flex items-center justify-between">
              <div class="flex items-center space-x-2 text-xs text-emerald-600 font-bold">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                <span>Active</span>
              </div>

              <button pButton 
                      [label]="syncingId() === ds.id ? 'Syncing...' : 'Sync Now'" 
                      [icon]="syncingId() === ds.id ? 'pi pi-spin pi-spinner' : 'pi pi-refresh'" 
                      class="p-button-sm p-button-outlined p-button-primary"
                      [disabled]="syncingId() === ds.id"
                      (click)="triggerSync(ds)"></button>
            </div>
          </div>
        } @empty {
          <div class="col-span-full bg-white p-16 rounded-3xl shadow-sm border border-gray-100 text-center text-gray-400">
            <i class="pi pi-database text-5xl mb-3 text-gray-300"></i>
            <p class="text-base font-bold text-[#155A8A]">No Data Sources registered for this project.</p>
            <p class="text-xs mt-1 text-[#666666]">Click "Add Data Source" to connect an internal API.</p>
          </div>
        }
      </div>

      <!-- Modal Dialog -->
      <p-dialog [(visible)]="displayDialog" header="Add API Data Source" [modal]="true" styleClass="w-full max-w-lg p-fluid rounded-3xl">
        <ng-template pTemplate="content">
          <div class="space-y-4 pt-2">
            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Connector Name *</label>
              <input pInputText [(ngModel)]="currentRequest.name" placeholder="e.g. Line 1 Production API" required />
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
          <button pButton label="Add Connector" icon="pi pi-check" class="p-button-accent" (click)="saveDataSource()"></button>
        </ng-template>
      </p-dialog>
    </div>
  `
})
export class DataSourcesComponent implements OnInit {
  private dataSourceService = inject(DataSourceService);
  private projectService = inject(ProjectService);
  private messageService = inject(MessageService);

  projects = signal<ProjectResponse[]>([]);
  selectedProjectId = 1;
  dataSources = signal<DataSourceResponse[]>([]);
  syncingId = signal<number | null>(null);

  displayDialog = false;

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

  currentRequest: DataSourceRequest = {
    name: '',
    url: '',
    token: '',
    type: 'PRODUCTION',
    syncFrequency: 'DAILY'
  };

  ngOnInit(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projs) => {
        this.projects.set(projs);
        if (projs.length > 0) {
          this.selectedProjectId = projs[0].id;
        }
        this.loadDataSources();
      }
    });
  }

  loadDataSources(): void {
    this.dataSourceService.getDataSourcesByProject(this.selectedProjectId).subscribe({
      next: (dsList) => this.dataSources.set(dsList),
      error: () => this.dataSources.set([])
    });
  }

  openCreateDialog(): void {
    this.currentRequest = {
      name: '',
      url: 'http://mock-api.avocarbon.com/prod',
      token: 'mock-token-secret',
      type: 'PRODUCTION',
      syncFrequency: 'DAILY'
    };
    this.displayDialog = true;
  }

  saveDataSource(): void {
    if (!this.currentRequest.name || !this.currentRequest.url) {
      this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: 'Name and URL are required' });
      return;
    }

    this.dataSourceService.createDataSource(this.selectedProjectId, this.currentRequest).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Data source added successfully' });
        this.displayDialog = false;
        this.loadDataSources();
      },
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to add data source' })
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
}
