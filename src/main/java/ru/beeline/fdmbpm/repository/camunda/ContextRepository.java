package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.Context;

import java.util.List;

public interface ContextRepository extends JpaRepository<Context, Integer> {

    List<Context> findByNameAndValue (String name, String Value);

    List<Context> findByCamundaProcessId(Integer camundaProcessId);
}
