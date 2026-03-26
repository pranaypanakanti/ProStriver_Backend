package com.ProStriver.admin.dto;

import com.ProStriver.entity.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    @NotNull
    private Role role;
}