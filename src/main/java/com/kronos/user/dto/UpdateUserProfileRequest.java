package com.kronos.user.dto;

import com.kronos.entity.enums.NotificationPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String fullName;
    private NotificationPreference notificationPreference;
}