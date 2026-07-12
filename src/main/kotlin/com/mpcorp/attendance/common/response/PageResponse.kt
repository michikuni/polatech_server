package com.mpcorp.attendance.common.response

import org.springframework.data.domain.Page

/** Stable, serialization-friendly page envelope (instead of exposing Spring's
 *  internal `PageImpl` shape, which is discouraged). */
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T : Any> from(page: Page<T>): PageResponse<T> = PageResponse(
            items = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
