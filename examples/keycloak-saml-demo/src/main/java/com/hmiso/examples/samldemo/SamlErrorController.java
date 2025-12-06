package com.hmiso.examples.samldemo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SamlErrorController {

    @GetMapping("/saml/error")
    public ResponseEntity<String> error() {
        return ResponseEntity.badRequest().body("Erreur SAML - consultez les logs pour plus de détails");
    }
}
