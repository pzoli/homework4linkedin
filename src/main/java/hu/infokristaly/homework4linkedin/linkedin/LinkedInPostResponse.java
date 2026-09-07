package hu.infokristaly.homework4linkedin.linkedin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A sikeresen létrejött LinkedIn-poszt azonosítója")
public record LinkedInPostResponse(
        @Schema(example = "urn:li:share:6844785523593134080")
        String postId
) {
}
