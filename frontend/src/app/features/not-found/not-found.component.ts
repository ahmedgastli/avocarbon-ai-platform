import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="min-h-screen bg-[#F4F6F9] flex flex-col items-center justify-center p-6 text-center">
      <div class="w-20 h-20 rounded-2xl bg-[#0076C8]/10 text-[#0076C8] flex items-center justify-center text-4xl font-extrabold mb-4">
        404
      </div>
      <h1 class="text-3xl font-black text-[#155A8A] mb-2">Page Not Found</h1>
      <p class="text-sm text-[#666666] max-w-md mb-6">
        The industrial dashboard route you are looking for does not exist or has been relocated within the platform.
      </p>
      <a routerLink="/dashboard" class="px-6 py-3 bg-[#0076C8] text-white font-bold rounded-xl shadow hover:bg-[#F58220] transition-colors inline-flex items-center space-x-2">
        <i class="pi pi-home"></i>
        <span>Return to Dashboard</span>
      </a>
    </div>
  `
})
export class NotFoundComponent {}
