import { Component, input, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside [class.w-\\[280px\\]]="expanded()" [class.w-20]="!expanded()" 
           class="bg-[#155A8A] text-white transition-all duration-300 ease-in-out flex flex-col justify-between shadow-2xl h-[calc(100vh-4rem)] sticky top-16 z-30 select-none border-r border-white/10 shrink-0">
      <!-- Nav Items List -->
      <div class="py-6 px-3 space-y-1.5 overflow-y-auto">
        @for (item of visibleMenuItems(); track item.label) {
          @if (item.action === 'logout') {
            <button (click)="authService.logout()" 
                    class="w-full flex items-center space-x-3.5 px-4 py-3.5 rounded-xl text-red-200 hover:bg-red-600/30 hover:text-white transition-all duration-200 group text-left">
              <i [class]="item.icon + ' text-lg transition-transform group-hover:scale-110 text-red-400'"></i>
              @if (expanded()) {
                <span class="text-xs font-bold tracking-wide whitespace-nowrap">{{ item.label }}</span>
              }
            </button>
          } @else if (item.disabled) {
            <div class="flex items-center justify-between px-4 py-3.5 rounded-xl text-gray-300 opacity-60 cursor-not-allowed">
              <div class="flex items-center space-x-3.5">
                <i [class]="item.icon + ' text-lg'"></i>
                @if (expanded()) {
                  <span class="text-xs font-semibold tracking-wide whitespace-nowrap">{{ item.label }}</span>
                }
              </div>
              @if (expanded()) {
                <span class="text-[9px] font-black bg-[#F58220] text-white px-2 py-0.5 rounded-md uppercase tracking-wider">SOON</span>
              }
            </div>
          } @else {
            <a [routerLink]="item.route" 
               routerLinkActive="bg-[#0076C8] text-white font-extrabold shadow-lg border-l-4 border-[#F58220]" 
               [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }"
               class="flex items-center space-x-3.5 px-4 py-3.5 rounded-xl text-gray-200 hover:bg-[#0076C8]/60 hover:text-white transition-all duration-200 group">
              <i [class]="item.icon + ' text-lg transition-transform group-hover:scale-110'"></i>
              @if (expanded()) {
                <span class="text-xs font-semibold tracking-wide whitespace-nowrap">{{ item.label }}</span>
              }
            </a>
          }
        }
      </div>

      <!-- Version Label Inside Sidebar Footer -->
      <div class="p-5 border-t border-white/10 bg-[#0076C8]/20 text-center">
        @if (expanded()) {
          <div class="text-xs font-black text-[#F58220] uppercase tracking-wider">Version 2.0</div>
          <div class="text-[11px] text-gray-300 font-medium mt-0.5">Powered by AVOCarbon</div>
        } @else {
          <div class="text-[10px] font-bold text-[#F58220]">v2.0</div>
        }
      </div>
    </aside>
  `
})
export class SidebarComponent {
  authService = inject(AuthService);
  expanded = input<boolean>(true);

  allMenuItems = [
    { label: 'Dashboard', icon: 'pi pi-th-large', route: '/dashboard' },
    { label: 'Projects', icon: 'pi pi-folder', route: '/projects' },
    { label: 'Data Sources', icon: 'pi pi-database', route: '/data-sources' },
    { label: 'Users', icon: 'pi pi-users', route: '/users', adminOnly: true },
    { label: 'Analytics', icon: 'pi pi-line-chart', route: '/analytics' },
    { label: 'AI Insights (Coming Soon)', icon: 'pi pi-sparkles', route: '/ai-insights', disabled: true },
    { label: 'Settings', icon: 'pi pi-cog', route: '/settings', adminOnly: true },
    { label: 'Logout', icon: 'pi pi-power-off', route: null, action: 'logout' }
  ];

  visibleMenuItems = computed(() => {
    const isAdmin = this.authService.isAdmin();
    return this.allMenuItems.filter(item => !item.adminOnly || isAdmin);
  });
}
