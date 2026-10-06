import { Interview } from '../../interview/models/interview.model';
import { JobDescription, ResumeMatch } from '../../job-description/models/job-description.model';

export type ApplicationStatus = 'SAVED' | 'PREPARING' | 'APPLIED' | 'ASSESSMENT' | 'INTERVIEW' | 'OFFER' | 'REJECTED' | 'WITHDRAWN';

export const APPLICATION_STATUSES: { value: ApplicationStatus; label: string; tone: '' | 'info' | 'success' | 'warning' | 'danger' }[] = [
  { value: 'SAVED', label: 'Saved', tone: '' },
  { value: 'PREPARING', label: 'Preparing', tone: 'info' },
  { value: 'APPLIED', label: 'Applied', tone: 'info' },
  { value: 'ASSESSMENT', label: 'Assessment', tone: 'warning' },
  { value: 'INTERVIEW', label: 'Interview', tone: 'warning' },
  { value: 'OFFER', label: 'Offer', tone: 'success' },
  { value: 'REJECTED', label: 'Rejected', tone: 'danger' },
  { value: 'WITHDRAWN', label: 'Withdrawn', tone: '' },
];

export function statusInfo(status: ApplicationStatus | null | undefined) {
  return APPLICATION_STATUSES.find((item) => item.value === status) ?? APPLICATION_STATUSES[0];
}

export interface JobApplication {
  id: number;
  jobDescriptionId: number;
  jobTitle: string;
  companyName: string;
  resumeId: number | null;
  resumeFileName: string | null;
  status: ApplicationStatus;
  appliedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
  /** 0-100, from the latest match of the application's resume (or any resume) */
  matchScore: number | null;
  matchId: number | null;
  tailoredResumeAt: string | null;
  coverLetterAt: string | null;
  interviewCount: number;
  latestInterviewId: number | null;
  latestInterviewStatus: 'IN_PROGRESS' | 'COMPLETED' | null;
  latestInterviewScore: number | null;
}

export interface TailoredResumeContent {
  name: string;
  contact: string[];
  summary: string;
  skills: string[];
  experience: { title: string; company: string; duration: string; location: string; bullets: string[] }[];
  projects: { name: string; description: string; technologies: string[] }[];
  education: { degree: string; institution: string; duration: string; details: string }[];
  certifications: string[];
}

export interface TailoredResume {
  applicationId: number;
  baseResumeId: number | null;
  baseResumeFileName: string | null;
  generatedAt: string;
  content: TailoredResumeContent;
  /** Skills the job asks for that are not on the resume, so they were deliberately left out. */
  omittedSkills: string[];
  /** AI suggestions dropped because the resume does not support them. */
  removedSuggestions: number;
}

export interface CoverLetterContent {
  senderName: string;
  senderContact: string[];
  date: string;
  recipient: string;
  subject: string;
  greeting: string;
  paragraphs: string[];
  closing: string;
}

export interface CoverLetter {
  applicationId: number;
  baseResumeId: number | null;
  updatedAt: string;
  content: CoverLetterContent;
  removedSentences: number;
  edited: boolean;
}

export interface SkillGapItem {
  name: string;
  /** Number of the user's jobs the skill appears in (overall view). */
  jobs: number;
  /** Average interview score for interview categories. */
  score: number | null;
}

export interface SkillGaps {
  strong: SkillGapItem[];
  developing: SkillGapItem[];
  missing: SkillGapItem[];
  recommendations: string[];
}

export interface Workspace {
  job: JobDescription;
  application: JobApplication | null;
  latestMatch: ResumeMatch | null;
  interviews: Interview[];
  skillGaps: SkillGaps;
  topics: string[];
  recentQuestions: string[];
}
