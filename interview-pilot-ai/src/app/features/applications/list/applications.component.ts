import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationService } from '../../../core/services/notification.service';
import { confirmAction } from '../../../shared/confirm-dialog.component';
import { APPLICATION_STATUSES, ApplicationStatus, JobApplication, statusInfo } from '../models/application.model';
import { ApplicationService } from '../services/application.service';

/** Application tracker: every job the user is preparing for or has applied to. */
@Component({
  imports: [MatTooltipModule, RouterLink, DatePipe, DecimalPipe, MatButtonModule, MatIconModule],
  template: `
    <header class="page-header">
      <div><h1>Applications</h1><p>Track every job from saved to offer, with its documents and interview practice.</p></div>
      <a mat-flat-button routerLink="/app/job-descriptions"><mat-icon svgIcon="job"></mat-icon>Job descriptions</a>
    </header>
    <section class="card">
      @if (loading()) {
        <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton short"></span>
      } @else if (failed()) {
        <div class="error-state"><mat-icon svgIcon="offline"></mat-icon><h3>Applications could not be loaded</h3><button mat-stroked-button (click)="load()">Retry</button></div>
      } @else if (!applications().length) {
        <div class="empty-state"><mat-icon svgIcon="application"></mat-icon><h3>No applications yet</h3>
          <p>Open a job description and generate a tailored resume or cover letter, or track it from its Application tab.</p>
          <a mat-flat-button routerLink="/app/job-descriptions">Open job descriptions</a></div>
      } @else {
        <div class="toolbar" role="group" aria-label="Filter by status">
          <button mat-stroked-button [class.active]="filter() === null" (click)="filter.set(null)">All ({{ applications().length }})</button>
          @for (s of usedStatuses(); track s.value) {
            <button mat-stroked-button [class.active]="filter() === s.value" (click)="filter.set(s.value)">{{ s.label }} ({{ count(s.value) }})</button>
          }
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead><tr><th>Role</th><th>Status</th><th class="hide-sm">Match</th><th class="hide-sm">Documents</th><th class="hide-sm">Interview</th><th class="hide-sm">Updated</th><th></th></tr></thead>
            <tbody>
              @for (a of visible(); track a.id) {
                <tr>
                  <td><a class="cell-title" [routerLink]="['/app/applications', a.id]">{{ a.jobTitle }}</a><div class="subtle">{{ a.companyName }}</div></td>
                  <td><span [class]="'pill ' + statusInfo(a.status).tone">{{ statusInfo(a.status).label }}</span></td>
                  <td class="hide-sm">{{ a.matchScore === null ? '–' : (a.matchScore | number: '1.0-0') + '%' }}</td>
                  <td class="hide-sm">
                    <mat-icon [class.off]="!a.tailoredResumeAt" [attr.aria-label]="a.tailoredResumeAt ? 'Tailored resume ready' : 'No tailored resume'" svgIcon="tailored-resume"></mat-icon>
                    <mat-icon [class.off]="!a.coverLetterAt" [attr.aria-label]="a.coverLetterAt ? 'Cover letter ready' : 'No cover letter'" svgIcon="cover-letter"></mat-icon>
                  </td>
                  <td class="hide-sm">{{ a.latestInterviewStatus === 'COMPLETED' ? (a.latestInterviewScore | number: '1.0-0') + '/100' : a.latestInterviewStatus ? 'In progress' : '–' }}</td>
                  <td class="hide-sm">{{ (a.updatedAt ?? a.createdAt) | date: 'MMM d' }}</td>
                  <td class="cell-actions"><a mat-button [routerLink]="['/app/job-descriptions', a.jobDescriptionId, 'workspace']">Workspace</a>
                    <button mat-icon-button matTooltip="Delete" [disabled]="deletingId() === a.id" (click)="remove(a)" [attr.aria-label]="'Delete application for ' + a.jobTitle"><mat-icon svgIcon="delete"></mat-icon></button></td>
                </tr>
              } @empty {
                <tr><td colspan="7" class="muted">No applications with this status.</td></tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: `
    .active { background: var(--app-primary-soft); color: var(--app-primary); }
    .mat-icon { color: var(--app-success); margin-right: 4px; }
    .mat-icon.off { color: var(--app-border-strong); }
  `,
})
export class ApplicationsComponent implements OnInit {
  private readonly service = inject(ApplicationService);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  readonly deletingId = signal<number | null>(null);
  readonly applications = signal<JobApplication[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly filter = signal<ApplicationStatus | null>(null);
  readonly statusInfo = statusInfo;
  readonly usedStatuses = computed(() => APPLICATION_STATUSES.filter((s) => this.applications().some((a) => a.status === s.value)));
  readonly visible = computed(() => this.applications().filter((a) => this.filter() === null || a.status === this.filter()));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.service.list().subscribe({
      next: (applications) => { this.applications.set(applications); this.loading.set(false); },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  remove(application: JobApplication): void {
    confirmAction(this.dialog, { title: 'Delete this application?', confirmLabel: 'Delete',
      message: `"${application.jobTitle}" with its tailored resume and cover letter will be removed. The job description stays.` })
      .subscribe(() => {
        this.deletingId.set(application.id);
        this.service.delete(application.id).subscribe({
          next: () => {
            this.applications.update((items) => items.filter((item) => item.id !== application.id));
            this.deletingId.set(null);
            this.notifications.success('Application deleted.');
          },
          error: () => this.deletingId.set(null),
        });
      });
  }

  count(status: ApplicationStatus): number {
    return this.applications().filter((a) => a.status === status).length;
  }
}
