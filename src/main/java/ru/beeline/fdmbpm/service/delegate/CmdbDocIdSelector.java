/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Выбор последнего процесса по дате статуса и разбор docId из контекста.
 */
public final class CmdbDocIdSelector {

    private CmdbDocIdSelector() {
    }

    public record Candidate(int processId, LocalDateTime statusCreatedDate) {
    }

    public static Integer latestProcessId(List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .filter(candidate -> candidate.statusCreatedDate() != null)
                .max(Comparator.comparing(Candidate::statusCreatedDate).thenComparingInt(Candidate::processId))
                .map(Candidate::processId)
                .orElse(null);
    }

    public static Integer parseDocId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
