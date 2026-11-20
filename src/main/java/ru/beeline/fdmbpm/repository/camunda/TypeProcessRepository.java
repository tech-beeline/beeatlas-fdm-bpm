/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmbpm.domain.TypeProcess;

import java.util.List;

@Repository
public interface TypeProcessRepository extends JpaRepository<TypeProcess, Integer> {
    TypeProcess findByAlias(String alias);

    List<TypeProcess> findAllByOrderByIdAsc();
}
