export type InterviewStatus = 'IN_PROGRESS' | 'COMPLETED';

export interface InterviewAnswer {
  id: number;
  questionId: number;
  answer: string;
  /** 0-100 */
  score: number | null;
  correctness: string | null;
  relevance: string | null;
  feedback: string | null;
  strengths: string[];
  improvements: string[];
  createdAt: string;
}

export interface InterviewQuestion {
  id: number;
  sequenceNumber: number;
  question: string;
  category: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  answer: InterviewAnswer | null;
}

export interface Interview {
  id: number;
  resumeId: number | null;
  jobDescriptionId: number | null;
  status: InterviewStatus;
  /** 0-100, set when completed */
  overallScore: number | null;
  startedAt: string;
  completedAt: string | null;
  jobTitle: string | null;
  companyName: string | null;
  resumeFileName: string | null;
  totalQuestions: number;
  answeredQuestions: number;
  nextQuestionId: number | null;
  /** Only present when a single interview is loaded. */
  questions: InterviewQuestion[] | null;
  /** True when an interview in progress for the same job and resume was returned instead of a new one. */
  reused?: boolean;
}

export interface CreateInterviewRequest {
  resumeId: number;
  jobDescriptionId: number;
  questionCount: number;
}
