package com.kronos.topic;

import com.kronos.entity.RevisionSchedule;
import com.kronos.entity.enums.RevisionStatus;
import com.kronos.repository.RevisionScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RevisionEndOfDayCanceller {

    private final RevisionScheduleRepository revisionScheduleRepository;

    private final Clock clock; // NEW

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cancelMissed() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);

        List<RevisionSchedule> pending =
                revisionScheduleRepository.findAllEmailedPendingForDate(yesterday, RevisionStatus.PENDING);

        if (pending.isEmpty()) return;

        pending.forEach(rs -> rs.setStatus(RevisionStatus.CANCELLED));
        revisionScheduleRepository.saveAll(pending);
    }
}