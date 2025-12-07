# Journal de remediation SAML / demo2

## Code et configuration
- **Context path** : ACS et SLO incluent maintenant le context path du WAR `/demo2` (SamlDemo2Configuration, SamlBootstrapListener).
- **Filtre** : chemins proteges, ACS et SLO alignes sur le context path (`/demo2/api/*`, `/demo2/login/saml2/sso/acs`, `/demo2/logout/saml`).
- **AuthnRequest** : construction SAML 2.0 valide (namespaces, Version, Issuer, NameIDPolicy, ProtocolBinding) dans `src/main/java/com/hmiso/saml/saml/AuthnRequestBuilder.java`.
- **RelayState / retour post-ACS** : le filtre enregistre et restaure l'URL initiale via `RelayStateStore` pour rediriger vers l'endpoint d'origine après ACS (`src/main/java/com/hmiso/saml/integration/SamlAuthenticationFilterHelper.java`).
- **Keycloak** : client `saml-sp` mis a jour avec les redirect URIs `/demo2/login/saml2/sso/*` (http/https) via `kcadm.sh`.

## Commandes executees
```powershell
# Rebuild librairie (skip jacoco)
mvn "-Djacoco.skip=true" -DskipTests install

# Rebuild WAR demo2
mvn -f examples/demo2/pom.xml package

# Redeploiement WildFly
Copy-Item C:\Users\hamdi\Desktop\POC\SmalLib\examples\demo2\target\demo2.war `
  -Destination C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\standalone\deployments\demo2.war -Force

# Redemarrage serveur
C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\bin\jboss-cli.bat --connect command=":shutdown"
start C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\bin\standalone.bat

# Mise a jour Keycloak (dans le conteneur)
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/ `
  --realm master --user admin --password admin123 --client admin-cli
docker exec keycloak /opt/keycloak/bin/kcadm.sh update clients/d1b9f706-8952-4038-909a-67c1f8e2d3a0 `
  -r saml-realm -s redirectUris+=http://localhost:8080/demo2/login/saml2/sso/* `
  -s redirectUris+=https://localhost:8080/demo2/login/saml2/sso/*
```

## Tests rapides
- `curl -k http://127.0.0.1:8080/demo2/api/whoami` renvoie une page HTML avec un formulaire POST vers Keycloak (SAMLRequest base64).
- Decode de la SAMLRequest : AuthnRequest valide (namespaces SAML 2.0, Issuer `saml-sp`, ACS `http://localhost:8080/demo2/login/saml2/sso/acs`).
- POST de la SAMLRequest vers Keycloak :
  ```powershell
  $resp = Invoke-WebRequest http://127.0.0.1:8080/demo2/api/whoami
  $saml = ([regex]'name="SAMLRequest" value="([^"]+)"').Match($resp.Content).Groups[1].Value
  $relay = ([regex]'name="RelayState" value="([^"]+)"').Match($resp.Content).Groups[1].Value
  curl -k -i -X POST https://localhost:8443/realms/saml-realm/protocol/saml `
    --data-urlencode "SAMLRequest=$saml" --data-urlencode "RelayState=$relay"
  ```
  -> HTTP 302 vers la page de login Keycloak (plus d'`Invalid Request`).
- Vérifier après authentification que l'utilisateur est renvoyé vers l'URL initiale (RelayState restauré) : `/demo2/api/whoami` doit répondre avec le principal SAML.

## A faire pour validation complete
- Ouvrir `http://127.0.0.1:8080/demo2/api/whoami` dans un navigateur, se connecter avec `demo-user/demo123` sur Keycloak, verifier la reponse JSON.
- Optionnel : ajouter un certificat/truststore si on souhaite forcer HTTPS cote SP.
