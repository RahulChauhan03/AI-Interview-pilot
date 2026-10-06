import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/http/api-endpoints';
import { ApiService } from '../../../core/http/api.service';

export interface Profile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
  updatedAt: string | null;
}

/** The signed-in user's own profile (/api/users/me). Only the name can be changed. */
@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly api = inject(ApiService);

  get(): Observable<Profile> {
    return this.api.get<Profile>(API_ENDPOINTS.MY_PROFILE);
  }

  update(firstName: string, lastName: string): Observable<Profile> {
    return this.api.put<Profile>(API_ENDPOINTS.MY_PROFILE, { firstName, lastName });
  }
}
