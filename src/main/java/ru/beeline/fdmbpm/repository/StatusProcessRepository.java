package ru.beeline.fdmbpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.StatusProcess;

public interface StatusProcessRepository extends JpaRepository<StatusProcess, Integer> {
}
