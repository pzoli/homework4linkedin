package hu.infokristaly.homework4linkedin.linkedin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/linkedin")
@Tag(name = "LinkedIn", description = "OAuth-kapcsolat és szöveges LinkedIn-posztok kezelése")
public class LinkedInController {
    private static final String OAUTH_STATE = "linkedin-oauth-state";
    private static final String LINKEDIN_MEMBER = "linkedin-member";
    private final LinkedInService linkedInService;
    private final SecureRandom secureRandom = new SecureRandom();

    public LinkedInController(LinkedInService linkedInService) {
        this.linkedInService = linkedInService;
    }

    /** Megnyitja a LinkedIn jóváhagyási képernyőjét. */
    @GetMapping("/authorize")
    @Operation(
            summary = "LinkedIn OAuth jóváhagyás indítása",
            description = "Böngészőben nyisd meg ezt a végpontot. A LinkedIn jóváhagyása után a rendszer "
                    + "eltárolja az aktuális böngésző munkamenetéhez tartozó hozzáférést."
    )
    @ApiResponse(responseCode = "302", description = "Átirányítás a LinkedIn bejelentkezési/jóváhagyási oldalára")
    public ResponseEntity<Void> authorize(@Parameter(hidden = true) HttpSession session) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(OAUTH_STATE, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(linkedInService.authorizationUri(state))
                .build();
    }

    /** LinkedIn ide irányít vissza a felhasználó jóváhagyása után. */
    @GetMapping("/callback")
    @Operation(summary = "OAuth visszahívás", description = "A LinkedIn hívja meg; kézzel általában nem szükséges használni.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A LinkedIn kapcsolat létrejött"),
            @ApiResponse(responseCode = "400", description = "A jóváhagyás elutasult vagy az OAuth state érvénytelen")
    })
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, name = "error") String error,
            @Parameter(hidden = true) HttpSession session
    ) {
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", error));
        }
        Object expectedState = session.getAttribute(OAUTH_STATE);
        session.removeAttribute(OAUTH_STATE);
        if (expectedState == null || !expectedState.equals(state) || code == null || code.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Érvénytelen OAuth state vagy hiányzó authorization code."));
        }
        LinkedInService.AuthenticatedMember member = linkedInService.exchangeCode(code);
        session.setAttribute(LINKEDIN_MEMBER, member);
        return ResponseEntity.ok(Map.of("message", "LinkedIn kapcsolat létrejött. Most POST /api/linkedin/posts hívással küldhetsz posztot.", "author", member.authorUrn()));
    }

    @PostMapping("/posts")
    @Operation(
            summary = "Szöveges LinkedIn-poszt közzététele",
            description = "Előtte ugyanebben a böngészőben végezd el a GET /api/linkedin/authorize OAuth-folyamatot. "
                    + "A Swagger UI a saját munkamenet-sütijét küldi a kéréshez."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "A poszt elkészült", content = @Content(schema = @Schema(implementation = LinkedInPostResponse.class))),
            @ApiResponse(responseCode = "400", description = "Üres vagy túl hosszú posztszöveg"),
            @ApiResponse(responseCode = "401", description = "Nincs LinkedIn OAuth-munkamenet")
    })
    public ResponseEntity<?> createPost(@RequestBody LinkedInPostRequest request, @Parameter(hidden = true) HttpSession session) {
        Object value = session.getAttribute(LINKEDIN_MEMBER);
        if (!(value instanceof LinkedInService.AuthenticatedMember member)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Előbb nyisd meg a GET /api/linkedin/authorize végpontot és hagyd jóvá a hozzáférést."));
        }
        String postId = linkedInService.createTextPost(member, request == null ? null : request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(new LinkedInPostResponse(postId));
    }
}
