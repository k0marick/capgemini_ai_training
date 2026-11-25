package com.todomanager.common.web;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable list envelope: { content, page, size, totalElements, totalPages }. */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static <T> PageResponse<T> of(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

}
