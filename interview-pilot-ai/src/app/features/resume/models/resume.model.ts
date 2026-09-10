import { ResumeStatus } from './resume-status.enum';

export interface ResumeSummary {
  id: number;
  originalFileName: string;
  status: ResumeStatus;
  summary: string | null;
  uploadTime: string;
}

export interface Resume {
  id: number;
  userId: number;
  originalFileName: string;
  storedFileName: string;
  fileExtension: string;
  mimeType: string | null;
  fileSize: number;
  storagePath: string;
  status: ResumeStatus;
  uploadTime: string;
  parseTime: string | null;
  processingTime: number | null;
  language: string | null;
  summary: string | null;
  createdAt: string;
  updatedAt: string | null;
}
