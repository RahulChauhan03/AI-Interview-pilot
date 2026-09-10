import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';
import { API_ENDPOINTS } from '../../../core/http/api-endpoints';
import { ChangePasswordRequest, ProfileDetails, ProfileUpdateRequest } from '../interfaces/profile.interface';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly api = inject(ApiService);

  getProfile(): Observable<ProfileDetails> {
    return this.api.get<ProfileDetails>(API_ENDPOINTS.AUTH.CURRENT_USER);
  }

  updateProfile(payload: ProfileUpdateRequest): Observable<ProfileDetails> {
    return this.api.put<ProfileDetails>('/api/users/profile', payload);
  }

  changePassword(payload: ChangePasswordRequest): Observable<void> {
    return this.api.put<void>('/api/users/change-password', payload);
  }
}
