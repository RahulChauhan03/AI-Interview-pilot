import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormArray, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { filter } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog.component';
import { ResumePickerComponent } from '../../components/resume-picker.component';
import { WorkspaceStore } from '../workspace.store';

const MAX_PARAGRAPHS = 8;

@Component({
  imports: [MatTooltipModule, DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule, ResumePickerComponent],
  template: `
    <section class="card">
      <div class="card-header">
        <div><h2>Cover letter</h2><p>Written only from your resume and this job description, addressed to the hiring manager. You can edit it before downloading.</p></div>
      </div>
      <div class="action-row">
        <app-resume-picker />
        <button mat-flat-button (click)="generate()" [disabled]="!!store.task() || editing() || store.resumeId() === null">
          <mat-icon svgIcon="write"></mat-icon>{{ store.task() === 'letter' ? 'Writing…' : letter() ? 'Regenerate' : 'Generate cover letter' }}</button>
      </div>
      @if (store.task() === 'letter') {
        <div class="inline-status"><mat-progress-bar mode="indeterminate"></mat-progress-bar>The AI is writing your cover letter. This can take a few minutes with the local AI model.</div>
      }
    </section>

    @if (letter(); as l) {
      @if (l.removedSentences > 0 && !l.edited) {
        <div class="notice warning"><mat-icon svgIcon="shield"></mat-icon>{{ l.removedSentences }} {{ l.removedSentences === 1 ? 'sentence was' : 'sentences were' }} removed because your resume does not support them.</div>
      }
      <div class="toolbar">
        <span class="subtle">{{ l.edited ? 'Edited' : 'Generated' }} {{ l.updatedAt | date: 'MMM d, y, h:mm a' }}</span>
        <span class="spacer"></span>
        @if (editing()) {
          <button mat-stroked-button (click)="editing.set(false)" [disabled]="saving()">Cancel</button>
          <button mat-flat-button (click)="save()" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save' }}</button>
        } @else {
          <button mat-stroked-button (click)="edit()" [disabled]="!!store.task()"><mat-icon svgIcon="edit"></mat-icon>Edit</button>
          <button mat-flat-button (click)="store.download('cover-letter/pdf')" [disabled]="!!store.task()"><mat-icon svgIcon="download"></mat-icon>Download PDF</button>
        }
      </div>
      <article class="paper" aria-label="Cover letter preview">
        <p><b>{{ l.content.senderName }}</b><br><span class="subtle">{{ l.content.senderContact.join('  |  ') }}</span></p>
        <p>{{ l.content.date }}</p>
        <p>{{ l.content.recipient }}</p>
        <p><b>{{ l.content.subject }}</b></p>
        <p>{{ l.content.greeting }}</p>
        @if (editing()) {
          @for (control of paragraphs.controls; track control; let i = $index) {
            <mat-form-field appearance="outline" class="paragraph-field">
              <mat-label>Paragraph {{ i + 1 }}</mat-label>
              <textarea matInput [formControl]="control" rows="5"></textarea>
              @if (control.invalid) { <mat-error>Write up to 2000 characters, or remove this paragraph.</mat-error> }
              <button mat-icon-button matSuffix matTooltip="Remove paragraph" (click)="paragraphs.removeAt(i)" [disabled]="paragraphs.length === 1" aria-label="Remove paragraph"><mat-icon svgIcon="delete"></mat-icon></button>
            </mat-form-field>
          }
          <button mat-button (click)="add()" [disabled]="paragraphs.length >= maxParagraphs"><mat-icon svgIcon="add"></mat-icon>Add paragraph</button>
        } @else {
          @for (p of l.content.paragraphs; track $index) { <p>{{ p }}</p> }
        }
        <p>{{ l.content.closing }}<br>{{ l.content.senderName }}</p>
      </article>
    } @else if (store.task() !== 'letter') {
      <section class="card empty-state">
        <mat-icon svgIcon="cover-letter"></mat-icon><h3>No cover letter yet</h3><p>Generate a cover letter that connects your real experience to this job.</p>
      </section>
    }
  `,
  styleUrl: './workspace-tabs.scss',
})
export class WorkspaceCoverLetterComponent {
  readonly store = inject(WorkspaceStore);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  readonly letter = this.store.coverLetter;
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly maxParagraphs = MAX_PARAGRAPHS;
  readonly paragraphs = new FormArray<FormControl<string>>([]);

  generate(): void {
    if (!this.letter()?.edited) { this.store.generateCoverLetter(); return; }
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Replace your edited letter?', message: 'Regenerating replaces the changes you made.', confirmLabel: 'Regenerate' },
    }).afterClosed().pipe(filter((ok): ok is true => ok === true)).subscribe(() => this.store.generateCoverLetter());
  }

  edit(): void {
    this.paragraphs.clear();
    this.letter()?.content.paragraphs.forEach((text) => this.paragraphs.push(this.control(text)));
    this.editing.set(true);
  }

  add(): void {
    this.paragraphs.push(this.control(''));
  }

  save(): void {
    if (this.paragraphs.invalid) { this.paragraphs.markAllAsTouched(); return; }
    this.saving.set(true);
    this.store.saveCoverLetter(this.paragraphs.getRawValue().map((text) => text.trim())).subscribe({
      next: () => { this.saving.set(false); this.editing.set(false); this.notifications.success('Cover letter saved.'); },
      error: () => this.saving.set(false),
    });
  }

  private control(text: string): FormControl<string> {
    return new FormControl(text, { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(2000)] });
  }
}
