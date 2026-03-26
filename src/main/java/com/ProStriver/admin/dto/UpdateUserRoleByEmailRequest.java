package com.ProStriver.admin.dto;

import com.ProStriver.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleByEmailRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private Role role;
}