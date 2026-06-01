package com.proStriver.user.dto;

import com.proStriver.entity.enums.NotificationPreference;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private String email;
    private LocalDateTime createdAt;
    private String fullName;
    private NotificationPreference notificationPreference;
}