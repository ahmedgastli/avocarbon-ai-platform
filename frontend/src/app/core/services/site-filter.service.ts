import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SiteFilterService {
  selectedSiteId = signal<string | null>(null);
  selectedSiteName = signal<string | null>(null);
  selectedCountry = signal<string | null>(null);

  selectSite(siteId: string | null, name: string | null = null, country: string | null = null): void {
    this.selectedSiteId.set(siteId);
    this.selectedSiteName.set(name);
    this.selectedCountry.set(country);
  }

  clearFilter(): void {
    this.selectedSiteId.set(null);
    this.selectedSiteName.set(null);
    this.selectedCountry.set(null);
  }
}
