export interface JobDescription {
  id: number;
  userId: number;
  companyName: string;
  jobTitle: string;
  jobDescription: string;
  createdAt: string;
  updatedAt: string | null;
}

export interface JobDescriptionRequest {
  companyName: string;
  jobTitle: string;
  jobDescription: string;
}

export interface ResumeMatch {
  id: number;
  resumeId: number;
  resumeFileName: string;
  jobDescriptionId: number;
  jobTitle: string;
  companyName: string;
  /** 0-100 */
  matchScore: number;
  strengths: string[];
  missingSkills: string[];
  recommendations: string[];
  createdAt: string;
  /** True when the previous result was returned because nothing changed. */
  reused: boolean;
}
