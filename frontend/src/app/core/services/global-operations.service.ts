import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface SiteModel {
  id: string;
  name: string;
  city: string;
  country: string;
  lat: number;
  lng: number;
  products: string[];
  isHeadquarters: boolean;
  status: 'Operational' | 'Maintenance' | 'Offline';
  connectedApis: number;
  projectsCount: number;
  metrics: {
    oee: number;
    availability: number;
    performance: number;
    quality: number;
    scrapRate: number;
  };
}

@Injectable({
  providedIn: 'root'
})
export class GlobalOperationsService {
  private sites: SiteModel[] = [
    {
      id: 'luxembourg',
      name: 'AVOCarbon Luxembourg (HQ)',
      city: 'Windhof',
      country: 'Luxembourg',
      lat: 49.6486,
      lng: 5.9622,
      products: ['Brushes', 'Assemblies', 'Chokes', 'Seals'],
      isHeadquarters: true,
      status: 'Operational',
      connectedApis: 3,
      projectsCount: 2,
      metrics: { oee: 0.915, availability: 0.94, performance: 0.96, quality: 0.991, scrapRate: 0.009 }
    },
    {
      id: 'tunisia',
      name: 'AVOCarbon Tunisia',
      city: 'Elfahs',
      country: 'Tunisia',
      lat: 36.3748,
      lng: 9.9075,
      products: ['Brushes', 'Assemblies', 'Chokes', 'Seals'],
      isHeadquarters: false,
      status: 'Operational',
      connectedApis: 2,
      projectsCount: 1,
      metrics: { oee: 0.821, availability: 0.86, performance: 0.88, quality: 0.982, scrapRate: 0.018 }
    },
    {
      id: 'france-poitiers',
      name: 'AVOCarbon France (Poitiers)',
      city: 'Poitiers',
      country: 'France',
      lat: 46.5802,
      lng: 0.3404,
      products: ['Brushes', 'Assemblies'],
      isHeadquarters: false,
      status: 'Maintenance',
      connectedApis: 1,
      projectsCount: 1,
      metrics: { oee: 0.784, availability: 0.82, performance: 0.85, quality: 0.965, scrapRate: 0.035 }
    },
    {
      id: 'france-amiens',
      name: 'AVOCarbon France (Cyclam)',
      city: 'Amiens',
      country: 'France',
      lat: 49.8941,
      lng: 2.2957,
      products: ['Seals', 'Bushings', 'Rotors/Vanes'],
      isHeadquarters: false,
      status: 'Operational',
      connectedApis: 2,
      projectsCount: 0,
      metrics: { oee: 0.850, availability: 0.88, performance: 0.90, quality: 0.990, scrapRate: 0.010 }
    },
    {
      id: 'germany',
      name: 'AVOCarbon Germany',
      city: 'Frankfurt',
      country: 'Germany',
      lat: 50.1109,
      lng: 8.6821,
      products: ['Brushes', 'Assemblies', 'Seals'],
      isHeadquarters: false,
      status: 'Offline',
      connectedApis: 0,
      projectsCount: 0,
      metrics: { oee: 0.0, availability: 0.0, performance: 0.0, quality: 0.0, scrapRate: 0.0 }
    },
    {
      id: 'india',
      name: 'AVOCarbon India',
      city: 'Chennai',
      country: 'India',
      lat: 13.0827,
      lng: 80.2707,
      products: ['Brushes', 'Assemblies', 'Chokes', 'Seals'],
      isHeadquarters: false,
      status: 'Operational',
      connectedApis: 2,
      projectsCount: 0,
      metrics: { oee: 0.765, availability: 0.80, performance: 0.82, quality: 0.970, scrapRate: 0.030 }
    },
    {
      id: 'china-tianjin',
      name: 'AVOCarbon China (Tianjin)',
      city: 'Tianjin',
      country: 'China',
      lat: 39.3434,
      lng: 117.3616,
      products: ['Brushes', 'Assemblies'],
      isHeadquarters: false,
      status: 'Operational',
      connectedApis: 2,
      projectsCount: 0,
      metrics: { oee: 0.880, availability: 0.91, performance: 0.92, quality: 0.992, scrapRate: 0.008 }
    },
    {
      id: 'china-kunshan',
      name: 'AVOCarbon China (Kunshan)',
      city: 'Kunshan',
      country: 'China',
      lat: 31.3850,
      lng: 120.9808,
      products: ['Assemblies', 'Chokes'],
      isHeadquarters: false,
      status: 'Operational',
      connectedApis: 1,
      projectsCount: 0,
      metrics: { oee: 0.812, availability: 0.85, performance: 0.87, quality: 0.980, scrapRate: 0.020 }
    },
    {
      id: 'korea',
      name: 'AVOCarbon Korea',
      city: 'Daegu',
      country: 'Korea',
      lat: 35.8714,
      lng: 128.6014,
      products: ['Brushes', 'Assemblies', 'Seals'],
      isHeadquarters: false,
      status: 'Offline',
      connectedApis: 0,
      projectsCount: 0,
      metrics: { oee: 0.0, availability: 0.0, performance: 0.0, quality: 0.0, scrapRate: 0.0 }
    },
    {
      id: 'mexico',
      name: 'AVOCarbon Mexico',
      city: 'Monterrey',
      country: 'Mexico',
      lat: 25.6866,
      lng: -100.3161,
      products: ['Brushes', 'Assemblies', 'Chokes', 'Seals'],
      isHeadquarters: false,
      status: 'Operational',
      connectedApis: 2,
      projectsCount: 0,
      metrics: { oee: 0.846, availability: 0.89, performance: 0.90, quality: 0.985, scrapRate: 0.015 }
    }
  ];

  getSites(): Observable<SiteModel[]> {
    return of(this.sites);
  }
}
