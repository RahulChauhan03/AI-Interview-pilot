-- Initial schema for Interview Pilot AI (MySQL).
--
-- This reproduces, column for column, the schema that Hibernate previously generated with
-- ddl-auto=create, including its constraint names, so that a fresh database built by Flyway
-- is identical to existing databases that were baselined at version 1.
--
-- Existing databases created before Flyway was introduced must NOT run this script; they are
-- baselined at version 1 instead (see spring.flyway.baseline-on-migrate in application.properties).

CREATE TABLE `users` (
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `resumes` (
  `is_deleted` bit(1) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `file_size` bigint NOT NULL,
  `parse_time` datetime(6) DEFAULT NULL,
  `processing_time` bigint DEFAULT NULL,
  `resume_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `upload_time` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `file_extension` varchar(255) NOT NULL,
  `language` varchar(255) DEFAULT NULL,
  `mime_type` varchar(255) DEFAULT NULL,
  `original_file_name` varchar(255) NOT NULL,
  `storage_path` varchar(255) NOT NULL,
  `stored_file_name` varchar(255) NOT NULL,
  `summary` text,
  `status` enum('FAILED','PARSED','PROCESSING','UPLOADED') NOT NULL,
  PRIMARY KEY (`resume_id`),
  UNIQUE KEY `UKosfvueynd81qlkvu46h40fe4n` (`stored_file_name`),
  KEY `FK340nuaivxiy99hslr3sdydfvv` (`user_id`),
  CONSTRAINT `FK340nuaivxiy99hslr3sdydfvv` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `password_reset_tokens` (
  `used` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `password_reset_token_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  PRIMARY KEY (`password_reset_token_id`),
  UNIQUE KEY `idx_password_reset_token_hash` (`token_hash`),
  KEY `idx_password_reset_user_id` (`user_id`),
  CONSTRAINT `fk_password_reset_token_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `job_descriptions` (
  `created_at` datetime(6) NOT NULL,
  `job_description_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `company_name` varchar(255) NOT NULL,
  `job_description` text NOT NULL,
  `job_title` varchar(255) NOT NULL,
  PRIMARY KEY (`job_description_id`),
  KEY `FK1jay4axsbmkxwh8k28mdqh3dt` (`user_id`),
  CONSTRAINT `FK1jay4axsbmkxwh8k28mdqh3dt` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `resume_job_match` (
  `overall_match_percentage` double DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `job_description_id` bigint NOT NULL,
  `match_id` bigint NOT NULL AUTO_INCREMENT,
  `resume_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `missing_skills` text,
  `recommendations` text,
  `strengths` text,
  PRIMARY KEY (`match_id`),
  KEY `FKnmn41gae56x72n29a6nt12ri7` (`job_description_id`),
  KEY `FKl0mn2j3v2wg4n4ygy7g99enn3` (`resume_id`),
  CONSTRAINT `FKl0mn2j3v2wg4n4ygy7g99enn3` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`resume_id`),
  CONSTRAINT `FKnmn41gae56x72n29a6nt12ri7` FOREIGN KEY (`job_description_id`) REFERENCES `job_descriptions` (`job_description_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `interview_sessions` (
  `overall_score` double DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `job_description_id` bigint DEFAULT NULL,
  `resume_id` bigint DEFAULT NULL,
  `session_id` bigint NOT NULL AUTO_INCREMENT,
  `started_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`session_id`),
  KEY `FKiahc2a95ntno0o0fu3hlhifv2` (`job_description_id`),
  KEY `FKhresfe6p1s53klvmqhxxissa2` (`resume_id`),
  KEY `FKoa5rgsdu7rqa8y74yph1fuqe5` (`user_id`),
  CONSTRAINT `FKhresfe6p1s53klvmqhxxissa2` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`resume_id`),
  CONSTRAINT `FKiahc2a95ntno0o0fu3hlhifv2` FOREIGN KEY (`job_description_id`) REFERENCES `job_descriptions` (`job_description_id`),
  CONSTRAINT `FKoa5rgsdu7rqa8y74yph1fuqe5` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `interview_questions` (
  `sequence_number` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `question_id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  `difficulty` varchar(255) DEFAULT NULL,
  `question` text NOT NULL,
  PRIMARY KEY (`question_id`),
  KEY `FKf1qet5d65jvfag4tqnc3oqyo8` (`session_id`),
  CONSTRAINT `FKf1qet5d65jvfag4tqnc3oqyo8` FOREIGN KEY (`session_id`) REFERENCES `interview_sessions` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `interview_answers` (
  `score` double DEFAULT NULL,
  `answer_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `question_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `ai_feedback` text,
  `answer` text NOT NULL,
  PRIMARY KEY (`answer_id`),
  KEY `FK4kqx5jj1pfofmdtxoihtq2sg5` (`question_id`),
  CONSTRAINT `FK4kqx5jj1pfofmdtxoihtq2sg5` FOREIGN KEY (`question_id`) REFERENCES `interview_questions` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `api_logs` (
  `completion_tokens` int DEFAULT NULL,
  `prompt_tokens` int DEFAULT NULL,
  `total_tokens` int DEFAULT NULL,
  `api_log_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `execution_time_ms` bigint DEFAULT NULL,
  `session_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `model_name` varchar(255) DEFAULT NULL,
  `provider` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`api_log_id`),
  KEY `FKbuh477lrm1fl0ao7borfi385r` (`session_id`),
  CONSTRAINT `FKbuh477lrm1fl0ao7borfi385r` FOREIGN KEY (`session_id`) REFERENCES `interview_sessions` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
