/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.repository.camunda;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Срез с произвольным смещением. Номер страницы у {@link org.springframework.data.domain.PageRequest}
 * задаёт смещение только кратно размеру страницы.
 */
public final class LimitOffset implements Pageable {

    private final int limit;
    private final long offset;
    private final Sort sort;

    public LimitOffset(int limit, long offset, Sort sort) {
        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return limit == 0 ? 0 : (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new LimitOffset(limit, offset + limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        if (offset == 0) {
            return this;
        }
        return new LimitOffset(limit, Math.max(0, offset - limit), sort);
    }

    @Override
    public Pageable first() {
        return new LimitOffset(limit, 0, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new LimitOffset(limit, (long) pageNumber * limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
