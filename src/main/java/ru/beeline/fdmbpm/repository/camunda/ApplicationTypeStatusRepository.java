/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.ApplicationTypeStatus;

import java.util.Optional;


public interface ApplicationTypeStatusRepository extends JpaRepository<ApplicationTypeStatus, Integer> {

    Optional<ApplicationTypeStatus> findByTypeIdAndAlias(Integer typeId, String alias);

    Optional<ApplicationTypeStatus> findByTypeIdAndSerialNumber(Integer typeId, Integer serialNumber);

    Optional<ApplicationTypeStatus> findByIdAndTypeId(Integer id, Integer typeId);
}
