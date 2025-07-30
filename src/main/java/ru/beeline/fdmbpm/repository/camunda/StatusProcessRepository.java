package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.StatusProcess;

public interface StatusProcessRepository extends JpaRepository<StatusProcess, Integer> {
    StatusProcess findByAliasAndTypeProcessId(String alias, Integer typeProcessId);
}
