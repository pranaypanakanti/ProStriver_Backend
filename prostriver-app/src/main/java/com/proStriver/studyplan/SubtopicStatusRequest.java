package com.proStriver.studyplan;

import jakarta.validation.constraints.NotNull;

public record SubtopicStatusRequest(@NotNull Boolean done) {
}