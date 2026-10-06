package com.interviewpilot.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComponentStatusDto {
    private final String name;
    /** UP or DOWN. */
    private final String status;
    /** Short, non-sensitive detail (never a connection string or exception message). */
    private final String detail;
}
