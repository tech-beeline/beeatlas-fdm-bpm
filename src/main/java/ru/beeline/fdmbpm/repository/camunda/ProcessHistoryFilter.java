/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.repository.camunda;

import java.time.LocalDateTime;
import java.util.List;

public record ProcessHistoryFilter(
        List<Integer> typeIds,
        LocalDateTime createDateFrom,
        LocalDateTime createDateTo,
        String contextName,
        String contextValue
) {
    public ProcessHistoryFilter {
        typeIds = typeIds == null ? List.of() : List.copyOf(typeIds);
    }
}
