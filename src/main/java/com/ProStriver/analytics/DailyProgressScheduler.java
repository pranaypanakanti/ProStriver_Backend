package com.ProStriver.analytics;

import com.ProStriver.entity.DailyProgress;
import com.ProStriver.entity.User;
import com.ProStriver.entity.enums.RevisionStatus;
import com.ProStriver.repository.DailyProgressRepository;
import com.ProStriver.repository.RevisionScheduleRepository;
import com.ProStriver.repository.TopicRepository;
import com.ProStriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class DailyProgressScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyProgressScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final UserRepository userRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final TopicRepository topicRepository;
    private final RevisionScheduleRepository revisionScheduleRepository;

    private final Clock clock;

    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void computeYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        LocalDateTime from = yesterday.atStartOfDay();
        LocalDateTime to = yesterday.plusDays(1).atStartOfDay();

        int totalProcessed = 0;
        int failures = 0;
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        Page<User> page;
        do {
            page = userRepository.findAll(pageable);

            for (User user : page.getContent()) {
                try {
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
                } catch (Exception e) {
                    log.error("DailyProgress: failed for user {}", user.getId(), e);
                    failures++;
                }
            }

            totalProcessed += page.getNumberOfElements();
            pageable = page.nextPageable();
        } while (page.hasNext());

        log.info("DailyProgress: computed {} for {} users ({} failures)", yesterday, totalProcessed, failures);
    }
}