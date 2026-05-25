package com.proStriver.user.dto;

import com.proStriver.entity.enums.NotificationPreference;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String fullName;
    private NotificationPreference notificationPreference;
}