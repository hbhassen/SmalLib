# Plan de tests SmalLib

# Objectif
# Ce fichier liste les piliers du plan de tests unitaire et d'intégration
# afin de garantir la couverture complète des exigences décrites dans
# SPECIFICATION.md et umlConception.md.

[scope]
modules = config, saml, binding, security, metadata, util, integration, public-api

[types]
unit = validation de chaque composant isolé (constructeurs, helpers, loaders)
integration = parcours bout-en-bout Authn, Response, SLO et chargements multi-sources

[principes]
# 1. Alignement : chaque exigence fonctionnelle ou sécurité dispose d'au moins
#    un test unitaire (comportement) ou d'intégration (enchaînement complet).
# 2. Traçabilité : les cas de test sont référencés par identifiants (TC-UX, TC-SAML...).
# 3. Données : utiliser des jeux de données synthétiques signés avec clés de test.
# 4. Automatisation : les suites seront orchestrées via Maven/Gradle (à préciser dans l'implémentation).

[familles]
# UNITAIRES
# - Chargeurs de configuration (JSON/YAML/Properties/Env/Classpath/Metadata)
# - Builders/validators SAML (AuthnRequest, Response, Logout)
# - Encodeurs de binding (Redirect/Post), RelayStateManager + stores
# - Services de sécurité (signatures, chiffrement, résolution credentials)
# - Parsing metadata + cache
# - Utilitaires XML/temps/compression/logs
# - API publique (factory, builder params) : contrats stables
#
# INTEGRATION
# - Flux Authn complet (SP -> IdP simulé -> SP)
# - Flux Logout complet (SP initie et répond)
# - Erreurs et sécurité (horodatage, audience, Recipient, InResponseTo, force HTTPS)
# - Intégration servlet/JASPIC avec filtres et redirections
# - Priorité de sources de configuration et rechargement

