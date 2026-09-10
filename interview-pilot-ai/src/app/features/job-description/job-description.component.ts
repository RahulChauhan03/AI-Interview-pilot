import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
@Component({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
  ],
  templateUrl: './job-description.component.html',
  styleUrl: './job-description.component.scss',
})
export class JobDescriptionComponent {
  fb = inject(FormBuilder);
  form = this.fb.nonNullable.group({
    company: ['', [Validators.required]],
    role: ['', [Validators.required]],
    description: ['', [Validators.required]],
  });
  jobs = [
    { role: 'Senior Product Designer', company: 'Figma', date: 'Jul 29' },
    { role: 'Product Designer', company: 'Notion', date: 'Jul 22' },
  ];
  save() {
    if (this.form.valid) {
      this.jobs.unshift({
        role: this.form.value.role!,
        company: this.form.value.company!,
        date: 'just now',
      });
      this.form.reset();
    }
  }
}
