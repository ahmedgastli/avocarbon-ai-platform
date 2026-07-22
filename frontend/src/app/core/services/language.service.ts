import { Injectable, signal, computed } from '@angular/core';

export type SupportedLanguage = 'en' | 'fr' | 'de';

export interface Translations {
  dashboard: string;
  globalOperations: string;
  projects: string;
  dataSources: string;
  users: string;
  analytics: string;
  aiInsights: string;
  settings: string;
  logout: string;
  searchPlaceholder: string;
  plantIntelligence: string;
  connected: string;
  version: string;
  poweredBy: string;
}

const DICTIONARY: Record<SupportedLanguage, Translations> = {
  en: {
    dashboard: 'Dashboard',
    globalOperations: 'Global Operations',
    projects: 'Projects',
    dataSources: 'Data Sources',
    users: 'Users',
    analytics: 'Analytics',
    aiInsights: 'AI Insights (Coming Soon)',
    settings: 'Settings',
    logout: 'Logout',
    searchPlaceholder: 'Search OEE metrics, projects, data sources...',
    plantIntelligence: 'Plant Intelligence',
    connected: 'Line 1 Connected',
    version: 'Version 2.0',
    poweredBy: 'Powered by AVOCarbon'
  },
  fr: {
    dashboard: 'Tableau de Bord',
    globalOperations: 'Opérations Globales',
    projects: 'Projets',
    dataSources: 'Sources de Données',
    users: 'Utilisateurs',
    analytics: 'Analytique',
    aiInsights: 'Insights IA (Bientôt)',
    settings: 'Paramètres',
    logout: 'Déconnexion',
    searchPlaceholder: 'Rechercher métriques TRS, projets...',
    plantIntelligence: 'Intelligence Industrielle',
    connected: 'Ligne 1 Connectée',
    version: 'Version 2.0',
    poweredBy: 'Propulsé par AVOCarbon'
  },
  de: {
    dashboard: 'Dashboard',
    globalOperations: 'Globale Operationen',
    projects: 'Projekte',
    dataSources: 'Datenquellen',
    users: 'Benutzer',
    analytics: 'Analytik',
    aiInsights: 'KI-Einblicke (Demnächst)',
    settings: 'Einstellungen',
    logout: 'Abmelden',
    searchPlaceholder: 'OEE-Metriken, Projekte suchen...',
    plantIntelligence: 'Anlagen-Intelligenz',
    connected: 'Linie 1 Verbunden',
    version: 'Version 2.0',
    poweredBy: 'Bereitgestellt von AVOCarbon'
  }
};

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  currentLang = signal<SupportedLanguage>('en');

  t = computed(() => DICTIONARY[this.currentLang()]);

  setLanguage(lang: SupportedLanguage): void {
    this.currentLang.set(lang);
    localStorage.setItem('pref_lang', lang);
  }

  constructor() {
    const saved = localStorage.getItem('pref_lang') as SupportedLanguage;
    if (saved && ['en', 'fr', 'de'].includes(saved)) {
      this.currentLang.set(saved);
    }
  }
}
