package com.kronos.admin;

import com.kronos.admin.dto.AdminRevisionPlanResponse;
import com.kronos.admin.dto.CreateRevisionPlanRequest;
import com.kronos.admin.dto.UpdateRevisionPlanRequest;
import com.kronos.common.exception.ApiException;
import com.kronos.entity.RevisionPlan;
import com.kronos.repository.RevisionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRevisionPlanService {

    private final RevisionPlanRepository revisionPlanRepository;

    @Transactional(readOnly = true)
    public List<AdminRevisionPlanResponse> list() {
        return revisionPlanRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminRevisionPlanResponse create(CreateRevisionPlanRequest req) {
        RevisionPlan rp = new RevisionPlan();
        String normalizedName = normalize(req.getName());
        if (revisionPlanRepository.findByName(normalizedName).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Revision plan name already exists");
        }
        rp.setName(normalizedName);
        rp.setDescription(req.getDescription() == null ? null : req.getDescription().trim());
        rp.setRevisionDaysPattern(normalizePattern(req.getRevisionDaysPattern()));

        revisionPlanRepository.save(rp);
        return toResponse(rp);
    }

    @Transactional
    public AdminRevisionPlanResponse update(UUID id, UpdateRevisionPlanRequest req) {
        RevisionPlan rp = revisionPlanRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Revision plan not found"));

        if (req.getName() != null) {
            String normalizedName = normalize(req.getName());
            revisionPlanRepository.findByName(normalizedName).ifPresent(existing -> {
                if (!existing.getId().equals(rp.getId())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Revision plan name already exists");
                }
            });
            rp.setName(normalizedName);
        }
        if (req.getDescription() != null) rp.setDescription(req.getDescription().trim());
        if (req.getRevisionDaysPattern() != null) rp.setRevisionDaysPattern(normalizePattern(req.getRevisionDaysPattern()));

        revisionPlanRepository.save(rp);
        return toResponse(rp);
    }

    @Transactional
    public void delete(UUID id) {
        if (!revisionPlanRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Revision plan not found");
        }
        revisionPlanRepository.deleteById(id);
    }

    private AdminRevisionPlanResponse toResponse(RevisionPlan rp) {
        return new AdminRevisionPlanResponse(
                rp.getId(),
                rp.getName(),
                rp.getDescription(),
                rp.getRevisionDaysPattern()
        );
    }

    private String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    private String normalizePattern(String p) {
        return p.trim().replaceAll("\\s+", "").replaceAll(",{2,}", ",");
    }
}