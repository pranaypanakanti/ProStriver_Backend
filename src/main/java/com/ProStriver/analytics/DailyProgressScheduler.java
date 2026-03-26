package com.ProStriver.analytics;

import com.ProStriver.entity.DailyProgress;
import com.ProStriver.entity.User;
import com.ProStriver.entity.enums.RevisionStatus;
import com.ProStriver.repository.DailyProgressRepository;
import com.ProStriver.repository.RevisionScheduleRepository;
import com.ProStriver.repository.TopicRepository;
import com.ProStriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class DailyProgressScheduler {

    private final UserRepository userRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final TopicRepository topicRepository;
    private final RevisionScheduleRepository revisionScheduleRepository;

    private final Clock clock;

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void computeYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        LocalDateTime from = yesterday.atStartOfDay();
        LocalDateTime to = yesterday.plusDays(1).atStartOfDay();

        List<User> users = userRepository.findAll();

        for (User user : users) {
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
}