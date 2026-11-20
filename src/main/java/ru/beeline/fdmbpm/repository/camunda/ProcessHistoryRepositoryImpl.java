/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.repository.camunda;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.Context;

import java.util.ArrayList;
import java.util.List;

public class ProcessHistoryRepositoryImpl implements ProcessHistoryRepository {

    @PersistenceContext(unitName = "processes")
    private EntityManager entityManager;

    @Override
    public long countHistory(ProcessHistoryFilter filter) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<CamundaProcess> process = query.from(CamundaProcess.class);
        query.select(builder.count(process));
        applyFilter(builder, query, process, filter);
        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public List<CamundaProcess> findHistory(ProcessHistoryFilter filter, int limit, int offset) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<CamundaProcess> query = builder.createQuery(CamundaProcess.class);
        Root<CamundaProcess> process = query.from(CamundaProcess.class);
        applyFilter(builder, query, process, filter);
        query.orderBy(builder.desc(process.get("createDate")), builder.desc(process.get("id")));
        return entityManager.createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    private void applyFilter(CriteriaBuilder builder, CriteriaQuery<?> query, Root<CamundaProcess> process,
                             ProcessHistoryFilter filter) {
        List<Predicate> predicates = new ArrayList<>();
        if (!filter.typeIds().isEmpty()) {
            predicates.add(process.get("typeProcessId").in(filter.typeIds()));
        }
        if (filter.createDateFrom() != null) {
            predicates.add(builder.greaterThanOrEqualTo(process.get("createDate"), filter.createDateFrom()));
        }
        if (filter.createDateTo() != null) {
            predicates.add(builder.lessThanOrEqualTo(process.get("createDate"), filter.createDateTo()));
        }
        if (filter.contextName() != null) {
            Subquery<Integer> contextMatch = query.subquery(Integer.class);
            Root<Context> context = contextMatch.from(Context.class);
            List<Predicate> contextPredicates = new ArrayList<>();
            contextPredicates.add(builder.equal(context.get("camundaProcessId"), process.get("id")));
            contextPredicates.add(builder.equal(context.get("name"), filter.contextName()));
            if (filter.contextValue() != null) {
                contextPredicates.add(builder.equal(context.get("value"), filter.contextValue()));
            }
            contextMatch.select(context.get("id")).where(contextPredicates.toArray(Predicate[]::new));
            predicates.add(builder.exists(contextMatch));
        }
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }
    }
}
