import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { NotificationService } from '../../core/services/notification.service';
import { ResumeStatus } from '../resume/models/resume-status.enum';
import { ResumeSummary } from '../resume/models/resume.model';
import { ResumeService } from '../resume/services/resume.service';
import { JobDescription, ResumeMatch } from '../job-description/models/job-description.model';
import { JobDescriptionService } from '../job-description/services/job-description.service';
import { MatchService, matchLabel } from './services/match.service';

@Component({
  imports: [RouterLink, DatePipe, DecimalPipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatProgressBarModule, MatSelectModule],
  templateUrl: './matches.component.html',
  styleUrl: './matches.component.scss',
})
export class MatchesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly matchService = inject(MatchService);
  private readonly jobService = inject(JobDescriptionService);
  private readonly resumeService = inject(ResumeService);
  private readonly notifications = inject(NotificationService);

  readonly matches = signal<ResumeMatch[]>([]);
  readonly resumes = signal<ResumeSummary[]>([]);
  readonly jobs = signal<JobDescription[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly analysing = signal(false);
  readonly matchLabel = matchLabel;

  readonly form = this.fb.group({
    resumeId: this.fb.control<number | null>(null, Validators.required),
    jobDescriptionId: this.fb.control<number | null>(null, Validators.required),
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    forkJoin({ matches: this.matchService.list(), resumes: this.resumeService.refresh(), jobs: this.jobService.list() }).subscribe({
      next: ({ matches, resumes, jobs }) => {
        const parsed = resumes.filter((resume) => resume.status === ResumeStatus.Parsed);
        this.matches.set(matches);
        this.resumes.set(parsed);
        this.jobs.set(jobs);
        this.form.patchValue({ resumeId: parsed[0]?.id ?? null, jobDescriptionId: jobs[0]?.id ?? null });
        this.loading.set(false);
      },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  analyse(): void {
    const { resumeId, jobDescriptionId } = this.form.getRawValue();
    if (this.form.invalid || this.analysing() || resumeId === null || jobDescriptionId === null) { this.form.markAllAsTouched(); return; }
    this.analysing.set(true);
    this.jobService.match(jobDescriptionId, resumeId).subscribe({
      next: (match) => {
        if (match.reused) this.notifications.info('Nothing changed since the last analysis, so that result is shown.');
        this.router.navigate(['/app/matches', match.id]);
      },
      error: () => this.analysing.set(false),
    });
  }
}
