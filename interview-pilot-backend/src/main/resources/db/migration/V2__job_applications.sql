-- Job application tracking. One application per job description. Generated documents (tailored resume,
-- cover letter) are stored as structured content and rendered to PDF when downloaded, so no files are kept.
-- Deleting a job description removes its application and documents.

CREATE TABLE `job_applications` (
  `application_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `job_description_id` bigint NOT NULL,
  `resume_id` bigint DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `applied_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`application_id`),
  UNIQUE KEY `uk_job_applications_job_description` (`job_description_id`),
  KEY `idx_job_applications_user` (`user_id`),
  KEY `idx_job_applications_resume` (`resume_id`),
  CONSTRAINT `fk_job_applications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_job_applications_job_description` FOREIGN KEY (`job_description_id`)
    REFERENCES `job_descriptions` (`job_description_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_job_applications_resume` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `application_documents` (
  `document_id` bigint NOT NULL AUTO_INCREMENT,
  `application_id` bigint NOT NULL,
  `document_type` varchar(30) NOT NULL,
  `base_resume_id` bigint DEFAULT NULL,
  `content` mediumtext NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`document_id`),
  UNIQUE KEY `uk_application_documents_type` (`application_id`, `document_type`),
  KEY `idx_application_documents_resume` (`base_resume_id`),
  CONSTRAINT `fk_application_documents_application` FOREIGN KEY (`application_id`)
    REFERENCES `job_applications` (`application_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_application_documents_resume` FOREIGN KEY (`base_resume_id`) REFERENCES `resumes` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
