package com.prostriver.admin.dto;

import com.prostriver.entity.enums.Role;
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