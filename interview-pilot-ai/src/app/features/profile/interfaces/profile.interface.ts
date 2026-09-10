export interface ProfileDetails {
  id?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  location?: string;
  linkedInUrl?: string;
  githubUrl?: string;
  yearsOfExperience?: number | null;
  currentJobTitle?: string;
  aboutMe?: string;
  role?: string;
  accountStatus?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProfileUpdateRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  location?: string;
  linkedInUrl?: string;
  githubUrl?: string;
  yearsOfExperience?: number | null;
  currentJobTitle?: string;
  aboutMe?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}
