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
  JOB_DESCRIPTIONS: 'job-descriptions',
  INTERVIEWS: 'interviews',
  MATCHES: 'matches',
  APPLICATIONS: 'applications',
  SKILL_GAPS: 'skill-gaps',
  MY_PROFILE: 'users/me',
  ADMIN: {
    STATS: 'admin/stats',
    USERS: 'admin/users',
    ACTIVITY: 'admin/activity',
    SYSTEM: 'admin/system',
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
