package com.sporthub.service;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

final class SearchPage {
    private SearchPage() {}

    static PageRequest of(int page, int size, String sortBy, String direction, Set<String> fields) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page phải >= 0 và size phải từ 1 đến 100");
        }
        if (!fields.contains(sortBy)) {
            throw new IllegalArgumentException("Trường sắp xếp không hợp lệ: " + sortBy);
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new IllegalArgumentException("direction phải là asc hoặc desc");
        }
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        if (!"id".equals(sortBy)) sort = sort.and(Sort.by("id"));
        return PageRequest.of(page, size, sort);
    }
}
