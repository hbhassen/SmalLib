# Guide developpeur : Integrer SmalLib (WildFly & Spring Boot)

Ce guide explique comment utiliser la librairie SAML SmalLib dans un projet Jakarta EE (WildFly) ou Spring Boot, et comment parametrer la configuration SAML.

## 1) Dependance Maven
Ajoutez la librairie dans votre `pom.xml` :
```xml
<dependency>
  <groupId>org.hmiso</groupId>
  <artifactId>smalsamlLib</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 2) Concepts de configuration
- `ServiceProviderConfig` : `entityId`, `assertionConsumerServiceUrl` (ACS), `logoutUrl` (SLO), `nameIdFormat`, binding d AuthnRequest (POST/Redirect), signatures.
- `IdentityProviderConfig` : URLs SSO/SLO IdP, metadata URL, bindings acceptes, verification de signature.
- `SecurityConfig` : algorithmes de signature/digest, chiffrement, horloges (skew).
- `protectedPaths` : chemins a proteger ; `acsPath` et `sloPath` : endpoints internes exposes par le filtre.
- `sessionAttributeKey` : cle de session pour stocker le `SamlPrincipal` ; `relayStateStore` : stocke l URL initiale pour le retour post-ACS.

## 3) WildFly / Jakarta EE
SmalLib fournit un filtre et un listener generiques (Jakarta Servlet) + un SPI `SamlConfigProvider` (ServiceLoader).

### Etapes
1. **Implémenter un provider** (dans votre WAR) :
```java
public class MySamlConfigProvider implements SamlConfigProvider {
  @Override
  public SamlAuthenticationFilterConfig provide(ServletContext ctx) {
    String ctxPath = ctx.getContextPath();
    String base = (ctxPath == null || ctxPath.isBlank() || "/".equals(ctxPath)) ? "" : ctxPath;
    return SamlAuthenticationFilterConfig.builder()
        .protectedPaths(List.of(base + "/api/*"))
        .acsPath(base + "/login/saml2/sso/acs")
        .sloPath(base + "/logout/saml")
        .sessionAttributeKey("saml.principal")
        .samlServiceProvider(buildServiceProvider(base))
        .relayStateStore(new RelayStateManager(Duration.ofMinutes(5), Clock.systemUTC()))
        .build();
  }
  private SamlServiceProvider buildServiceProvider(String base) {
    SamlConfiguration conf = /* creer ServiceProviderConfig + IdentityProviderConfig + SecurityConfig */;
    return new DefaultSamlServiceProviderFactory().create(conf);
  }
}
```
2. **Declarer le provider** : fichier `src/main/resources/META-INF/services/org.hmiso.saml.integration.jakarta.SamlConfigProvider` contenant :
```
com.example.MySamlConfigProvider
```
3. **Deployer** : le listener `org.hmiso.saml.integration.jakarta.SamlBootstrapListener` et le filtre `org.hmiso.saml.integration.jakarta.SamlJakartaFilter` (annoté `@WebFilter`) sont dans la librairie ; aucun filtre/listener custom n est requis dans le WAR, seule la config via le provider.

### Points d attention
- Adapter `entityId`, ACS/SLO, et URLs IdP (SSO/SLO) a votre contexte (`http://host:port/<context>/...`).
- Si HTTPS strict, fournir le truststore au conteneur (ex : `-Djavax.net.ssl.trustStore=...`).
- Pour forcer AuthnRequest en Redirect, mettre `authnRequestBinding=HTTP_REDIRECT` dans `ServiceProviderConfig`.

## 4) Spring Boot
SmalLib fournit un filtre Servlet Jakarta ; pour Spring Boot (Tomcat/Jetty/Undertow embed), enregistrez le filtre et le listener.

### Etapes
1. **Config SAML** : creer une classe de configuration similaire au provider ci-dessus, produisant un `SamlAuthenticationFilterConfig` et un `SamlServiceProvider` (ACS/SLO incluent `server.servlet.context-path` si defini).
2. **Beans Servlet** (Java config) :
```java
@Bean
public ServletContextInitializer samlInitializer(SamlAuthenticationFilterConfig cfg, SamlAuthenticationFilterHelper helper) {
  return ctx -> {
    ctx.setAttribute(SamlJakartaFilter.CONFIG_KEY, cfg);
    ctx.setAttribute(SamlJakartaFilter.HELPER_KEY, helper);
  };
}

@Bean
public SamlAuthenticationFilterHelper samlHelper(SamlAuthenticationFilterConfig cfg) {
  return new SamlAuthenticationFilterHelper(cfg, new SamlSessionHelper(), new DefaultSamlAuditLogger(), new DefaultSamlErrorHandler("/saml/error"));
}

@Bean
public FilterRegistrationBean<SamlJakartaFilter> samlFilter() {
  FilterRegistrationBean<SamlJakartaFilter> reg = new FilterRegistrationBean<>(new SamlJakartaFilter());
  reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
  reg.addUrlPatterns("/*");
  return reg;
}
```
3. **Declarer les endpoints** : `acsPath` et `sloPath` doivent pointer sur des routes exposees par le filtre (pas de controller Spring a ecrire pour ACS/SLO).
4. **Test** : acceder a une route protegee (ex: `/api/whoami`), verifier la redirection vers l IdP, puis la reponse JSON avec le `SamlPrincipal` en session.

## 5) Variantes de configuration
- **Bindings** : POST (defaut) ou Redirect pour AuthnRequest ; ACS accepte POST (réponse IdP). SLO via Redirect ou POST selon `IdentityProviderConfig`.
- **Signatures** : activer/désactiver signature des AuthnRequest via `ServiceProviderConfig` et choix de l algo (`SecurityConfig`).
- **RelayState** : fourni par SmalLib via `RelayStateStore` pour restaurer l URL initiale.
- **Session** : cle de session configurable ; invalider la session via `/logout/saml` (SLO).
- **Erreur** : chemin d erreur configuré dans `DefaultSamlErrorHandler` (ex: `/saml/error`).

## 6) Debug / verification
- Decoder l AuthnRequest (Base64) pour verifier `Destination`, `AssertionConsumerServiceURL`, `Issuer`, `ProtocolBinding`.
- Activer les logs DEBUG pour `org.hmiso.saml` pour tracer le flux (AuthnRequest, ACS, SLO).
- Sur l IdP (Keycloak), autoriser les redirect URIs correspondant a l ACS (`http(s)://host:port/context/login/saml2/sso/*`).

Ce guide couvre l integration standard Servlet/Jakarta (WildFly) et Spring Boot embarque. Adaptez les URLs et la politique TLS a votre environnement.
