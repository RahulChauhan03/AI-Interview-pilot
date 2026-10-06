import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { filter, forkJoin, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationService } from '../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { MatchService, matchLabel } from '../matches/services/match.service';
import { JobDescription, ResumeMatch } from './models/job-description.model';
import { JobDescriptionService } from './services/job-description.service';

@Component({
  imports: [RouterLink, DatePipe, DecimalPipe, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './job-description.component.html',
})
export class JobDescriptionComponent implements OnInit {
  private readonly jobService = inject(JobDescriptionService);
  private readonly matchService = inject(MatchService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly jobs = signal<JobDescription[]>([]);
  /** Latest match per job description id. */
  readonly latestMatch = signal<Map<number, ResumeMatch>>(new Map());
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly matchLabel = matchLabel;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    forkJoin({ jobs: this.jobService.list(), matches: this.matchService.list() }).subscribe({
      next: ({ jobs, matches }) => {
        const latest = new Map<number, ResumeMatch>();
        matches.forEach((match) => { if (!latest.has(match.jobDescriptionId)) latest.set(match.jobDescriptionId, match); });
        this.jobs.set(jobs);
        this.latestMatch.set(latest);
        this.loading.set(false);
      },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  remove(job: JobDescription): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete job description?', message: `This removes "${job.jobTitle}" and its match results.`, confirmLabel: 'Delete' },
    }).afterClosed().pipe(
      filter((confirmed): confirmed is true => confirmed === true),
      switchMap(() => { this.deletingId.set(job.id); return this.jobService.delete(job.id); }),
    ).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.jobs.update((jobs) => jobs.filter((item) => item.id !== job.id));
        this.notifications.success('Job description deleted.');
      },
      error: () => this.deletingId.set(null),
    });
  }
}
