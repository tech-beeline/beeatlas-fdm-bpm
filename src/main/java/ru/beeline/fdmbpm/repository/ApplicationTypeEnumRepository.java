package ru.beeline.fdmbpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.ApplicationTypeEnum;

public interface ApplicationTypeEnumRepository extends JpaRepository<ApplicationTypeEnum, Integer> {
    ApplicationTypeEnum findByAlias(String alias);
}
