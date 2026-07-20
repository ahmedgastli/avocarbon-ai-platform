import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AnalyticsService } from '../../core/services/analytics.service';
import { ProjectService } from '../../core/services/project.service';
import { KpiAggregationResponse } from '../../core/models/analytics.model';
import { ProjectResponse } from '../../core/models/project.model';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';

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
    TableModule
  ],
  template: `
    <div class="space-y-6 pb-8">
      <!-- Header Card -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
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
          <button pButton label="Refresh" icon="pi pi-refresh" class="p-button-outlined p-button-primary" (click)="loadAnalytics()"></button>
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
          <button pButton label="Export CSV" icon="pi pi-file-export" class="p-button-sm p-button-outlined p-button-secondary" (click)="dt.exportCSV()"></button>
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
    </div>
  `
})
export class AnalyticsComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);
  private projectService = inject(ProjectService);

  projects = signal<ProjectResponse[]>([]);
  selectedProjectId = 1;
  history = signal<KpiAggregationResponse[]>([]);

  multiChartData: any;
  multiChartOptions: any;

  ngOnInit(): void {
    this.setupChartOptions();
    this.projectService.getAllProjects().subscribe({
      next: (projs) => {
        this.projects.set(projs);
        if (projs.length > 0) {
          this.selectedProjectId = projs[0].id;
        }
        this.loadAnalytics();
      }
    });
  }

  loadAnalytics(): void {
    this.analyticsService.getKpiHistory(this.selectedProjectId).subscribe({
      next: (data) => {
        this.history.set(data);
        this.updateMultiChart(data);
      }
    });
  }

  private setupChartOptions(): void {
    this.multiChartOptions = {
      plugins: {
        legend: { position: 'top' }
      },
      scales: {
        x: { grid: { display: false } },
        y: { min: 0.5, max: 1.0, ticks: { callback: (v: number) => (v * 100) + '%' } }
      },
      responsive: true,
      maintainAspectRatio: false
    };
  }

  private updateMultiChart(data: KpiAggregationResponse[]): void {
    const labels = data.map(h => new Date(h.calculationTimestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));

    this.multiChartData = {
      labels: labels,
      datasets: [
        {
          label: 'OEE',
          data: data.map(d => d.oee),
          borderColor: '#0076C8',
          backgroundColor: '#0076C8',
          tension: 0.3,
          pointRadius: 2
        },
        {
          label: 'Availability',
          data: data.map(d => d.availability),
          borderColor: '#10B981',
          backgroundColor: '#10B981',
          tension: 0.3,
          pointRadius: 2
        },
        {
          label: 'Performance',
          data: data.map(d => d.performance),
          borderColor: '#F58220',
          backgroundColor: '#F58220',
          tension: 0.3,
          pointRadius: 2
        },
        {
          label: 'Quality',
          data: data.map(d => d.quality),
          borderColor: '#6366F1',
          backgroundColor: '#6366F1',
          tension: 0.3,
          pointRadius: 2
        }
      ]
    };
  }
}
