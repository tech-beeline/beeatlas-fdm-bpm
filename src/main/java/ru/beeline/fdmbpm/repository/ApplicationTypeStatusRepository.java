package ru.beeline.fdmbpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.ApplicationTypeStatus;

import java.util.Optional;


public interface ApplicationTypeStatusRepository extends JpaRepository<ApplicationTypeStatus, Integer> {

    ApplicationTypeStatus findByTypeIdAndAlias(Integer typeId, String alias);

    Optional <ApplicationTypeStatus> findByTypeIdAndSerialNumber(Integer typeId, Integer serialNumber);
}
