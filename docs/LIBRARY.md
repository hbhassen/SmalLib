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
- **integration** : filtres Servlet et module JASPIC + hooks d'audit/erreur.

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

Chaque fonctionnalité est commentée dans le code source avec les exigences (`E7`, `E8`, `E10`, etc.) pour faciliter la vérification.
