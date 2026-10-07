import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { NotificationService } from '../../../core/services/notification.service';
import { JobDescriptionService } from '../services/job-description.service';

/** Create (/new) or edit (/:id/edit) a job description. */
@Component({
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './job-description-form.component.html',
})
export class JobDescriptionFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly jobService = inject(JobDescriptionService);
  private readonly notifications = inject(NotificationService);
  private readonly idParam = inject(ActivatedRoute).snapshot.paramMap.get('id');

  readonly editingId = this.idParam ? Number(this.idParam) : null;
  readonly loading = signal(this.editingId !== null);
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    jobTitle: ['', [Validators.required, Validators.maxLength(255)]],
    companyName: ['', [Validators.required, Validators.maxLength(255)]],
    jobDescription: ['', [Validators.required, Validators.maxLength(10000)]],
  });

  ngOnInit(): void {
    if (this.editingId === null) return;
    this.jobService.get(this.editingId).subscribe({
      next: (job) => {
        this.form.setValue({ jobTitle: job.jobTitle, companyName: job.companyName, jobDescription: job.jobDescription });
        this.loading.set(false);
      },
      error: () => this.router.navigateByUrl('/app/job-descriptions'),
    });
  }

  save(): void {
    if (this.form.invalid || this.saving()) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const request = this.form.getRawValue();
    const save$ = this.editingId === null ? this.jobService.create(request) : this.jobService.update(this.editingId, request);
    save$.subscribe({
      next: (job) => {
        if (job.reused) this.notifications.info('You already saved this job description, so it was opened instead.');
        else this.notifications.success(this.editingId === null ? 'Job description saved.' : 'Job description updated.');
        this.router.navigate(['/app/job-descriptions', job.id]);
      },
      error: () => this.saving.set(false),
    });
  }
}
