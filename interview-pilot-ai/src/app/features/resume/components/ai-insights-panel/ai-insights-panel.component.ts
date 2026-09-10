import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { ParsedResume } from '../../models/parsed-resume.model';

@Component({ selector: 'app-ai-insights-panel', imports: [CommonModule, MatButtonModule, MatChipsModule, MatIconModule], templateUrl: './ai-insights-panel.component.html', styleUrl: './ai-insights-panel.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class AiInsightsPanelComponent {
  readonly parsed = input.required<ParsedResume>(); readonly expanded = signal(true);
  readonly score = computed(() => Math.min(98, 62 + Math.min(25, this.parsed().skills.length) + Math.min(10, this.parsed().keywords.length / 2)));
  readonly strengths = computed(() => this.parsed().technicalSkills.slice(0, 4));
  readonly recommendations = computed(() => {
    const parsed = this.parsed();
    return [
      !parsed.projects.length ? 'Add project details to strengthen the technical profile.' : null,
      !parsed.achievements.length ? 'Add measurable achievements to demonstrate impact.' : null,
      !parsed.keywords.length ? 'Include target-role keywords to improve ATS discoverability.' : null,
    ].filter((recommendation): recommendation is string => recommendation !== null);
  });
  readonly roles = computed(() => this.parsed().designations.slice(0, 3));
}
