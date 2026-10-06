import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ExperienceTimelineComponent } from '../components/experience-timeline/experience-timeline.component';
import { ParsedResume, ResumeRecord } from '../models/parsed-resume.model';
import { ResumeStatus } from '../models/resume-status.enum';
import { Resume } from '../models/resume.model';
import { ResumeService } from '../services/resume.service';

const PLACEHOLDER = /^(none|n\/?a|null|undefined|-+|not (available|specified|provided|mentioned))$/i;

interface Field {
  label: string;
  value: string;
  link: boolean;
}

/** Everything the AI extracted from one resume (the parsed MongoDB document), section by section. */
@Component({
  imports: [RouterLink, DatePipe, DecimalPipe, MatButtonModule, MatIconModule, ExperienceTimelineComponent],
  templateUrl: './resume-detail.component.html',
  styleUrl: './resume-detail.component.scss',
})
export class ResumeDetailComponent implements OnInit {
  private readonly resumeService = inject(ResumeService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly resume = signal<Resume | null>(null);
  readonly parsed = signal<ParsedResume | null>(null);
  readonly profile = signal<Field[]>([]);
  readonly name = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.resumeService.get(this.id).pipe(
      switchMap((resume) => forkJoin({
        resume: of(resume),
        parsed: resume.status === ResumeStatus.Parsed ? this.resumeService.getParsed(this.id) : of(null),
      })),
    ).subscribe({
      next: ({ resume, parsed }) => {
        this.resume.set(resume);
        this.parsed.set(parsed);
        if (parsed) this.readProfile(parsed.personalInformation ?? {});
        this.loading.set(false);
      },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  /**
   * First real value among the given keys (also reads resumes parsed in the older free-form format).
   * Placeholders the AI writes for missing data ("None", "N/A", …) count as empty.
   */
  value(record: ResumeRecord, ...keys: string[]): string {
    for (const key of keys) {
      const value = record?.[key];
      if (typeof value === 'number') return String(value);
      if (typeof value === 'string' && value.trim() && !PLACEHOLDER.test(value.trim())) return value.trim();
    }
    return '';
  }

  /** Fields of an education/project entry other than the ones shown as its title. */
  details(record: ResumeRecord, ...exclude: string[]): { label: string; value: string }[] {
    return Object.entries(record ?? {})
      .filter(([key, value]) => !exclude.includes(key) && value !== null && !(typeof value === 'string' && (!value.trim() || PLACEHOLDER.test(value.trim())))
        && !(Array.isArray(value) && !value.length))
      .map(([key, value]) => ({ label: this.labelFor(key), value: Array.isArray(value) ? value.join(', ') : String(value) }));
  }

  private readProfile(info: ResumeRecord): void {
    this.name.set([this.value(info, 'firstName', 'First Name'), this.value(info, 'lastName', 'Last Name')].filter(Boolean).join(' '));
    const fields: Field[] = [
      { label: 'Email', value: this.value(info, 'email', 'Email'), link: false },
      { label: 'Phone', value: this.value(info, 'phone', 'Phone'), link: false },
      { label: 'Location', value: this.value(info, 'location', 'Location'), link: false },
      { label: 'LinkedIn', value: this.value(info, 'linkedIn', 'LinkedIn', 'linkedin'), link: true },
      { label: 'GitHub', value: this.value(info, 'github', 'Github', 'GitHub'), link: true },
      { label: 'Portfolio', value: this.value(info, 'portfolio', 'Portfolio'), link: true },
    ];
    this.profile.set(fields.filter((field) => field.value));
  }

  private labelFor(key: string): string {
    const spaced = key.replace(/([a-z])([A-Z])/g, '$1 $2');
    return spaced.charAt(0).toUpperCase() + spaced.slice(1);
  }

  href(value: string): string {
    return /^https?:\/\//.test(value) ? value : `https://${value}`;
  }
}
