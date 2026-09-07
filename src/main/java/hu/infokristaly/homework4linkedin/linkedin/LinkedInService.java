package hu.infokristaly.homework4linkedin.linkedin;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
public class LinkedInService {
    private static final String AUTHORIZATION_ENDPOINT = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_ENDPOINT = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String USERINFO_ENDPOINT = "https://api.linkedin.com/v2/userinfo";
    private static final String POSTS_ENDPOINT = "https://api.linkedin.com/rest/posts";

    private final LinkedInProperties properties;
    private final RestClient httpClient;

    public LinkedInService(LinkedInProperties properties) {
        this.properties = properties;
        this.httpClient = RestClient.create();
    }

    public URI authorizationUri(String state) {
        properties.validateConfigured();
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_ENDPOINT)
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("state", state)
                .queryParam("scope", "openid profile w_member_social")
                .build()
                .encode()
                .toUri();
    }

    public AuthenticatedMember exchangeCode(String authorizationCode) {
        properties.validateConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        form.add("redirect_uri", properties.redirectUri());
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        TokenResponse token = httpClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new IllegalStateException("A LinkedIn nem adott vissza access tokent.");
        }

        UserInfo user = httpClient.get()
                .uri(USERINFO_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
                .retrieve()
                .body(UserInfo.class);
        if (user == null || user.subject() == null || user.subject().isBlank()) {
            throw new IllegalStateException("A LinkedIn userinfo válaszából hiányzik a sub azonosító.");
        }
        return new AuthenticatedMember(token.accessToken(), "urn:li:person:" + user.subject());
    }

    public String createTextPost(AuthenticatedMember member, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("A poszt szövege nem lehet üres.");
        }
        if (text.length() > 3_000) {
            throw new IllegalArgumentException("A poszt szövege legfeljebb 3000 karakter lehet.");
        }

        LinkedInPost post = new LinkedInPost(
                member.authorUrn(), text, "PUBLIC",
                new Distribution("MAIN_FEED", List.of(), List.of()),
                "PUBLISHED", false
        );
        return httpClient.post()
                .uri(POSTS_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + member.accessToken())
                .header("Linkedin-Version", properties.apiVersion())
                .header("X-Restli-Protocol-Version", "2.0.0")
                .contentType(MediaType.APPLICATION_JSON)
                .body(post)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("A LinkedIn posztolás sikertelen: HTTP " + response.getStatusCode());
                    }
                    String postId = response.getHeaders().getFirst("x-restli-id");
                    if (postId == null || postId.isBlank()) {
                        throw new IllegalStateException("A LinkedIn válaszából hiányzik az x-restli-id fejléc.");
                    }
                    return postId;
                });
    }

    public record AuthenticatedMember(String accessToken, String authorUrn) {
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record UserInfo(@JsonProperty("sub") String subject) {
    }

    private record LinkedInPost(
            String author, String commentary, String visibility, Distribution distribution,
            String lifecycleState, boolean isReshareDisabledByAuthor
    ) {
    }

    private record Distribution(
            String feedDistribution, List<Object> targetEntities, List<Object> thirdPartyDistributionChannels
    ) {
    }
}
