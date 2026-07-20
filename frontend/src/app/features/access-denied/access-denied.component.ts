import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-[calc(100vh-8rem)] flex flex-col items-center justify-center p-6 text-center">
      <div class="w-20 h-20 rounded-3xl bg-red-50 text-red-600 flex items-center justify-center text-3xl font-black mb-4 border border-red-200 shadow-sm">
        403
      </div>
      <h1 class="text-2xl font-black text-[#155A8A] mb-2 tracking-tight">Access Restricted</h1>
      <p class="text-xs text-[#666666] max-w-md mb-6 font-medium">
        You do not possess the required administrator privileges to view this area. Please contact your system administrator if you require access.
      </p>
      <a routerLink="/dashboard" class="px-6 py-3 bg-[#0076C8] text-white font-bold rounded-2xl shadow hover:bg-[#F58220] transition-colors inline-flex items-center space-x-2 text-xs">
        <i class="pi pi-arrow-left"></i>
        <span>Return to Operations Dashboard</span>
      </a>
    </div>
  `
})
export class AccessDeniedComponent {}
