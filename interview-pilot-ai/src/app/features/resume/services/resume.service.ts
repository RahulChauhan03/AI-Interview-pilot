import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, shareReplay, tap } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/http/api-endpoints';
import { ApiService, ApiUploadEvent } from '../../../core/http/api.service';
import { ParsedResume } from '../models/parsed-resume.model';
import { Resume, ResumeSummary } from '../models/resume.model';
import { ResumeUpload } from '../models/upload.model';

@Injectable({ providedIn: 'root' })
export class ResumeService {
  private readonly api = inject(ApiService);
  private readonly resumesSubject = new BehaviorSubject<ResumeSummary[]>([]);
  readonly resumes$ = this.resumesSubject.asObservable().pipe(shareReplay({ bufferSize: 1, refCount: true }));

  refresh(): Observable<ResumeSummary[]> {
    return this.api.get<ResumeSummary[]>(API_ENDPOINTS.RESUME.LIST).pipe(
      tap((resumes) => this.resumesSubject.next(resumes)),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }

  upload(file: File): Observable<ApiUploadEvent<ResumeUpload>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.api.uploadWithProgress<ResumeUpload>(API_ENDPOINTS.RESUME.UPLOAD, formData);
  }

  get(id: number): Observable<Resume> {
    return this.api.get<Resume>(`${API_ENDPOINTS.RESUME.DETAILS}/${id}`);
  }

  getParsed(id: number): Observable<ParsedResume> {
    return this.api.get<ParsedResume>(`${API_ENDPOINTS.RESUME.PARSED}/${id}/parsed`);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`${API_ENDPOINTS.RESUME.DELETE}/${id}`);
  }
}
