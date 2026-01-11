# Guide Developpeur - Integration SmalLib avec WildFly (module + dependency provided)

Ce guide explique comment integrer SmalLib dans un projet Java (WAR) en utilisant WildFly comme serveur
et en exposant SmalLib comme module WildFly. L'objectif est d'avoir un WAR leger (dependance `provided`)
et une configuration SAML centralisee via `saml-config.yml`.

## 1) Prerequis
- Java 17
- WildFly 31.x
- Maven
- SmalLib compilee (`mvn -DskipTests package` a la racine de SmalLib)
- Un IdP SAML (ex: Keycloak) accessible par votre serveur

## 2) Creer le module WildFly

### 2.1 Dossier du module
Creer le dossier du module dans WildFly :

```
WILDFLY_HOME\modules\com\hmiso\smalib\main
```

Exemple avec votre chemin :
```
C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\modules\com\hmiso\smalib\main
```

### 2.2 Copier les JARs requis
Copier dans ce dossier :
- `smalib-0.1.0-SNAPSHOT.jar` (depuis `SmalLib\target`)
- `snakeyaml-2.2.jar` (depuis le repository Maven local)

**Important** : ne pas copier `slf4j-api` dans le module. WildFly fournit deja SLF4J.

### 2.3 Ajouter module.xml
Creer `module.xml` dans `...modules\com\hmiso\smalib\main` :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="com.hmiso.smalib">
    <resources>
        <resource-root path="smalib-0.1.0-SNAPSHOT.jar"/>
        <resource-root path="snakeyaml-2.2.jar"/>
    </resources>
    <dependencies>
        <module name="jakarta.servlet.api"/>
        <module name="org.slf4j"/>
        <module name="java.desktop"/>
    </dependencies>
</module>
```

Pourquoi `java.desktop` ? SnakeYAML utilise `java.beans` (module JDK `java.desktop`).

### 2.4 Redemarrer WildFly
Redemarrer WildFly apres creation/modification du module :

```
WILDFLY_HOME\bin\jboss-cli.bat --connect command=":shutdown"
WILDFLY_HOME\bin\standalone.bat
```

## 3) Adapter le projet WAR (demo2 ou autre)

### 3.1 POM Maven (dependency provided)
Dans votre projet WAR, utiliser SmalLib avec `scope` provided :

```xml
<dependency>
  <groupId>com.hmiso</groupId>
  <artifactId>smalib</artifactId>
  <version>${smalib.version}</version>
  <scope>provided</scope>
</dependency>
```

### 3.2 Declarer la dependance module (jboss-deployment-structure)
Ajouter `WEB-INF/jboss-deployment-structure.xml` :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jboss-deployment-structure>
    <deployment>
        <dependencies>
            <module name="com.hmiso.smalib"/>
        </dependencies>
    </deployment>
</jboss-deployment-structure>
```

### 3.3 Declaration web.xml (listener + filtre + JAX-RS)
Ajouter `WEB-INF/web.xml` pour declarer la configuration servlet :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0"
         metadata-complete="true">

    <listener>
        <listener-class>com.hmiso.saml.integration.SamlBootstrapListener</listener-class>
    </listener>

    <filter>
        <filter-name>CorsFilter</filter-name>
        <filter-class>com.hmiso.saml.integration.CorsFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>CorsFilter</filter-name>
        <url-pattern>/api/*</url-pattern>
    </filter-mapping>

    <filter>
        <filter-name>ApiNavigationFilter</filter-name>
        <filter-class>com.hmiso.saml.integration.ApiNavigationFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>ApiNavigationFilter</filter-name>
        <url-pattern>/api/*</url-pattern>
    </filter-mapping>

    <filter>
        <filter-name>SamlJakartaFilter</filter-name>
        <filter-class>com.hmiso.saml.integration.SamlJakartaFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>SamlJakartaFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <context-param>
        <param-name>resteasy.servlet.mapping.prefix</param-name>
        <param-value>/api</param-value>
    </context-param>

    <servlet>
        <servlet-name>JaxRsServlet</servlet-name>
        <servlet-class>org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher</servlet-class>
        <init-param>
            <param-name>jakarta.ws.rs.Application</param-name>
            <param-value>com.myapp.MyApplication</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>JaxRsServlet</servlet-name>
        <url-pattern>/api/*</url-pattern>
    </servlet-mapping>

    <servlet>
        <servlet-name>SamlErrorServlet</servlet-name>
        <servlet-class>com.myapp.SamlErrorServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>SamlErrorServlet</servlet-name>
        <url-pattern>/saml/error</url-pattern>
    </servlet-mapping>
</web-app>
```

Notes :
- Le listener charge la configuration YAML et initialise le filtre.
- Le filtre gere ACS/SLO et la redirection vers l'IdP.
- JAX-RS est configure via web.xml (pas besoin de `@ApplicationPath`).

## 4) Configuration YAML (saml-config.yml)

Placer `saml-config.yml` dans `src/main/resources/` du WAR.
Le fichier est charge depuis le classpath du WAR ou depuis une valeur externe :
- `-Dsaml.config.path=C:\...\saml-config.yml`
- `SAML_CONFIG_PATH=C:\...\saml-config.yml`

Exemple minimal :

```yaml
app:
  protected-paths:
    - "/api/*"
  session-attribute-key: "saml.principal"
  error-path: "/saml/error"
  cors-enabled: true
  cors-allowed-origins:
    - "http://localhost:4200"
  cors-allowed-methods:
    - "GET"
    - "POST"
    - "OPTIONS"
  cors-allowed-headers:
    - "Authorization"
    - "Content-Type"
    - "X-Requested-With"
  cors-expose-headers:
    - "X-Auth-Token"
  cors-allow-credentials: true
  block-browser-navigation: true
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

Notes importantes :
- `app.protected-paths` est obligatoire.
- Les chemins `app.*`, `acs-path` et `slo-path` sont automatiquement prefixes par le context path du WAR.
- `force-https-redirect` force la redirection HTTPS sur le client si le serveur est derriere un proxy HTTPS.

## 5) Securisation (recommandations)

### 5.1 Forcer HTTPS
Si votre application est exposee en HTTPS, activer :
```
security:
  force-https-redirect: true
```

### 5.2 Signatures et algorithmes
Utiliser des algorithmes modernes :
- `signature-algorithm: rsa-sha256`
- `digest-algorithm: sha256`

Si l'IdP signe les assertions et les messages, conservez :
```
identity-provider:
  want-assertions-signed: true
  want-messages-signed: true
```

### 5.3 Session et relay state
Le relay state sert a revenir sur l'URL initiale apres ACS.
Pour limiter les attaques replay, gardez un TTL court :
```
app:
  relay-state-ttl-minutes: 5
```

### 5.4 Gestion logout
SLO est declenche via `/logout/saml`.
Assurez-vous que l'IdP a un endpoint SLO actif, et que le `slo-path` et `single-logout-service-url` sont corrects.

### 5.5 Journalisation
En module WildFly, les logs SLF4J sont rediriges vers le logmanager WildFly.
Si vous avez des erreurs de log, verifier que `org.slf4j` est bien declare dans `module.xml`.

## 6) Build et deploiement

1. Construire SmalLib :
```
mvn -DskipTests package
```

2. Copier le JAR vers le module WildFly :
```
copy target\smalib-0.1.0-SNAPSHOT.jar WILDFLY_HOME\modules\com\hmiso\smalib\main
```

3. Rebuild le WAR :
```
mvn -f examples/demo2/pom.xml package
```

4. Deployer sur WildFly :
```
copy examples\demo2\target\demo2.war WILDFLY_HOME\standalone\deployments\
```

5. Redemarrer WildFly si le module a change.

## 7) Depannage rapide

- **Missing /saml-config.yml on the classpath**
  - Verifier que `saml-config.yml` est bien dans `src/main/resources` du WAR.
  - Ou utiliser `-Dsaml.config.path=...`.

- **NoClassDefFoundError: java/beans/IntrospectionException**
  - Ajouter `java.desktop` dans `module.xml` du module SmalLib.

- **SLF4J: No SLF4J providers were found**
  - Declarer `org.slf4j` dans `module.xml`.
  - Eviter d'embarquer un `slf4j-api` local dans le module.

## 8) Checklist de validation
- Le WAR demarre sans erreurs dans WildFly.
- `GET /api/whoami` redirige vers l'IdP.
- L'authentification renvoie un JSON avec `nameId` et `sessionIndex`.
- `GET /logout/saml` declenche le SLO et invalide la session.
