package com.prostriver.topic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prostriver.common.redis.RedisService;
import com.prostriver.common.exception.ApiException;
import com.prostriver.entity.RevisionSchedule;
import com.prostriver.entity.enums.RevisionStatus;
import com.prostriver.entity.enums.TopicStatus;
import com.prostriver.repository.RevisionScheduleRepository;
import com.prostriver.repository.TopicRepository;
import com.prostriver.repository.UserRepository;
import com.prostriver.topic.dto.TodayRevisionItemResponse;
import com.prostriver.topic.dto.UpcomingRevisionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevisionService {

    private final RevisionScheduleRepository revisionScheduleRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final Clock clock;

    @Autowired
    private RedisService redisService;

    @Transactional(readOnly = true)
    public List<TodayRevisionItemResponse> today(String emailRaw) {
        UUID userId = userRepository.findByEmail(emailRaw.toLowerCase().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();

        LocalDate today = LocalDate.now(clock);

        String key = "revisions:today:user:" + String.valueOf(userId);

        List<TodayRevisionItemResponse> responses = redisService.get(
                key,
                new TypeReference<List<TodayRevisionItemResponse>>(){}
        );
        if(responses != null) {
            return responses;
        }

        List<TodayRevisionItemResponse> revisions = revisionScheduleRepository.findTodayForUser(userId, today, RevisionStatus.PENDING)
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

        if(!revisions.isEmpty()) {
            redisService.set(key,
                    revisions,
                    10L);
        }

        return revisions;
    }

    @Transactional(readOnly = true)
    public List<UpcomingRevisionResponse> upcoming(String emailRaw) {
        UUID userId = userRepository.findByEmail(emailRaw.toLowerCase().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();

        LocalDate today = LocalDate.now(clock);

        String key = "revisions:upcoming:user:" + String.valueOf(userId);

        List<UpcomingRevisionResponse> responses = redisService.get(
                key,
                new TypeReference<List<UpcomingRevisionResponse>>(){}
        );
        if(responses != null) {
            return responses;
        }

        List<UpcomingRevisionResponse> revisions = revisionScheduleRepository.findUpcomingForUser(userId, today)
                .stream()
                .map(rs -> new UpcomingRevisionResponse(
                        rs.getId(),
                        rs.getTopic().getId(),
                        rs.getTopic().getSubject(),
                        rs.getTopic().getTitle(),
                        rs.getDayNumber(),
                        rs.getScheduledDate(),
                        rs.getStatus().name()
                ))
                .toList();

        if(!revisions.isEmpty()) {
            redisService.set(
                    key,
                    revisions,
                    10L
            );
        }
        return revisions;
    }

    @Transactional
    public void complete(String emailRaw, UUID revisionScheduleId) {
        UUID userId = userRepository.findByEmail(emailRaw.toLowerCase().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();

        String key1 = "revisions:today:user:" + String.valueOf(userId);
        String key2 = "revisions:upcoming:user:" + String.valueOf(userId);

        redisService.delete(key1);
        redisService.delete(key2);


        RevisionSchedule rs = revisionScheduleRepository.findActiveByIdAndUserId(revisionScheduleId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Revision not found"));

        if (rs.getTopic().getStatus() != TopicStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Revisions can only be completed for ACTIVE topics");
        }

        if (rs.getStatus() != RevisionStatus.PENDING) return;

        rs.setStatus(RevisionStatus.COMPLETED);
        rs.setCompletedAt(LocalDateTime.now(clock));
        revisionScheduleRepository.save(rs);

        boolean allDone = revisionScheduleRepository.findAllActiveByTopicId(rs.getTopic().getId()).stream()
                .allMatch(x -> x.getStatus() == RevisionStatus.COMPLETED || x.getStatus() == RevisionStatus.CANCELLED);

        if (allDone) {
            rs.getTopic().setStatus(TopicStatus.COMPLETED);
            topicRepository.save(rs.getTopic());
        }
    }
}