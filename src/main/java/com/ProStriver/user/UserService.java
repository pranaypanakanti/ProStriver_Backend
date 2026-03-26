package com.ProStriver.user;

import com.ProStriver.common.exception.ApiException;
import com.ProStriver.entity.User;
import com.ProStriver.repository.UserRepository;
import com.ProStriver.user.dto.UpdateUserProfileRequest;
import com.ProStriver.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMe(String emailRaw) {
        User user = getUserByEmail(emailRaw);
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse patchMe(String emailRaw, UpdateUserProfileRequest req) {

        if (req.getFullName() == null && req.getNotificationPreference() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No fields provided to update");
        }

        User user = getUserByEmail(emailRaw);

        if (req.getFullName() != null) {
            String normalizedName = normalizeFullName(req.getFullName());
            validateFullName(normalizedName);
            user.setFullName(normalizedName);
        }

        if (req.getNotificationPreference() != null) {
            user.setNotificationPreference(req.getNotificationPreference());
        }

        userRepository.save(user);
        return toProfile(user);
    }

    private User getUserByEmail(String emailRaw) {
        String email = emailRaw.toLowerCase().trim();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getCreatedAt(),
                user.getFullName(),
                user.getNotificationPreference()
        );
    }

    private String normalizeFullName(String input) {
        return input.trim().replaceAll("\\s+", " ");
    }

    private void validateFullName(String fullName) {
        if (fullName.length() < 2 || fullName.length() > 80) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Full name must be between 2 and 80 characters");
        }
        if (fullName.chars().anyMatch(ch -> Character.isISOControl(ch))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Full name contains invalid characters");
        }
    }
}