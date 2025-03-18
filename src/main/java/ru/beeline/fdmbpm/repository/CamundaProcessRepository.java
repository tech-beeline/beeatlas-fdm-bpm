package ru.beeline.fdmbpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.CamundaProcess;

import java.util.Optional;

public interface CamundaProcessRepository extends JpaRepository<CamundaProcess, Integer> {

    Optional<CamundaProcess> findByProcId(String id);
    Optional<CamundaProcess> findByBusinessKey(String id);
}
