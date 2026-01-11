# Integration demo2 + demo2-front (CORS + JWT)

## Objectif
Resoudre le probleme CORS entre demo2 (WildFly) et demo2-front (Angular),
permettre la lecture du header X-Auth-Token, l'envoi du cookie de session
et la redirection SSO via HTTP_POST.

## Changements cote backend (demo2)
1) Ajout d'un filtre CORS:
   - Fichier: examples/demo2/src/main/java/com/hmiso/examples/demo2/CorsFilter.java
   - Autorise l'origine http://localhost:4200
   - Expose le header X-Auth-Token
   - Supporte OPTIONS (preflight)

2) Declaration du filtre dans web.xml:
   - Fichier: examples/demo2/src/main/webapp/WEB-INF/web.xml
   - Mapping sur /api/* avant le filtre SAML

Headers CORS renvoyes:
- Access-Control-Allow-Origin: http://localhost:4200
- Access-Control-Allow-Credentials: true
- Access-Control-Allow-Methods: GET,POST,OPTIONS
- Access-Control-Allow-Headers: Authorization,Content-Type,X-Requested-With
- Access-Control-Expose-Headers: X-Auth-Token

## Changements cote frontend (demo2-front)
1) Envoi du cookie de session pour l'appel initial:
   - Fichier: examples/demo2-front/src/app/whoami.service.ts
   - Ajout de withCredentials: true

2) Gestion du SSO HTTP_POST dans l'appel API:
   - La reponse HTML (formulaire SAML) est detectee.
   - Le formulaire est reconstruit et auto-soumis.
   - Le RelayState est force vers l'URL du frontend.
   - Le navigateur est redirige vers l'IdP puis revient sur le frontend.

3) Ajout d'un bouton qui appelle une nouvelle API:
   - Endpoint: http://localhost:8080/demo2/api/message
   - Le clic declenche l'appel et affiche le message retourne.
   - Le Bearer token est injecte via l interceptor existant.

## Redeploiement
1) Rebuild module SmalLib (si necessaire):
   mvn -DskipTests package
   Copy-Item -Force target\smalib-0.1.0-SNAPSHOT.jar \
     C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\modules\com\hmiso\smalib\main\smalib-0.1.0-SNAPSHOT.jar

2) Rebuild demo2:
   mvn -f examples\demo2\pom.xml package
   Copy-Item -Force examples\demo2\target\demo2.war \
     C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\standalone\deployments\demo2.war

3) Rebuild demo2-front:
   cd examples\demo2-front
   npm run build

4) Lancer le frontend:
   ng serve

## Notes
- Le preflight OPTIONS doit renvoyer 204 sans passer par le filtre SAML.
- Le header X-Auth-Token est lisible cote Angular grace a Access-Control-Expose-Headers.
- L'envoi du cookie de session est possible grace a withCredentials + Allow-Credentials.
- Le POST SSO est gere en reconstruisant le formulaire HTML cote front.
