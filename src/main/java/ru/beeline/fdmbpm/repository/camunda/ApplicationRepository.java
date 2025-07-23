package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.Application;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    Optional<Application> findByBusinessKey(String businessKey);
    Optional<Application> findByProcessId(String processId);

    List<Application> findAllByTypeIdInAndExecutorIdNull(List<Integer> typeId);

    Optional<List<Application>> findAllByAuthorId(Integer authorId);

    Optional<List<Application>> findAllByExecutorId(Integer ExecutorId);
}
