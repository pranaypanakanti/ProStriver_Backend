package com.prostriver.user.dto;

import com.prostriver.entity.enums.NotificationPreference;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String fullName;
    private NotificationPreference notificationPreference;
}