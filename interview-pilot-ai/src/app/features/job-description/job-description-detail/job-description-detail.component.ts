import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { filter, forkJoin, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog.component';
import { ResumeStatus } from '../../resume/models/resume-status.enum';
import { ResumeSummary } from '../../resume/models/resume.model';
import { ResumeService } from '../../resume/services/resume.service';
import { matchLabel } from '../../matches/services/match.service';
import { JobDescription, ResumeMatch } from '../models/job-description.model';
import { JobDescriptionService } from '../services/job-description.service';

@Component({
  imports: [RouterLink, DatePipe, DecimalPipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatProgressBarModule, MatSelectModule],
  templateUrl: './job-description-detail.component.html',
  styleUrl: './job-description-detail.component.scss',
})
export class JobDescriptionDetailComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly jobService = inject(JobDescriptionService);
  private readonly resumeService = inject(ResumeService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly job = signal<JobDescription | null>(null);
  readonly matches = signal<ResumeMatch[]>([]);
  readonly resumes = signal<ResumeSummary[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly analysing = signal(false);
  readonly resumeControl = new FormControl<number | null>(null, Validators.required);
  readonly matchLabel = matchLabel;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    forkJoin({ job: this.jobService.get(this.id), matches: this.jobService.matches(this.id), resumes: this.resumeService.refresh() }).subscribe({
      next: ({ job, matches, resumes }) => {
        const parsed = resumes.filter((resume) => resume.status === ResumeStatus.Parsed);
        this.job.set(job);
        this.matches.set(matches);
        this.resumes.set(parsed);
        this.resumeControl.setValue(parsed[0]?.id ?? null);
        this.loading.set(false);
      },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  analyse(): void {
    const resumeId = this.resumeControl.value;
    if (resumeId === null || this.analysing()) { this.resumeControl.markAsTouched(); return; }
    this.analysing.set(true);
    this.jobService.match(this.id, resumeId).subscribe({
      next: (match) => {
        if (match.reused) this.notifications.info('Nothing changed since the last analysis, so that result is shown.');
        this.router.navigate(['/app/matches', match.id]);
      },
      error: () => this.analysing.set(false),
    });
  }

  remove(): void {
    const job = this.job();
    if (!job) return;
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete job description?', message: `This removes "${job.jobTitle}" and its match results.`, confirmLabel: 'Delete' },
    }).afterClosed().pipe(
      filter((confirmed): confirmed is true => confirmed === true),
      switchMap(() => this.jobService.delete(job.id)),
    ).subscribe({
      next: () => {
        this.notifications.success('Job description deleted.');
        this.router.navigateByUrl('/app/job-descriptions');
      },
    });
  }
}
