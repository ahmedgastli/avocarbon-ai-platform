import { Component, AfterViewInit, OnInit, inject, signal, computed, PLATFORM_ID, OnDestroy } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GlobalOperationsService, SiteModel } from '../../core/services/global-operations.service';
import { SiteFilterService } from '../../core/services/site-filter.service';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

import { AuthService } from '../../core/services/auth.service';

import * as L from 'leaflet';

@Component({
  selector: 'app-global-operations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    DropdownModule,
    ButtonModule,
    DialogModule,
    TagModule,
    ToastModule
  ],
  providers: [MessageService],
  template: `
    <div class="space-y-6 pb-8">
      <p-toast></p-toast>

      <!-- 1. Top Section: Global KPI Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-5">
        <!-- KPI 1: Total Sites -->
        <div class="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 flex items-center justify-between">
          <div>
            <span class="text-[10px] font-black text-gray-400 uppercase tracking-wider">Total Sites</span>
            <div class="text-3xl font-black text-[#155A8A] mt-1">{{ totalSites() }}</div>
          </div>
          <div class="w-10 h-10 rounded-xl bg-[#0076C8]/10 text-[#0076C8] flex items-center justify-center">
            <i class="pi pi-map text-lg"></i>
          </div>
        </div>

        <!-- KPI 2: Countries -->
        <div class="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 flex items-center justify-between">
          <div>
            <span class="text-[10px] font-black text-gray-400 uppercase tracking-wider">Countries</span>
            <div class="text-3xl font-black text-[#155A8A] mt-1">{{ totalCountries() }}</div>
          </div>
          <div class="w-10 h-10 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center">
            <i class="pi pi-globe text-lg"></i>
          </div>
        </div>

        <!-- KPI 3: Connected APIs -->
        <div class="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 flex items-center justify-between">
          <div>
            <span class="text-[10px] font-black text-gray-400 uppercase tracking-wider">Connected APIs</span>
            <div class="text-3xl font-black text-[#155A8A] mt-1">{{ totalConnectedApis() }}</div>
          </div>
          <div class="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center">
            <i class="pi pi-sync text-lg"></i>
          </div>
        </div>

        <!-- KPI 4: Avg Global OEE -->
        <div class="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 flex items-center justify-between">
          <div>
            <span class="text-[10px] font-black text-gray-400 uppercase tracking-wider">Avg Global OEE</span>
            <div class="text-3xl font-black text-[#155A8A] mt-1">{{ formatPercent(avgGlobalOee()) }}</div>
          </div>
          <div class="w-10 h-10 rounded-xl bg-[#F58220]/10 text-[#F58220] flex items-center justify-center">
            <i class="pi pi-cog text-lg"></i>
          </div>
        </div>

        <!-- KPI 5: Online Sites -->
        <div class="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 flex items-center justify-between">
          <div>
            <span class="text-[10px] font-black text-gray-400 uppercase tracking-wider">Online Sites</span>
            <div class="text-3xl font-black text-emerald-600 mt-1">{{ onlineSites() }}</div>
          </div>
          <div class="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center">
            <i class="pi pi-check text-lg"></i>
          </div>
        </div>

        <!-- KPI 6: Offline Sites -->
        <div class="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 flex items-center justify-between">
          <div>
            <span class="text-[10px] font-black text-gray-400 uppercase tracking-wider">Offline Sites</span>
            <div class="text-3xl font-black text-red-500 mt-1">{{ offlineSites() }}</div>
          </div>
          <div class="w-10 h-10 rounded-xl bg-red-100 text-red-500 flex items-center justify-center">
            <i class="pi pi-times text-lg"></i>
          </div>
        </div>
      </div>

      <!-- 2. Global Filter Controls Card -->
      <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <div class="flex items-center justify-between border-b border-gray-100 pb-3 mb-4">
          <h3 class="text-sm font-black text-[#155A8A] uppercase tracking-wider flex items-center">
            <i class="pi pi-sliders-h mr-2 text-[#0076C8]"></i> Global Map Filter Panel
          </h3>
          <button pButton label="Reset Filters" icon="pi pi-refresh" class="p-button-text p-button-sm text-xs" (click)="resetFilters()"></button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div class="field">
            <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Country</label>
            <p-dropdown [options]="countryOptions" [(ngModel)]="filterCountry" placeholder="All Countries" (onChange)="applyFilters()" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
          </div>

          <div class="field">
            <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Product Type</label>
            <p-dropdown [options]="productOptions" [(ngModel)]="filterProduct" placeholder="All Products" (onChange)="applyFilters()" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
          </div>

          <div class="field">
            <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Site Scope</label>
            <p-dropdown [options]="siteOptions" [(ngModel)]="filterSite" placeholder="All Sites" (onChange)="applyFilters()" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
          </div>

          <div class="field">
            <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Operational Status</label>
            <p-dropdown [options]="statusOptions" [(ngModel)]="filterStatus" placeholder="All Statuses" (onChange)="applyFilters()" styleClass="w-full text-xs rounded-xl" appendTo="body"></p-dropdown>
          </div>
        </div>
      </div>

      <!-- 3. Layout Grid: Map and Site Detail Panel -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Map Container (Left 2 cols on desktop) -->
        <div class="lg:col-span-2 bg-white p-4 rounded-3xl shadow-sm border border-gray-100 h-[600px] flex flex-col justify-between relative overflow-hidden">
          <div class="text-xs font-bold text-[#155A8A] uppercase tracking-wider pb-2 flex items-center justify-between border-b border-gray-100 z-10 bg-white">
            <span>🌍 Interactive Operations Map</span>
            @if (siteFilterService.selectedSiteName(); as name) {
              <span class="text-[10px] bg-[#0076C8] text-white px-2 py-0.5 rounded-full uppercase tracking-wider">
                Filtered: {{ name }}
              </span>
            }
          </div>

          <!-- Map Element -->
          <div id="map-container" class="w-full h-full rounded-2xl border border-gray-100 z-0"></div>
        </div>

        <!-- Site Details Sidebar (Right 1 col on desktop) -->
        <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between min-h-[500px]">
          @if (selectedSite(); as site) {
            <div class="space-y-5">
              <!-- Site Header -->
              <div class="flex items-start justify-between pb-3 border-b border-gray-100">
                <div>
                  <div class="flex items-center space-x-1.5">
                    <img src="assets/logo.svg" alt="AVOCarbon" class="h-4 object-contain" />
                    <span class="text-[9px] font-black text-gray-400 uppercase tracking-widest">{{ site.country }}</span>
                  </div>
                  <h3 class="text-lg font-black text-[#155A8A] tracking-tight mt-1">{{ site.name }}</h3>
                  <p class="text-xs text-gray-500 font-semibold flex items-center">
                    <i class="pi pi-map-marker mr-1 text-[#F58220]"></i> {{ site.city }}, {{ site.country }}
                  </p>
                </div>
                <p-tag [value]="site.status" [severity]="getStatusSeverity(site.status)"></p-tag>
              </div>

              <!-- Product Portfolio -->
              <div>
                <span class="block text-[10px] font-black text-[#155A8A] uppercase tracking-wider mb-1.5">Site Product Portfolio</span>
                <div class="flex flex-wrap gap-1.5">
                  @for (prod of site.products; track prod) {
                    <span class="px-2.5 py-1 bg-gray-50 border border-gray-200 text-gray-600 rounded-lg text-[10px] font-bold">
                      {{ prod }}
                    </span>
                  }
                </div>
              </div>

              <!-- Live OEE Diagnostics -->
              <div class="space-y-3">
                <span class="block text-[10px] font-black text-[#155A8A] uppercase tracking-wider pb-1.5 border-b border-gray-100">Live OEE Diagnostics</span>
                
                <div class="flex justify-between items-center text-xs">
                  <span class="font-medium text-gray-600">Site OEE Score</span>
                  <span class="font-extrabold text-[#0076C8] text-sm">{{ formatPercent(site.metrics.oee) }}</span>
                </div>
                <div class="w-full bg-gray-100 h-2 rounded-full overflow-hidden">
                  <div class="bg-[#0076C8] h-full rounded-full transition-all duration-300" [style.width.%]="site.metrics.oee * 100"></div>
                </div>

                <div class="grid grid-cols-3 gap-2 pt-2 text-center text-[10px]">
                  <div class="bg-emerald-50/50 p-2 rounded-xl border border-emerald-100">
                    <div class="text-emerald-700 font-black">{{ formatPercent(site.metrics.availability) }}</div>
                    <div class="text-gray-500 font-semibold uppercase mt-0.5">Availability</div>
                  </div>
                  <div class="bg-[#F58220]/10 p-2 rounded-xl border border-[#F58220]/20">
                    <div class="text-[#F58220] font-black">{{ formatPercent(site.metrics.performance) }}</div>
                    <div class="text-gray-500 font-semibold uppercase mt-0.5">Performance</div>
                  </div>
                  <div class="bg-indigo-50 p-2 rounded-xl border border-indigo-100">
                    <div class="text-indigo-700 font-black">{{ formatPercent(site.metrics.quality) }}</div>
                    <div class="text-gray-500 font-semibold uppercase mt-0.5">Quality</div>
                  </div>
                </div>
              </div>

              <!-- General Stats -->
              <div class="space-y-2 text-xs pt-2">
                <div class="flex justify-between items-center p-2 bg-gray-50 rounded-xl border border-gray-100">
                  <span class="font-semibold text-gray-500">Connected Rest APIs</span>
                  <span class="font-extrabold text-[#155A8A]">{{ site.connectedApis }} Endpoints</span>
                </div>
                <div class="flex justify-between items-center p-2 bg-gray-50 rounded-xl border border-gray-100">
                  <span class="font-semibold text-gray-500">Active OEE Projects</span>
                  <span class="font-extrabold text-[#155A8A]">{{ site.projectsCount }} Projects</span>
                </div>
              </div>
            </div>

            <!-- Context Filter Controls -->
            <div class="space-y-2 pt-4 border-t border-gray-100">
              <button (click)="selectSiteContext(site)" 
                      class="w-full py-3 bg-[#0076C8] hover:bg-[#F58220] text-white font-black rounded-2xl shadow-md transition-all duration-200 text-xs flex items-center justify-center space-x-2">
                <i class="pi pi-filter"></i>
                <span>Set Global Site Filter</span>
              </button>
              @if (siteFilterService.selectedSiteId() === site.id) {
                <button (click)="clearSiteContext()" 
                        class="w-full py-2.5 bg-gray-50 hover:bg-gray-100 text-gray-600 font-bold rounded-2xl border border-gray-200 transition-all duration-200 text-[11px] flex items-center justify-center space-x-1.5">
                  <i class="pi pi-times"></i>
                  <span>Remove Scope Filter</span>
                </button>
              }
            </div>
          } @else {
            <!-- Empty Scope State -->
            <div class="my-auto text-center py-12 text-gray-400 space-y-3">
              <i class="pi pi-globe text-5xl text-gray-300"></i>
              <div class="font-bold text-[#155A8A]">No Site Selected</div>
              <p class="text-xs max-w-[200px] mx-auto leading-relaxed text-gray-500">
                Click any AVOCarbon marker on the world map to inspect diagnostics and load scope.
              </p>
            </div>
          }
        </div>
      </div>
    </div>
  `
})
export class GlobalOperationsComponent implements OnInit, AfterViewInit, OnDestroy {
  private platformId = inject(PLATFORM_ID);
  private operationsService = inject(GlobalOperationsService);
  siteFilterService = inject(SiteFilterService);
  private messageService = inject(MessageService);
  authService = inject(AuthService);

  private map: L.Map | null = null;
  private markersGroup: L.LayerGroup | null = null;

  // Filter States
  filterCountry = '';
  filterProduct = '';
  filterSite = '';
  filterStatus = '';

  // Options lists
  countryOptions: any[] = [{ label: 'All Countries', value: '' }];
  productOptions: any[] = [{ label: 'All Products', value: '' }];
  siteOptions: any[] = [{ label: 'All Sites', value: '' }];
  statusOptions: any[] = [
    { label: 'All Statuses', value: '' },
    { label: 'Operational', value: 'Operational' },
    { label: 'Maintenance', value: 'Maintenance' },
    { label: 'Offline', value: 'Offline' }
  ];

  allSites = signal<SiteModel[]>([]);
  filteredSites = signal<SiteModel[]>([]);
  selectedSite = signal<SiteModel | null>(null);

  // Compute Stats
  totalSites = computed(() => this.filteredSites().length);
  totalCountries = computed(() => new Set(this.filteredSites().map(s => s.country)).size);
  totalConnectedApis = computed(() => this.filteredSites().reduce((acc, s) => acc + s.connectedApis, 0));
  avgGlobalOee = computed(() => {
    const operational = this.filteredSites().filter(s => s.status !== 'Offline');
    if (operational.length === 0) return 0;
    return operational.reduce((acc, s) => acc + s.metrics.oee, 0) / operational.length;
  });
  onlineSites = computed(() => this.filteredSites().filter(s => s.status !== 'Offline').length);
  offlineSites = computed(() => this.filteredSites().filter(s => s.status === 'Offline').length);

  ngOnInit(): void {
    this.operationsService.getSites().subscribe({
      next: (sitesList) => {
        const authorizedSites = sitesList.filter(s => this.authService.isSiteAuthorized(s.id));
        this.allSites.set(authorizedSites);
        this.filteredSites.set(authorizedSites);
        this.buildFilterOptions(authorizedSites);

        // Pre-select site if filter state matches
        const currentActiveFilterId = this.siteFilterService.selectedSiteId();
        if (currentActiveFilterId) {
          const match = authorizedSites.find(s => s.id === currentActiveFilterId);
          if (match) this.selectedSite.set(match);
        }
      }
    });
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => this.initializeMap(), 100);
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initializeMap(): void {
    // Standard worldwide centering
    this.map = L.map('map-container', {
      zoomControl: true,
      minZoom: 1.5,
      maxZoom: 10
    }).setView([20, 0], 2);

    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
    }).addTo(this.map);

    this.markersGroup = L.layerGroup().addTo(this.map);
    this.renderMarkers();
  }

  private renderMarkers(): void {
    if (!this.map || !this.markersGroup) return;

    this.markersGroup.clearLayers();

    this.filteredSites().forEach(site => {
      let statusColor = '#0076C8'; // Default blue
      if (site.status === 'Operational') statusColor = '#10B981'; // Green
      else if (site.status === 'Maintenance') statusColor = '#F58220'; // Orange
      else if (site.status === 'Offline') statusColor = '#EF4444'; // Red

      // Render crisp custom SVG div icon with pulse effect
      const customIcon = L.divIcon({
        className: 'custom-div-icon',
        html: `
          <div class="relative flex items-center justify-center" style="transform: translate(-12px, -12px)">
            <span class="absolute inline-flex h-8 w-8 animate-ping rounded-full" style="background-color: ${statusColor}; opacity: 0.15"></span>
            <div class="relative w-8 h-8 rounded-full flex items-center justify-center shadow-lg border-2 border-white text-white" 
                 style="background-color: ${statusColor}">
              <i class="pi ${site.isHeadquarters ? 'pi-home' : 'pi-building'} text-xs"></i>
            </div>
          </div>
        `,
        iconSize: [28, 28]
      });

      const marker = L.marker([site.lat, site.lng], { icon: customIcon });

      marker.on('click', () => {
        this.selectedSite.set(site);
      });

      this.markersGroup?.addLayer(marker);
    });
  }

  private buildFilterOptions(sitesList: SiteModel[]): void {
    const countries = Array.from(new Set(sitesList.map(s => s.country))).sort();
    const products = Array.from(new Set(sitesList.flatMap(s => s.products))).sort();

    countries.forEach(c => this.countryOptions.push({ label: c, value: c }));
    products.forEach(p => this.productOptions.push({ label: p, value: p }));
    sitesList.forEach(s => this.siteOptions.push({ label: s.name, value: s.id }));
  }

  applyFilters(): void {
    let result = this.allSites();

    if (this.filterCountry) {
      result = result.filter(s => s.country === this.filterCountry);
    }
    if (this.filterProduct) {
      result = result.filter(s => s.products.includes(this.filterProduct));
    }
    if (this.filterSite) {
      result = result.filter(s => s.id === this.filterSite);
    }
    if (this.filterStatus) {
      result = result.filter(s => s.status === this.filterStatus);
    }

    this.filteredSites.set(result);
    this.renderMarkers();
  }

  resetFilters(): void {
    this.filterCountry = '';
    this.filterProduct = '';
    this.filterSite = '';
    this.filterStatus = '';
    this.filteredSites.set(this.allSites());
    this.renderMarkers();
  }

  selectSiteContext(site: SiteModel): void {
    this.siteFilterService.selectSite(site.id, site.name, site.country);
    this.messageService.add({
      severity: 'success',
      summary: 'Site Filter Set',
      detail: `Data dashboard scoped to ${site.name}`
    });
  }

  clearSiteContext(): void {
    this.siteFilterService.clearFilter();
    this.messageService.add({
      severity: 'info',
      summary: 'Filter Removed',
      detail: 'Dashboard scoped to global AVOCarbon worldwide operations'
    });
  }

  formatPercent(val?: number): string {
    if (val === undefined || val === null) return '0.0%';
    return (val * 100).toFixed(1) + '%';
  }

  getStatusSeverity(status: string): 'success' | 'warn' | 'danger' | 'secondary' {
    switch (status) {
      case 'Operational': return 'success';
      case 'Maintenance': return 'warn';
      case 'Offline': return 'danger';
      default: return 'secondary';
    }
  }
}
