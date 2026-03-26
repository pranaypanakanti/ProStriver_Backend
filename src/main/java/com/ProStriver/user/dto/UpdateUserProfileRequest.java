package com.ProStriver.user.dto;

import com.ProStriver.entity.enums.NotificationPreference;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String fullName;
    private NotificationPreference notificationPreference;
}