package com.prostriver.user;

import com.prostriver.user.dto.UpdateUserProfileRequest;
import com.prostriver.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Profile("api")
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