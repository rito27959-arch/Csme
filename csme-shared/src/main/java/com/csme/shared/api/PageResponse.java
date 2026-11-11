package com.csme.shared.api;

import java.util.List;

/** Page de résultats. Toutes les listes potentiellement volumineuses sont paginées. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public PageResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }
}
