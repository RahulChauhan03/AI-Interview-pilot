import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-delete-resume-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `<h2 mat-dialog-title>Delete resume?</h2><mat-dialog-content>This will remove <b>{{ fileName }}</b> from your library.</mat-dialog-content><mat-dialog-actions align="end"><button mat-button mat-dialog-close>Cancel</button><button mat-flat-button color="warn" [mat-dialog-close]="true">Delete</button></mat-dialog-actions>`,
})
export class DeleteResumeDialogComponent { constructor(@Inject(MAT_DIALOG_DATA) readonly fileName: string) {} }
