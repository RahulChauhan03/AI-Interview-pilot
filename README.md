# AI Interview Pilot

AI Interview Pilot is a full-stack AI-powered platform designed to help candidates prepare for job applications, analyze resumes against job descriptions, practice interviews, and receive AI-generated feedback.

The project is built with Java Spring Boot, Angular, MySQL, MongoDB, and Ollama.

---

## 🚀 Features

### 🔐 Authentication & Security
- User registration and login
- JWT-based authentication
- Role-based authorization
- Secure environment-based configuration
- Protected APIs
- Password reset functionality

### 📄 Resume Management
- Upload PDF resumes
- Resume file validation
- Resume text extraction using Apache Tika
- AI-powered resume parsing
- Parsed resume data stored in MongoDB
- Resume processing status tracking
- Resume ownership validation

### 💼 Job Description Management
- Create job descriptions
- View job descriptions
- Update job descriptions
- Delete job descriptions
- User-specific job descriptions

### 🎯 Resume & Job Matching
- Match resumes against job descriptions
- AI-powered match analysis
- Match score
- Matched skills
- Missing skills
- Strengths and gaps
- AI-generated matching summary

### 🎤 AI Interview
- Generate interview sessions from resume + job description
- AI-generated interview questions
- Interview session management
- Answer submission
- AI-powered answer evaluation
- Interview scoring
- Feedback and improvement suggestions

### 🤖 AI Processing
- Local AI processing using Ollama
- Configurable AI model
- Currently optimized for `llama3.2:1b`
- Structured AI responses
- AI response validation
- Timeout handling
- Retry handling
- Circuit breaker
- Controlled AI failure handling

### 🗄️ Database
- MySQL for transactional application data
- MongoDB for parsed resume/AI data
- Flyway database migrations
- Hibernate schema validation
- Foreign key constraints and indexes

---

## 🏗️ Architecture

```text
                    ┌─────────────────────┐
                    │      Angular UI     │
                    │     Frontend        │
                    └──────────┬──────────┘
                               │
                               │ REST API
                               ▼
                    ┌─────────────────────┐
                    │   Spring Boot API   │
                    │      Backend        │
                    └──────────┬──────────┘
                               │
             ┌─────────────────┼─────────────────┐
             │                 │                 │
             ▼                 ▼                 ▼
       ┌───────────┐     ┌───────────┐    ┌───────────┐
       │   MySQL   │     │  MongoDB  │    │  Ollama   │
       │           │     │           │    │    AI     │
       │ App Data  │     │ Resume AI │    │ Processing │
       └───────────┘     └───────────┘    └───────────┘
