import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AdminUser } from '../models/admin.model';
import { AdminService } from '../services/admin.service';

/** Read-only user list (the backend has no user-management operations yet). */
@Component({
  imports: [DatePipe, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './admin-users.component.html',
})
export class AdminUsersComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly users = signal<AdminUser[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly search = signal('');
  readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    return term
      ? this.users().filter((user) => `${user.firstName} ${user.lastName} ${user.email}`.toLowerCase().includes(term))
      : this.users();
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.adminService.users().subscribe({
      next: (users) => { this.users.set(users); this.loading.set(false); },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }
}
