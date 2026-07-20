import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-logo',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="inline-flex items-center cursor-pointer select-none">
      <img src="assets/logo.svg" 
           alt="AVOCarbon Group" 
           [class]="heightClass()"
           class="object-contain transition-opacity hover:opacity-90" />
    </div>
  `
})
export class LogoComponent {
  size = input<'small' | 'normal' | 'large'>('normal');

  get heightClass(): () => string {
    return () => {
      switch (this.size()) {
        case 'small': return 'h-7';
        case 'large': return 'h-16';
        default: return 'h-9';
      }
    };
  }
}
