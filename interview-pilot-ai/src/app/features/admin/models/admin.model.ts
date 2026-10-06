export interface AdminStats {
  users: number;
  admins: number;
  resumes: number;
  resumesParsed: number;
  resumesInProgress: number;
  resumesFailed: number;
  jobDescriptions: number;
  matches: number;
  interviews: number;
  interviewsCompleted: number;
  answers: number;
}

export interface ComponentStatus {
  name: string;
  status: 'UP' | 'DOWN';
  detail: string;
}

export interface Activity {
  time: string;
  type: 'USER' | 'RESUME' | 'MATCH' | 'INTERVIEW';
  description: string;
  userEmail: string | null;
  status: string;
  durationMs: number | null;
}

export interface AdminUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}
