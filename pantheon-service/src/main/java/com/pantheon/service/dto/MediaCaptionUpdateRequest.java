package com.pantheon.service.dto;

/** Body of {@code PATCH /api/daily-reports/{id}/media/{mediaId}} — updates a media entry's caption only. {@code caption} may be {@code null}/blank to clear it. */
public record MediaCaptionUpdateRequest(String caption) {
}
