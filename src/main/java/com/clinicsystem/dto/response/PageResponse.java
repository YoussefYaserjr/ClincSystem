package com.clinicsystem.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PageResponse<T>(List<T> content, int page, int size,
                              long totalElements, int totalPages, boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        List<T> content = page.getContent().stream().map(mapper).collect(Collectors.toList());
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
