# Démo SmalLib + Keycloak (SAML)

Cette application Spring Boot expose l'API `/api/whoami` protégée par un flux SAML 2.0 en s'appuyant sur la librairie **SmalLib** et un realm Keycloak préconfiguré.

## Pré-requis
- Java 17
- Maven
- Keycloak accessible sur `https://localhost:8443` avec le realm SAML suivant :

```json
{
  "realm": "saml-realm",
  "enabled": true,
  "displayName": "SAML Identity Provider Realm",
  "id": "saml-realm",
  "sslRequired": "external",
  "registrationAllowed": false,
  "clients": [
    {
      "clientId": "saml-sp",
      "protocol": "saml",
      "enabled": true,
      "attributes": {
        "saml.assertion.signature": "true",
        "saml.force.post.binding": "true",
        "saml.multivalued.roles": "false",
        "saml.signing.private.key": "",
        "saml.signing.certificate": "",
        "saml_force_name_id_format": "true",
        "saml_name_id_format": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
      },
      "redirectUris": [
        "https://localhost:8080/login/saml2/sso/*",
        "http://localhost:8080/login/saml2/sso/*"
      ],
      "protocolMappers": [
        {"name": "email", "protocol": "saml", "protocolMapper": "saml-user-property-mapper", "consentRequired": false,
          "config": {"user.attribute": "email", "friendly.name": "email", "attribute.name": "email", "attribute.nameformat": "Basic"}}
      ]
    }
  ],
  "users": [
    {
      "username": "demo-user",
      "enabled": true,
      "emailVerified": true,
      "email": "demo.user@example.com",
      "credentials": [
        {"type": "password", "value": "demo123", "temporary": false}
      ],
      "realmRoles": ["user"],
      "attributes": {"firstName": "Demo", "lastName": "User"}
    }
  ],
  "roles": {"realm": [{"name": "user", "description": "Default user role"}]}
}
```

> Importez le JSON ci-dessus dans Keycloak puis générez le certificat et la clé de signature côté client `saml-sp`.

## Architecture de l'exemple
- `SamlDemoConfiguration` construit la `SamlConfiguration` (SP + IdP) avec les URLs Keycloak (`/realms/saml-realm/protocol/saml`).
- `SamlFilter` s'appuie sur `SamlAuthenticationFilterHelper` pour :
  - rediriger vers l'IdP si aucune session SAML n'est présente ;
  - consommer l'ACS `/login/saml2/sso/acs` (binding POST) ;
  - déclencher le Single Logout via `/logout/saml`.
- `UserController` expose `/api/whoami` qui renvoie le `SamlPrincipal` en session.

## Lancer la démo
1. Construire et installer la librairie SmalLib dans le repository local Maven :
   ```bash
   mvn install
   ```
2. Démarrer l'application depuis le répertoire `examples/keycloak-saml-demo` :
   ```bash
   mvn spring-boot:run
   ```
3. Accéder à `http://localhost:8080/api/whoami` :
   - l'utilisateur est redirigé vers Keycloak ;
   - après authentification (par exemple `demo-user` / `demo123`), l'API retourne le sujet et les attributs SAML.

## Points d'extension
- Ajustez les URLs ACS/SLO dans `SamlDemoConfiguration` si l'application écoute sur HTTPS ou sur un port différent.
- Remplacez les valeurs de keystore dans `SecurityConfig` pour activer la signature des requêtes vers Keycloak.
