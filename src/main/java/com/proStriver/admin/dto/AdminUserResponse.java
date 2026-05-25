package com.proStriver.admin.dto;

import com.proStriver.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AdminUserResponse {
    private UUID id;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
}