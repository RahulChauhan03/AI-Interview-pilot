import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Resume, JobDescription, Interview, User } from '../models';
import { ApiService } from '../http/api.service';
import { API_ENDPOINTS } from '../http/api-endpoints';

@Injectable({ providedIn: 'root' })
export class ResumeService {
  private readonly api = inject(ApiService);

  getAll(): Observable<Resume[]> {
    return this.api.get<Resume[]>(API_ENDPOINTS.RESUME.LIST);
  }

  upload(file: File): Observable<Resume> {
    const body = new FormData();
    body.append('file', file);
    return this.api.upload<Resume>(API_ENDPOINTS.RESUME.UPLOAD, body);
  }

  delete(id: string): Observable<null> {
    return this.api.delete<null>(`${API_ENDPOINTS.RESUME.DELETE}/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class JobDescriptionService {
  private readonly api = inject(ApiService);

  getAll(): Observable<JobDescription[]> {
    return this.api.get<JobDescription[]>(API_ENDPOINTS.RESUME.LIST);
  }

  create(
    item: Omit<JobDescription, 'id' | 'createdAt'> & { description: string },
  ): Observable<JobDescription> {
    return this.api.post<JobDescription>(API_ENDPOINTS.RESUME.LIST, item);
  }
}

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private readonly api = inject(ApiService);

  getAll(): Observable<Interview[]> {
    return this.api.get<Interview[]>(API_ENDPOINTS.INTERVIEW.HISTORY);
  }

  start(payload: {
    resumeId?: string;
    jobDescriptionId?: string;
  }): Observable<Interview> {
    return this.api.post<Interview>(API_ENDPOINTS.INTERVIEW.START, payload);
  }
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly api = inject(ApiService);

  getProfile(): Observable<User> {
    return this.api.get<User>(API_ENDPOINTS.PROFILE.GET);
  }

  updateProfile(user: Partial<User>): Observable<User> {
    return this.api.put<User>(API_ENDPOINTS.PROFILE.UPDATE, user);
  }
}
