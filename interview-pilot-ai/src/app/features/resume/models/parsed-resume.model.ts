export type ResumeRecord = Record<string, unknown>;

export interface ParsedResume {
  resumeId: number;
  personalInformation: ResumeRecord;
  experience: ResumeRecord[];
  education: ResumeRecord[];
  skills: string[];
  projects: ResumeRecord[];
  certifications: string[];
  achievements: string[];
  languages: string[];
  softSkills: string[];
  technicalSkills: string[];
  companies: string[];
  designations: string[];
  yearsOfExperience: string | null;
  summary: string | null;
  keywords: string[];
  createdAt: string;
}
