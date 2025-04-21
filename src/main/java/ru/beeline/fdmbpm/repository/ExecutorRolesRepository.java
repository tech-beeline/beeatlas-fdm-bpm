package ru.beeline.fdmbpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.ExecutorRoles;

import java.util.List;

public interface ExecutorRolesRepository extends JpaRepository<ExecutorRoles,Integer> {

    List<ExecutorRoles> findByTypeId(Integer typeId);

    List<ExecutorRoles> findByRoleIn(List<String> roles);
}
