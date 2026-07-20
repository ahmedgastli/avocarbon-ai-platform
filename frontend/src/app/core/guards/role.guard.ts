import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const expectedRole = route.data?.['role'] as string;

  if (authService.isAuthenticated()) {
    if (!expectedRole || authService.currentUserSignal()?.role === expectedRole) {
      return true;
    }
    router.navigate(['/access-denied']);
    return false;
  }

  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
