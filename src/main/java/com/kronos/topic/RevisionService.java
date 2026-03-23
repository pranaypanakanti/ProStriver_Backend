package com.kronos.topic;

import com.kronos.common.exception.ApiException;
import com.kronos.entity.RevisionSchedule;
import com.kronos.entity.enums.RevisionStatus;
import com.kronos.entity.enums.TopicStatus;
import com.kronos.repository.RevisionScheduleRepository;
import com.kronos.repository.TopicRepository;
import com.kronos.repository.UserRepository;
import com.kronos.topic.dto.TodayRevisionItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevisionService {

    private final RevisionScheduleRepository revisionScheduleRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository; // NEW

    @Transactional(readOnly = true)
    public List<TodayRevisionItemResponse> today(String emailRaw) {
        UUID userId = userRepository.findByEmail(emailRaw.toLowerCase().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();

        LocalDate today = LocalDate.now();

        return revisionScheduleRepository.findTodayForUser(userId, today, RevisionStatus.PENDING)
                .stream()
                .map(rs -> new TodayRevisionItemResponse(
                        rs.getId(),
                        rs.getTopic().getId(),
                        rs.getTopic().getSubject(),
                        rs.getTopic().getTitle(),
                        rs.getDayNumber(),
                        rs.getScheduledDate()
                ))
                .toList();
    }

    @Transactional
    public void complete(String emailRaw, UUID revisionScheduleId) {
        UUID userId = userRepository.findByEmail(emailRaw.toLowerCase().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();

        RevisionSchedule rs = revisionScheduleRepository.findActiveByIdAndUserId(revisionScheduleId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Revision not found"));

        if (rs.getTopic().getStatus() != TopicStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Revisions can only be completed for ACTIVE topics");
        }

        if (rs.getStatus() != RevisionStatus.PENDING) return;

        rs.setStatus(RevisionStatus.COMPLETED);
        rs.setCompletedAt(LocalDateTime.now());
        revisionScheduleRepository.save(rs);

        boolean allDone = revisionScheduleRepository.findAllActiveByTopicId(rs.getTopic().getId()).stream()
                .allMatch(x -> x.getStatus() == RevisionStatus.COMPLETED || x.getStatus() == RevisionStatus.CANCELLED);

        if (allDone) {
            rs.getTopic().setStatus(TopicStatus.COMPLETED);
            topicRepository.save(rs.getTopic());
        }
    }
}