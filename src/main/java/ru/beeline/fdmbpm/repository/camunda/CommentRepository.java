/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    List<Comment> findAllByApplicationId(Integer applicationId);
}
