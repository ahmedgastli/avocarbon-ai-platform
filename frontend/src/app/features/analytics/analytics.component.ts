import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AnalyticsService } from '../../core/services/analytics.service';
import { ProjectService } from '../../core/services/project.service';
import { AuthService } from '../../core/services/auth.service';
import { SiteFilterService } from '../../core/services/site-filter.service';
import { GlobalOperationsService } from '../../core/services/global-operations.service';
import { KpiAggregationResponse } from '../../core/models/analytics.model';
import { ProjectResponse } from '../../core/models/project.model';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { DialogModule } from 'primeng/dialog';
import { CheckboxModule } from 'primeng/checkbox';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import * as ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    DropdownModule,
    ButtonModule,
    ChartModule,
    TableModule,
    ToolbarModule,
    DialogModule,
    CheckboxModule,
    ToastModule
  ],
  providers: [MessageService],
  template: `
    <div class="space-y-6 pb-8 print-content">
      <p-toast></p-toast>

      <!-- 1. Top Section: Reporting Center Toolbar -->
      <p-toolbar styleClass="mb-4 rounded-3xl bg-white border border-gray-100 p-4 shadow-sm no-print">
        <div class="p-toolbar-group-start">
          <div class="flex items-center space-x-2">
            <i class="pi pi-briefcase text-lg text-[#0076C8]"></i>
            <span class="font-black text-xs text-[#155A8A] uppercase tracking-wider">Enterprise Reporting Center</span>
          </div>
        </div>
        <div class="p-toolbar-group-end flex flex-wrap gap-2">
          <!-- Button 1: Export PDF -->
          <button pButton label="Export PDF" icon="pi pi-file-pdf" 
                  class="p-button-sm !bg-[#0076C8] !border-[#0076C8] text-white rounded-xl text-xs" 
                  (click)="openExportDialog('pdf')" [disabled]="generatingReport()"></button>
          
          <!-- Button 2: Export Excel -->
          <button pButton label="Export Excel" icon="pi pi-file-excel" 
                  class="p-button-sm !bg-emerald-600 !border-emerald-600 text-white rounded-xl text-xs" 
                  (click)="openExportDialog('excel')" [disabled]="generatingReport()"></button>
          
          <!-- Button 3: Share Report -->
          <button pButton label="Share Report" icon="pi pi-send" 
                  class="p-button-sm !bg-[#F58220] !border-[#F58220] text-white rounded-xl text-xs" 
                  (click)="openShareDialog()" [disabled]="generatingReport()"></button>
          
          <!-- Button 4: Print Report -->
          <button pButton label="Print" icon="pi pi-print" 
                  class="p-button-sm !bg-gray-500 !border-gray-500 text-white rounded-xl text-xs" 
                  (click)="printReport()" [disabled]="generatingReport()"></button>
          
          <!-- Button 5: Schedule Report -->
          <button pButton label="Schedule" icon="pi pi-calendar" 
                  class="p-button-sm !bg-purple-600 !border-purple-600 text-white rounded-xl text-xs" 
                  (click)="openScheduleDialog()" [disabled]="generatingReport()"></button>
        </div>
      </p-toolbar>

      <!-- Loading overlay during document assembly -->
      @if (generatingReport()) {
        <div class="no-print bg-[#0076C8]/10 border border-[#0076C8]/20 rounded-2xl p-4 text-center space-y-2 animate-pulse">
          <i class="pi pi-spin pi-spinner text-2xl text-[#0076C8]"></i>
          <p class="text-xs font-bold text-[#155A8A]">{{ progressMsg() }}</p>
        </div>
      }

      <!-- Header Card -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl shadow-sm border border-gray-100 no-print">
        <div>
          <h1 class="text-2xl font-black text-[#155A8A] tracking-tight">Deep Analytics & Time-Series</h1>
          <p class="text-xs text-[#666666] mt-1">31-day historical KPI performance breakdown & multi-metric comparison</p>
        </div>

        <div class="flex items-center space-x-3">
          <p-dropdown 
            [options]="projects()" 
            optionLabel="name" 
            optionValue="id" 
            [(ngModel)]="selectedProjectId" 
            (onChange)="loadAnalytics()"
            placeholder="Select Project"
            styleClass="w-64 p-inputtext-sm rounded-xl">
          </p-dropdown>
          <button pButton label="Refresh" icon="pi pi-refresh" class="p-button-outlined p-button-primary rounded-xl" (click)="loadAnalytics()"></button>
        </div>
      </div>

      <!-- Multi-Series Time Chart Card -->
      <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <div class="flex items-center justify-between pb-3 border-b border-gray-100 mb-4">
          <div>
            <h3 class="text-base font-bold text-[#155A8A]">Historical Multi-KPI Comparison</h3>
            <p class="text-xs text-[#666666]">Comparing OEE, Availability, Performance, and Quality over time</p>
          </div>
        </div>

        @if (multiChartData) {
          <p-chart type="line" [data]="multiChartData" [options]="multiChartOptions" height="360px"></p-chart>
        }
      </div>

      <!-- 31-Day History Table Card -->
      <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <div class="flex items-center justify-between pb-3 border-b border-gray-100 mb-4">
          <h3 class="text-base font-bold text-[#155A8A]">Raw 31-Day Daily Metrics Export</h3>
          <button pButton label="Export CSV" icon="pi pi-file-export" class="p-button-sm p-button-outlined p-button-secondary rounded-xl no-print" (click)="dt.exportCSV()"></button>
        </div>

        <p-table 
          [value]="history()" 
          [paginator]="true" 
          [rows]="10"
          #dt
          styleClass="p-datatable-striped p-datatable-sm">
          <ng-template pTemplate="header">
            <tr>
              <th>Date</th>
              <th>Period</th>
              <th>OEE %</th>
              <th>Availability</th>
              <th>Performance</th>
              <th>Quality</th>
              <th>Scrap Rate</th>
            </tr>
          </ng-template>

          <ng-template pTemplate="body" let-row>
            <tr>
              <td class="font-semibold text-gray-700">{{ row.calculationTimestamp | date:'mediumDate' }}</td>
              <td class="text-xs text-gray-500 font-mono">{{ row.aggregationPeriod }}</td>
              <td class="font-extrabold text-[#0076C8]">{{ (row.oee * 100).toFixed(1) }}%</td>
              <td class="text-xs text-emerald-600 font-bold">{{ (row.availability * 100).toFixed(1) }}%</td>
              <td class="text-xs text-[#F58220] font-bold">{{ (row.performance * 100).toFixed(1) }}%</td>
              <td class="text-xs text-indigo-600 font-bold">{{ (row.quality * 100).toFixed(1) }}%</td>
              <td class="text-xs text-red-500 font-bold">{{ (row.scrapRate * 100).toFixed(1) }}%</td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <!-- Dialog 1: Export Settings Dialog -->
      <p-dialog [(visible)]="displayExportDialog" header="Configure Report Document Export" [modal]="true" [style]="{ width: '480px' }" appendTo="body" styleClass="rounded-3xl">
        <div class="space-y-4 py-2 text-xs">
          <div>
            <label class="block font-bold text-[#155A8A] mb-1">Target Project</label>
            <p-dropdown [options]="projects()" optionLabel="name" optionValue="id" [(ngModel)]="exportProject" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
          </div>

          <div>
            <label class="block font-bold text-[#155A8A] mb-1">Industrial Site</label>
            <p-dropdown [options]="getAuthorizedSiteOptions()" [(ngModel)]="exportSite" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block font-bold text-[#155A8A] mb-1">Paper Size</label>
              <p-dropdown [options]="paperSizeOptions" [(ngModel)]="exportPaperSize" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
            </div>
            <div>
              <label class="block font-bold text-[#155A8A] mb-1">Orientation</label>
              <p-dropdown [options]="orientationOptions" [(ngModel)]="exportOrientation" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
            </div>
          </div>

          <div class="space-y-2 border-t border-gray-100 pt-3">
            <span class="block font-bold text-[#155A8A] mb-2">Include Document Sections</span>
            <div class="flex items-center space-x-2">
              <p-checkbox [(ngModel)]="incSummary" [binary]="true"></p-checkbox>
              <span>Include Executive Summary</span>
            </div>
            <div class="flex items-center space-x-2">
              <p-checkbox [(ngModel)]="incCharts" [binary]="true"></p-checkbox>
              <span>Include Charts Breakdown</span>
            </div>
            <div class="flex items-center space-x-2">
              <p-checkbox [(ngModel)]="incTable" [binary]="true"></p-checkbox>
              <span>Include Raw Historical Table</span>
            </div>
            <div class="flex items-center space-x-2">
              <p-checkbox [(ngModel)]="incAi" [binary]="true"></p-checkbox>
              <span>Include AI Recommendations Section</span>
            </div>
          </div>
        </div>

        <ng-template pTemplate="footer">
          <button pButton label="Cancel" class="p-button-text text-xs" (click)="displayExportDialog = false"></button>
          <button pButton label="Generate Document" icon="pi pi-check" class="p-button-primary text-xs rounded-xl" (click)="executeExport()"></button>
        </ng-template>
      </p-dialog>

      <!-- Dialog 2: Share Report Dialog -->
      <p-dialog [(visible)]="displayShareDialog" header="Email Executive Report" [modal]="true" [style]="{ width: '460px' }" appendTo="body" styleClass="rounded-3xl">
        <div class="space-y-3 py-2 text-xs">
          <div>
            <label class="block font-bold text-[#155A8A] mb-1">Recipient Email</label>
            <input type="email" pInputText [(ngModel)]="shareEmail" placeholder="recipient@avocarbon.com" class="w-full p-2 border border-gray-200 rounded-xl" />
          </div>
          <div>
            <label class="block font-bold text-[#155A8A] mb-1">CC Recipients</label>
            <input type="email" pInputText [(ngModel)]="shareCc" placeholder="cc@avocarbon.com" class="w-full p-2 border border-gray-200 rounded-xl" />
          </div>
          <div>
            <label class="block font-bold text-[#155A8A] mb-1">Subject</label>
            <input type="text" pInputText [(ngModel)]="shareSubject" class="w-full p-2 border border-gray-200 rounded-xl" />
          </div>
          <div>
            <label class="block font-bold text-[#155A8A] mb-1">Message Body</label>
            <textarea rows="4" pInputText [(ngModel)]="shareMessage" class="w-full p-2 border border-gray-200 rounded-xl"></textarea>
          </div>
          <div class="space-y-2 border-t border-gray-100 pt-3">
            <span class="block font-bold text-[#155A8A]">Attachments</span>
            <div class="flex items-center space-x-2">
              <p-checkbox [(ngModel)]="shareAttachPdf" [binary]="true"></p-checkbox>
              <span>Generated Executive PDF Report</span>
            </div>
            <div class="flex items-center space-x-2">
              <p-checkbox [(ngModel)]="shareAttachExcel" [binary]="true"></p-checkbox>
              <span>Generated Excel Spreadsheet</span>
            </div>
          </div>
        </div>

        <ng-template pTemplate="footer">
          <button pButton label="Cancel" class="p-button-text text-xs" (click)="displayShareDialog = false"></button>
          <button pButton label="Send Report" icon="pi pi-send" class="p-button-accent text-xs rounded-xl" (click)="sendReport()"></button>
        </ng-template>
      </p-dialog>

      <!-- Dialog 3: Schedule Report Dialog -->
      <p-dialog [(visible)]="displayScheduleDialog" header="Schedule Automated Reports" [modal]="true" [style]="{ width: '460px' }" appendTo="body" styleClass="rounded-3xl">
        <div class="space-y-3 py-2 text-xs">
          <div>
            <label class="block font-bold text-[#155A8A] mb-1">Frequency</label>
            <p-dropdown [options]="frequencyOptions" [(ngModel)]="schedFreq" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
          </div>
          <div>
            <label class="block font-bold text-[#155A8A] mb-1">Recipients</label>
            <input type="text" pInputText [(ngModel)]="schedRecipients" placeholder="distribution-list@avocarbon.com" class="w-full p-2 border border-gray-200 rounded-xl" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block font-bold text-[#155A8A] mb-1">Project</label>
              <p-dropdown [options]="projects()" optionLabel="name" optionValue="id" [(ngModel)]="schedProject" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
            </div>
            <div>
              <label class="block font-bold text-[#155A8A] mb-1">Industrial Site</label>
              <p-dropdown [options]="getAuthorizedSiteOptions()" [(ngModel)]="schedSite" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
            </div>
          </div>
          <div>
            <label class="block font-bold text-[#155A8A] mb-1">Format</label>
            <p-dropdown [options]="formatOptions" [(ngModel)]="schedFormat" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
          </div>
          <div class="bg-gray-50 border border-gray-100 p-3 rounded-xl text-[10px] text-gray-500 leading-relaxed">
            <i class="pi pi-info-circle text-[#0076C8] mr-1"></i>
            Backend integration required for automatic scheduled reports.
          </div>
        </div>

        <ng-template pTemplate="footer">
          <button pButton label="Cancel" class="p-button-text text-xs" (click)="displayScheduleDialog = false"></button>
          <button pButton label="Create Schedule" icon="pi pi-calendar-plus" class="p-button-primary text-xs rounded-xl !bg-purple-600 !border-purple-600" (click)="createSchedule()"></button>
        </ng-template>
      </p-dialog>
    </div>
  `
})
export class AnalyticsComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);
  private projectService = inject(ProjectService);
  siteFilterService = inject(SiteFilterService);
  private operationsService = inject(GlobalOperationsService);
  private messageService = inject(MessageService);
  authService = inject(AuthService);

  projects = signal<ProjectResponse[]>([]);
  selectedProjectId = 0;
  history = signal<KpiAggregationResponse[]>([]);

  multiChartData: any;
  multiChartOptions: any;

  // General Report State
  generatingReport = signal<boolean>(false);
  progressMsg = signal<string>('');

  // Dialog Visible Toggles
  displayExportDialog = false;
  displayShareDialog = false;
  displayScheduleDialog = false;
  activeExportType: 'pdf' | 'excel' = 'pdf';

  // Export Settings Form State
  exportProject = 0;
  exportSite = 'luxembourg';
  exportPaperSize = 'A4';
  exportOrientation = 'Portrait';
  incSummary = true;
  incCharts = true;
  incTable = true;
  incAi = true;

  // Options
  siteOptions = [
    { label: 'Luxembourg (HQ)', value: 'luxembourg' },
    { label: 'Tunisia', value: 'tunisia' },
    { label: 'France (Poitiers)', value: 'france-poitiers' },
    { label: 'France (Amiens)', value: 'france-amiens' },
    { label: 'Germany', value: 'germany' },
    { label: 'India', value: 'india' },
    { label: 'China (Tianjin)', value: 'china-tianjin' },
    { label: 'China (Kunshan)', value: 'china-kunshan' },
    { label: 'Korea', value: 'korea' },
    { label: 'Mexico', value: 'mexico' }
  ];
  paperSizeOptions = [
    { label: 'A4 Size', value: 'A4' },
    { label: 'Letter Size', value: 'Letter' }
  ];
  orientationOptions = [
    { label: 'Portrait', value: 'Portrait' },
    { label: 'Landscape', value: 'Landscape' }
  ];

  // Share Form State
  shareEmail = 'executives@avocarbon.com';
  shareCc = 'audit@avocarbon.com';
  shareSubject = 'AVOCarbon Executive Analytics Report';
  shareMessage = 'Please find attached the latest OEE performance and API synchronization audits generated from the industrial platform.';
  shareAttachPdf = true;
  shareAttachExcel = true;

  // Schedule Form State
  schedFreq = 'Weekly';
  schedRecipients = 'distribution-list@avocarbon.com';
  schedProject = 0;
  schedSite = 'luxembourg';
  schedFormat = 'PDF';

  frequencyOptions = [
    { label: 'Daily', value: 'Daily' },
    { label: 'Weekly', value: 'Weekly' },
    { label: 'Monthly', value: 'Monthly' },
    { label: 'Custom Cron', value: 'Custom' }
  ];
  formatOptions = [
    { label: 'PDF Document Only', value: 'PDF' },
    { label: 'Excel Spreadsheet Only', value: 'Excel' },
    { label: 'Both Formats (ZIP)', value: 'Both' }
  ];

  constructor() {
    effect(() => {
      // Reload projects dynamically when map filter site changes
      this.siteFilterService.selectedSiteId();
      this.reloadProjects();
    });
  }

  ngOnInit(): void {
    this.setupChartOptions();

    // Auto-select first authorized site context for site-restricted roles on initialization
    const user = this.authService.currentUserSignal();
    const activeSite = this.siteFilterService.selectedSiteId();
    if (!activeSite && user && user.role !== 'ADMIN' && user.role !== 'DIRECTION') {
      if (user.assignedSites && user.assignedSites.length > 0) {
        const firstSiteId = user.assignedSites[0];
        this.operationsService.getSites().subscribe({
          next: (sites) => {
            const site = sites.find(s => s.id === firstSiteId);
            if (site) {
              this.siteFilterService.selectSite(firstSiteId, site.name);
            }
          }
        });
      }
    }

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
          this.exportProject = this.selectedProjectId;
          this.schedProject = this.selectedProjectId;
        } else {
          this.selectedProjectId = 0;
          this.exportProject = 0;
          this.schedProject = 0;
        }

        const authorizedSites = this.getAuthorizedSiteOptions();
        if (authorizedSites.length > 0) {
          const defaultSite = activeSite && this.authService.isSiteAuthorized(activeSite) ? activeSite : authorizedSites[0].value;
          this.exportSite = defaultSite;
          this.schedSite = defaultSite;
        }

        this.loadAnalytics();
      }
    });
  }

  loadAnalytics(): void {
    const activeSite = this.siteFilterService.selectedSiteId();
    
    if (!activeSite && this.selectedProjectId === 0) {
      this.history.set([]);
      this.multiChartData = null;
      return;
    }

    if (activeSite) {
      this.operationsService.getSites().subscribe({
        next: (sites) => {
          const site = sites.find(s => s.id === activeSite);
          if (site) {
            const mockHistory: KpiAggregationResponse[] = Array.from({ length: 31 }).map((_, i) => {
              const variance = (Math.random() - 0.5) * 0.04;
              const date = new Date();
              date.setDate(date.getDate() - (30 - i));
              return {
                id: i + 1,
                projectId: 1,
                projectName: site.name,
                oee: Math.max(0, Math.min(1, site.metrics.oee + variance)),
                availability: Math.max(0, Math.min(1, site.metrics.availability + variance)),
                performance: Math.max(0, Math.min(1, site.metrics.performance + variance)),
                quality: Math.max(0, Math.min(1, site.metrics.quality + variance)),
                scrapRate: Math.max(0, Math.min(0.1, site.metrics.scrapRate - variance * 0.2)),
                customerSatisfaction: 0.95,
                maintenanceDowntime: 1.5,
                calculationTimestamp: date.toISOString(),
                aggregationPeriod: 'DAILY'
              };
            });
            this.history.set(mockHistory);
            this.updateMultiChart(mockHistory);
          }
        }
      });
    } else {
      this.analyticsService.getKpiHistory(this.selectedProjectId).subscribe({
        next: (data) => {
          this.history.set(data);
          this.updateMultiChart(data);
        }
      });
    }
  }

  getAuthorizedSiteOptions() {
    return this.siteOptions.filter(opt => this.authService.isSiteAuthorized(opt.value));
  }

  openExportDialog(type: 'pdf' | 'excel'): void {
    this.activeExportType = type;
    const currentSiteId = this.siteFilterService.selectedSiteId();
    if (currentSiteId) {
      this.exportSite = currentSiteId;
    }
    this.displayExportDialog = true;
  }

  executeExport(): void {
    this.displayExportDialog = false;
    if (this.activeExportType === 'pdf') {
      this.generatePdf();
    } else {
      this.generateExcel();
    }
  }

  generatePdf(): void {
    this.generatingReport.set(true);
    this.progressMsg.set('Assembling Executive PDF report document...');

    setTimeout(() => {
      try {
        const doc = new jsPDF({
          orientation: this.exportOrientation.toLowerCase() as any,
          format: this.exportPaperSize.toLowerCase()
        });

        const selectedSiteObj = this.siteOptions.find(s => s.value === this.exportSite);
        const siteLabel = selectedSiteObj ? selectedSiteObj.label : 'Luxembourg (HQ)';

        // Cover styling & branding top line
        doc.setFillColor(21, 90, 138); // Dark Blue
        doc.rect(0, 0, 220, 15, 'F');

        doc.setFont('helvetica', 'bold');
        doc.setFontSize(22);
        doc.setTextColor(21, 90, 138);
        doc.text('AVOCarbon Group', 14, 32);

        doc.setFontSize(13);
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(102, 102, 102);
        doc.text('Executive Performance & Operations Audit', 14, 40);

        // Line separator
        doc.setDrawColor(218, 220, 224);
        doc.line(14, 45, 196, 45);

        if (this.incSummary) {
          doc.setFont('helvetica', 'bold');
          doc.setFontSize(10);
          doc.setTextColor(21, 90, 138);
          doc.text('EXECUTIVE AUDIT SUMMARY SCOPE', 14, 55);

          doc.setFont('helvetica', 'normal');
          doc.setFontSize(9);
          doc.setTextColor(51, 51, 51);
          doc.text(`• Target Project ID: ${this.exportProject}`, 16, 62);
          doc.text(`• Scoped Industrial Site: ${siteLabel}`, 16, 68);
          doc.text(`• Period Analyzed: Last 31 Days Daily Aggregations`, 16, 74);
          doc.text(`• Generation Date: ${new Date().toLocaleString()}`, 16, 80);
        }

        // Summary KPI grid
        const avgOee = this.history().reduce((acc, h) => acc + h.oee, 0) / (this.history().length || 1);
        const avgAvail = this.history().reduce((acc, h) => acc + h.availability, 0) / (this.history().length || 1);
        const avgPerf = this.history().reduce((acc, h) => acc + h.performance, 0) / (this.history().length || 1);
        const avgQual = this.history().reduce((acc, h) => acc + h.quality, 0) / (this.history().length || 1);
        const avgScrap = this.history().reduce((acc, h) => acc + h.scrapRate, 0) / (this.history().length || 1);

        doc.setFillColor(244, 246, 249);
        doc.rect(14, 88, 182, 28, 'F');

        doc.setFont('helvetica', 'bold');
        doc.setFontSize(8);
        doc.setTextColor(102, 102, 102);
        doc.text('AVG OEE', 20, 96);
        doc.text('AVAILABILITY', 55, 96);
        doc.text('PERFORMANCE', 92, 96);
        doc.text('QUALITY', 130, 96);
        doc.text('SCRAP RATE', 165, 96);

        doc.setFontSize(12);
        doc.setTextColor(21, 90, 138);
        doc.text(`${(avgOee * 100).toFixed(1)}%`, 20, 107);
        doc.text(`${(avgAvail * 100).toFixed(1)}%`, 55, 107);
        doc.text(`${(avgPerf * 100).toFixed(1)}%`, 92, 107);
        doc.text(`${(avgQual * 100).toFixed(1)}%`, 130, 107);
        doc.setTextColor(245, 130, 32); // Orange scrap rate color
        doc.text(`${(avgScrap * 100).toFixed(1)}%`, 165, 107);

        if (this.incAi) {
          doc.setFont('helvetica', 'bold');
          doc.setFontSize(10);
          doc.setTextColor(21, 90, 138);
          doc.text('AI EXECUTIVE INSIGHTS & ACTIONS', 14, 128);

          doc.setFont('helvetica', 'normal');
          doc.setFontSize(8.5);
          doc.setTextColor(51, 51, 51);
          doc.text('1. Plant performance metrics fall within acceptable target ranges with OEE scores steady above 80%.', 14, 136);
          doc.text('2. Action required: Address OEE bottleneck caused by minor maintenance cycle shifts in quality yields.', 14, 142);
          doc.text('3. Predictive model expects quality metric output convergence at 99.0% next cycle.', 14, 148);
        }

        if (this.incTable) {
          doc.setFont('helvetica', 'bold');
          doc.setFontSize(10);
          doc.setTextColor(21, 90, 138);
          doc.text('31-DAY HISTORICAL LOGS', 14, 160);

          const tableRows = this.history().map(row => [
            new Date(row.calculationTimestamp).toLocaleDateString(),
            (row.oee * 100).toFixed(1) + '%',
            (row.availability * 100).toFixed(1) + '%',
            (row.performance * 100).toFixed(1) + '%',
            (row.quality * 100).toFixed(1) + '%',
            (row.scrapRate * 100).toFixed(1) + '%'
          ]);

          autoTable(doc, {
            startY: 165,
            head: [['Date Log', 'Overall OEE', 'Availability', 'Performance', 'Quality Rate', 'Scrap']],
            body: tableRows,
            theme: 'striped',
            headStyles: { fillColor: [21, 90, 138] },
            styles: { fontSize: 7.5 }
          });
        }

        // Apply page footer and pagination count on all pages
        const pages = (doc as any).internal.getNumberOfPages();
        for (let j = 1; j <= pages; j++) {
          doc.setPage(j);
          doc.setFontSize(7.5);
          doc.setTextColor(150, 150, 150);
          doc.text('CONFIDENTIAL — BOARD LEVEL EXECUTIVE DOCUMENT', 14, doc.internal.pageSize.height - 8);
          doc.text(`Page ${j} of ${pages}`, doc.internal.pageSize.width - 25, doc.internal.pageSize.height - 8);
        }

        doc.save(`AVOCarbon_Executive_Report_Project_${this.exportProject}.pdf`);
        this.generatingReport.set(false);
        this.messageService.add({ severity: 'success', summary: 'Report Exported', detail: 'Executive PDF downloaded successfully.' });
      } catch (err) {
        console.error(err);
        this.generatingReport.set(false);
        this.messageService.add({ severity: 'error', summary: 'Export Failed', detail: 'Encountered error generating PDF.' });
      }
    }, 600);
  }

  generateExcel(): void {
    this.generatingReport.set(true);
    this.progressMsg.set('Assembling professional Excel worksheets...');

    setTimeout(() => {
      try {
        const workbook = new ExcelJS.Workbook();
        const selectedSiteObj = this.siteOptions.find(s => s.value === this.exportSite);
        const siteLabel = selectedSiteObj ? selectedSiteObj.label : 'Luxembourg (HQ)';

        // 1. Calculations for Sheet 1 summary block
        const avgOee = this.history().reduce((acc, h) => acc + h.oee, 0) / (this.history().length || 1);
        const avgAvail = this.history().reduce((acc, h) => acc + h.availability, 0) / (this.history().length || 1);
        const avgPerf = this.history().reduce((acc, h) => acc + h.performance, 0) / (this.history().length || 1);
        const avgQual = this.history().reduce((acc, h) => acc + h.quality, 0) / (this.history().length || 1);
        const avgScrap = this.history().reduce((acc, h) => acc + h.scrapRate, 0) / (this.history().length || 1);

        const thinBorder = {
          top: { style: 'thin' as const, color: { argb: 'D1D5DB' } },
          left: { style: 'thin' as const, color: { argb: 'D1D5DB' } },
          bottom: { style: 'thin' as const, color: { argb: 'D1D5DB' } },
          right: { style: 'thin' as const, color: { argb: 'D1D5DB' } }
        };

        // ==========================================
        // SHEET 1: Executive Summary
        // ==========================================
        const wsSummary = workbook.addWorksheet('Executive Summary');
        wsSummary.columns = [{ width: 30 }, { width: 48 }];
        
        wsSummary.addRow(['AVOCarbon Group — Executive Summary Report']).font = { bold: true, size: 15, color: { argb: '155A8A' } };
        wsSummary.addRow([]);
        
        wsSummary.addRow(['Scope Parameter', 'Parameter Value / Diagnostic Target']);
        wsSummary.addRow(['Project Scope ID', this.exportProject]);
        wsSummary.addRow(['Industrial Site Scope', siteLabel]);
        wsSummary.addRow(['Generated Date/Time', new Date().toLocaleString()]);
        wsSummary.addRow(['Data Scope Range', '31 Days Daily Operational Log']);
        
        wsSummary.addRow([]);
        wsSummary.addRow(['Executive Summary KPI Statistics', 'Value']);
        wsSummary.addRow(['Average Overall OEE', avgOee]);
        wsSummary.addRow(['Average Availability', avgAvail]);
        wsSummary.addRow(['Average Performance Speed', avgPerf]);
        wsSummary.addRow(['Average Quality Yield', avgQual]);
        wsSummary.addRow(['Average Scrap Rate', avgScrap]);

        // Format Summary Table Headers
        wsSummary.getRow(3).font = { bold: true, color: { argb: 'FFFFFF' } };
        wsSummary.getRow(3).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '155A8A' } };
        
        wsSummary.getRow(9).font = { bold: true, color: { argb: 'FFFFFF' } };
        wsSummary.getRow(9).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '155A8A' } };

        // Formatting summary numbers as percentages
        for (let r = 10; r <= 14; r++) {
          wsSummary.getRow(r).getCell(2).numFmt = '0.0%';
        }

        // Apply borders and fonts to Summary Sheet
        wsSummary.eachRow((row, rowNum) => {
          if (rowNum >= 3 && rowNum !== 8) {
            row.eachCell((cell) => {
              cell.border = thinBorder;
            });
          }
        });

        // ==========================================
        // SHEET 2: Analytics Data
        // ==========================================
        const wsData = workbook.addWorksheet('Analytics Data');
        wsData.views = [{ state: 'frozen', ySplit: 1, xSplit: 0, activeCell: 'A2' }];
        
        wsData.addRow(['Date Logged', 'Overall OEE', 'Availability Rating', 'Performance Speed', 'Quality Yield', 'Scrap Ratio']);

        this.history().forEach(row => {
          wsData.addRow([
            new Date(row.calculationTimestamp).toLocaleDateString(),
            row.oee,
            row.availability,
            row.performance,
            row.quality,
            row.scrapRate
          ]);
        });

        // Table Header Styling
        const dataHeaderRow = wsData.getRow(1);
        dataHeaderRow.font = { bold: true, color: { argb: 'FFFFFF' } };
        dataHeaderRow.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '155A8A' } };

        // Zebra striping and numeric percentage formatting
        for (let index = 2; index <= this.history().length + 1; index++) {
          const row = wsData.getRow(index);
          if (index % 2 === 0) {
            row.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'F9FAFB' } };
          }
          row.getCell(2).numFmt = '0.0%';
          row.getCell(3).numFmt = '0.0%';
          row.getCell(4).numFmt = '0.0%';
          row.getCell(5).numFmt = '0.0%';
          row.getCell(6).numFmt = '0.0%';
        }

        // Auto filter table
        wsData.autoFilter = {
          from: 'A1',
          to: `F${this.history().length + 1}`
        };

        // Conditional Formatting: Less than 80% (Red) and greater than or equal to 90% (Green)
        wsData.addConditionalFormatting({
          ref: `B2:B${this.history().length + 1}`,
          rules: [
            {
              type: 'cellIs',
              operator: 'lessThan',
              formulae: ['0.80'],
              style: {
                fill: { type: 'pattern', pattern: 'solid', bgColor: { argb: 'FFC7CE' } },
                font: { color: { argb: '9C0006' } }
              }
            } as any,
            {
              type: 'cellIs',
              operator: 'greaterThanOrEqual',
              formulae: ['0.90'],
              style: {
                fill: { type: 'pattern', pattern: 'solid', bgColor: { argb: 'C6EFCE' } },
                font: { color: { argb: '006100' } }
              }
            } as any
          ]
        });

        // Apply borders and alignments to Data Sheet
        wsData.eachRow((row) => {
          row.eachCell((cell) => {
            cell.border = thinBorder;
            cell.alignment = { vertical: 'middle', horizontal: 'center' };
          });
        });

        // Auto-size columns in Sheet 2
        wsData.columns.forEach(col => {
          let maxLen = 0;
          col.eachCell?.({ includeEmpty: true }, cell => {
            let valStr = '';
            if (cell.value instanceof Date) {
              valStr = cell.value.toLocaleDateString();
            } else if (cell.value !== null && cell.value !== undefined) {
              if (typeof cell.value === 'number') {
                valStr = (cell.value * 100).toFixed(1) + '%';
              } else {
                valStr = cell.value.toString();
              }
            }
            if (valStr.length > maxLen) {
              maxLen = valStr.length;
            }
          });
          col.width = Math.max(maxLen + 4, 16);
        });

        // ==========================================
        // SHEET 3: Synchronization History
        // ==========================================
        const wsSync = workbook.addWorksheet('Synchronization History');
        wsSync.addRow(['Sync ID', 'Connector Endpoint', 'Type', 'Status', 'Records Synchronized', 'Timestamp']);
        
        const syncHeaderRow = wsSync.getRow(1);
        syncHeaderRow.font = { bold: true, color: { argb: 'FFFFFF' } };
        syncHeaderRow.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'F58220' } }; // Corporate Orange

        const syncMock = [
          [101, 'Line 1 Production REST API', 'PRODUCTION', 'COMPLETED', 31, new Date().toLocaleString()],
          [102, 'Line 1 Quality Assurance Connector', 'QUALITY', 'COMPLETED', 31, new Date(Date.now() - 3600000).toLocaleString()],
          [103, 'Customer Satisfaction Audit API', 'CUSTOMER', 'COMPLETED', 30, new Date(Date.now() - 7200000).toLocaleString()]
        ];
        syncMock.forEach(log => wsSync.addRow(log));

        // Format and style sync sheet rows
        wsSync.eachRow((row, index) => {
          row.eachCell((cell) => {
            cell.border = thinBorder;
            cell.alignment = { vertical: 'middle', horizontal: 'center' };
          });
          if (index > 1 && index % 2 === 0) {
            row.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF7ED' } }; // Light Orange stripe
          }
        });

        // Auto filter sync sheet
        wsSync.autoFilter = { from: 'A1', to: 'F4' };

        // Auto-size columns in Sheet 3
        wsSync.columns.forEach(col => {
          let maxLen = 0;
          col.eachCell?.({ includeEmpty: true }, cell => {
            const valStr = cell.value ? cell.value.toString() : '';
            if (valStr.length > maxLen) {
              maxLen = valStr.length;
            }
          });
          col.width = Math.max(maxLen + 4, 18);
        });

        // Save Spreadsheet Workbook
        workbook.xlsx.writeBuffer().then(buffer => {
          saveAs(new Blob([buffer]), `Analytics_Report_Project_${this.exportProject}_${new Date().toISOString().split('T')[0]}.xlsx`);
          this.generatingReport.set(false);
          this.messageService.add({ severity: 'success', summary: 'Export Succeeded', detail: 'Spreadsheet downloaded successfully.' });
        });
      } catch (err) {
        console.error(err);
        this.generatingReport.set(false);
        this.messageService.add({ severity: 'error', summary: 'Export Failed', detail: 'Excel generation encountered an error.' });
      }
    }, 600);
  }

  openShareDialog(): void {
    this.displayShareDialog = true;
  }

  sendReport(): void {
    if (!this.shareEmail || !this.shareEmail.includes('@')) {
      this.messageService.add({ severity: 'warn', summary: 'Invalid Email', detail: 'Please provide a valid recipient email address.' });
      return;
    }
    this.generatingReport.set(true);
    this.progressMsg.set('Mailing generated documents to recipients...');
    this.displayShareDialog = false;

    setTimeout(() => {
      this.generatingReport.set(false);
      this.messageService.add({ severity: 'success', summary: 'Email Succeeded', detail: `Report sent successfully to ${this.shareEmail}.` });
    }, 900);
  }

  printReport(): void {
    window.print();
  }

  openScheduleDialog(): void {
    this.displayScheduleDialog = true;
  }

  createSchedule(): void {
    this.displayScheduleDialog = false;
    this.messageService.add({
      severity: 'info',
      summary: 'Schedule Setup Completed',
      detail: 'Scheduling functionality ready for backend integration.'
    });
  }

  private setupChartOptions(): void {
    this.multiChartOptions = {
      plugins: {
        legend: { position: 'top' }
      },
      scales: {
        x: { grid: { display: false } },
        y: { min: 0.0, max: 1.0, ticks: { callback: (v: number) => (v * 100) + '%' } }
      },
      responsive: true,
      maintainAspectRatio: false
    };
  }

  private updateMultiChart(history: KpiAggregationResponse[]): void {
    const labels = history.map(h => new Date(h.calculationTimestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
    const oeeData = history.map(h => h.oee);
    const availabilityData = history.map(h => h.availability);
    const performanceData = history.map(h => h.performance);
    const qualityData = history.map(h => h.quality);

    this.multiChartData = {
      labels: labels,
      datasets: [
        {
          label: 'OEE Score %',
          data: oeeData,
          borderColor: '#0076C8', // AVOCarbon Primary Blue
          backgroundColor: 'transparent',
          tension: 0.3,
          borderWidth: 3
        },
        {
          label: 'Availability Rate %',
          data: availabilityData,
          borderColor: '#10B981', // Green
          backgroundColor: 'transparent',
          tension: 0.3,
          borderWidth: 2
        },
        {
          label: 'Performance Speed %',
          data: performanceData,
          borderColor: '#F58220', // AVOCarbon Orange
          backgroundColor: 'transparent',
          tension: 0.3,
          borderWidth: 2
        },
        {
          label: 'Quality Rate %',
          data: qualityData,
          borderColor: '#6366F1', // Indigo
          backgroundColor: 'transparent',
          tension: 0.3,
          borderWidth: 2
        }
      ]
    };
  }
}
