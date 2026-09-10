import { environment } from '../../../environments/environment';

export const apiConfig = {
  baseUrl: environment.apiUrl,
  auth: {
    login: 'auth/login',
    register: 'auth/register',
    logout: 'auth/logout',
    profile: 'auth/profile',
  },
} as const;
