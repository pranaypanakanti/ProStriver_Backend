package com.ProStriver.admin;

import com.ProStriver.admin.dto.AdminUserResponse;
import com.ProStriver.admin.dto.UpdateUserRoleByEmailRequest;
import com.ProStriver.admin.dto.UpdateUserRoleRequest;
import com.ProStriver.common.exception.ApiException;
import com.ProStriver.entity.User;
import com.ProStriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String q, Pageable pageable) {
        Page<User> page = (q == null || q.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByEmailContainingIgnoreCase(q.trim(), pageable);

        return page.map(this::toResponse);
    }

    @Transactional
    public AdminUserResponse updateRole(UUID userId, UpdateUserRoleRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        user.setRole(req.getRole());
        userRepository.save(user);

        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse updateRoleByEmail(UpdateUserRoleByEmailRequest req) {
        String email = req.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        user.setRole(req.getRole());
        userRepository.save(user);

        return toResponse(user);
    }

    private AdminUserResponse toResponse(User u) {
        return new AdminUserResponse(
                u.getId(),
                u.getEmail(),
                u.getRole(),
                u.getCreatedAt()
        );
    }
}