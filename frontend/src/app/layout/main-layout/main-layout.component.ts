import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { TopbarComponent } from '../topbar/topbar.component';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, TopbarComponent, SidebarComponent],
  template: `
    <div class="min-h-screen flex flex-col bg-[#F4F6F9]">
      <app-topbar (toggleSidebar)="sidebarExpanded.set(!sidebarExpanded())"></app-topbar>
      <div class="flex flex-1 overflow-hidden">
        <app-sidebar [expanded]="sidebarExpanded()"></app-sidebar>
        <main class="flex-1 p-8 overflow-y-auto max-h-[calc(100vh-4rem)]">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `
})
export class MainLayoutComponent {
  sidebarExpanded = signal<boolean>(true);
}
