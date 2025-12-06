# Demo2 – Application Jakarta EE avec SmalLib et Keycloak (WildFly 31)

Cette application `demo2` illustre l'utilisation de la librairie **SmalLib** dans un environnement Jakarta EE (WildFly 31). Elle se déploie sous forme de WAR et expose l'API protégée `/api/whoami` en SAML 2.0.

## Pré-requis
- Java 17
- Maven
- WildFly 31 (ou tout conteneur Jakarta EE 10 compatible WAR)
- Keycloak accessible sur `https://localhost:8443` avec la configuration suivante :

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
          "config": {"user.attribute": "email", "friendly.name": "email", "attribute.name": "email", "attribute.nameformat": "Basic"}},
        {"name": "username", "protocol": "saml", "protocolMapper": "saml-user-property-mapper", "consentRequired": false,
          "config": {"user.attribute": "username", "friendly.name": "username", "attribute.name": "username", "attribute.nameformat": "Basic"}},
        {"name": "roles", "protocol": "saml", "protocolMapper": "saml-role-list-mapper", "consentRequired": false,
          "config": {"single": "false", "attribute.nameformat": "Basic", "attribute.name": "Role", "friendly.name": "role"}}
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

  "roles": {
    "realm": [
      {"name": "user", "description": "Default user role"},
      {"name": "admin", "description": "Administrator"}
    ]
  }
}
```

## Architecture
- `SamlBootstrapListener` construit la configuration SAML (SP + IdP Keycloak), instancie le `SamlAuthenticationFilterHelper` et l'enregistre dans le contexte Servlet.
- `SamlJakartaFilter` protège tous les chemins, gère l'ACS `/login/saml2/sso/acs`, le SLO `/logout/saml` et redirige vers Keycloak selon les bindings SAML.
- `Demo2Application` active JAX-RS sous `/api`.
- `WhoAmIResource` lit le `SamlPrincipal` en session (`saml.principal`) et renvoie ses attributs au format JSON.
- `SamlErrorServlet` sert un message simple sur `/saml/error` en cas d'erreur SAML.

## Construction et déploiement
1. Installer la librairie SmalLib dans votre repository Maven local (à la racine du dépôt) :
   ```bash
   mvn install
   ```
2. Construire l'archive WAR demo2 :
   ```bash
   mvn -f examples/demo2/pom.xml package
   ```
3. Déployer `examples/demo2/target/demo2.war` sur WildFly 31 (copie dans `standalone/deployments/` ou via la CLI).

## Utilisation
1. Démarrer WildFly et vérifier que Keycloak écoute bien sur `https://localhost:8443` avec le realm ci-dessus.
2. Accéder à `http://localhost:8080/api/whoami` :
   - l'utilisateur est redirigé vers Keycloak pour authentification ;
   - une fois authentifié (ex. `demo-user` / `demo123`), l'API renvoie le `NameID`, le `sessionIndex` et les attributs SAML.
3. Pour fermer la session SAML, appeler `http://localhost:8080/logout/saml`.
