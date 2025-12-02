package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.fdmbpm.domain.CamundaProcess;

import java.util.Optional;

public interface CamundaProcessRepository extends JpaRepository<CamundaProcess, Integer> {

    Optional<CamundaProcess> findByProcId(String id);

    Optional<CamundaProcess> findByBusinessKey(String id);

    @Modifying
    @Query("update CamundaProcess p set p.isAsync = true where p.id = :procId and p.isAsync = false")
    int markAsyncTrueIfFalse(@Param("procId") Integer procId);

    Optional<CamundaProcess> findByProcIdAndBusinessKey(String processId, String businessKey);
}
