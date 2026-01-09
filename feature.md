# Feature: Validation SAML + Session Serveur + JWT court

Ce guide explique ce qui a ete ajoute a SmalLib pour:
- valider completement la SAMLResponse
- creer une session serveur cote SP (autorite)
- emettre un JWT ephemere (10s) lie a la session

## 1) Validation SAML complete
Apres reception de la SAMLResponse, le SP valide:
- Signature XML: Response et/ou Assertion selon la configuration
- Issuer (accepte aussi une forme URL qui termine par "/<entity-id>" si l'entity-id est un nom court)
- InResponseTo
- Destination
- AudienceRestriction
- NotBefore / NotOnOrAfter (avec clock skew)
- Replay protection (cache en memoire sur ID Assertion/Response)

Si une validation echoue: erreur et redirection vers le handler d'erreur.

## 2) Session serveur (autorite)
Si la validation est OK, le SP cree une session serveur:
- sessionId interne
- identite utilisateur (NameID)
- attributs / roles (issus de l'assertion)
- horodatage + expiration = min(NotOnOrAfter, politique SP)

La session serveur est la reference d'autorite. Un JWT valide
cryptographiquement est rejete si la session serveur est expiree ou invalidee.

## 3) JWT ephemere (10s)
Le SP emet un JWT court lie a la session:
- sub: NameID
- sid: sessionId serveur
- iat, exp (exp = iat + 10s)
- jti
- roles (optionnel)

Le JWT est renvoye via le header:
X-Auth-Token: <jwt>
sur les reponses des ressources protegees.

## 4) Configuration YAML
Ajouts dans `saml-config.yml`:

app:
  server-session-attribute-key: "saml.server.session"
  session-max-minutes: 60

security:
  jwt-secret: "change-me-demo2"
  jwt-ttl-seconds: 10

Notes:
- `jwt-secret` est obligatoire pour un secret stable. S'il est absent,
  SmalLib genere une valeur ephemere au demarrage (warning log).
- `session-max-minutes` = politique SP. L'expiration finale est:
  min(NotOnOrAfter, session-max-minutes).

## 5) Flux complet
1. Requete sur /api/* -> AuthnRequest -> IdP
2. SAMLResponse validee (issuer, destination, audience, time window, signature)
3. Creation session serveur + session HTTP
4. La requete initiale est servie, reponse contient X-Auth-Token
5. Si un JWT est envoye ensuite, il est valide et lie a la session serveur

## 6) Build & redeploy (WildFly module)
1. Construire SmalLib:
   mvn test
   mvn -DskipTests package

2. Copier le jar dans le module WildFly:
   C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\modules\com\hmiso\smalib\main\smalib-0.1.0-SNAPSHOT.jar

3. Construire demo2:
   mvn -f examples\demo2\pom.xml package

4. Copier le war:
   C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\standalone\deployments\demo2.war

5. Redemarrer WildFly ou redeployer.

## 7) Remarque sur Keycloak
Aucune modification de la configuration Keycloak n'est necessaire.
Assurez-vous simplement que `identity-provider.entity-id` correspond
a la valeur Issuer de la SAMLResponse (ou a son suffixe `/realms/<nom>`).
