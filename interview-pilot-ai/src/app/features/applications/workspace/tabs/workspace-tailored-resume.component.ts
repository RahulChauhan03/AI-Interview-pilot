import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ResumePickerComponent } from '../../components/resume-picker.component';
import { WorkspaceStore } from '../workspace.store';

@Component({
  imports: [RouterLink, DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule, ResumePickerComponent],
  template: `
    <section class="card">
      <div class="card-header">
        <div><h2>Tailored resume</h2>
          <p>The AI only rewords and reorders what is already in your resume. It never adds experience, skills, numbers or employers.</p></div>
      </div>
      <div class="action-row">
        <app-resume-picker />
        <button mat-flat-button (click)="store.generateTailoredResume()" [disabled]="!!store.task() || store.resumeId() === null">
          <mat-icon svgIcon="ai"></mat-icon>{{ store.task() === 'resume' ? 'Generating…' : resume() ? 'Regenerate' : 'Generate tailored resume' }}</button>
      </div>
      @if (store.task() === 'resume') {
        <div class="inline-status"><mat-progress-bar mode="indeterminate"></mat-progress-bar>
          Matching your resume with the job (if needed) and rewriting it. This can take a few minutes with the local AI model.</div>
      }
    </section>

    @if (resume(); as r) {
      @if (otherBase()) {
        <div class="notice warning"><mat-icon svgIcon="info"></mat-icon>This version was generated from {{ r.baseResumeFileName }}. Regenerate to use the selected resume.</div>
      }
      @if (r.omittedSkills.length) {
        <div class="notice"><mat-icon svgIcon="ban"></mat-icon><span>Not added because they are not on your resume: <b>{{ r.omittedSkills.join(', ') }}</b>.
          See <a routerLink="/app/skill-gaps">skill gaps</a> for how to close them.</span></div>
      }
      @if (r.removedSuggestions > 0) {
        <div class="notice warning"><mat-icon svgIcon="shield"></mat-icon>{{ r.removedSuggestions }} AI {{ r.removedSuggestions === 1 ? 'suggestion was' : 'suggestions were' }} removed because your resume does not support them.</div>
      }
      <div class="toolbar">
        <span class="subtle">Generated {{ r.generatedAt | date: 'MMM d, y, h:mm a' }} from {{ r.baseResumeFileName }}</span>
        <span class="spacer"></span>
        <button mat-flat-button (click)="store.download('tailored-resume/pdf')" [disabled]="!!store.task()"><mat-icon svgIcon="download"></mat-icon>Download PDF</button>
      </div>
      <article class="paper" aria-label="Tailored resume preview">
        <h2 class="name">{{ r.content.name }}</h2>
        <p class="subtle">{{ r.content.contact.join('  |  ') }}</p>
        @if (r.content.summary) { <h3>Summary</h3><p>{{ r.content.summary }}</p> }
        @if (r.content.skills.length) { <h3>Skills</h3><p>{{ r.content.skills.join(', ') }}</p> }
        @if (r.content.experience.length) {
          <h3>Experience</h3>
          @for (job of r.content.experience; track $index) {
            <p><b>{{ job.title }}{{ job.company ? ' - ' + job.company : '' }}</b><br><span class="subtle">{{ job.duration }}{{ job.location ? '  |  ' + job.location : '' }}</span></p>
            <ul>@for (b of job.bullets; track $index) { <li>{{ b }}</li> }</ul>
          }
        }
        @if (r.content.projects.length) {
          <h3>Projects</h3>
          @for (p of r.content.projects; track $index) {
            <p><b>{{ p.name }}</b><br>{{ p.description }}@if (p.technologies.length) {<br><span class="subtle">Technologies: {{ p.technologies.join(', ') }}</span>}</p>
          }
        }
        @if (r.content.education.length) {
          <h3>Education</h3>
          @for (e of r.content.education; track $index) {
            <p><b>{{ e.degree || e.institution }}</b><br><span class="subtle">{{ [e.degree ? e.institution : '', e.duration, e.details].filter(isText).join('  |  ') }}</span></p>
          }
        }
        @if (r.content.certifications.length) {
          <h3>Certifications</h3><ul>@for (c of r.content.certifications; track $index) { <li>{{ c }}</li> }</ul>
        }
      </article>
    } @else if (store.task() !== 'resume') {
      <section class="card empty-state">
        <mat-icon svgIcon="tailored-resume"></mat-icon><h3>No tailored resume yet</h3>
        <p>Generate a version of your resume that puts what this job asks for first.</p>
      </section>
    }
  `,
  styleUrl: './workspace-tabs.scss',
})
export class WorkspaceTailoredResumeComponent {
  readonly store = inject(WorkspaceStore);
  readonly resume = this.store.tailoredResume;
  readonly otherBase = computed(() => {
    const base = this.resume()?.baseResumeId;
    return base != null && this.store.resumeId() !== null && base !== this.store.resumeId();
  });
  readonly isText = (value: string) => !!value;
}
