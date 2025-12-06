# Prompt de développement pour la librairie

Utilise ce prompt pour guider toutes les contributions liées à la librairie Java Maven du projet principal. Il vise à garantir des exigences claires, une spécification complète, une couverture de tests maximale et une documentation d'intégration fiable, sans laisser place aux hallucinations.

1. **Compréhension et cadrage**  
   - Reformule brièvement l'objectif et le périmètre de la fonctionnalité demandée.  
   - Liste les exigences fonctionnelles et non fonctionnelles explicites et implicites, en signalant les points à clarifier.  
   - Refuse toute supposition non justifiée par une source du dépôt ou des instructions explicites.

2. **Spécification détaillée**  
   - Définis les interfaces publiques, contrats d'API et modèles de données avec types, validations et erreurs attendues.  
   - Documente les flux principaux, les cas limites et les scénarios d'échec.  
   - Évite le jargon ambigu et référence les fichiers ou modules concernés.

3. **Plan de tests exhaustif**  
   - Élabore une stratégie de tests avec priorité aux tests unitaires, complétés par des tests d'intégration et de contrat si nécessaire.  
   - Couvre chaque exigence par au moins un test; précise les données d'entrée, les résultats attendus et les cas limites.  
   - Mesure la couverture et refuse de diminuer la couverture existante sans justification documentée.

4. **Implémentation Maven**  
   - Respecte la structure standard d'un projet Maven (src/main/java, src/test/java, pom.xml).  
   - Ajoute les dépendances avec des versions figées et vérifie la compatibilité.  
   - Fournis des commentaires ciblés et courts uniquement lorsque la lisibilité l'exige.

5. **Documentation et intégration**  
   - Mets à jour ou crée la documentation utilisateur et développeur, y compris un guide d'intégration de la librairie dans le projet principal (exemples d'utilisation, configuration, compatibilité).  
   - Ajoute ou met à jour les changelogs ou notes de version si pertinent.  
   - Inclue des extraits de configuration Maven (ex. dependency, repository) testés ou déjà présents dans le projet; n'invente pas de coordonnées artefact.

6. **Vérifications finales**  
   - Exécute les tests automatisés et analyse les rapports.  
   - Fournis un résumé des changements, des risques connus et des étapes de validation.  
   - Signale explicitement toute hypothèse résiduelle ou donnée manquante.

7. **Anti-hallucination**  
   - Appuie chaque affirmation sur une source vérifiable du dépôt ou sur l'énoncé utilisateur.  
   - Si une information manque, demande une clarification ou indique qu'elle est indisponible.  
   - Ne crée jamais de fichiers, d'APIs ou de dépendances fictifs.
