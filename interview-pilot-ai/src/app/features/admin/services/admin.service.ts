import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/http/api-endpoints';
import { ApiService } from '../../../core/http/api.service';
import { Activity, AdminStats, AdminUser, ComponentStatus } from '../models/admin.model';

/** Admin-only, read-only endpoints; the backend answers 403 for anyone without the ADMIN role. */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly api = inject(ApiService);

  stats(): Observable<AdminStats> {
    return this.api.get<AdminStats>(API_ENDPOINTS.ADMIN.STATS);
  }

  users(): Observable<AdminUser[]> {
    return this.api.get<AdminUser[]>(API_ENDPOINTS.ADMIN.USERS);
  }

  activity(limit = 50): Observable<Activity[]> {
    return this.api.get<Activity[]>(`${API_ENDPOINTS.ADMIN.ACTIVITY}?limit=${limit}`);
  }

  system(): Observable<ComponentStatus[]> {
    return this.api.get<ComponentStatus[]>(API_ENDPOINTS.ADMIN.SYSTEM);
  }
}
