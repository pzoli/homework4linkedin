package hu.infokristaly.homework4linkedin.linkedin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Közzéteendő LinkedIn szöveges poszt")
public record LinkedInPostRequest(
        @Schema(description = "A poszt szövege", example = "Örömmel osztom meg az új projektemet!", maxLength = 3000)
        String text
) {
}
