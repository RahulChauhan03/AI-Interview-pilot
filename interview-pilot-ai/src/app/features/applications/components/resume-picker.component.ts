import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { WorkspaceStore } from '../workspace/workspace.store';

/** The base resume used by the workspace tabs (shared selection). */
@Component({
  selector: 'app-resume-picker',
  imports: [RouterLink, MatFormFieldModule, MatSelectModule],
  template: `
    @if (store.resumes().length) {
      <mat-form-field appearance="outline" class="full-width" subscriptSizing="dynamic">
        <mat-label>Base resume</mat-label>
        <mat-select [value]="store.resumeId()" (selectionChange)="store.resumeId.set($event.value)" [disabled]="!!store.task()">
          @for (resume of store.resumes(); track resume.id) { <mat-option [value]="resume.id">{{ resume.originalFileName }}</mat-option> }
        </mat-select>
      </mat-form-field>
    } @else {
      <p class="muted">You need a processed resume first. <a routerLink="/app/resumes">Upload one →</a></p>
    }
  `,
})
export class ResumePickerComponent {
  readonly store = inject(WorkspaceStore);
}
