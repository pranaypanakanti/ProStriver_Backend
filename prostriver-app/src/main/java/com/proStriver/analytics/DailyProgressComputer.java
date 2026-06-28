package com.proStriver.analytics;

import com.proStriver.entity.DailyProgress;
import com.proStriver.entity.User;
import com.proStriver.entity.enums.RevisionStatus;
import com.proStriver.repository.DailyProgressRepository;
import com.proStriver.repository.RevisionScheduleRepository;
import com.proStriver.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class DailyProgressComputer {

    private final DailyProgressRepository dailyProgressRepository;
    private final TopicRepository topicRepository;
    private final RevisionScheduleRepository revisionScheduleRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void computeForUser(User user, LocalDate yesterday) {
        LocalDateTime from = yesterday.atStartOfDay();
        LocalDateTime to = yesterday.plusDays(1).atStartOfDay();

        int topicsCreated = (int) topicRepository.countCreatedByUserIdAndRange(user.getId(), from, to);
        int emailed = (int) revisionScheduleRepository.countEmailedForUserAndDate(user.getId(), yesterday);
        int completed = (int) revisionScheduleRepository.countEmailedForUserAndDateByStatus(user.getId(), yesterday, RevisionStatus.COMPLETED);

        DailyProgress dp = dailyProgressRepository.findByUserIdAndDate(user.getId(), yesterday)
                .orElseGet(DailyProgress::new);

        dp.setUser(user);
        dp.setDate(yesterday);
        dp.setTopicsCreated(topicsCreated);
        dp.setRevisionsEmailed(emailed);
        dp.setRevisionsCompleted(completed);

        dailyProgressRepository.save(dp);
    }
}