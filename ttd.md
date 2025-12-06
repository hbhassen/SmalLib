# 📄 TDD – Exigences, jeux de tests et prompts guidés (SmalLib)

Ce document liste les exigences issues de `SPECIFICATION.md`, `plantest.md` et `umlConception.md`, et propose pour chacune :
- des **tests attendus** (unitaires ou intégration) assurant la couverture complète ;
- un **prompt cible** explicite pour générer automatiquement les tests et la fonctionnalité correspondante.

> Objectif : faciliter une approche TDD où chaque exigence est traduite en cas de test avant l'implémentation.

## 1. Exigences transverses
- **E1 – API publique stable** : les interfaces `SamlServiceProvider`, `SamlServiceProviderFactory`, `SamlPrincipal`, `SamlConfiguration` restent contractuelles.
  - Tests : `TC-API-01`, `TC-API-02` (création thread-safe, validation des signatures de méthodes, gestion d'erreurs de binding ou paramètres nuls).
  - Prompt : « Génère des tests de contrat pour l'API publique SamlServiceProvider/Fabric, vérifiant thread-safety et validation des entrées (bindings supportés, paramètres requis). Implémente ensuite les méthodes pour faire passer ces tests sans casser les signatures. »

- **E2 – Modules internes isolés** (`config`, `saml`, `binding`, `security`, `metadata`, `util`, `integration`) : peuvent évoluer sans briser l'API publique mais doivent respecter leurs responsabilités.
  - Tests : référentiels par module ci-dessous + scénarios E2E (`TC-E2E-*`).
  - Prompt : « Pour chaque module interne, écris d'abord des tests vérifiant la responsabilité décrite (chargement config, encodage Redirect/POST, validation SAML, etc.), puis code l'implémentation minimale pour réussir les tests. »

- **E3 – Conformité SAML et sécurité** : checklist de validation (signature, audience, Recipient, InResponseTo, horodatage ±clockSkew, relaystate opaque, HTTPS forcé si configuré).
  - Tests : `TC-SAML-02`, `TC-SEC-*`, `TC-E2E-SEC-*`.
  - Prompt : « Crée des tests couvrant la checklist sécurité SAML : signature valide/invalidée, audience/Recipient incorrects, horodatage hors fenêtre clockSkew, InResponseTo manquant, HTTPS obligatoire. Implémente les validateurs pour que seuls les messages conformes passent. »

## 2. Module `config`
- **E4 – Chargement multi-sources** : support YAML/JSON/Properties, substitution d'environnement, profil dev/prod, priorisation env → classpath YAML → FS JSON → metadata URL.
  - Tests : `TC-CONFIG-01`, `TC-CONFIG-02`, `TC-CONFIG-04`, `TC-E2E-CONFIG-01`.
  - Prompt : « Écris des tests pour chaque loader (YAML/JSON/Properties/env/metadata) vérifiant schéma complet `SamlConfiguration`, priorité des sources et sélection de profil. Développe les loaders jusqu'à succès des tests. »

- **E5 – Validation stricte** : URI mal formée, certificat absent, binding non supporté ou keystore invalide déclenchent une erreur explicite.
  - Tests : `TC-CONFIG-03`, `TC-SEC-04`.
  - Prompt : « Conçois des tests qui rejettent configuration invalide (URI, certificats, bindings, keystore). Implémente validateurs qui lèvent des exceptions précises. »

- **E6 – Hot-reload optionnel** : rechargement sans fuite de ressources.
  - Tests : `TC-CONFIG-05`.
  - Prompt : « Ajoute des tests simulant un rechargement de configuration (fichiers/metadata) et vérifie que la nouvelle instance remplace l'ancienne sans fuite. Implémente le mécanisme minimal pour les faire passer. »

## 3. Module `saml`
- **E7 – AuthnRequest builder** : requête signée avec NameID/binding configurés, horodatage respectant clockSkew.
  - Tests : `TC-SAML-01`, `TC-E2E-AUTHN-01/02`.
  - Prompt : « Rédige des tests construisant AuthnRequest avec NameID, Binding et signature attendus, horodatage dans la fenêtre clockSkew. Implémente le builder jusqu'à succès. »

- **E8 – Validation SAMLResponse** : rejets pour audience/Recipient invalides, InResponseTo manquant, signature cassée, horodatage expiré, SubjectConfirmation bearer seulement.
  - Tests : `TC-SAML-02`, `TC-E2E-SEC-01/02`, `TC-E2E-AUTHN-*`.
  - Prompt : « Crée des tests couvrant chaque contrôle (signature, audience, Recipient, InResponseTo, horodatage, subject bearer). Implémente `SamlResponseValidator` pour qu'ils passent. »

- **E9 – Extraction d'attributs** : conversion assertion → `SamlPrincipal` avec attributs standards et personnalisés.
  - Tests : `TC-SAML-03`, `TC-E2E-AUTHN-*`.
  - Prompt : « Écris des tests transformant des assertions de test en `SamlPrincipal` complet (NameID, email, attributs custom). Implémente `AssertionExtractor` en conséquence. »

- **E10 – Logout** : construction `LogoutRequest`, validation `LogoutResponse`, gestion SessionIndex et signature.
  - Tests : `TC-SAML-04`, `TC-E2E-LOGOUT-*`.
  - Prompt : « Ajoute des tests pour le cycle SLO (build request, valider response avec InResponseTo/horodatage/signature, invalidation session). Implémente builders/validateurs pour passer. »

## 4. Module `binding`
- **E11 – Encodage Redirect** : deflate + Base64 + signature query param, `RelayState` signé.
  - Tests : `TC-BIND-01`, `TC-E2E-AUTHN-01`, `TC-E2E-LOGOUT-01`.
  - Prompt : « Crée des tests vérifiant l'encodage Redirect (compression, Base64, signature, relaystate signé). Implémente `RedirectBindingEncoder` pour réussir. »

- **E12 – Encodage POST** : formulaire auto-posté, signature enveloppée.
  - Tests : `TC-BIND-02`, `TC-E2E-AUTHN-02`, `TC-E2E-LOGOUT-02`.
  - Prompt : « Rédige des tests validant le formulaire POST auto-soumis avec signature enveloppée. Implémente `PostBindingEncoder` pour conformité. »

- **E13 – RelayState opaque** : génération, persistance (`RelayStateStore`), expiration et anti-rejeu.
  - Tests : `TC-BIND-03`, `TC-E2E-SEC-04`.
  - Prompt : « Ajoute des tests garantissant un RelayState opaque persisté côté serveur avec TTL et protection anti-rejeu. Implémente `RelayStateManager` et `RelayStateStore` fake pour passer. »

## 5. Module `security`
- **E14 – Initialisation OpenSAML** : idempotente et thread-safe.
  - Tests : `TC-SEC-01`.
  - Prompt : « Écris des tests appelant plusieurs fois l'initialisation OpenSAML en parallèle et vérifie l'idempotence. Implémente `OpenSamlInitializer` en conséquence. »

- **E15 – Signature et validation** : RSA-SHA256 par défaut, algorithme configurable, validation avec truststore.
  - Tests : `TC-SEC-02`, `TC-E2E-SEC-01`.
  - Prompt : « Crée des tests de signature/validation avec algorithmes configurables et truststore. Implémente `SignatureService` jusqu'à validation. »

- **E16 – Chiffrement optionnel** : déchiffrement si certificat IdP/algorithme fourni.
  - Tests : `TC-SEC-03` (et future extension EncryptedAssertion).
  - Prompt : « Ajoute des tests conditionnels pour chiffrement/déchiffrement quand l'IdP fournit un certificat. Implémente `EncryptionService` minimal pour les faire passer. »

- **E17 – Résolution credentials** : chargement keystore/truststore PKCS12/JKS/PEM, erreurs claires sur alias/mot de passe invalides.
  - Tests : `TC-SEC-04`.
  - Prompt : « Rédige des tests de résolution de credentials couvrant formats PKCS12/JKS/PEM et erreurs d'alias/mdp. Implémente `CredentialResolver` pour réussir. »

## 6. Module `metadata`
- **E18 – Parsing metadata** : extraction endpoints/bindings/certificats depuis `EntityDescriptor` (Keycloak), conformité aux bindings supportés.
  - Tests : `TC-META-01`.
  - Prompt : « Crée des tests de parsing de metadata Keycloak simulées, vérifiant extraction des endpoints/bindings/certificats. Implémente `MetadataParser` jusqu'à succès. »

- **E19 – Cache TTL et rafraîchissement** : invalidation sur expiration ou changement ETag/Last-Modified.
  - Tests : `TC-META-02`, `TC-E2E-CONFIG-02`.
  - Prompt : « Ajoute des tests pour le cache de metadata avec TTL et détection d'ETag modifié. Implémente `EntityDescriptorCache` pour passer. »

## 7. Module `util`
- **E20 – XML sécurisé** : canonicalisation, désactivation XXE, marshalling/unmarshalling sécurisé.
  - Tests : `TC-UTIL-01`.
  - Prompt : « Rédige des tests garantissant canonicalisation et protection XXE dans `XmlUtils`. Implémente la sécurité XML demandée. »

- **E21 – Gestion du temps** : comparaison horodatages avec dérive ±2 min par défaut, configurable.
  - Tests : `TC-UTIL-02`.
  - Prompt : « Crée des tests pour `TimeUtils` comparant horodatages avec marge clockSkew configurable. Implémente pour succès. »

- **E22 – Compression SAML Redirect** : deflate/inflate compatibles.
  - Tests : `TC-UTIL-03` (utilisé par `TC-BIND-01`).
  - Prompt : « Ajoute des tests deflate/inflate compatibles SAML Redirect. Implémente `CompressionUtils`. »

- **E23 – Logging** : niveaux DEBUG/TRACE activables, masquage des secrets.
  - Tests : `TC-UTIL-04`.
  - Prompt : « Écris des tests vérifiant que `LoggingUtils` active DEBUG/TRACE et masque secrets. Implémente en conséquence. »

## 8. Module `integration`
- **E24 – Servlet filter** : redirection vers IdP si session absente, prise en compte `forceAuthn`/`isPassive`, reconstitution URL via RelayState.
  - Tests : `TC-INT-01`, `TC-E2E-WEB-01`.
  - Prompt : « Crée des tests d'intégration pour `SamlFilter` simulant requête protégée → redirection IdP → retour SAMLResponse, avec RelayState et flags forceAuthn/isPassive. Implémente le filtre pour faire passer. »

- **E25 – JASPIC** : négociation et propagation du `SamlPrincipal` dans le `Subject`.
  - Tests : `TC-INT-02`, `TC-E2E-WEB-02`.
  - Prompt : « Ajoute des tests pour `JaspiServerAuthModule` plaçant `SamlPrincipal` et rôles dans le `Subject`. Implémente jusqu'au succès. »

- **E26 – Hooks d'audit et d'erreur** : logs structurés des succès/erreurs, mapping vers codes HTTP.
  - Tests : `TC-INT-03`, `TC-E2E-SEC-*`.
  - Prompt : « Rédige des tests vérifiant que `SamlAuditLogger` et `SamlErrorHandler` sont appelés sur succès/erreur avec codes HTTP appropriés. Implémente les hooks. »

## 9. Scénarios bout-en-bout
- **E27 – Authn complet Redirect/POST** : génération AuthnRequest → encodage → IdP simulé → validation → principal en session/contexte JASPIC.
  - Tests : `TC-E2E-AUTHN-01`, `TC-E2E-AUTHN-02`.
  - Prompt : « Construis des tests d'orchestration Authn (Redirect puis POST) utilisant des fixtures SAML signées ; valide Recipient, horodatage, relaystate persistant et extraction du principal. Implémente le flux pour les faire passer. »

- **E28 – Logout complet** : SP initie ou répond à un logout, validation InResponseTo, signature, invalidation de session.
  - Tests : `TC-E2E-LOGOUT-01`, `TC-E2E-LOGOUT-02`.
  - Prompt : « Ajoute des tests couvrant les deux sens du SLO (initiation SP, réponse IdP). Implémente les chemins logout pour succès. »

- **E29 – Résilience et sécurité** : signature corrompue, horodatage expiré, HTTPS requis, RelayState inconnu/expiré → rejet + audit.
  - Tests : `TC-E2E-SEC-01` à `TC-E2E-SEC-04`.
  - Prompt : « Crée des tests injectant erreurs (signature, horodatage, HTTPS, relaystate) et vérifie rejet + audit. Implémente validations/redirects pour succès. »

- **E30 – Configuration & métadonnées** : démarrage avec configuration composite, rafraîchissement metadata sur ETag modifié.
  - Tests : `TC-E2E-CONFIG-01`, `TC-E2E-CONFIG-02`.
  - Prompt : « Ajoute des tests d'amorçage configuration multi-sources et de rafraîchissement metadata (ETag). Implémente orchestration des loaders/caches pour réussir. »

- **E31 – Intégration web** : `SamlFilter` et `JaspiServerAuthModule` intègrent principal/rôles ; redirections correctes.
  - Tests : `TC-E2E-WEB-01`, `TC-E2E-WEB-02`.
  - Prompt : « Écris des tests web simulés vérifiant redirections 302 vers IdP, restitution de l'URL d'origine via RelayState et population du `Subject` JASPIC. Implémente intégration. »

## 10. Backlog v2+ (préparer tests futurs)
- **E32 – EncryptedAssertion** : support du déchiffrement assertion.
  - Tests futurs : extension `TC-SEC-03` + nouveaux cas.
  - Prompt : « Prépare des tests pour decrypt assertions SAML via clé SP, puis implémente `DecryptionService`. »

- **E33 – Multi-IdP** : sélection d'IdP par domaine/paramètre.
  - Tests futurs : nouveaux loaders/router.
  - Prompt : « Conçois des tests couvrant la sélection dynamique d'IdP et leur configuration, puis implémente le routeur. »

- **E34 – Assertion cache** : TTL pour éviter revalidations.
  - Tests futurs : cache hit/miss.
  - Prompt : « Ajoute des tests pour un cache d'assertion avec TTL et invalidation, puis implémente `AssertionCache`. »

- **E35 – Audit trail BD** : logs structurés vers JDBC/NoSQL.
  - Tests futurs : persistance/audit.
  - Prompt : « Écris des tests d'audit persistant (JDBC/NoSQL) pour `SamlAuditLogger`, puis implémente l'adapteur. »

- **E36 – Hot-reload avancé / SDK frameworks** : watcher fichiers/JMX, modules Quarkus/Micronaut, monitoring/metrics.
  - Tests futurs : watchers et modules d'intégration.
  - Prompt : « Crée des tests pour hot-reload via watcher et exposition de métriques/SDK frameworks, puis implémente les modules. »

