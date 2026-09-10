import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { ResumeRecord } from '../../models/parsed-resume.model';

@Component({ selector: 'app-experience-timeline', imports: [CommonModule, MatChipsModule], templateUrl: './experience-timeline.component.html', styleUrl: './experience-timeline.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class ExperienceTimelineComponent {
  readonly entries = input.required<readonly ResumeRecord[]>();
  value(entry: ResumeRecord, ...keys: string[]): string { for (const key of keys) { const value = entry[key]; if (typeof value === 'string' || typeof value === 'number') return String(value); } return 'Not specified'; }
  list(entry: ResumeRecord, ...keys: string[]): string[] { for (const key of keys) { const value = entry[key]; if (Array.isArray(value)) return value.map(String); if (typeof value === 'string' && value.includes(',')) return value.split(',').map((item) => item.trim()); } return []; }
}
