import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Resume } from '../../models/resume.model';

@Component({ selector: 'app-resume-header', imports: [CommonModule, DatePipe, MatButtonModule, MatChipsModule, MatIconModule, MatTooltipModule], templateUrl: './resume-header.component.html', styleUrl: './resume-header.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class ResumeHeaderComponent { readonly resume = input.required<Resume>(); readonly score = input.required<number>(); readonly close = output<void>(); }
