package org.hmiso.examples.samldemo;

import org.hmiso.saml.api.SamlPrincipal;
import org.hmiso.saml.integration.SamlSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {
    private final SamlSessionHelper sessionHelper;
    private final SamlDemoProperties properties;

    public UserController(SamlSessionHelper sessionHelper, SamlDemoProperties properties) {
        this.sessionHelper = sessionHelper;
        this.properties = properties;
    }

    @GetMapping("/whoami")
    public ResponseEntity<?> whoAmI(HttpSession session) {
        Optional<SamlPrincipal> principal = sessionHelper.retrievePrincipalFromSession(session, properties.sessionAttributeKey());
        if (principal.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utilisateur non authentifié");
        }
        SamlPrincipal value = principal.get();
        return ResponseEntity.ok(new UserProfile(value.getNameId(), value.getAttributes()));
    }

    public record UserProfile(String subject, java.util.Map<String, Object> attributes) {
    }
}
