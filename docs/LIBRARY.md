# Documentation de la librairie SmalLib

Cette documentation synthétise les éléments clés de `SPECIFICATION.md`, `ttd.md` et `plantest.md` afin d'expliquer comment utiliser et valider la librairie.

## Panorama des modules
- **public-api** : `SamlServiceProvider`, `SamlServiceProviderFactory`, `SamlPrincipal`, `SamlConfiguration` (contrats stables).
- **config** : modèles `ServiceProviderConfig`, `IdentityProviderConfig`, `SecurityConfig` et loaders (YAML/JSON/Properties, metadata) décrits dans la spécification.
- **saml** : builders et validateurs (`AuthnRequestBuilder`, `SamlResponseValidator`, `LogoutRequestBuilder`, `LogoutResponseValidator`, `AssertionExtractor`).
- **binding** : encodeurs HTTP-Redirect/POST et gestion du `RelayState` opaque.
- **security** : initialisation OpenSAML, signature/chiffrement, résolution de keystores.
- **metadata** : parsing `EntityDescriptor` et cache TTL.
- **util** : utilitaires XML, horodatages, compression, logs.
- **integration** : filtres Servlet et JASPIC (Bearer JWT) + hooks d'audit/erreur avec helpers (`SamlAuthenticationFilterHelper`,
  `SamlSessionHelper`, `SessionTimeoutPolicy`, `SamlServerAuthModuleHelper`, `WildFlySecurityMappingHelper`, `JaasHelper`) pour
  WildFly/Servlet.

## Traçabilité exigences → tests
Les identifiants `TC-*` décrits dans `plantest.md` sont repris dans les méthodes de test pour faciliter la couverture :
- `TC-SAML-01/02` : builders/validateurs SAML.
- `TC-BIND-01/02/03` : encodeurs Redirect/POST et `RelayStateManager`.
- `TC-UTIL-02` : gestion du `clockSkew`.
- `TC-API-01/02` : contrats de la factory publique.
- `TC-E2E-AUTHN-01`, `TC-E2E-LOGOUT-01` : parcours d'authentification et de logout bout-en-bout simplifiés.

## Exécution des tests
```
mvn test
```
Les cas de test couvrent les parcours unitaires et une orchestration end-to-end minimale pour s'assurer que les dépendances entre modules fonctionnent.

## Intégration minimale
1. Construire une `SamlConfiguration` via un `ConfigLoader` (YAML/JSON/etc.).
2. Instancier `SamlServiceProvider` via `SamlServiceProviderFactory`.
3. Utiliser `initiateAuthentication`/`processSamlResponse` pour le cycle SSO et `initiateLogout`/`processLogoutResponse` pour le SLO.
4. Pour les applications WAR/EAR, brancher `SamlAuthenticationFilterHelper` (Servlet) ou `SamlServerAuthModuleHelper` (JASPIC)
   afin de protéger les URLs (`protectedPaths`) et stocker le principal en session (`sessionAttributeKey`).


## Integration Servlet par configuration YAML
SmalLib fournit des classes pretes a l'emploi pour un WAR Servlet :
- `SamlBootstrapListener`
- `SamlJakartaFilter`
- `SamlAppYamlConfigLoader`
- `SamlAppConfiguration`

Placer un fichier `saml-config.yml` dans le classpath (ex: `WEB-INF/classes/`).
Exemple minimal :

```yaml
app:
  protected-paths:
    - "/api/*"
  session-attribute-key: "saml.principal"
  error-path: "/saml/error"
  jaspic-enabled: true
  relay-state-ttl-minutes: 5

service-provider:
  entity-id: "saml-sp"
  base-url: "http://localhost:8080"
  acs-path: "/login/saml2/sso/acs"
  slo-path: "/logout/saml"
  name-id-format: "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
  authn-request-binding: "HTTP_POST"
  want-assertions-signed: true

identity-provider:
  entity-id: "saml-realm"
  single-sign-on-service-url: "https://localhost:8443/realms/saml-realm/protocol/saml"
  single-logout-service-url: "https://localhost:8443/realms/saml-realm/protocol/saml"
  metadata-url: "https://localhost:8443/realms/saml-realm/protocol/saml/descriptor"
  want-assertions-signed: true
  want-messages-signed: true
  supported-bindings:
    - "HTTP_POST"
    - "HTTP_REDIRECT"

security:
  clock-skew: "PT2M"
  signature-algorithm: "rsa-sha256"
  digest-algorithm: "sha256"
  encryption-algorithm: "aes256"
  force-https-redirect: false
  enable-detailed-logging: true
```

Surcharge possible avec `-Dsaml.config.path=...` ou la variable `SAML_CONFIG_PATH`.
Les chemins app.* sont relatifs au context path du WAR (le prefixe est ajoute automatiquement).

Chaque fonctionnalité est commentée dans le code source avec les exigences (`E7`, `E8`, `E10`, etc.) pour faciliter la vérification.
