import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { ResumeStatus } from '../resume/models/resume-status.enum';
import { ResumeSummary } from '../resume/models/resume.model';
import { ResumeService } from '../resume/services/resume.service';
import { JobDescription } from '../job-description/models/job-description.model';
import { JobDescriptionService } from '../job-description/services/job-description.service';
import { Interview } from './models/interview.model';
import { InterviewService } from './services/interview.service';

/** Interview list and setup. "?resumeId=&jobDescriptionId=" opens the setup pre-filled (e.g. from a match). */
@Component({
  imports: [RouterLink, DatePipe, DecimalPipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatProgressBarModule, MatSelectModule],
  templateUrl: './interview.component.html',
  styleUrl: './interview.component.scss',
})
export class InterviewComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly query = inject(ActivatedRoute).snapshot.queryParamMap;
  private readonly interviewService = inject(InterviewService);
  private readonly resumeService = inject(ResumeService);
  private readonly jobService = inject(JobDescriptionService);

  readonly interviews = signal<Interview[]>([]);
  readonly resumes = signal<ResumeSummary[]>([]);
  readonly jobs = signal<JobDescription[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly showSetup = signal(this.query.has('jobDescriptionId'));
  readonly creating = signal(false);
  readonly questionCounts = [3, 5, 7, 10];

  readonly setupForm = this.fb.group({
    resumeId: this.fb.control<number | null>(null, Validators.required),
    jobDescriptionId: this.fb.control<number | null>(null, Validators.required),
    questionCount: this.fb.nonNullable.control(5),
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    forkJoin({ interviews: this.interviewService.list(), resumes: this.resumeService.refresh(), jobs: this.jobService.list() }).subscribe({
      next: ({ interviews, resumes, jobs }) => {
        const parsed = resumes.filter((resume) => resume.status === ResumeStatus.Parsed);
        this.interviews.set(interviews);
        this.resumes.set(parsed);
        this.jobs.set(jobs);
        this.setupForm.patchValue({
          resumeId: this.pick(this.query.get('resumeId'), parsed.map((resume) => resume.id)),
          jobDescriptionId: this.pick(this.query.get('jobDescriptionId'), jobs.map((job) => job.id)),
        });
        this.loading.set(false);
      },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  start(): void {
    const { resumeId, jobDescriptionId, questionCount } = this.setupForm.getRawValue();
    if (this.setupForm.invalid || this.creating() || resumeId === null || jobDescriptionId === null) { this.setupForm.markAllAsTouched(); return; }
    this.creating.set(true);
    this.interviewService.create({ resumeId, jobDescriptionId, questionCount }).subscribe({
      next: (interview) => this.router.navigate(['/app/interviews', interview.id]),
      error: () => this.creating.set(false),
    });
  }

  link(interview: Interview): unknown[] {
    return interview.status === 'COMPLETED' ? ['/app/interviews', interview.id, 'result'] : ['/app/interviews', interview.id];
  }

  /** The id from the URL when it is one of the options, otherwise the first option. */
  private pick(requested: string | null, options: number[]): number | null {
    const id = Number(requested);
    return options.includes(id) ? id : (options[0] ?? null);
  }
}
