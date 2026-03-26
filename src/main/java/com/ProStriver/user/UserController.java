package com.ProStriver.user;

import com.ProStriver.user.dto.UpdateUserProfileRequest;
import com.ProStriver.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.getMe(authentication.getName()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> patchMe(
            Authentication authentication,
            @RequestBody @Valid UpdateUserProfileRequest req
    ) {
        return ResponseEntity.ok(userService.patchMe(authentication.getName(), req));
    }
}