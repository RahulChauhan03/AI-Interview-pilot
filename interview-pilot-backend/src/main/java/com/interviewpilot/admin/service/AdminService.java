package com.interviewpilot.admin.service;

import com.interviewpilot.admin.dto.ActivityDto;
import com.interviewpilot.admin.dto.AdminStatsDto;
import com.interviewpilot.admin.dto.ComponentStatusDto;
import java.util.List;

/** Read-only, system-level information for administrators (access is restricted in SecurityConfig). */
public interface AdminService {
    AdminStatsDto stats();
    List<ActivityDto> recentActivity(int limit);
    List<ComponentStatusDto> systemStatus();
}
