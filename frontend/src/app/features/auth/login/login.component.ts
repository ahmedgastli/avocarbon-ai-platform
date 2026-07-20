import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LogoComponent } from '../../../shared/components/logo/logo.component';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LogoComponent,
    CardModule,
    ButtonModule,
    CheckboxModule
  ],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-[#155A8A] via-[#0076C8] to-[#155A8A] flex flex-col justify-between p-6 relative overflow-hidden">
      <!-- Background Decorative Blobs -->
      <div class="absolute -top-32 -left-32 w-96 h-96 bg-[#F58220]/20 rounded-full blur-3xl pointer-events-none"></div>
      <div class="absolute -bottom-32 -right-32 w-96 h-96 bg-[#0076C8]/40 rounded-full blur-3xl pointer-events-none"></div>

      <!-- Top Tag -->
      <div class="flex items-center justify-between z-10">
        <app-logo [size]="'small'"></app-logo>
        <span class="text-xs font-bold text-white bg-white/10 px-3 py-1 rounded-full border border-white/20">
          Plant Intelligence v2.0
        </span>
      </div>

      <!-- Centered White Login Card -->
      <div class="w-full max-w-md mx-auto my-auto bg-white rounded-3xl shadow-2xl p-8 border border-white/20 z-10 relative">
        <div class="text-center mb-8">
          <div class="mb-4 inline-block">
            <app-logo [size]="'large'"></app-logo>
          </div>
          <h2 class="text-xl font-black text-[#155A8A] tracking-tight">Plant Intelligence Sign In</h2>
          <p class="text-xs text-[#666666] italic mt-1 font-medium">Doing the right things, the right way.</p>
        </div>

        @if (errorMessage()) {
          <div class="mb-5 p-3.5 bg-red-50 border-l-4 border-red-500 text-red-700 text-xs rounded-xl font-semibold shadow-2xs">
            {{ errorMessage() }}
          </div>
        }

        <form (ngSubmit)="onLogin()" class="space-y-5">
          <!-- Email Field -->
          <div class="space-y-1.5">
            <label class="block text-xs font-extrabold text-[#155A8A] uppercase tracking-wider">Email Address</label>
            <div class="relative w-full">
              <i class="pi pi-envelope absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-sm z-10 pointer-events-none"></i>
              <input type="email" [(ngModel)]="email" name="email" required placeholder="admin@avocarbon.com" class="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:border-[#0076C8] focus:ring-2 focus:ring-[#0076C8]/20 focus:outline-none transition-all" />
            </div>
          </div>

          <!-- Password Field -->
          <div class="space-y-1.5">
            <label class="block text-xs font-extrabold text-[#155A8A] uppercase tracking-wider">Password</label>
            <div class="relative w-full">
              <i class="pi pi-lock absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-sm z-10 pointer-events-none"></i>
              <input type="password" [(ngModel)]="password" name="password" required placeholder="••••••••" class="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:border-[#0076C8] focus:ring-2 focus:ring-[#0076C8]/20 focus:outline-none transition-all" />
            </div>
          </div>

          <!-- Remember Me & Forgot Password -->
          <div class="flex items-center justify-between text-xs pt-1">
            <label class="flex items-center space-x-2 cursor-pointer">
              <p-checkbox [(ngModel)]="rememberMe" [binary]="true" name="rememberMe"></p-checkbox>
              <span class="text-[#666666] font-medium">Remember me</span>
            </label>
            <a href="#" (click)="$event.preventDefault()" class="text-[#0076C8] hover:underline font-bold">Forgot password?</a>
          </div>

          <!-- Sign In Button -->
          <button type="submit" 
                  [disabled]="loading()"
                  class="w-full py-3.5 px-4 bg-[#0076C8] hover:bg-[#F58220] text-white font-black rounded-2xl shadow-lg hover:shadow-xl transition-all duration-200 flex items-center justify-center space-x-2 text-sm">
            @if (loading()) {
              <i class="pi pi-spin pi-spinner text-lg"></i>
              <span>Signing in...</span>
            } @else {
              <i class="pi pi-sign-in text-base"></i>
              <span>Sign In to Dashboard</span>
            }
          </button>
        </form>

        <div class="mt-8 pt-6 border-t border-gray-100 text-center text-[11px] text-[#666666]">
          Protected by AVOCarbon Enterprise Security.
        </div>
      </div>

      <!-- Bottom Tagline -->
      <div class="text-center text-xs text-white/80 z-10 font-medium">
        &copy; 2026 AVOCarbon Group. All Rights Reserved.
      </div>
    </div>
  `
})
export class LoginComponent {
  authService = inject(AuthService);
  private router = inject(Router);

  email = 'admin@avocarbon.com';
  password = 'admin12345';
  rememberMe = true;

  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  onLogin(): void {
    if (!this.email || !this.password) {
      this.errorMessage.set('Please enter your email and password.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err: any) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message || 'Invalid email or password. Please try again.');
      }
    });
  }
}
