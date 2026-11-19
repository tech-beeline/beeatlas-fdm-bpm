/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.repository.camunda;

import ru.beeline.fdmbpm.domain.CamundaProcess;

import java.util.List;

public interface ProcessHistoryRepository {

    long countHistory(ProcessHistoryFilter filter);

    List<CamundaProcess> findHistory(ProcessHistoryFilter filter, int limit, int offset);
}
