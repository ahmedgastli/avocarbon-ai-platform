import { Component, OnInit, inject, signal, effect, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AnalyticsService } from '../../core/services/analytics.service';
import { ProjectService } from '../../core/services/project.service';
import { SiteFilterService } from '../../core/services/site-filter.service';
import { GlobalOperationsService } from '../../core/services/global-operations.service';
import { KpiAggregationResponse } from '../../core/models/analytics.model';
import { ProjectResponse } from '../../core/models/project.model';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TableModule } from 'primeng/table';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    DropdownModule,
    ButtonModule,
    ChartModule,
    SkeletonModule,
    TagModule,
    TableModule
  ],
  template: `
    <div class="space-y-8 pb-8">
      <!-- Active Site Filter Banner -->
      @if (siteFilterService.selectedSiteName(); as name) {
        <div class="flex items-center justify-between bg-[#0076C8] text-white px-6 py-3.5 rounded-2xl shadow-md border border-[#155A8A] animate-fadein">
          <div class="flex items-center space-x-3 text-xs">
            <i class="pi pi-globe text-base animate-spin" style="animation-duration: 4s"></i>
            <div>
              <span class="font-black uppercase tracking-wider">Scoped Workspace Active</span>
              <span class="mx-2 opacity-50">|</span>
              <span class="font-medium">Displaying telemetry scoped to site: </span>
              <span class="font-extrabold underline decoration-[#F58220] decoration-2">{{ name }}</span>
            </div>
          </div>
          <button (click)="clearSiteScope()" 
                  class="px-3 py-1 bg-white/10 hover:bg-white/20 rounded-lg text-[10px] font-black uppercase tracking-wider transition-colors border border-white/20">
            Clear Scope Filter
          </button>
        </div>
      }

      <!-- Executive Dashboard Top Header -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <div>
          <div class="flex items-center space-x-2">
            <span class="px-3 py-0.5 rounded-full text-[10px] font-black uppercase tracking-widest bg-[#0076C8]/10 text-[#0076C8]">
              Executive Plant Intelligence
            </span>
            <span class="text-xs text-gray-300">•</span>
            <span class="text-xs font-semibold text-gray-500">Live Production Dashboard</span>
          </div>
          <h1 class="text-2xl font-black text-[#155A8A] tracking-tight mt-1">Industrial Operations & OEE Metrics</h1>
        </div>

        <div class="flex items-center space-x-3">
          <label class="text-xs font-bold text-[#155A8A] uppercase tracking-wider hidden sm:inline">Project Scope:</label>
          <p-dropdown 
            [options]="projects()" 
            optionLabel="name" 
            optionValue="id" 
            [(ngModel)]="selectedProjectId" 
            (onChange)="onProjectChange()"
            placeholder="Select Project"
            styleClass="w-64 p-inputtext-sm rounded-xl">
          </p-dropdown>
          <button (click)="loadDashboardData()" pButton icon="pi pi-refresh" class="p-button-outlined p-button-primary" title="Refresh Metrics"></button>
        </div>
      </div>

      @if (loading()) {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          @for (item of [1,2,3,4]; track item) {
            <div class="bg-white p-6 rounded-3xl border border-gray-100 shadow-sm">
              <p-skeleton width="60%" height="1.2rem" styleClass="mb-3"></p-skeleton>
              <p-skeleton width="100%" height="2.5rem" styleClass="mb-3"></p-skeleton>
              <p-skeleton width="40%" height="1rem"></p-skeleton>
            </div>
          }
        </div>
      } @else {
        <!-- 1. Top Section: Four Large KPI Cards -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          
          <!-- Card 1: OEE -->
          <div class="bg-white rounded-3xl p-6 shadow-sm border-t-4 border-[#0076C8] hover:-translate-y-1 hover:shadow-xl transition-all duration-300 group">
            <div class="flex items-center justify-between">
              <span class="text-xs font-black text-[#666666] uppercase tracking-wider">Overall OEE</span>
              <div class="w-12 h-12 rounded-2xl bg-[#0076C8]/10 text-[#0076C8] flex items-center justify-center transition-transform group-hover:scale-110">
                <i class="pi pi-cog text-2xl"></i>
              </div>
            </div>
            <div class="mt-4 flex items-baseline justify-between">
              <span class="text-4xl font-black text-[#155A8A] tracking-tight">{{ formatPercent(summary()?.oee) }}</span>
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-extrabold bg-emerald-50 text-emerald-700">
                <i class="pi pi-arrow-up text-[10px] mr-1"></i> +2.4%
              </span>
            </div>
            <p class="text-xs text-[#666666] mt-2 font-medium">Overall Equipment Effectiveness</p>
          </div>

          <!-- Card 2: Availability -->
          <div class="bg-white rounded-3xl p-6 shadow-sm border-t-4 border-emerald-500 hover:-translate-y-1 hover:shadow-xl transition-all duration-300 group">
            <div class="flex items-center justify-between">
              <span class="text-xs font-black text-[#666666] uppercase tracking-wider">Availability</span>
              <div class="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center transition-transform group-hover:scale-110">
                <i class="pi pi-clock text-2xl"></i>
              </div>
            </div>
            <div class="mt-4 flex items-baseline justify-between">
              <span class="text-4xl font-black text-[#155A8A] tracking-tight">{{ formatPercent(summary()?.availability) }}</span>
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-extrabold bg-emerald-50 text-emerald-700">
                <i class="pi pi-arrow-up text-[10px] mr-1"></i> +1.1%
              </span>
            </div>
            <p class="text-xs text-[#666666] mt-2 font-medium">Actual Uptime vs Planned</p>
          </div>

          <!-- Card 3: Performance -->
          <div class="bg-white rounded-3xl p-6 shadow-sm border-t-4 border-[#F58220] hover:-translate-y-1 hover:shadow-xl transition-all duration-300 group">
            <div class="flex items-center justify-between">
              <span class="text-xs font-black text-[#666666] uppercase tracking-wider">Performance</span>
              <div class="w-12 h-12 rounded-2xl bg-[#F58220]/10 text-[#F58220] flex items-center justify-center transition-transform group-hover:scale-110">
                <i class="pi pi-bolt text-2xl"></i>
              </div>
            </div>
            <div class="mt-4 flex items-baseline justify-between">
              <span class="text-4xl font-black text-[#155A8A] tracking-tight">{{ formatPercent(summary()?.performance) }}</span>
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-extrabold bg-[#F58220]/15 text-[#F58220]">
                Optimal
              </span>
            </div>
            <p class="text-xs text-[#666666] mt-2 font-medium">Production Speed Rating</p>
          </div>

          <!-- Card 4: Quality & Scrap -->
          <div class="bg-white rounded-3xl p-6 shadow-sm border-t-4 border-indigo-500 hover:-translate-y-1 hover:shadow-xl transition-all duration-300 group">
            <div class="flex items-center justify-between">
              <span class="text-xs font-black text-[#666666] uppercase tracking-wider">Quality Rate</span>
              <div class="w-12 h-12 rounded-2xl bg-indigo-100 text-indigo-600 flex items-center justify-center transition-transform group-hover:scale-110">
                <i class="pi pi-check-circle text-2xl"></i>
              </div>
            </div>
            <div class="mt-4 flex items-baseline justify-between">
              <span class="text-4xl font-black text-[#155A8A] tracking-tight">{{ formatPercent(summary()?.quality) }}</span>
              <span class="text-xs font-extrabold text-red-500 bg-red-50 px-2 py-0.5 rounded-md">
                Scrap: {{ formatPercent(summary()?.scrapRate) }}
              </span>
            </div>
            <p class="text-xs text-[#666666] mt-2 font-medium">First-Pass Defect Free Yield</p>
          </div>

        </div>

        <!-- 2. Main Section: Large 31-Day OEE Trend Chart -->
        <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-gray-100 mb-6 gap-2">
            <div>
              <h3 class="text-lg font-black text-[#155A8A] tracking-tight">31-Day OEE Performance Curve</h3>
              <p class="text-xs text-[#666666]">Aggregated daily OEE performance timeline from PostgreSQL logs</p>
            </div>
            <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-[#0076C8] text-white">
              <i class="pi pi-chart-line mr-1 text-[10px]"></i> Power BI Executive View
            </span>
          </div>

          @if (lineChartData) {
            <p-chart type="line" [data]="lineChartData" [options]="lineChartOptions" height="340px"></p-chart>
          }
        </div>

        <!-- 3. Middle Section: Recent Activity Card & Synchronization Card -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          <!-- Activity Card (1 col) -->
          <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between pb-3 border-b border-gray-100 mb-4">
                <h3 class="text-base font-bold text-[#155A8A]">Recent Plant Activity</h3>
                <i class="pi pi-history text-[#0076C8]"></i>
              </div>

              <div class="space-y-4">
                <div class="flex items-start space-x-3 text-xs">
                  <div class="w-2 h-2 rounded-full bg-[#0076C8] mt-1.5 shrink-0"></div>
                  <div>
                    <div class="font-bold text-[#155A8A]">Daily KPI Aggregation Executed</div>
                    <div class="text-gray-500">Calculated OEE for Line Optimization</div>
                    <div class="text-[10px] text-gray-400 mt-0.5">10 mins ago</div>
                  </div>
                </div>

                <div class="flex items-start space-x-3 text-xs">
                  <div class="w-2 h-2 rounded-full bg-[#F58220] mt-1.5 shrink-0"></div>
                  <div>
                    <div class="font-bold text-[#155A8A]">Manual API Synchronization Triggered</div>
                    <div class="text-gray-500">Imported REST API logs</div>
                    <div class="text-[10px] text-gray-400 mt-0.5">1 hour ago</div>
                  </div>
                </div>
              </div>
            </div>

            <div class="pt-4 border-t border-gray-100 text-center">
              <span class="text-xs font-extrabold text-[#0076C8] cursor-pointer hover:underline">View All System Events &rarr;</span>
            </div>
          </div>

          <!-- Synchronization Card (2 cols) -->
          <div class="lg:col-span-2 bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
            <div class="flex items-center justify-between pb-3 border-b border-gray-100 mb-4">
              <div>
                <h3 class="text-base font-bold text-[#155A8A]">API Data Synchronization Status</h3>
                <p class="text-xs text-[#666666]">Active REST API connectors & audit sync logs</p>
              </div>
              <span class="px-2.5 py-0.5 text-[10px] font-extrabold uppercase rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                All Connectors Healthy
              </span>
            </div>

            <p-table [value]="filteredSyncLogs()" styleClass="p-datatable-striped p-datatable-sm">
              <ng-template pTemplate="header">
                <tr>
                  <th>Sync ID</th>
                  <th>Connector Name</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Records</th>
                  <th>Timestamp</th>
                </tr>
              </ng-template>

              <ng-template pTemplate="body" let-log>
                <tr>
                  <td class="font-bold text-gray-500">#{{ log.id }}</td>
                  <td class="font-semibold text-[#155A8A]">{{ log.name }}</td>
                  <td>
                    <span class="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-blue-50 text-[#0076C8]">
                      {{ log.type }}
                    </span>
                  </td>
                  <td>
                    <p-tag [value]="log.status" severity="success"></p-tag>
                  </td>
                  <td class="font-mono text-xs font-bold text-gray-700">{{ log.records }} Items</td>
                  <td class="text-xs text-gray-500">{{ log.timestamp | date:'shortTime' }}</td>
                </tr>
              </ng-template>
            </p-table>
          </div>

        </div>
      }
    </div>
  `
})
export class DashboardComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);
  private projectService = inject(ProjectService);
  siteFilterService = inject(SiteFilterService);
  private operationsService = inject(GlobalOperationsService);
  authService = inject(AuthService);

  projects = signal<ProjectResponse[]>([]);
  selectedProjectId = 0;
  summary = signal<KpiAggregationResponse | null>(null);
  history = signal<KpiAggregationResponse[]>([]);
  loading = signal<boolean>(true);

  lineChartData: any;
  lineChartOptions: any;

  recentSyncLogs = [
    { id: 101, name: 'Line 1 Production REST API', type: 'PRODUCTION', status: 'COMPLETED', records: 31, timestamp: new Date(), siteId: 'luxembourg' },
    { id: 102, name: 'Line 1 Quality Assurance Connector', type: 'QUALITY', status: 'COMPLETED', records: 31, timestamp: new Date(Date.now() - 3600000), siteId: 'luxembourg' },
    { id: 103, name: 'Customer Satisfaction Audit API', type: 'CUSTOMER', status: 'COMPLETED', records: 30, timestamp: new Date(Date.now() - 7200000), siteId: 'tunisia' }
  ];

  filteredSyncLogs = computed(() => {
    const activeSite = this.siteFilterService.selectedSiteId();
    let logs = this.recentSyncLogs;
    if (activeSite) {
      logs = logs.filter(log => log.siteId === activeSite);
    }
    return logs.filter(log => this.authService.isSiteAuthorized(log.siteId));
  });

  constructor() {
    effect(() => {
      // Reload projects dynamically when map filter site changes
      this.siteFilterService.selectedSiteId();
      this.loadProjects();
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

    this.loadProjects();
  }

  loadProjects(): void {
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
        } else {
          this.selectedProjectId = 0;
        }
        this.loadDashboardData();
      },
      error: () => this.loadDashboardData()
    });
  }

  onProjectChange(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading.set(true);

    const activeSite = this.siteFilterService.selectedSiteId();

    if (!activeSite && this.selectedProjectId === 0) {
      this.summary.set(null);
      this.history.set([]);
      this.loading.set(false);
      return;
    }

    if (activeSite) {
      // Fetch site statistics from operations metadata
      this.operationsService.getSites().subscribe({
        next: (sites) => {
          const site = sites.find(s => s.id === activeSite);
          if (site) {
            const mockSummary: KpiAggregationResponse = {
              id: 1,
              projectId: 1,
              projectName: site.name,
              oee: site.metrics.oee,
              availability: site.metrics.availability,
              performance: site.metrics.performance,
              quality: site.metrics.quality,
              scrapRate: site.metrics.scrapRate,
              customerSatisfaction: 0.95,
              maintenanceDowntime: 1.5,
              calculationTimestamp: new Date().toISOString(),
              aggregationPeriod: 'DAILY'
            };
            this.summary.set(mockSummary);

            // Generate timeline based on site baseline
            const mockHistory: KpiAggregationResponse[] = Array.from({ length: 31 }).map((_, i) => {
              const variance = (Math.random() - 0.5) * 0.05;
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
            this.updateLineChart(mockHistory);
          }
          this.loading.set(false);
        }
      });
    } else {
      // Standard global load
      this.analyticsService.getKpiSummary(this.selectedProjectId).subscribe({
        next: (sum) => this.summary.set(sum),
        error: () => this.summary.set(null)
      });

      this.analyticsService.getKpiHistory(this.selectedProjectId).subscribe({
        next: (hist) => {
          this.history.set(hist);
          this.updateLineChart(hist);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    }
  }

  clearSiteScope(): void {
    this.siteFilterService.clearFilter();
  }

  formatPercent(val?: number): string {
    if (val === undefined || val === null) return '0.0%';
    return (val * 100).toFixed(1) + '%';
  }

  private setupChartOptions(): void {
    this.lineChartOptions = {
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { display: false } },
        y: { min: 0.0, max: 1.0, ticks: { callback: (v: number) => (v * 100) + '%' } }
      },
      responsive: true,
      maintainAspectRatio: false
    };
  }

  private updateLineChart(history: KpiAggregationResponse[]): void {
    const labels = history.map(h => new Date(h.calculationTimestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
    const data = history.map(h => h.oee);

    this.lineChartData = {
      labels: labels,
      datasets: [
        {
          label: 'OEE Rating',
          data: data,
          fill: true,
          borderColor: '#0076C8',
          backgroundColor: 'rgba(0, 118, 200, 0.08)',
          tension: 0.4,
          pointRadius: 3,
          pointHoverRadius: 6
        }
      ]
    };
  }
}
