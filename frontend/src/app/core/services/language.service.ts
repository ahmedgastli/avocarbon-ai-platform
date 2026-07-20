import { Injectable, signal } from '@angular/core';

export type SupportedLanguage = 'en' | 'fr' | 'de';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  currentLang = signal<SupportedLanguage>('en');

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
