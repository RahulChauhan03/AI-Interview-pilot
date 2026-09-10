import { ResumeStatus } from './resume-status.enum';

export interface ResumeUpload {
  id: number;
  originalFileName: string;
  status: ResumeStatus;
  uploadTime: string;
}
