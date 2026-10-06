package com.interviewpilot.application.entity;

/** Values of job_applications.status (a VARCHAR column, so stored via {@code name()}). */
public enum ApplicationStatus {
    SAVED,
    PREPARING,
    APPLIED,
    ASSESSMENT,
    INTERVIEW,
    OFFER,
    REJECTED,
    WITHDRAWN
}
