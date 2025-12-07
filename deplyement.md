# Deploiement WildFly demo2

## Prerequis
- WildFly 31.0.1.Final installe sous `C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final`
- WAR construit : `C:\Users\hamdi\Desktop\POC\SmalLib\examples\demo2\target\demo2.war`

## Etapes effectuees
1. Deployer le WAR dans WildFly :
```powershell
Copy-Item -Path "C:\Users\hamdi\Desktop\POC\SmalLib\examples\demo2\target\demo2.war" -Destination "C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\standalone\deployments\demo2.war" -Force
```
2. Creer l'utilisateur de gestion `admin/admin123` (mode standalone, non interactif) :
```powershell
C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\bin\add-user.bat -u admin -p admin123 -s
```
3. Demarrer le serveur :
```powershell
C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\bin\standalone.bat
```

## Verifications
- L'utilisateur de management est enregistre dans `C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\standalone\configuration\mgmt-users.properties` (ligne `admin=...`).
- Appli accessible via `http://localhost:8080/demo2/`.
- Console d'admin : `http://localhost:9990/` (login admin/admin123).

## Arret du serveur
- Depuis CLI :
```powershell
C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\bin\jboss-cli.bat --connect command=":shutdown"
```
- Ou fermer la fenetre `standalone.bat` / terminer le processus Java.
