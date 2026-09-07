package hu.infokristaly.homework4linkedin.linkedin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "linkedin")
public record LinkedInProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String apiVersion
) {
    public void validateConfigured() {
        if (isBlank(clientId) || isBlank(clientSecret) || isBlank(redirectUri)) {
            throw new IllegalStateException("A linkedin.client-id, linkedin.client-secret és linkedin.redirect-uri beállítása kötelező.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
