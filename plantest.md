# 🧪 Plan de tests unitaire et d'intégration – SmalLib

Ce plan est dérivé directement des exigences fonctionnelles, techniques et de sécurité définies dans `SPECIFICATION.md` et `umlConception.md`. Il vise **100 % de couverture** des fonctionnalités décrites pour l'API publique et les modules internes (`config`, `saml`, `binding`, `security`, `metadata`, `util`, `integration`).

## 1. Principes généraux

- **Traçabilité** : chaque cas de test est identifié par un code (ex. `TC-CONFIG-01`).
- **Types de tests** :
  - *Unitaires* : vérification isolée de chaque classe/fonction, avec mocks pour les dépendances (OpenSAML, servlet API, keystores).
  - *Intégration* : scénarios bout-en-bout incluant plusieurs modules (ex. génération AuthnRequest → encodage → IdP simulé → validation → principal).
- **Jeux de données** : certificats de test signés, métadonnées Keycloak fictives, payloads SAML générés via OpenSAML.
- **Automatisation** : exécution via Maven/Gradle (`test`), rapports de couverture (JaCoCo) pour garantir 100 % des exigences.

## 2. Plan des tests unitaires (par module)

### 2.1 Module `config`
- `TC-CONFIG-01` Charger YAML/JSON/Properties : chargeurs renvoient une `SamlConfiguration` valide, avec substitution d'environnement `${VAR}`.
- `TC-CONFIG-02` Priorité multi-sources : vérifie l’ordre env → classpath YAML → FS JSON → metadata URL.
- `TC-CONFIG-03` Validation stricte : URI mal formée, certificat absent, binding non supporté déclenchent une exception précise.
- `TC-CONFIG-04` Profils multiples : sélection d’un profil (dev/prod) retourne les URLs correspondantes.
- `TC-CONFIG-05` Hot-reload optionnel : rechargement remet à jour `SamlConfiguration` sans fuite de ressources.

### 2.2 Module `saml`
- `TC-SAML-01` `AuthnRequestBuilder` : construit une requête signée avec NameID/Binding configurés et horodatage dans la fenêtre de `clockSkew`.
- `TC-SAML-02` `SamlResponseValidator` : rejette Audience/Recipient invalides, InResponseTo absent, signature cassée ou horodatage expiré.
- `TC-SAML-03` `AssertionExtractor` : extrait attributs standard (NameID, email) et valeurs personnalisées, génère un `SamlPrincipal` complet.
- `TC-SAML-04` `LogoutRequestBuilder` et `LogoutResponseValidator` : gestion SessionIndex, StatusCode, et signature de logout.

### 2.3 Module `binding`
- `TC-BIND-01` `RedirectBindingEncoder` : compresse/encode en Base64 + signature query param, gère `RelayState` signé.
- `TC-BIND-02` `PostBindingEncoder` : génère un formulaire auto-posté avec signature enveloppée.
- `TC-BIND-03` `RelayStateManager` : génération opaque, persistance via `RelayStateStore`, expiration et anti-rejeu.

### 2.4 Module `security`
- `TC-SEC-01` `OpenSamlInitializer` : initialisation idempotente et thread-safe.
- `TC-SEC-02` `SignatureService` : signature RSA-SHA256, validation avec truststore, choix d’algorithme configurable.
- `TC-SEC-03` `EncryptionService` : chiffrement/déchiffrement conditionnel (certificat IdP présent), algorithme optionnel.
- `TC-SEC-04` `CredentialResolver` : charge clés depuis keystore/truststore (PKCS12/JKS/PEM), erreurs si alias ou mot de passe invalides.

### 2.5 Module `metadata`
- `TC-META-01` `MetadataParser` : parsing d’un `EntityDescriptor` (Keycloak) extrait endpoints/bindings/certificats.
- `TC-META-02` `EntityDescriptorCache` : stockage TTL, invalidation sur expiration ou changement d’ETag/Last-Modified.

### 2.6 Module `util`
- `TC-UTIL-01` `XmlUtils` : canonicalisation, suppression d’entités externes (XXE), marshalling/unmarshalling sécurisé.
- `TC-UTIL-02` `TimeUtils` : comparaison d’horodatages avec dérive ±2 min par défaut.
- `TC-UTIL-03` `CompressionUtils` : deflate/inflate compatible SAML Redirect.
- `TC-UTIL-04` `LoggingUtils` : niveaux DEBUG/TRACE activables, masquage des secrets.

### 2.7 Module `integration`
- `TC-INT-01` `SamlFilter` : redirection automatique vers l’IdP quand la session est absente, prise en compte `forceAuthn`/`isPassive`.
- `TC-INT-02` `JaspiServerAuthModule` : négociation JASPIC, propagation du `SamlPrincipal` dans le `Subject`.
- `TC-INT-03` Hooks `SamlAuditLogger` / `SamlErrorHandler` : log structuré des succès/erreurs et mapping vers codes HTTP.

### 2.8 API publique
- `TC-API-01` `SamlServiceProviderFactory` : création thread-safe d’instances avec cache de configuration.
- `TC-API-02` `SamlServiceProvider` : respect des signatures méthodes, erreurs contrôlées pour paramètres nuls ou bindings non supportés.

## 3. Plan des tests d'intégration

### 3.1 Parcours Authn complet (Redirect et POST)
- **TC-E2E-AUTHN-01** : génération AuthnRequest (SP) → encodage Redirect → IdP simulé renvoie SAMLResponse signée → `SamlResponseValidator` → `AssertionExtractor` → `SamlPrincipal` présent dans le contexte Servlet/JASPIC.
- **TC-E2E-AUTHN-02** : même flux en binding POST avec formulaire auto-posté, vérifie Recipient/ACS, RelayState persistant et horodatage.

### 3.2 Parcours Logout (SLO)
- **TC-E2E-LOGOUT-01** : SP initie logout Redirect signé, IdP renvoie `LogoutResponse` → validation, session applicative détruite.
- **TC-E2E-LOGOUT-02** : SP reçoit `LogoutRequest` IdP, vérifie signature et SessionIndex, renvoie `LogoutResponse`.

### 3.3 Résilience et sécurité
- **TC-E2E-SEC-01** : message avec signature corrompue → rejet + audit d’erreur.
- **TC-E2E-SEC-02** : horodatage expiré ou à plus de `clockSkew` → rejet.
- **TC-E2E-SEC-03** : URL ACS non HTTPS quand `forceHttpsRedirect=true` → blocage.
- **TC-E2E-SEC-04** : relayState inconnu/expiré → rejet et log.

### 3.4 Configuration et métadonnées
- **TC-E2E-CONFIG-01** : démarrage avec configuration composite (env + YAML + metadata URL) → `SamlServiceProvider` opérationnel.
- **TC-E2E-CONFIG-02** : renouvellement de métadonnées (ETag modifié) → cache rafraîchi, nouveaux certificats pris en compte.

### 3.5 Intégration web
- **TC-E2E-WEB-01** : `SamlFilter` sur requête protégée renvoie 302 vers IdP avec RelayState calculé, puis réintègre la réponse pour établir la session utilisateur.
- **TC-E2E-WEB-02** : `JaspiServerAuthModule` dans un conteneur simulé établit un `Subject` contenant `SamlPrincipal` et rôles dérivés des attributs.

## 4. Critères d’acceptation

- **Couverture** : 100 % des exigences listées dans `SPECIFICATION.md` et `umlConception.md` mappées à un cas de test.
- **Qualité** : taux de réussite ≥ 95 % sur tests automatiques, logs d’audit pour chaque échec de sécurité.
- **Sécurité** : aucun test ne persiste de secrets en clair (keystore/truststore), mocks utilisés pour certificats de démo.

## 5. Traçabilité exigences → tests

| Domaine | Exigence clé | Cas de test associé |
| --- | --- | --- |
| Config | Multi-sources + validation stricte | `TC-CONFIG-01` à `TC-CONFIG-05`, `TC-E2E-CONFIG-01` |
| SAML métier | Authn/Response/Logout + NameID/SessionIndex | `TC-SAML-01` à `TC-SAML-04`, `TC-E2E-AUTHN-01/02`, `TC-E2E-LOGOUT-01/02` |
| Binding | Redirect/POST + RelayState | `TC-BIND-01` à `TC-BIND-03`, `TC-E2E-AUTHN-01/02` |
| Sécurité | Signature, chiffrement, horodatage, HTTPS | `TC-SEC-01` à `TC-SEC-04`, `TC-E2E-SEC-01` à `TC-E2E-SEC-04` |
| Métadonnées | Parsing + cache | `TC-META-01`, `TC-META-02`, `TC-E2E-CONFIG-02` |
| Utilitaires | XML/temps/compression/logs | `TC-UTIL-01` à `TC-UTIL-04` |
| Intégration | Servlet/JASPIC + hooks | `TC-INT-01` à `TC-INT-03`, `TC-E2E-WEB-01/02` |
| API publique | Contrats stables | `TC-API-01`, `TC-API-02` |

## 6. Données et environnements

- **Environnement CI** : JVM 17+, OpenSAML 4.3+, dépendances mock servlet API.
- **Clés/certificats** : générés pour les tests (2048 bits) ; stockés en ressources de test, non valides en production.
- **Serveur IdP simulé** : endpoints HTTP locaux qui renvoient des réponses SAML signées via fixtures.

## 7. Maintenance du plan

- Le tableau de traçabilité sert de checklist lors de l’ajout d’exigences futures.
- Les identifiants `TC-*` devront être repris dans les noms de méthodes de test pour faciliter le suivi Jacoco/CI.
