export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: 'auth/login',
    REGISTER: 'auth/register',
    FORGOT_PASSWORD: 'auth/forgot-password',
    RESET_PASSWORD: 'auth/reset-password',
    LOGOUT: 'auth/logout',
    CURRENT_USER: 'auth/me',
  },
  RESUME: {
    UPLOAD: 'resumes/upload',
    LIST: 'resumes',
    DETAILS: 'resumes',
    PARSED: 'resumes',
    DELETE: 'resumes',
  },
  INTERVIEW: {
    START: '/interview/start',
    HISTORY: '/interview/history',
  },
  PROFILE: {
    GET: '/profile',
    UPDATE: '/profile',
  },
} as const;
