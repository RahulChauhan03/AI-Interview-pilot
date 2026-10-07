import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable, filter } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel: string;
}

/** Generic yes/no dialog; closes with true when confirmed. */
@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `<h2 mat-dialog-title>{{ data.title }}</h2><mat-dialog-content>{{ data.message }}</mat-dialog-content><mat-dialog-actions align="end"><button mat-button mat-dialog-close>Cancel</button><button mat-flat-button color="warn" [mat-dialog-close]="true">{{ data.confirmLabel }}</button></mat-dialog-actions>`,
})
export class ConfirmDialogComponent { constructor(@Inject(MAT_DIALOG_DATA) readonly data: ConfirmDialogData) {} }

/** Opens the confirm dialog; emits only when the user confirms. */
export function confirmAction(dialog: MatDialog, data: ConfirmDialogData): Observable<true> {
  return dialog.open(ConfirmDialogComponent, { data }).afterClosed().pipe(filter((confirmed): confirmed is true => confirmed === true));
}
