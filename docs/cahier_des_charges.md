# Cahier des Charges - AVOCarbon AI Platform

## Plateforme Intelligente d'Analyse Industrielle et de Génération de Rapports Assistée par l'IA

**Entreprise :** AVOCarbon Group  
**Durée du stage :** 8 semaines  

### Technologies principales
- **Backend :** Spring Boot 3.5, Spring Security, JPA, PostgreSQL, Swagger / OpenAPI, APScheduler
- **Frontend :** Angular 20, PrimeNG, TailwindCSS, Charts
- **Cloud & DevOps :** Azure (App Service, Azure SQL, Key Vault, Monitor), Docker, GitHub, GitHub Actions
- **IA :** OpenAI, MCP, Prompt Engineering

---

## 1. Contexte & Objectifs

AVOCarbon dispose de plusieurs applications industrielles internes (Satisfaction client, Qualité, Production, Déchets/Scrap, Réclamations, KPI, Projets) dont les données sont aujourd'hui dispersées. 

L'objectif est de concevoir une plateforme web modulaire pour centraliser ces flux de données, calculer automatiquement des indicateurs clés (KPI), générer des tableaux de bord interactifs et des rapports (Web/PDF) accompagnés d'analyses prédictives et de recommandations assistées par l'IA.

---

## 2. Rôles Utilisateurs (RBAC)

1. **Administrateur :** Gestion des utilisateurs, rôles, projets, APIs sources, planification et configuration générale.
2. **Responsable Production :** Suivi des KPI de production, analyse de rendement, consultation de rapports et gestion des alertes de production.
3. **Responsable Qualité :** Suivi des défauts, rebus (scrap) et réclamations, consultation des recommandations IA de calibration/qualité.
4. **Direction :** Vue globale consolidée, comparaison de la performance des différents projets et prise de décision stratégique.

---

## 3. Architecture Cible & Organisation Modulaire

```
Applications internes ──> REST APIs ──> Data Integration (Collector) 
                                                  │
                                                  ▼
PostgreSQL <── JPA <── Analytics Engine <── Service / Business Layer
                         │                        │
                         ▼                        ▼
                   Reporting Engine           AI Engine
                         │                        │
                         ▼                        ▼
                  Dashboard Angular       Rapports PDF & Recommandations
```

### Modules Recommandés
- **Module Identity & Security :** Gestion des utilisateurs, de l'authentification JWT et des rôles (RBAC).
- **Module Data Integration :** Connexion aux APIs sources externes, synchronisation et planification via APScheduler.
- **Module Analytics :** Calcul et agrégation des KPIs (Production, Rebuts, OEE, Satisfaction, Maintenance).
- **Module AI Insights :** Intégration de l'IA (OpenAI/MCP) pour générer des analyses, recommandations et prévisions.
- **Module Reporting :** Exports Excel/CSV, rapports web et PDF.
- **Module Notifications :** Alertes (Info, Warning, Critical) et routage.
- **Module Administration :** Journalisation, planification et configuration des sources.

---

## 4. Planning des Sprints

- **Sprint 0 :** Architecture, Authentification (Spring Security + JWT), Base de données (PostgreSQL).
- **Sprint 1 :** Utilisateurs (CRUD & Rôles), Projets, APIs (Sources de données).
- **Sprint 2 :** Synchronisation (Manuel/Auto/Planifié), KPI, Dashboard interactif.
- **Sprint 3 :** Rapports (Web & PDF), Graphiques interactifs (PrimeNG / Charts).
- **Sprint 4 :** IA, Analyses décisionnelles et Recommandations prédictives.
- **Sprint 5 :** Notifications (Alerte Rebuts/Qualité/Indisponibilité), Historique.
- **Sprint 6 :** Déploiement Azure, CI/CD (Docker & GitHub Actions).
- **Sprint 7 :** Optimisations, Tests finaux, Documentation technique et utilisateur.
