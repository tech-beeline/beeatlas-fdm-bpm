package ru.beeline.fdmbpm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmbpm.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
}
