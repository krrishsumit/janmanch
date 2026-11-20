package com.janmanch.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;

/** A simple page of results. Serializable so it can be stored in the Redis cache. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages)
        implements Serializable {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(new ArrayList<>(page.getContent()), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
