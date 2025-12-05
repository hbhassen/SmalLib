
📐 SPÉCIFICATION TECHNIQUE COMPLÈTE – Architecture SmalLib
I. Architecture générale (Vue d'ensemble)
Code
┌─────────────────────────────────────────────────────────────────┐
│                     Application WAR/EAR                          │
│                    (Servlet Filter/JASPIC)                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      SmalLib JAR                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ PUBLIC API (Interfaces stables)                          │  │
│  │ - SamlServiceProvider, SamlServiceProviderFactory        │  │
│  │ - SamlPrincipal, SamlConfiguration                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ CORE Modules (Internes/Privés)                           │  │
│  ├─ config     → Charger/Manager configuration              │  │
│  ├─ saml       → AuthnRequest, SAMLResponse, SLO            │  │
│  ├─ binding    → HTTP-Redirect, HTTP-POST, RelayState       │  │
│  ├─ security   → Init OpenSAML, signatures, keystores       │  │
│  ├─ metadata   → Parser EntityDescriptor, certs, endpoints  │  │
│  ├─ util       → XML, logs, horodatages, compression        │  │
│  └─ integration→ Servlet Filter, JASPIC helpers             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ DEPENDENCIES (External)                                  │  │
│  │ - OpenSAML 4.3+, Shibboleth java-support                │  │
│  │ - SLF4J, Jakarta Servlet API (provided)                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────────────────────────┐
        │    Keycloak (IdP SAML) / Autre IdP     │
        │ /auth/realms/{realm}/protocol/saml/...│
        └─────────────────────────────────────────┘
II. Module config – Gestion de la configuration
Objectif
Abstraire la configuration SAML en modèles Java indépendants du format ou de l'IdP, permettant le chargement depuis multiples sources.

Structure logique
Code
com.yourcompany.saml. config
├── SamlConfiguration
│   ├── ServiceProviderConfig (SP)
│   │   ├─ entityId: String
│   │   ├─ assertionConsumerServiceUrl: URI
│   │   ├─ singleLogoutServiceUrl: URI (optionnel)
│   │   ├─ nameIdFormat: String (emailAddress, persistent, transient…)
│   │   ├─ authnRequestBinding: BindingType (REDIRECT, POST)
│   │   ├─ wantAssertionsSigned: Boolean
│   │   └─ supportedNameIdFormats: List<String>
│   │
│   ├── IdentityProviderConfig (IdP)
│   │   ├─ entityId: String
│   │   ├─ singleSignOnServiceUrl: URI
│   │   ├─ singleLogoutServiceUrl: URI (optionnel)
│   │   ├─ signingCertificate: X509Certificate (ou provider)
│   │   ├─ encryptionCertificate: X509Certificate (optionnel)
│   │   ├─ metadataUrl: URI (optionnel, pour chargement auto)
│   │   ├─ wantAssertionsSigned: Boolean
│   │   ├─ wantMessagesSigned: Boolean
│   │   └─ supportedBindings: List<BindingType>
│   │
│   └── SecurityConfig
│       ├─ clockSkewDuration: Duration (±2 min par défaut)
│       ├─ signatureAlgorithm: String (RSA-SHA256 par défaut)
│       ├─ digestAlgorithm: String (SHA-256 par défaut)
│       ├─ keystore: KeystoreConfig
│       ├─ truststore: KeystoreConfig
│       ├─ encryptionAlgorithm: String (optionnel)
│       ├─ forceHttpsRedirect: Boolean
│       └─ enableDetailedLogging: Boolean (DEBUG/TRACE activé)
│
├── KeystoreConfig
│   ├─ path: Path (ou URI)
│   ├─ password: String (SecureString recommend.)
│   ├─ keyAlias: String
│   ├─ keyPassword: String
│   └─ type: KeystoreType (PKCS12, JKS, PEM…)
│
├── BindingType (Enum)
│   ├─ HTTP_REDIRECT
│   └─ HTTP_POST
│
├── ConfigLoader (Interface)
│   ├─ load(): SamlConfiguration
│   └─ validate(): Boolean
│
└── Implémentations de ConfigLoader
    ├─ JsonConfigLoader (Jackson)
    ├─ YamlConfigLoader (SnakeYAML)
    ├─ PropertiesConfigLoader (java.util.Properties)
    ├─ KeycloakMetadataConfigLoader (URL + parsing XML)
    ├─ EnvironmentVariableConfigLoader
    └─ ClasspathResourceConfigLoader
Fonctionnalités clés
Chargement multi-source : priorité configurable (env vars → classpath YAML → file system JSON → metadata URL)
Validation stricte : vérifier présence champs requis, format URIs, certificats valides, etc.
Substitution d'env : support ${VAR_NAME} dans les configs YAML/JSON (sécurité : password ne jamais en clair).
Profils de configuration : support multi-IdP (prod, dev, test), multi-realm Keycloak.
Hot-reload optionnel : recharger config sans redémarrage app (non prioritaire v1).
Exemple de structure config YAML (pour référence)
YAML
service-provider:
  entity-id: "https://myapp.example.com/saml"
  assertion-consumer-service-url: "https://myapp.example.com/saml/acs"
  single-logout-service-url: "https://myapp.example.com/saml/slo"
  name-id-format: "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
  authn-request-binding: "HTTP_REDIRECT"
  want-assertions-signed: true

identity-provider:
  entity-id: "https://keycloak.example.com/auth/realms/myrealm"
  single-sign-on-service-url: "https://keycloak.example.com/auth/realms/myrealm/protocol/saml"
  single-logout-service-url: "https://keycloak.example.com/auth/realms/myrealm/protocol/saml/logout"
  metadata-url: "https://keycloak.example.com/auth/realms/myrealm/protocol/saml/descriptor"
  want-assertions-signed: true
  want-messages-signed: false

security:
  clock-skew: "PT2M"
  signature-algorithm: "RSA_SHA256"
  digest-algorithm: "SHA_256"
  keystore:
    path: "${KEYSTORE_PATH:/app/config/sp.pkcs12}"
    password: "${KEYSTORE_PASSWORD}"
    key-alias: "sp-key"
    type: "PKCS12"
  truststore:
    path: "${TRUSTSTORE_PATH:/app/config/truststore.jks}"
    password: "${TRUSTSTORE_PASSWORD}"
    type: "JKS"
  enable-detailed-logging: false
III. Module saml – Métier SAML
Objectif
Gérer la logique SAML métier : création AuthnRequest, parsing/validation SAMLResponse, gestion SLO.

Structure logique
Code
com.yourcompany. saml.saml
├── SamlServiceProvider (Interface)
│   ├─ buildAuthnRequest(SamlAuthnRequestParameters)
│   │   ↓ returns SamlAuthnRequest
│   ├─ processResponse(SamlResponseContext)
│   │   ↓ returns SamlAuthenticationResult
│   ├─ buildLogoutRequest(SamlLogoutRequestParameters)
│   │   ↓ returns SamlLogoutRequest
│   └─ processLogoutResponse(SamlLogoutResponseContext)
│       ↓ returns SamlLogoutResult
│
├── DTOs & Models
│   ├─ SamlAuthnRequest
│   │   ├─ samlRequest: String (Base64-encoded)
│   │   ├─ relayState: String
│   │   ├─ binding: BindingType
│   │   ├─ destination: URI (IdP SSO URL)
│   │   └─ postActionForm: String (optionnel pour POST binding)
│   │
│   ├─ SamlAuthnRequestParameters (Builder)
│   │   ├─ requestedRelayState: Optional<String>
│   │   ├─ requestedNameIdFormat: Optional<String>
│   │   ├─ forceAuthn: Optional<Boolean>
│   │   ├─ isPassive: Optional<Boolean>
│   │   └─ allowedClockSkew: Optional<Duration>
│   │
│   ├─ SamlResponseContext (Builder)
│   │   ├─ samlResponse: String (paramètre HTTP POST)
│   │   ├─ relayState: String (optionnel)
│   │   └─ fromHttpServletRequest(HttpServletRequest): static method
│   │
│   ├─ SamlAuthenticationResult
│   │   ├─ principal: SamlPrincipal
│   │   ├─ sessionNotOnOrAfter: Instant
│   │   ├─ inResponseTo: String (pour traçabilité)
│   │   └─ rawAssertion: Optional<Assertion> (pour introspection)
│   │
│   ├─ SamlPrincipal
│   │   ├─ subject: String (NameID)
│   │   ├─ email: String (optionnel)
│   │   ├─ username: String
│   │   ├─ roles: Set<String> (multi-valués)
│   │   ├─ attributes: Map<String, List<String>>
│   │   ├─ sessionIndex: String
│   │   ├─ authenticatedAt: Instant
│   │   └─ getTtl(): Duration (jusqu'à sessionNotOnOrAfter)
│   │
│   ├─ SamlLogoutRequest
│   │   ├─ samlRequest: String (Base64)
│   │   ├─ relayState: String
│   │   ├─ destination: URI (IdP SLO URL)
│   │   └─ binding: BindingType
│   │
│   ├─ SamlLogoutResponseContext (Builder)
│   │   ├─ samlResponse: String
│   │   └─ relayState: String
│   │
│   └─ SamlLogoutResult
│       ├─ success: Boolean
│       └─ statusCode: SamlStatusCode
│
├── Exceptions
│   ├─ SamlException (parent)
│   ├─ SamlConfigurationException
│   ├─ SamlValidationException
│   │   └─ statusCode: SamlStatusCode
│   ├─ SamlSecurityException
│   └─ SamlBindingException
│
├── SamlStatusCode (Enum)
│   ├─ SUCCESS
│   ├─ AUTHN_FAILED
│   ├─ INVALID_NAMEID_POLICY
│   ├─ NO_AUTHN_CONTEXT
│   ├─ REQUEST_DENIED
│   └─ ...  (tous les codes SAML 2.0 std)
│
└── SamlServiceProviderImpl (Implémentation)
    ├─ Constructor: SamlConfiguration, SamlSecurityValidator
    ├─ Délègue validation à SamlSecurityValidator
    └─ Délègue bindings à SamlBindingEncoder/Decoder
Validations dans processResponse
Status de la Response : doit être urn:oasis:names:tc:SAML:2.0:status:Success
Signature XML :
Valider signature Response (si wantMessagesSigned)
Valider signature Assertion (si wantAssertionsSigned)
Vérifier certificats en truststore
Conditions :
NotBefore : pas avant (+ clock skew)
NotOnOrAfter : pas après (+ clock skew)
AudienceRestriction : contient SP entityId
Recipient : égale ACS URL
InResponseTo : corresponds au requestID AuthnRequest
Subject Confirmation :
Type = "urn:oasis:names:tc:SAML:2. 0:cm:bearer"
Recipient & NotOnOrAfter en SubjectConfirmationData
Assertion :
Contient au moins une AttributeStatement (ou au minimum le NameID)
SessionIndex présent pour SLO
Extraction d'attributs
Code
SAMLResponse XML
└─ Response
   └─ Assertion
      ├─ Subject
      │  └─ NameID → principal.subject (email format pour Keycloak)
      │
      └─ AttributeStatement
         ├─ Attribute[@Name="email"]
         │  └─ AttributeValue → principal.email
         ├─ Attribute[@Name="username"]
         │  └─ AttributeValue → principal.username
         ├─ Attribute[@Name="role"] (multi-valué)
         │  ├─ AttributeValue → rôle 1
         │  ├─ AttributeValue → rôle 2
         │  └─ ...  → principal.roles (Set)
         └─ Attribute[@Name="... "] (custom)
            └─ → principal.attributes (Map)
Support multi-valued : Si un attribut SAML contient plusieurs AttributeValue, tous collectés dans une List<String>.

IV. Module binding – Gestion des bindings SAML
Objectif
Encoder/décoder les messages SAML selon les bindings HTTP-Redirect et HTTP-POST.

Structure logique
Code
com. yourcompany.saml.binding
├── BindingType (Enum)
│   ├─ HTTP_REDIRECT
│   └─ HTTP_POST
│
├── SamlBindingEncoder (Interface)
│   ├─ encodeAuthnRequest(AuthnRequest, RelayState, BindingType)
│   │   ↓ returns SamlAuthnRequest (avec URL/form)
│   └─ encodeLogoutRequest(LogoutRequest, RelayState, BindingType)
│       ↓ returns SamlLogoutRequest
│
├── SamlBindingDecoder (Interface)
│   ├─ decode(HttpParameters)
│   │   ↓ returns DecodedSamlMessage
│   └─ isValidBinding(DecodedSamlMessage, ExpectedBindingType)
│
├── RelayStateManager (Interface)
│   ├─ create(originalUrl, ttl)
│   │   ↓ returns opaque relayStateId + stores mapping server-side
│   ├─ retrieve(relayStateId)
│   │   ↓ returns original URL or fails
│   ├─ validate(relayStateId)
│   │   ↓ returns Boolean
│   └─ invalidate(relayStateId)
│
├── RelayStateStore (Interface) – Abstraction du stockage
│   ├─ store(relayStateId, originalUrl, expiresAt): Void
│   ├─ get(relayStateId): Optional<String>
│   ├─ remove(relayStateId): Boolean
│   └─ isExpired(relayStateId): Boolean
│
└── Implémentations
    ├─ HttpRedirectBindingEncoder
    │   └─ Compression DEFLATE + Base64 + URL-encoding
    ├─ HttpPostBindingEncoder
    │   └─ Base64 + generation HTML form auto-post
    ├─ InMemoryRelayStateStore (dev/test)
    ├─ HttpSessionRelayStateStore (prod WAR single-server)
    └─ CustomRelayStateStore (extensibility: Redis, BD…)
HTTP-Redirect Binding
Conversion AuthnRequest XML → URL :

Code
1. XML sérialisé (compact)
2.  DEFLATE compression
3. Base64 encoding
4. URL-encode
5. Construire: https://idp.com/saml/sso? SAMLRequest=<encoded>&RelayState=<relay>
HTTP-POST Binding
Conversion AuthnRequest XML → HTML form :

HTML
<form method="POST" action="https://idp.com/saml/sso">
  <input type="hidden" name="SAMLRequest" value="<base64-xml>"/>
  <input type="hidden" name="RelayState" value="<relayStateId>"/>
  <input type="submit" value="Click here to continue... "/>
</form>
<script>document.forms[0].submit();</script>
RelayState
Stratégie : Générer un UUID opaque, le stocker côté serveur avec mapping → URL d'origine.

Options de stockage :

InMemoryRelayStateStore : Cache ConcurrentHashMap (dev/test)
HttpSessionRelayStateStore : session.setAttribute("relayState_" + id, originalUrl) (prod simple)
RedisRelayStateStore : Clé Redis avec TTL (prod scalé)
DatabaseRelayStateStore : Table en BD (flexibilité haute)
TTL : 15-30 min par défaut (configurable).

V. Module security – Gestion de la sécurité & OpenSAML
Objectif
Initialiser OpenSAML, valider signatures XML, gérer keystores/truststores, clock skew.

Structure logique
Code
com.yourcompany.saml.security
├── SamlSecurityInitializer (Singleton)
│   ├─ getInstance(): SamlSecurityInitializer
│   ├─ initialize(): void
│   │   └─ InitializationService.initialize() (OpenSAML once-only)
│   ├─ isInitialized(): Boolean
│   └─ shutdown(): void (optionnel, pour test)
│
├── SamlSecurityValidator
│   ├─ validateResponse(Response, SamlConfiguration, expectedInResponseTo)
│   │   └─ Lance SamlValidationException si fail
│   ├─ validateAssertion(Assertion, SamlConfiguration)
│   │   └─ Lance SamlValidationException si fail
│   ├─ verifySignature(SignableXMLObject, Credential)
│   │   └─ Lance SamlSecurityException si invalid
│   └─ validateConditions(Conditions, SamlConfiguration)
│       └─ Lance SamlValidationException si fail (NotBefore, NotOnOrAfter, AudienceRestriction…)
│
├── KeystoreManager (Interface)
│   ├─ loadKeystore(KeystoreConfig): KeyStore
│   ├─ loadTruststore(KeystoreConfig): KeyStore
│   ├─ getSigningCredential(KeystoreConfig): Credential (OpenSAML)
│   ├─ getIdPCertificate(IdentityProviderConfig): X509Certificate
│   └─ validateCertificateChain(X509Certificate, KeyStore): Boolean
│
├── ClockSkewValidator
│   ├─ validateNotBefore(Instant, Duration): Boolean
│   ├─ validateNotOnOrAfter(Instant, Duration): Boolean
│   └─ getCurrentTimeWithSkew(Duration): Instant
│
├── SignatureValidator (Wrapper OpenSAML)
│   ├─ validateXmlSignature(SignableXMLObject, Credential)
│   ├─ validateResponseSignature(Response, Credential)
│   ├─ validateAssertionSignature(Assertion, Credential)
│   └─ handleSignatureFailure(Exception): SamlSecurityException
│
└── CertificateValidator
    ├─ loadFromPEM(String): X509Certificate
    ├─ loadFromBase64(String): X509Certificate
    ├─ loadFromKeystore(KeyStore, String): X509Certificate
    ├─ validateExpiration(X509Certificate): Boolean
    └─ validateSubjectDN(X509Certificate): Boolean
Initialisation OpenSAML
Important : Utiliser InitializationService.initialize() une seule fois au démarrage JVM.

Pattern Singleton :

Code
SamlSecurityInitializer. getInstance(). initialize();
// idempotent si déjà initialisé
À placer :

Dans un ServletContextListener (WAR)
Dans un EJB @Startup (EAR)
Dans le static initializer du SamlServiceProviderFactory
Validation des conditions temporelles
Code
Current time: T
Clock Skew: ±2 min (configurable)

NotBefore condition:
  T >= (assertion.notBefore - skew) ✓

NotOnOrAfter condition:
  T < (assertion.notOnOrAfter + skew) ✓

SessionNotOnOrAfter (pour SLO):
  Session valide jusqu'à: assertion.sessionNotOnOrAfter
VI. Module metadata – Parsing/chargement metadata IdP
Objectif
Récupérer automatiquement la configuration IdP depuis le metadata XML SAML 2.0, notamment le certificat de signature et les URLs.

Structure logique
Code
com.yourcompany. saml.metadata
├── SamlMetadataLoader (Interface)
│   ├─ load(metadataUrl): EntityDescriptor
│   └─ load(metadataXmlString): EntityDescriptor
│
├── SamlMetadataParser
│   ├─ parseEntityDescriptor(Element): EntityDescriptor
│   ├─ extractIdPSSODescriptor(EntityDescriptor): IDPSSODescriptor
│   ├─ extractSigningCertificates(IDPSSODescriptor): List<X509Certificate>
│   ├─ extractEncryptionCertificates(IDPSSODescriptor): List<X509Certificate>
│   ├─ extractSingleSignOnServiceUrl(IDPSSODescriptor, BindingType): URI
│   ├─ extractSingleLogoutServiceUrl(IDPSSODescriptor, BindingType): Optional<URI>
│   ├─ extractNameIDFormats(IDPSSODescriptor): List<String>
│   └─ extractEntityId(EntityDescriptor): String
│
├── KeycloakMetadataConfigLoader (extends ConfigLoader)
│   ├─ Constructor: metadataUrl
│   ├─ load(): SamlConfiguration
│   │   ├─ Fetch metadata XML depuis URL
│   │   ├─ Parse certificats + URLs
│   │   ├─ Filler IdentityProviderConfig
│   │   └─ Retourner config (SP config pré-existante requise)
│   └─ validate(): Boolean
│       └─ Vérifier signature metadata (optionnel mais recommandé)
│
├── MetadataValidation
│   ├─ validateSignature(EntityDescriptor, X509Certificate): Boolean
│   ├─ validateNotBefore(EntityDescriptor): Boolean
│   └─ validateNotOnOrAfter(EntityDescriptor): Boolean
│
└── CertificateExtractor
    ├─ extractFromKeyDescriptor(KeyDescriptor): X509Certificate
    └─ loadFromPEM(Element): X509Certificate
Cas d'utilisation
Auto-configuration depuis Keycloak :

Code
metadataUrl = "https://keycloak.example.com/auth/realms/myrealm/protocol/saml/descriptor"
loader = new KeycloakMetadataConfigLoader(metadataUrl);
config. setIdPConfig(loader.loadIdPConfig());
Fallback : Si metadata non accessible, utiliser config statique (JSON, YAML…).

VII. Module integration – Helpers pour WildFly/Servlet/JASPIC
Objectif
Faciliter l'intégration avec une application WAR/EAR sur WildFly, notamment pour Servlet Filters et JASPIC.

Structure logique
Code
com.yourcompany.saml. integration
├── Servlet Filter Utilities
│   ├─ SamlAuthenticationFilterConfig (Builder)
│   │   ├─ protectedPaths: List<String> (ex: /secure/*)
│   │   ├─ acsPath: String (ex: /saml/acs)
│   │   ├─ sloPath: String (ex: /saml/slo)
│   │   ├─ samlServiceProvider: SamlServiceProvider
│   │   ├─ sessionAttributeKey: String (default: "saml.principal")
│   │   └─ relayStateStore: RelayStateStore
│   │
│   ├─ SamlAuthenticationFilterHelper
│   │   ├─ shouldRedirectToIdP(HttpRequest, HttpResponse)
│   │   │   ↓ retourne Boolean + URL IdP si oui
│   │   ├─ handleAcsRequest(HttpRequest, HttpResponse)
│   │   │   ↓ valide Response, stocke Principal, redirect origin
│   │   ├─ handleSloRequest(HttpRequest, HttpResponse)
│   │   │   ↓ invalide session, log out
│   │   └─ extractPrincipalFromSession(HttpSession)
│   │       ↓ retourne Optional<SamlPrincipal>
│   │
│   └─ Example FilterRegistration (annotations)
│       └─ @WebFilter, @WebListener pour autoconfiguration
│
├── JASPIC / WildFly Security
│   ├─ SamlServerAuthModuleHelper
│   │   ├─ createSubjectFromPrincipal(SamlPrincipal): Subject
│   │   ├─ createCallerPrincipal(SamlPrincipal): Principal
│   │   ├─ createGroupsFromRoles(Set<String>): Set<Principal>
│   │   └─ populateSubjectPrincipals(Subject, SamlPrincipal): Void
│   │
│   ├─ WildFlySecurityMappingHelper
│   │   ├─ mapRolesToWildFlyRoles(Set<String>): Set<String>
│   │   │   └─ (support mapping SAML rôles → WildFly roles si config)
│   │   └─ getWildFlyPrincipal(SamlPrincipal): org.wildfly.security.auth.principal. NamePrincipal
│   │
│   └─ JaasHelper
│       ├─ createJaasSubject(SamlPrincipal): Subject
│       └─ createLoginContext(SamlPrincipal, String): LoginContext (si JaaS)
│
├── Session Management
│   ├─ SamlSessionHelper
│   │   ├─ storePrincipalInSession(HttpSession, SamlPrincipal): Void
│   │   ├─ retrievePrincipalFromSession(HttpSession): Optional<SamlPrincipal>
│   │   ├─ invalidateSession(HttpSession): Void
│   │   └─ getSessionRemainingTtl(HttpSession, SamlPrincipal): Duration
│   │
│   └─ SessionTimeoutPolicy
│       ├─ enforceSessionTimeout: Boolean (default: true)
│       ├─ sessionTimeoutAction: Enum (LOGOUT, WARN, REFRESH…)
│       └─ onSessionExpiry(HttpSession): void
│
├── Error Handling
│   ├─ SamlErrorHandler (Interface)
│   │   ├─ handleValidationError(SamlValidationException): String (error page URL)
│   │   ├─ handleSecurityError(SamlSecurityException): String
│   │   └─ handleBindingError(SamlBindingException): String
│   │
│   ├─ DefaultSamlErrorHandler
│   │   └─ Redirection vers /saml/error avec message/code
│   │
│   └─ CustomErrorHandlers (extension point)
│
├── Logging & Audit
│   ├─ SamlAuditLogger (Interface)
│   │   ├─ logAuthnRequestInitiated(SamlAuthnRequest, HttpRequest): Void
│   │   ├─ logAuthenticationSuccess(SamlAuthenticationResult): Void
│   │   ├─ logAuthenticationFailure(Exception, HttpRequest): Void
│   │   ├─ logLogoutInitiated(SamlPrincipal): Void
│   │   └─ logLogoutSuccess(SamlLogoutResult): Void
│   │
│   └─ DefaultSamlAuditLogger
│       └─ Utilise SLF4J pour logging structuré
│
└── Configuration Example (web.xml)
    ├─ Filter deployment descriptor
    ├─ Listener pour init OpenSAML
    └─ Resources protégées + mapping ACS/SLO
Pattern Servlet Filter – Approche recommandée
Code
Requête HTTP
    ↓
┌─── Est-ce une requête protégée ? (/secure/*)
│   └─ Non : passer au prochain filter
│   └─ Oui :
│       ├─ Principal existant en session ?  
│       │  └─ Oui : ajouter à request, passer
│       │  └─ Non : rediriger vers IdP (buildAuthnRequest)
│
├─── Est-ce l'endpoint ACS ? (/saml/acs)
│   └─ Oui :
│       ├─ Récupérer SAMLResponse
│       ├─ Valider (processResponse)
│       ├─ Stocker Principal en session
│       ├─ Rediriger vers URL d'origine (RelayState)
│
├─── Est-ce l'endpoint SLO ? (/saml/slo)
│   └─ Oui :
│       ├─ Invalider session
│       ├─ Construire LogoutRequest vers IdP (optionnel)
│       ├─ Rediriger vers page "logged out"
│
└─── Sinon : passer au prochain filter
Pattern JASPIC (ServerAuthModule)
Alternative plus robuste, intégration directe avec WildFly security realms :

Code
Requête HTTP
    ↓
JASPIC ServerAuthModule. validateRequest()
    ├─ Existe-t-il un cookie/header SAML ? 
    │  └─ Oui : extraire Principal, populer Subject
    │  └─ Non : rediriger vers IdP
    │
    └─ Callback Handler WildFly
       └─ Populate CallerPrincipal + Groups
Avantage JASPIC : Intégration plus fine avec WildFly security model, compatible avec @RolesAllowed, @PermitAll, etc.

VIII. Module util – Utilitaires
Objectif
Fournir des utilitaires pour XML parsing, compression, horodatage, génération UUID, logging…

Structure logique
Code
com.yourcompany. saml.util
├── XmlUtility
│   ├─ prettyPrint(Element): String (pour debug)
│   ├─ serialize(XMLObject): String (compact)
│   ├─ parse(String): Document
│   └─ validateXmlWellFormed(String): Boolean
│
├── CompressionUtility
│   ├─ deflate(byte[]): byte[]
│   ├─ inflate(byte[]): byte[]
│   └─ deflateAndBase64Encode(String): String
│
├── EncodingUtility
│   ├─ base64Encode(byte[]): String
│   ├─ base64Decode(String): byte[]
│   ├─ urlEncode(String): String
│   └─ urlDecode(String): String
│
├── TimestampUtility
│   ├─ now(): Instant (UTC)
│   ├─ nowWithSkew(Duration): Instant
│   ├─ parseXmlDateTime(String): Instant
│   └─ formatAsXmlDateTime(Instant): String
│
├── UuidUtility
│   ├─ generateRequestId(): String (_<UUID>)
│   ├─ generateRelayStateId(): String
│   └─ generateSessionId(): String
│
├── CertificateUtility
│   ├─ loadPemCertificate(String): X509Certificate
│   ├─ extractSubjectDN(X509Certificate): String
│   └─ formatCertificateFingerprint(X509Certificate): String
│
└── LoggingUtility
    ├─ sanitizeForLog(String): String (masquer infos sensibles)
    ├─ redactPassword(String): String
    └─ obfuscateEmail(String): String
IX. Flux end-to-end (Sequence diagrams)
Scénario 1 : Authentification SP-initiée (HTTP-Redirect)
Code
Application WAR          SmalLib (Filter)        IdP (Keycloak)
   |                          |                        |
   |--- GET /secure/page      |                        |
   |                          |                        |
   |                     [1] Pas de Principal
   |                          |                        |
   |                     [2] buildAuthnRequest()       |
   |                          |                        |
   |                     [3] HTTP-Redirect encode      |
   |                          |                        |
   |<-- 302 redirect to IdP SSO with SAMLRequest & RelayState
   |                          |                        |
   |                                                   [4] GET /saml/sso? SAMLRequest=...
   |                                                   |
   |                                                  [5] User logs in (Keycloak form)
   |                                                   |
   |                                     POST /acs (SAMLResponse)
   |<------ 302 redirect to /saml/acs? SAMLResponse=...&RelayState=
   |                          |
   |--- POST /saml/acs        |
   |                          |
   |                     [6] decode SAMLResponse
   |                          |
   |                     [7] validate (signature, conditions, etc.)
   |                          |
   |                     [8] extract Principal
   |                          |
   |                     [9] store in session
   |                          |
   |                   [10] retrieve original URL from RelayState
   |                          |
   |<-- 302 redirect to /secure/page
   |
   |--- GET /secure/page      |
   |
   |                     [11] Principal found in session
   |                          |
   |                     [12] Request allowed
   |
   |<-- 200 HTML page
Scénario 2 : Single Logout (SLO)
Code
Application WAR          SmalLib              IdP (Keycloak)
   |                          |                      |
   |--- GET /saml/logout       |                      |
   |                          |                      |
   |                     [1] buildLogoutRequest()    |
   |                          |                      |
   |<-- 302 redirect to IdP SLO with LogoutRequest
   |                          |                      |
   |                                              [2] Keycloak logs out user
   |                                              |
   |                          <-- POST /saml/slo (LogoutResponse)
   |                          |
   |                     [3] validate LogoutResponse |
   |                          |
   |                     [4] invalidate session      |
   |                          |
   |<-- 200 "logged out" page
X. Matrice d'intégration : WAR vs EAR
Aspect	WAR	EAR
Packaging de SmalLib	lib/smalLib.jar	lib/smalLib.jar (EAR lib/)
Configuration centralisée	Dans app (war/WEB-INF/classes)	Dans EAR lib/ ou module partagé
Filter	Enregistrement via web.xml ou annotation	Via web.xml de chaque WAR ou EAR-level
ClassLoader	WAR classloader	EAR classloader (partagé)
Multiple apps	Chacune son config (isolation)	Partage config (centralisée)
Scalabilité	RelayStateStore session-local	RelayStateStore Redis/DB (recommandé)
XI. Sécurité – Checklist validations
Code
☐ Signature XML validée (Response + Assertion)
☐ Certificat IdP en truststore ou metadata
☐ Clock skew validé (±2 min)
☐ NotBefore & NotOnOrAfter vérifiés
☐ AudienceRestriction contient SP entityId
☐ Recipient = ACS URL configurée
☐ InResponseTo = requestID de l'AuthnRequest
☐ Status = Success (pas AuthnFailed, etc.)
☐ Subject Confirmation Type = bearer
☐ RelayState : opaque, server-side stored, TTL
☐ Logs audit : tous les événements majeurs tracés
☐ Gestion du clock skew : configurable
☐ Rejet de messages non signés si config exige
☐ Pas d'attribut sensible (password) dans SAML
XII. Points clés de déploiement sur WildFly 31
OpenSAML initialization : AppListener ou ServletContextListener pour appel unique à InitializationService. initialize()

Keystores/Truststores : Placer dans un répertoire accessible en lecture (ex: /opt/wildfly/conf/saml/)

Configuration :

YAML/JSON dans WEB-INF/classes/ (WAR) ou conf/ (EAR)
Env vars pour passwords (ne jamais en clair)
Logging : Configurer logback. xml ou log4j2.xml pour déboguer OpenSAML (niveau DEBUG)

Module partagé (EAR) : Si plusieurs WARs, placer SmalLib dans lib/ de l'EAR et referencer depuis chaque WAR-pom.xml

HTTPS obligatoire : ACS URL doit être HTTPS; redirect http→https si needed

XIII. Résumé des interfaces publiques
Interface	Responsabilité
SamlServiceProvider	API métier principale (AuthnRequest, Response, SLO)
SamlServiceProviderFactory	Factory pour créer une instance provider
SamlPrincipal	DTO du principal authentifié
SamlConfiguration	Modèle de config (SP, IdP, Security)
ConfigLoader	Chargement config multi-sources
RelayStateStore	Abstraction du stockage RelayState
SamlAuditLogger	Logging audit
SamlErrorHandler	Gestion centralisée des erreurs
XIV. Prochaines étapes / Backlog v2+
 Support EncryptedAssertion (déchiffrement avec clé SP)
 Multi-IdP (plusieurs Keycloak ou IdP mixtes)
 Assertion cache (pour perf, avec TTL)
 OIDC / OAuth2 federation
 Audit trail BD (login, logout, errors)
 Hot-reload config (sans redémarrage)
 SDK intégration pour frameworks (Quarkus, Micronaut)
 Monitoring/metrics (Prometheus)
