package com.prostriver.topic;

import com.prostriver.entity.enums.NotificationPreference;

import java.util.List;
import java.util.UUID;

public record RevisionDigestData(
        String email,
        NotificationPreference preference,
        List<Item> items
) {
    public record Item(UUID scheduleId, String subject, String title, int dayNumber) {}

    public List<UUID> scheduleIds() {
        return items.stream().map(Item::scheduleId).toList();
    }
}