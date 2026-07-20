import { Component, inject, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { ProjectService } from '../../core/services/project.service';
import { LanguageService, SupportedLanguage } from '../../core/services/language.service';
import { LogoComponent } from '../../shared/components/logo/logo.component';
import { DropdownModule } from 'primeng/dropdown';
import { FormsModule } from '@angular/forms';

export interface SearchResultItem {
  title: string;
  category: 'Pages' | 'Projects' | 'Data Sources' | 'Users';
  icon: string;
  route: string;
}

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterLink, LogoComponent, DropdownModule, FormsModule],
  template: `
    <header class="h-16 bg-white border-b border-gray-200 px-6 flex items-center justify-between shadow-xs sticky top-0 z-40">
      <!-- Left: Toggle, Official Logo & Breadcrumb -->
      <div class="flex items-center space-x-6">
        <button (click)="toggleSidebar.emit()" 
                class="p-2 rounded-xl text-gray-500 hover:text-[#0076C8] hover:bg-gray-100 transition-colors focus:outline-none"
                title="Toggle Sidebar">
          <i class="pi pi-bars text-xl"></i>
        </button>

        <app-logo [size]="'small'" routerLink="/dashboard"></app-logo>

        <!-- Dynamic Breadcrumb Navigation -->
        <div class="hidden md:flex items-center space-x-2 text-xs text-[#666666] border-l border-gray-200 pl-6">
          <i class="pi pi-home text-[#0076C8]"></i>
          <span>/</span>
          <span class="font-bold text-[#155A8A] capitalize">{{ currentRouteTitle() }}</span>
        </div>
      </div>

      <!-- Center: Integrated Global Search Container -->
      <div class="hidden lg:flex items-center flex-1 max-w-md mx-8 relative">
        <div class="relative w-full">
          <i class="pi pi-search absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-sm z-10 pointer-events-none"></i>
          <input type="text" 
                 [(ngModel)]="searchQuery"
                 (input)="onSearchInput()"
                 (focus)="searchFocused.set(true)"
                 placeholder="Search OEE metrics, projects, data sources..." 
                 class="w-full pl-10 pr-4 py-2 bg-gray-100/80 hover:bg-gray-100 focus:bg-white text-xs font-medium text-gray-800 rounded-xl border border-transparent focus:border-[#0076C8] focus:ring-2 focus:ring-[#0076C8]/20 focus:outline-none transition-all duration-200 shadow-2xs" />
        </div>

        <!-- Live Search Suggestions Overlay -->
        @if (searchFocused() && searchQuery.trim().length > 0) {
          <div class="absolute top-12 left-0 right-0 bg-white rounded-2xl shadow-2xl border border-gray-100 py-3 z-50 max-h-80 overflow-y-auto">
            @for (item of searchResults(); track item.title) {
              <div (click)="selectSearchResult(item)" 
                   class="px-4 py-2.5 hover:bg-[#0076C8]/10 cursor-pointer flex items-center justify-between transition-colors">
                <div class="flex items-center space-x-3">
                  <div class="w-7 h-7 rounded-lg bg-gray-100 text-[#0076C8] flex items-center justify-center text-xs">
                    <i [class]="item.icon"></i>
                  </div>
                  <div>
                    <div class="text-xs font-bold text-[#155A8A]">{{ item.title }}</div>
                    <div class="text-[10px] text-gray-400 font-semibold">{{ item.category }}</div>
                  </div>
                </div>
                <i class="pi pi-arrow-right text-xs text-gray-400"></i>
              </div>
            } @empty {
              <div class="px-4 py-6 text-center text-xs text-gray-400">
                No matching results found for "{{ searchQuery }}"
              </div>
            }
          </div>
        }
      </div>

      <!-- Right Controls: Language Selector, Notifications, User Avatar -->
      <div class="flex items-center space-x-4">
        <!-- Language Selector -->
        <p-dropdown [options]="languages" 
                    [(ngModel)]="currentLanguage" 
                    (onChange)="onLanguageChange($event.value)"
                    optionLabel="label" 
                    optionValue="value" 
                    styleClass="p-inputtext-sm w-20 border-0 bg-gray-50 rounded-xl text-xs font-semibold">
        </p-dropdown>

        <!-- Notification Bell -->
        <button class="relative p-2.5 rounded-xl text-gray-500 hover:text-[#0076C8] hover:bg-gray-100 transition-colors" title="Notifications">
          <i class="pi pi-bell text-lg"></i>
          <span class="absolute top-2 right-2 w-2 h-2 rounded-full bg-[#F58220]"></span>
        </button>

        <!-- User Profile Avatar & Role -->
        @if (authService.currentUserSignal(); as user) {
          <div class="flex items-center space-x-3 border-l border-gray-200 pl-4">
            <div class="w-9 h-9 rounded-xl bg-[#155A8A] text-white flex items-center justify-center font-black text-xs shadow-sm border border-[#0076C8]">
              {{ user.firstName[0] }}{{ user.lastName[0] }}
            </div>
            <div class="hidden xl:block text-left text-xs">
              <div class="font-extrabold text-[#155A8A] tracking-tight">{{ user.firstName }} {{ user.lastName }}</div>
              <div class="text-[#666666] text-[10px] font-bold uppercase tracking-wider">{{ user.role }}</div>
            </div>
          </div>
        }
      </div>
    </header>
  `
})
export class TopbarComponent {
  authService = inject(AuthService);
  langService = inject(LanguageService);
  private projectService = inject(ProjectService);
  private router = inject(Router);
  
  toggleSidebar = output<void>();
  currentRouteTitle = signal<string>('Dashboard');

  searchQuery = '';
  searchFocused = signal<boolean>(false);
  searchResults = signal<SearchResultItem[]>([]);

  languages = [
    { label: 'EN', value: 'en' },
    { label: 'FR', value: 'fr' },
    { label: 'DE', value: 'de' }
  ];
  currentLanguage = this.langService.currentLang();

  constructor() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      const url = event.urlAfterRedirects || event.url;
      const cleanUrl = url.split('?')[0].replace('/', '');
      this.currentRouteTitle.set(cleanUrl.replace('-', ' ') || 'Dashboard');
    });
  }

  onLanguageChange(lang: SupportedLanguage): void {
    this.langService.setLanguage(lang);
  }

  onSearchInput(): void {
    const query = this.searchQuery.trim().toLowerCase();
    if (!query) {
      this.searchResults.set([]);
      return;
    }

    const items: SearchResultItem[] = [];

    const pages = [
      { title: 'Operations Dashboard', category: 'Pages' as const, icon: 'pi pi-th-large', route: '/dashboard' },
      { title: 'Project Management', category: 'Pages' as const, icon: 'pi pi-folder', route: '/projects' },
      { title: 'API Data Sources', category: 'Pages' as const, icon: 'pi pi-database', route: '/data-sources' },
      { title: 'Deep Analytics & Trends', category: 'Pages' as const, icon: 'pi pi-line-chart', route: '/analytics' }
    ];

    if (this.authService.isAdmin()) {
      pages.push(
        { title: 'Personnel & User Management', category: 'Pages' as const, icon: 'pi pi-users', route: '/users' },
        { title: 'Platform Administration Settings', category: 'Pages' as const, icon: 'pi pi-cog', route: '/settings' }
      );
    }

    pages.forEach(p => {
      if (p.title.toLowerCase().includes(query)) {
        items.push(p);
      }
    });

    this.projectService.getAllProjects().subscribe({
      next: (projs) => {
        projs.forEach(proj => {
          if (proj.name.toLowerCase().includes(query) || proj.description?.toLowerCase().includes(query)) {
            items.push({
              title: proj.name,
              category: 'Projects',
              icon: 'pi pi-folder-open',
              route: '/projects'
            });
          }
        });
        this.searchResults.set(items);
      },
      error: () => this.searchResults.set(items)
    });
  }

  selectSearchResult(item: SearchResultItem): void {
    this.searchFocused.set(false);
    this.searchQuery = '';
    this.router.navigate([item.route]);
  }
}
