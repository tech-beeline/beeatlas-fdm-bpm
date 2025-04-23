package ru.beeline.fdmbpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.Application;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    Optional<Application> findByBusinessKey(String businessKey);

    List<Application> findAllByTypeIdIn(List<Integer> typeId);

    Optional<List<Application>> findAllByAuthorId(Integer authorId);

    Optional<List<Application>> findAllByExecutorId(Integer ExecutorId);
}
