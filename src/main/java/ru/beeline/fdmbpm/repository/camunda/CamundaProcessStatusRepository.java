/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;

import java.util.List;
import java.util.Optional;

public interface CamundaProcessStatusRepository extends JpaRepository<CamundaProcessStatus, Integer> {

    Optional<CamundaProcessStatus> findFirstByCamundaProcessIdOrderByCreatedDateDesc(Integer id);

    List<CamundaProcessStatus> findByCamundaProcessId(Integer id);

    Optional<CamundaProcessStatus> findByCamundaProcessIdAndStatusProcessId(Integer id, Integer statusProcessId);
}
