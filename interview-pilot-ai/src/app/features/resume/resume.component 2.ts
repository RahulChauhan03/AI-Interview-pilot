import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
@Component({
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './resume.component 2.html',
  styleUrl: './resume.component.scss',
})
export class ResumeComponent {
  resumes = [
    { name: 'Alex Smith — Product Designer', date: 'Jul 30, 2026', status: 'Ready' },
    { name: 'Alex Smith — General Resume', date: 'Jul 12, 2026', status: 'Ready' },
  ];

  selectedFile?: File;

onFileSelected(event: Event): void {

  const input = event.target as HTMLInputElement;

  if (!input.files?.length) {
    return;
  }

  this.selectedFile = input.files[0];

  console.log(this.selectedFile);
}
}
