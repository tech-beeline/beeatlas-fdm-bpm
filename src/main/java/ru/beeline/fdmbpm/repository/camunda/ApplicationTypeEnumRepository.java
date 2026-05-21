/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.ApplicationTypeEnum;

import java.util.Optional;

public interface ApplicationTypeEnumRepository extends JpaRepository<ApplicationTypeEnum, Integer> {
    Optional <ApplicationTypeEnum> findByAlias(String alias);
}
