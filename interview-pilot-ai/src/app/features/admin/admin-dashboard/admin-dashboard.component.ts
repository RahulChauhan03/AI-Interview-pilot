import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Activity, AdminStats, ComponentStatus } from '../models/admin.model';
import { AdminService } from '../services/admin.service';

/** System-level overview for administrators. All numbers come from /api/admin; nothing is estimated. */
@Component({
  imports: [RouterLink, DatePipe, MatButtonModule, MatIconModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly stats = signal<AdminStats | null>(null);
  readonly statsFailed = signal(false);
  readonly system = signal<ComponentStatus[] | null>(null);
  readonly checkingSystem = signal(false);
  readonly activity = signal<Activity[] | null>(null);

  ngOnInit(): void {
    this.loadStats();
    this.checkSystem();
    this.adminService.activity(8).subscribe({ next: (activity) => this.activity.set(activity), error: () => this.activity.set([]) });
  }

  loadStats(): void {
    this.statsFailed.set(false);
    this.adminService.stats().subscribe({ next: (stats) => this.stats.set(stats), error: () => this.statsFailed.set(true) });
  }

  /** Loaded on its own so a slow health check never holds up the rest of the page. */
  checkSystem(): void {
    this.checkingSystem.set(true);
    this.adminService.system().subscribe({
      next: (system) => { this.system.set(system); this.checkingSystem.set(false); },
      error: () => { this.system.set([]); this.checkingSystem.set(false); },
    });
  }
}
