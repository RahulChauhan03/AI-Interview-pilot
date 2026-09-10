import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, Inject, ViewChild, computed, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { ResumeHeaderComponent } from './components/resume-header/resume-header.component';
import { ResumeSidebarComponent } from './components/resume-sidebar/resume-sidebar.component';
import { ResumeSectionComponent } from './components/resume-section/resume-section.component';
import { InfoCardComponent } from './components/info-card/info-card.component';
import { SummaryCardComponent } from './components/summary-card/summary-card.component';
import { ExperienceTimelineComponent } from './components/experience-timeline/experience-timeline.component';
import { SkillChipListComponent } from './components/skill-chip-list/skill-chip-list.component';
import { AiInsightsPanelComponent } from './components/ai-insights-panel/ai-insights-panel.component';
import { ParsedResume, ResumeRecord } from './models/parsed-resume.model';
import { Resume } from './models/resume.model';

export interface ResumeDetailsDialogData { resume: Resume; parsed: ParsedResume; }

@Component({
  selector: 'app-resume-view-dialog',
  imports: [CommonModule, MatDialogModule, MatExpansionModule, MatIconModule, ResumeHeaderComponent, ResumeSidebarComponent, ResumeSectionComponent, InfoCardComponent, SummaryCardComponent, ExperienceTimelineComponent, SkillChipListComponent, AiInsightsPanelComponent],
  templateUrl: './resume-details-dialog.component.html',
  styleUrl: './resume-details-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResumeDetailsDialogComponent implements AfterViewInit {
  @ViewChild('contentArea') private contentArea?: ElementRef<HTMLElement>;
  readonly activeSection = signal('personal');
  readonly personal = computed(() => this.data.parsed.personalInformation);
  readonly score = computed(() => Math.min(98, 62 + Math.min(25, this.data.parsed.skills.length) + Math.min(10, this.data.parsed.keywords.length / 2)));

  constructor(@Inject(MAT_DIALOG_DATA) readonly data: ResumeDetailsDialogData, private readonly dialogRef: MatDialogRef<ResumeDetailsDialogComponent>) {}
  ngAfterViewInit(): void { this.contentArea?.nativeElement.addEventListener('scroll', () => this.updateActiveSection(), { passive: true }); }
  close(): void { this.dialogRef.close(); }
  scrollTo(sectionId: string): void { document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' }); this.activeSection.set(sectionId); }
  value(record: ResumeRecord, ...keys: string[]): string | null { for (const key of keys) { const value = record[key]; if (typeof value === 'string' || typeof value === 'number') return String(value); } return null; }
  entries(record: ResumeRecord): [string, unknown][] { return Object.entries(record); }
  display(value: unknown): string { return Array.isArray(value) ? value.map(String).join(', ') : typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value ?? 'Not specified'); }
  private updateActiveSection(): void { const area = this.contentArea?.nativeElement; if (!area) return; const ids = ['personal','summary','experience','education','projects','technical-skills','soft-skills','certifications','languages','achievements','keywords']; const match = ids.find((id) => { const element = document.getElementById(id); return element && element.getBoundingClientRect().top >= area.getBoundingClientRect().top && element.getBoundingClientRect().top < area.getBoundingClientRect().top + 170; }); if (match) this.activeSection.set(match); }
}
