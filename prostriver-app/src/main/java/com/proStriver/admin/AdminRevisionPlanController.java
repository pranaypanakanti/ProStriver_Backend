package com.proStriver.admin;

import com.proStriver.admin.dto.AdminRevisionPlanResponse;
import com.proStriver.admin.dto.CreateRevisionPlanRequest;
import com.proStriver.admin.dto.UpdateRevisionPlanRequest;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminRevisionPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}