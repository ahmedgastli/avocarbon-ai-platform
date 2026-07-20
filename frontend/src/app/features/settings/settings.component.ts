import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    InputTextModule,
    ButtonModule,
    InputNumberModule,
    ToastModule,
    TagModule
  ],
  providers: [MessageService],
  template: `
    <div class="space-y-6 pb-8">
      <p-toast></p-toast>

      <!-- Header Card -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <div>
          <div class="flex items-center space-x-2">
            <span class="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-[#0076C8]/10 text-[#0076C8]">
              Admin Restricted
            </span>
            <span class="text-xs text-gray-300">•</span>
            <span class="text-xs font-semibold text-gray-500">System Configuration</span>
          </div>
          <h1 class="text-2xl font-black text-[#155A8A] tracking-tight mt-1">Platform Administration Settings</h1>
        </div>

        <button pButton label="Save Platform Configuration" icon="pi pi-check" class="p-button-accent" (click)="saveSettings()"></button>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Card 1: Industrial KPI Thresholds -->
        <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 space-y-4">
          <div class="pb-3 border-b border-gray-100 flex items-center justify-between">
            <div>
              <h3 class="text-base font-bold text-[#155A8A]">Industrial Target Thresholds</h3>
              <p class="text-xs text-[#666666]">Configure target OEE and alarm boundaries</p>
            </div>
            <i class="pi pi-cog text-xl text-[#0076C8]"></i>
          </div>

          <div class="space-y-4 text-xs">
            <div class="field">
              <label class="block font-bold text-[#155A8A] uppercase mb-1">Target OEE Benchmark (%)</label>
              <p-inputNumber [(ngModel)]="oeeTarget" [suffix]="'%'" [min]="50" [max]="100" styleClass="w-full"></p-inputNumber>
            </div>

            <div class="field">
              <label class="block font-bold text-[#155A8A] uppercase mb-1">Scrap Rate Warning Limit (%)</label>
              <p-inputNumber [(ngModel)]="scrapWarningLimit" [suffix]="'%'" [min]="0" [max]="20" styleClass="w-full"></p-inputNumber>
            </div>

            <div class="field">
              <label class="block font-bold text-[#155A8A] uppercase mb-1">Automated Data Sync Frequency (Minutes)</label>
              <p-inputNumber [(ngModel)]="syncInterval" [suffix]="' mins'" [min]="5" [max]="1440" styleClass="w-full"></p-inputNumber>
            </div>
          </div>
        </div>

        <!-- Card 2: Database & System Status -->
        <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 space-y-4">
          <div class="pb-3 border-b border-gray-100 flex items-center justify-between">
            <div>
              <h3 class="text-base font-bold text-[#155A8A]">System Health & PostgreSQL Status</h3>
              <p class="text-xs text-[#666666]">Core engine diagnostic & database connection</p>
            </div>
            <i class="pi pi-database text-xl text-emerald-600"></i>
          </div>

          <div class="space-y-3 text-xs">
            <div class="flex items-center justify-between p-3 bg-gray-50 rounded-xl border border-gray-100">
              <span class="font-semibold text-gray-700">Database Connection</span>
              <p-tag value="PostgreSQL Active (5432)" severity="success"></p-tag>
            </div>

            <div class="flex items-center justify-between p-3 bg-gray-50 rounded-xl border border-gray-100">
              <span class="font-semibold text-gray-700">JWT Token Security</span>
              <p-tag value="HS512 Signature Active" severity="info"></p-tag>
            </div>

            <div class="flex items-center justify-between p-3 bg-gray-50 rounded-xl border border-gray-100">
              <span class="font-semibold text-gray-700">Audit Log Retention</span>
              <span class="font-bold text-[#155A8A]">90 Days Historical</span>
            </div>

            <div class="flex items-center justify-between p-3 bg-gray-50 rounded-xl border border-gray-100">
              <span class="font-semibold text-gray-700">Plant Location</span>
              <span class="font-bold text-[#155A8A]">AVOCarbon Facility 1</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class SettingsComponent {
  private messageService = inject(MessageService);

  oeeTarget = 85;
  scrapWarningLimit = 3;
  syncInterval = 60;

  saveSettings(): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Settings Saved',
      detail: 'Platform configuration updated successfully'
    });
  }
}
