import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer class="h-10 bg-white border-t border-gray-200 px-6 flex items-center justify-between text-xs text-[#666666]">
      <div>
        <span class="font-semibold text-[#155A8A]">AVOCarbon Group</span> &copy; 2026. <span class="italic text-gray-400">Doing the right things, the right way.</span>
      </div>
      <div class="flex items-center space-x-4">
        <span>Sprint 2 Active</span>
        <span class="text-gray-300">|</span>
        <span class="text-[#0076C8] font-medium">PostgreSQL & OEE Analytics</span>
      </div>
    </footer>
  `
})
export class FooterComponent {}
