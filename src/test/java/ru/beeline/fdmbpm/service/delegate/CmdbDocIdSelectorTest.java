/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CmdbDocIdSelectorTest {

    @Test
    void latestProcess_isLastByDateThenId() {
        LocalDateTime earlier = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime later = earlier.plusDays(1);
        Integer latest = CmdbDocIdSelector.latestProcessId(List.of(
                new CmdbDocIdSelector.Candidate(2, earlier),
                new CmdbDocIdSelector.Candidate(5, later),
                new CmdbDocIdSelector.Candidate(9, later)
        ));
        assertEquals(9, latest);
    }

    @Test
    void processWithoutDate_isDropped() {
        assertNull(CmdbDocIdSelector.latestProcessId(List.of(
                new CmdbDocIdSelector.Candidate(1, null)
        )));
    }

    @Test
    void parseDocId_acceptsIntegerAndRejectsBlankAndText() {
        assertEquals(15, CmdbDocIdSelector.parseDocId(" 15 "));
        assertNull(CmdbDocIdSelector.parseDocId(" "));
        assertNull(CmdbDocIdSelector.parseDocId("abc"));
        assertNull(CmdbDocIdSelector.parseDocId(null));
    }
}
