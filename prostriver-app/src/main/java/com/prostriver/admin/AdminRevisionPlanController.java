package com.prostriver.admin;

import com.prostriver.admin.dto.AdminRevisionPlanResponse;
import com.prostriver.admin.dto.CreateRevisionPlanRequest;
import com.prostriver.admin.dto.UpdateRevisionPlanRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Profile("api")
@RestController
@RequestMapping("/api/admin/revision-plans")
@RequiredArgsConstructor
public class AdminRevisionPlanController {

    private final AdminRevisionPlanService adminRevisionPlanService;

    @PostMapping("/create")
    public ResponseEntity<AdminRevisionPlanResponse> create(@Valid @RequestBody CreateRevisionPlanRequest req) {
        return ResponseEntity.ok(adminRevisionPlanService.create(req));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminRevisionPlanResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRevisionPlanRequest req
    ) {
        return ResponseEntity.ok(adminRevisionPlanService.update(id, req));
    }

}