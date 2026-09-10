import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({ selector: 'app-resume-section', imports: [CommonModule, MatIconModule], templateUrl: './resume-section.component.html', styleUrl: './resume-section.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class ResumeSectionComponent {
  readonly sectionId = input.required<string>();
  readonly title = input.required<string>();
  readonly icon = input.required<string>();
  readonly subtitle = input<string>('');
}
