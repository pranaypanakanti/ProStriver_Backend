package com.ProStriver.admin;

import com.ProStriver.admin.dto.AdminRevisionPlanResponse;
import com.ProStriver.admin.dto.CreateRevisionPlanRequest;
import com.ProStriver.admin.dto.UpdateRevisionPlanRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/revision-plans")
@RequiredArgsConstructor
public class AdminRevisionPlanController {

    private final AdminRevisionPlanService adminRevisionPlanService;

    @GetMapping
    public ResponseEntity<List<AdminRevisionPlanResponse>> list() {
        return ResponseEntity.ok(adminRevisionPlanService.list());
    }

    @PostMapping
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