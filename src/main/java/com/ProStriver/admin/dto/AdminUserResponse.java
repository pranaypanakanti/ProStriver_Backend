package com.ProStriver.admin.dto;

import com.ProStriver.entity.enums.Role;
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