package com.ProStriver.topic;

import com.ProStriver.entity.RevisionSchedule;
import com.ProStriver.entity.enums.RevisionStatus;
import com.ProStriver.repository.RevisionScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class RevisionEndOfDayCanceller {

    private static final Logger log = LoggerFactory.getLogger(RevisionEndOfDayCanceller.class);

    private final RevisionScheduleRepository revisionScheduleRepository;

    private final Clock clock;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void cancelMissed() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        log.info("RevisionEndOfDayCanceller: checking for missed revisions on {}", yesterday);

        List<RevisionSchedule> pending =
                revisionScheduleRepository.findAllEmailedPendingForDate(yesterday, RevisionStatus.PENDING);

        if (pending.isEmpty()) {
            log.info("RevisionEndOfDayCanceller: no missed revisions for {}", yesterday);
            return;
        }

        pending.forEach(rs -> rs.setStatus(RevisionStatus.CANCELLED));
        revisionScheduleRepository.saveAll(pending);

        log.info("RevisionEndOfDayCanceller: cancelled {} missed revisions for {}", pending.size(), yesterday);
    }
}