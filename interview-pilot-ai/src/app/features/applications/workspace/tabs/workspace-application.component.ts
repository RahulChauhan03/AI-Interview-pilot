import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApplicationPanelComponent } from '../../components/application-panel.component';
import { WorkspaceStore } from '../workspace.store';

@Component({
  imports: [MatButtonModule, MatIconModule, ApplicationPanelComponent],
  template: `
    @if (store.application(); as app) {
      <app-application-panel [application]="app" (changed)="store.applicationChanged($event)" (deleted)="store.applicationDeleted()" />
    } @else {
      <section class="card empty-state">
        <mat-icon svgIcon="application"></mat-icon><h3>Not tracked yet</h3>
        <p>Track this job to follow its status from saved to offer. It is also tracked automatically when you generate a document.</p>
        <button mat-flat-button (click)="store.track()" [disabled]="!!store.task()">Track this application</button>
      </section>
    }
  `,
})
export class WorkspaceApplicationComponent {
  readonly store = inject(WorkspaceStore);
}
