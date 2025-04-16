package ru.beeline.fdmbpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;

public interface StatusProcessRepository extends JpaRepository<StatusProcess, Integer> {
    StatusProcess findByAliasAnAndTypeProcessId(String alias, Integer typeProcessId);
}
