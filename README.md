# CSME ERP

ERP de gestion commerciale multi-entreprises : application de bureau Windows **JavaFX + AtlantaFX (thème Cupertino, inspiré de macOS)**, API centrale **Spring Boot**, base **PostgreSQL** avec isolation des entreprises garantie par la base elle-même.

> Version 0.4.0 — phases 1 à 5 du cahier des charges : architecture, interface principale, authentification, entreprises / agences / départements, utilisateurs, rôles et permissions.

---

## Architecture

```
┌───────────────────────────────┐   HTTPS · REST · JSON   ┌──────────────────────────────┐
│  csme-desktop  (Windows)      │ ──────────────────────▶ │  csme-server  (cloud)        │
│  JavaFX 21 + AtlantaFX        │   jeton JWT 15 min      │  Spring Boot 3.5 · Java 21   │
│  MVVM · aucun accès à la base │ ◀────────────────────── │  sécurité · règles · audit   │
└───────────────────────────────┘                         └──────────────┬───────────────┘
                 ▲                                                       │ JPA / Flyway
                 │ csme-shared : DTO, permissions, routes                │
                 └───────────────────────────────────────────────────────▼
                                                        ┌──────────────────────────────┐
                                                        │  PostgreSQL 16               │
                                                        │  Row Level Security par      │
                                                        │  entreprise (company_id)     │
                                                        └──────────────────────────────┘
```

| Module | Rôle | Dépendances |
|---|---|---|
| `csme-shared` | Contrats partagés : DTO, permissions `MODULE:ACTION`, rôles prédéfinis, routes | aucune |
| `csme-server` | API : `domain` (entités, règles) · `application` (cas d'usage) · `infrastructure` (persistance, sécurité) · `api` (contrôleurs) · `tenancy` (isolation) | shared |
| `csme-desktop` | Client lourd : connexion, coque (barre latérale, barre supérieure), tableaux de bord, entreprises, agences, départements, utilisateurs, rôles et permissions, paramètres, audit | shared |

Les règles d'architecture (le domaine ne dépend de rien, les contrôleurs ne touchent jamais la persistance) sont vérifiées à chaque build par **ArchUnit**.

### Isolation multi-entreprises — deux verrous indépendants

1. **Application** : l'entreprise courante vient uniquement de la revendication `cid` du jeton signé, jamais d'un paramètre client. Chaque requête filtre par `company_id`, et une entité ne peut pas être écrite pour une autre entreprise (garde `@PrePersist`).
2. **Base de données** : chaque table métier est protégée par une politique **PostgreSQL Row Level Security** (`FORCE ROW LEVEL SECURITY`). L'API se connecte avec un rôle `csme_app` non propriétaire et non superutilisateur ; pour chaque connexion empruntée au pool, elle positionne `app.company_id`. Même un `findAll()` sans filtre ne renvoie que les lignes de l'entreprise.

La portée « système » (contournement RLS) n'est accessible que par `SystemTransactions`, utilisé pour la connexion et les opérations du super administrateur. Le test `TenantIsolationIT` prouve les deux verrous à chaque commit.

### Sécurité

- Mots de passe hachés **BCrypt** (encodeur délégant, migrable vers Argon2).
- Jeton d'accès **JWT HS256 de 15 min** ; jeton de rafraîchissement opaque de 12 h, stocké **haché (SHA-256)**, **rotation** à chaque usage, **réutilisation détectée** → toute la famille est révoquée.
- **Verrouillage** après 5 échecs (15 min), message d'erreur générique, temps de réponse égalisé pour les comptes inconnus.
- Permissions **module × action** vérifiées **côté serveur** sur chaque point d'entrée (refus par défaut ; un test échoue si un point d'entrée n'a pas d'annotation de sécurité).
- **Journal d'audit** immuable : l'API n'a que `SELECT, INSERT` sur `audit_logs`. Valeurs avant/après, acteur, entreprise, IP, résultat. Les refus d'accès sont journalisés.
- **Pas d'élévation de privilèges** : on ne peut accorder (à un rôle ou à un compte) que des permissions que l'on possède ; impossible de se retirer son propre droit de gérer les rôles ; le rôle « Directeur général » est verrouillé.
- Erreurs sans trace technique (référence à rechercher dans les journaux serveur).
- Client : HTTPS obligatoire hors poste local, jetons en mémoire uniquement.

---

## Démarrer en développement

Prérequis : **JDK 21**, **Maven 3.9+**, **Docker** (ou un PostgreSQL 16 local).

```bash
# 1. Base de données (crée csme, csme_test et les rôles csme_owner / csme_app)
docker compose up -d

# 2. API — les migrations Flyway s'appliquent au démarrage
export CSME_DB_APP_PASSWORD=csme_app_dev
export CSME_DB_OWNER_PASSWORD=csme_owner_dev
export CSME_JWT_SECRET="$(openssl rand -base64 48)"
export CSME_BOOTSTRAP_ADMIN_EMAIL=admin@votre-domaine.com
export CSME_BOOTSTRAP_ADMIN_PASSWORD='ChoisissezUnMotDePasse1'
mvn -pl csme-server -am spring-boot:run

# 3. Application de bureau (autre terminal)
mvn -pl csme-desktop -am javafx:run
```

Connectez-vous avec le super administrateur, créez une entreprise (« Entreprises » → « Nouvelle entreprise ») : ses 12 rôles prédéfinis et son directeur général sont créés automatiquement. Connectez-vous ensuite avec ce directeur général.

### Tests

```bash
mvn verify          # unitaires + intégration (PostgreSQL requis, base csme_test)
mvn test            # unitaires seulement
```

## Configuration

### API (variables d'environnement)

| Variable | Rôle | Défaut |
|---|---|---|
| `CSME_DB_URL` | URL JDBC | `jdbc:postgresql://localhost:5432/csme` |
| `CSME_DB_APP_USER` / `CSME_DB_APP_PASSWORD` | rôle d'exécution (RLS appliquée) | `csme_app` / — |
| `CSME_DB_OWNER_USER` / `CSME_DB_OWNER_PASSWORD` | rôle des migrations | `csme_owner` / — |
| `CSME_JWT_SECRET` | secret de signature, **≥ 32 octets** (le serveur refuse de démarrer sinon) | — |
| `CSME_BOOTSTRAP_ADMIN_EMAIL` / `_PASSWORD` | création du premier super administrateur | — |
| `CSME_REPORTING_TIME_ZONE` | fuseau des statistiques journalières | `UTC` |
| `PORT` | port HTTP | `8080` |

En production, l'API est placée derrière un proxy TLS (HTTPS) ; `server.forward-headers-strategy=framework` conserve l'IP réelle du client dans l'audit.

### Client Windows

Adresse du serveur, par ordre de priorité : `-Dcsme.server.url=…`, variable `CSME_SERVER_URL`, fichier `%APPDATA%\CSME ERP\config.properties` (`server.url=https://erp.exemple.com`), sinon `http://localhost:8080`.

## Intégration continue

`.github/workflows/ci.yml` :

1. **Tests serveur** sur Ubuntu avec un vrai PostgreSQL 16 (unitaires, intégration, isolation, architecture).
2. **Build Windows** sur `windows-latest` : compilation du client, puis `jpackage` produit un **installateur `.msi`** et une **version portable `.zip`** (artefact `csme-erp-windows`).
3. **Publication** : pousser un tag `vX.Y.Z` crée une Release GitHub avec l'installateur et le jar serveur.

## Avancement

| Phase | Contenu | État |
|---|---|---|
| 1 | Solution, modules, dépendances, modèle initial, migrations | ✅ |
| 2 | Interface principale : coque, navigation, tableaux de bord, thème clair/sombre | ✅ |
| 3 | Authentification : connexion, rafraîchissement, déconnexion, changement de mot de passe, verrouillage | ✅ |
| 4 | Entreprises et agences | ✅ entreprises (plateforme), informations générales, agences, départements rattachés aux agences |
| 5 | Utilisateurs et permissions | ✅ utilisateurs (rôles, agences, département, historique de connexion), rôles personnalisables, matrice module × action |
| 6 → 18 | RH, clients, produits, stocks, achats, ventes, caisse, finance… | ⏳ visibles dans la barre latérale avec leur phase |

### Hypothèses à confirmer par CSME

- **Rôles prédéfinis** : leurs permissions par défaut (voir `DefaultRoles`) sont une proposition raisonnable, modifiable par entreprise.
- **Politique de mot de passe** : 10 caractères minimum, lettres et chiffres.
- **Durées** : session 15 min renouvelée automatiquement, reconnexion après 12 h, verrouillage 15 min après 5 échecs.
- **Changement de permissions** : appliqué aux utilisateurs concernés à leur prochain renouvellement de session (15 min au plus).
- **Paramètres** : le code, la devise et le pays de l'entreprise ne sont pas modifiables après création ; les postes restent un champ texte jusqu'au module RH (phase 6).
- **Tableau de bord** : seuls les indicateurs calculables avec les modules livrés sont affichés (comptes, agences, connexions, activité). Chiffre d'affaires, stock, dépenses… apparaîtront avec leurs modules — aucun chiffre fictif n'est affiché.
- Aucune devise, taux fiscal ou plan comptable n'est imposé : la devise et le pays sont saisis à la création de l'entreprise.
