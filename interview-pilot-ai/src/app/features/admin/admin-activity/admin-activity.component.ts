import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { Activity } from '../models/admin.model';
import { AdminService } from '../services/admin.service';

type ActivityFilter = 'ALL' | Activity['type'];

/** Recent events derived from existing records; contains ids, statuses and timings only — no user content. */
@Component({
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatButtonToggleModule, MatIconModule],
  templateUrl: './admin-activity.component.html',
})
export class AdminActivityComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly activity = signal<Activity[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly filter = signal<ActivityFilter>('ALL');
  readonly filters: ActivityFilter[] = ['ALL', 'USER', 'RESUME', 'MATCH', 'INTERVIEW'];
  readonly visible = computed(() => this.filter() === 'ALL' ? this.activity() : this.activity().filter((event) => event.type === this.filter()));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.adminService.activity(100).subscribe({
      next: (activity) => { this.activity.set(activity); this.loading.set(false); },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }
}
