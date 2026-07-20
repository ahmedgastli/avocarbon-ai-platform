import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { UserRequest, UserResponse, Role } from '../../core/models/auth.model';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { MessageService, ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DialogModule,
    DropdownModule,
    TagModule,
    ToastModule,
    ConfirmDialogModule
  ],
  providers: [MessageService, ConfirmationService],
  template: `
    <div class="space-y-6 pb-8">
      <p-toast></p-toast>
      <p-confirmDialog header="Delete Confirmation" icon="pi pi-exclamation-triangle"></p-confirmDialog>

      <!-- Header Toolbar Card -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <div>
          <h1 class="text-2xl font-black text-[#155A8A] tracking-tight">Personnel & Security Management</h1>
          <p class="text-xs text-[#666666] mt-1">Manage platform access, role privileges, and security credentials</p>
        </div>

        <button pButton label="Add New User" icon="pi pi-user-plus" class="p-button-accent" (click)="openCreateDialog()"></button>
      </div>

      <!-- Users DataTable Card -->
      <div class="bg-white rounded-3xl p-6 shadow-sm border border-gray-100">
        <p-table 
          [value]="users()" 
          [paginator]="true" 
          [rows]="10" 
          [loading]="loading()"
          [globalFilterFields]="['id', 'firstName', 'lastName', 'email', 'role']"
          #dt
          styleClass="p-datatable-striped">
          
          <ng-template pTemplate="caption">
            <div class="flex items-center justify-between pb-3">
              <span class="text-sm font-bold text-[#155A8A]">Registered Personnel ({{ users().length }})</span>
              <div class="p-inputgroup w-72">
                <span class="p-inputgroup-addon bg-gray-50 text-gray-400"><i class="pi pi-search"></i></span>
                <input pInputText type="text" (input)="dt.filterGlobal($any($event.target).value, 'contains')" placeholder="Search all columns..." class="p-inputtext-sm text-xs" />
              </div>
            </div>
          </ng-template>

          <ng-template pTemplate="header">
            <tr>
              <th pSortableColumn="id">ID <p-sortIcon field="id"></p-sortIcon></th>
              <th pSortableColumn="firstName">Name <p-sortIcon field="firstName"></p-sortIcon></th>
              <th pSortableColumn="email">Email Address <p-sortIcon field="email"></p-sortIcon></th>
              <th pSortableColumn="role">Security Role <p-sortIcon field="role"></p-sortIcon></th>
              <th>Status</th>
              <th pSortableColumn="createdAt">Registered Date <p-sortIcon field="createdAt"></p-sortIcon></th>
              <th class="text-right">Actions</th>
            </tr>
          </ng-template>

          <ng-template pTemplate="body" let-user>
            <tr>
              <td class="font-bold text-gray-400">#{{ user.id }}</td>
              <td class="font-bold text-[#155A8A]">
                <div class="flex items-center space-x-2.5">
                  <div class="w-8 h-8 rounded-xl bg-[#0076C8]/10 text-[#0076C8] font-black flex items-center justify-center text-xs border border-[#0076C8]/20">
                    {{ user.firstName[0] }}{{ user.lastName[0] }}
                  </div>
                  <span>{{ user.firstName }} {{ user.lastName }}</span>
                </div>
              </td>
              <td class="text-xs text-gray-600 font-mono">{{ user.email }}</td>
              <td>
                <p-tag [value]="user.role" [severity]="getRoleSeverity(user.role)"></p-tag>
              </td>
              <td>
                <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-emerald-50 text-emerald-700">
                  Active
                </span>
              </td>
              <td class="text-xs text-gray-500 font-medium">{{ user.createdAt | date:'mediumDate' }}</td>
              <td class="text-right space-x-2">
                <button pButton icon="pi pi-pencil" class="p-button-rounded p-button-text p-button-info p-button-sm" (click)="openEditDialog(user)"></button>
                <button pButton icon="pi pi-trash" class="p-button-rounded p-button-text p-button-danger p-button-sm" (click)="confirmDelete(user)"></button>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>

      <!-- Add / Edit User Modal Dialog -->
      <p-dialog [(visible)]="displayDialog" [header]="isEditMode ? 'Edit User' : 'Create New User'" [modal]="true" styleClass="w-full max-w-lg p-fluid rounded-3xl">
        <ng-template pTemplate="content">
          <div class="space-y-4 pt-2">
            <div class="grid grid-cols-2 gap-4">
              <div class="field">
                <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">First Name *</label>
                <input pInputText [(ngModel)]="currentRequest.firstName" placeholder="Jane" required />
              </div>
              <div class="field">
                <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Last Name *</label>
                <input pInputText [(ngModel)]="currentRequest.lastName" placeholder="Smith" required />
              </div>
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Email Address *</label>
              <input pInputText type="email" [(ngModel)]="currentRequest.email" placeholder="jane.smith@avocarbon.com" required />
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">
                Password {{ isEditMode ? '(Optional — Leave blank to keep existing)' : '*' }}
              </label>
              <input pInputText type="password" [(ngModel)]="currentRequest.password" [placeholder]="isEditMode ? '••••••••' : 'Password123!'" [required]="!isEditMode" />
            </div>

            <div class="field">
              <label class="block text-xs font-bold text-[#155A8A] uppercase mb-1">Security Role *</label>
              <p-dropdown [options]="roleOptions" 
                          optionLabel="label" 
                          optionValue="value" 
                          [(ngModel)]="currentRequest.role" 
                          placeholder="Select Role"
                          appendTo="body"
                          styleClass="w-full">
              </p-dropdown>
            </div>
          </div>
        </ng-template>

        <ng-template pTemplate="footer">
          <button pButton label="Cancel" icon="pi pi-times" class="p-button-text" (click)="displayDialog = false"></button>
          <button pButton [label]="isEditMode ? 'Save User' : 'Create User'" icon="pi pi-check" class="p-button-accent" (click)="saveUser()"></button>
        </ng-template>
      </p-dialog>
    </div>
  `
})
export class UsersComponent implements OnInit {
  private userService = inject(UserService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  users = signal<UserResponse[]>([]);
  loading = signal<boolean>(true);

  displayDialog = false;
  isEditMode = false;
  selectedUserId: number | null = null;

  roleOptions = [
    { label: 'Administrator (ADMIN)', value: 'ADMIN' },
    { label: 'Production Manager', value: 'PRODUCTION_MANAGER' },
    { label: 'Quality Manager', value: 'QUALITY_MANAGER' },
    { label: 'Direction / Executive', value: 'DIRECTION' }
  ];

  currentRequest: UserRequest = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'PRODUCTION_MANAGER'
  };

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.userService.getAllUsers().subscribe({
      next: (list) => {
        this.users.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.messageService.add({ severity: 'error', summary: 'Fetch Error', detail: err.error?.message || 'Failed to load users' });
      }
    });
  }

  openCreateDialog(): void {
    this.isEditMode = false;
    this.selectedUserId = null;
    this.currentRequest = {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      role: 'PRODUCTION_MANAGER'
    };
    this.displayDialog = true;
  }

  openEditDialog(user: UserResponse): void {
    this.isEditMode = true;
    this.selectedUserId = user.id;
    this.currentRequest = {
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      password: '',
      role: user.role
    };
    this.displayDialog = true;
  }

  saveUser(): void {
    if (!this.currentRequest.email || !this.currentRequest.firstName || !this.currentRequest.lastName) {
      this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: 'First name, last name, and email are required' });
      return;
    }

    if (!this.isEditMode && (!this.currentRequest.password || this.currentRequest.password.trim() === '')) {
      this.messageService.add({ severity: 'error', summary: 'Validation Error', detail: 'Password is required when registering a new user' });
      return;
    }

    const payload: UserRequest = {
      firstName: this.currentRequest.firstName,
      lastName: this.currentRequest.lastName,
      email: this.currentRequest.email,
      role: this.currentRequest.role
    };

    if (this.currentRequest.password && this.currentRequest.password.trim() !== '') {
      payload.password = this.currentRequest.password;
    }

    if (this.isEditMode && this.selectedUserId) {
      this.userService.updateUser(this.selectedUserId, payload).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'User Saved', detail: 'User profile updated successfully' });
          this.displayDialog = false;
          this.loadUsers();
        },
        error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to update user' })
      });
    } else {
      this.userService.createUser(payload).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'User Created', detail: 'New user registered successfully' });
          this.displayDialog = false;
          this.loadUsers();
        },
        error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to create user' })
      });
    }
  }

  confirmDelete(user: UserResponse): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to remove ${user.firstName} ${user.lastName}?`,
      accept: () => {
        this.userService.deleteUser(user.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'User Deleted', detail: 'User deleted successfully' });
            this.loadUsers();
          },
          error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Failed to delete user' })
        });
      }
    });
  }

  getRoleSeverity(role: Role): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (role) {
      case 'ADMIN': return 'info';
      case 'PRODUCTION_MANAGER': return 'success';
      case 'QUALITY_MANAGER': return 'warn';
      case 'DIRECTION': return 'secondary';
      default: return 'info';
    }
  }
}
