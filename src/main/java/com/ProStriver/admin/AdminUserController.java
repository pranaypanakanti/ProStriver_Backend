package com.ProStriver.admin;

import com.ProStriver.admin.dto.AdminUserResponse;
import com.ProStriver.admin.dto.UpdateUserRoleByEmailRequest;
import com.ProStriver.admin.dto.UpdateUserRoleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminUserService.listUsers(q, pageable));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<AdminUserResponse> updateRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest req
    ) {
        return ResponseEntity.ok(adminUserService.updateRole(userId, req));
    }

    @PatchMapping("/role-by-email")
    public ResponseEntity<AdminUserResponse> updateRoleByEmail(
            @Valid @RequestBody UpdateUserRoleByEmailRequest req
    ) {
        return ResponseEntity.ok(adminUserService.updateRoleByEmail(req));
    }
}