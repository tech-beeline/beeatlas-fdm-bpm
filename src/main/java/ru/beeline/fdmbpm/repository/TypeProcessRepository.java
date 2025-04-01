package ru.beeline.fdmbpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.TypeProcess;

public interface TypeProcessRepository extends JpaRepository<TypeProcess, Integer> {
}
