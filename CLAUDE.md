# CLAUDE.md

Ce fichier fournit des indications à Claude Code (claude.ai/code) pour travailler dans ce dépôt.

## Configuration Maven

Toutes les commandes Maven doivent utiliser le fichier de settings personnalisé et le dépôt local dédié :

```bash
mvn -s /opt/homebrew/Cellar/maven/3.9.12-perso/libexec/conf/settings.xml \
    -Dmaven.repo.local=/Users/72337B/workspace/perso/repoperso \
    <commande>
```

## Commandes

```bash
# Build
mvn -s /opt/homebrew/Cellar/maven/3.9.12-perso/libexec/conf/settings.xml -Dmaven.repo.local=/Users/72337B/workspace/perso/repoperso clean package

# Lancer en local (profil dev)
mvn -s /opt/homebrew/Cellar/maven/3.9.12-perso/libexec/conf/settings.xml -Dmaven.repo.local=/Users/72337B/workspace/perso/repoperso spring-boot:run -Dspring-boot.run.profiles=dev

# Tests unitaires
mvn -s /opt/homebrew/Cellar/maven/3.9.12-perso/libexec/conf/settings.xml -Dmaven.repo.local=/Users/72337B/workspace/perso/repoperso test

# Tests d'intégration (nécessite Docker pour Testcontainers)
mvn -s /opt/homebrew/Cellar/maven/3.9.12-perso/libexec/conf/settings.xml -Dmaven.repo.local=/Users/72337B/workspace/perso/repoperso verify

# Lancer une seule classe ou méthode de test
mvn -s /opt/homebrew/Cellar/maven/3.9.12-perso/libexec/conf/settings.xml -Dmaven.repo.local=/Users/72337B/workspace/perso/repoperso test -Dtest=NomDeLaClasse
mvn -s /opt/homebrew/Cellar/maven/3.9.12-perso/libexec/conf/settings.xml -Dmaven.repo.local=/Users/72337B/workspace/perso/repoperso test -Dtest=NomDeLaClasse#nomDeLaMethode

# Analyse SonarQube
mvn -s /opt/homebrew/Cellar/maven/3.9.12-perso/libexec/conf/settings.xml -Dmaven.repo.local=/Users/72337B/workspace/perso/repoperso sonar:sonar -Dsonar.projectKey=<KEY> -Dsonar.organization=<ORG>

# Démarrer les dépendances (PostgreSQL)
docker compose up -d postgres
```

## Architecture

Architecture hexagonale (clean architecture) avec trois packages principaux sous `com.roomify.api` :

- **`presentation/`** — Contrôleurs REST (`endpoint/`) et resolvers GraphQL, ainsi que les modèles de requête/réponse (`models/in`, `models/out`).
- **`domain/`** — Logique métier pure. Contient les services, les modèles/enums du domaine, les interfaces API (`api/`) qu'implémentent les services, et les interfaces SPI (`spi/`) que l'infrastructure doit implémenter.
- **`infrastucture/`** — Entités JPA, repositories Spring Data, implémentations des SPI du domaine, filtre JWT, configuration Spring Security, et client email Brevo.
- **`shared/`** — Types d'exceptions personnalisés, `GlobalExceptionHandler`, et utilitaires (génération de token, normalisation d'email, validation).

### Pattern clé : inversion via SPI

Le domaine ne touche jamais directement l'infrastructure. Il définit des interfaces SPI (ex. `UserSpi`, `RoleSpi`, `EmailVerificationSpi`, `EmailSenderSpi`) dont dépendent les services. Les classes d'infrastructure les implémentent. C'est le principal point d'extension lors de l'ajout de nouvelles fonctionnalités.

### Câblage des services

Les services du domaine implémentent des interfaces API (ex. `AuthService implements AuthApi`). Les contrôleurs injectent l'interface API, jamais le service concret.

### Email piloté par événements

L'envoi d'emails est découplé via `ApplicationEventPublisher` de Spring. Le domaine publie des événements ; l'infrastructure les écoute et délègue au client HTTP Brevo via WebClient, protégé par un décorateur Resilience4j `@Retry`.

### Stratégie de tests

- `**/*Test.java` → tests unitaires, exécutés par Surefire (`mvn test`).
- `**/*IT.java` → tests d'intégration, exécutés par Failsafe (`mvn verify`). Tous les ITs étendent `AbstractIntegrationTest`, qui démarre un vrai conteneur PostgreSQL via Testcontainers et lie la datasource via `@DynamicPropertySource`.

## Stack technique

| Sujet           | Choix                                                 |
|-----------------|-------------------------------------------------------|
| Framework       | Spring Boot 4.0.2, Java 21                            |
| Base de données | PostgreSQL 16, migrations Flyway (`db/migration/`)    |
| ORM             | Spring Data JPA / Hibernate                           |
| Sécurité        | JWT stateless (jjwt 0.11.5), Spring Security          |
| Mapping         | MapStruct 1.6.3                                       |
| Résilience      | Resilience4j (`@RateLimiter`, `@Retry`)               |
| Docs API        | springdoc-openapi 3.0.1 (Swagger UI) + Spring GraphQL |
| Email           | API Brevo via WebClient réactif                       |
| Boilerplate     | Lombok                                                |

## Variables d'environnement

```
API_BASE_URL=http://localhost:8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/roomify
SPRING_DATASOURCE_USERNAME=roomify
SPRING_DATASOURCE_PASSWORD=roomify
JWT_SECRET=<secret>
BREVO_API_KEY=<clé>
```

Le profil `dev` (`application-dev.yml`) active les logs de debug. Docker Compose fournit une instance PostgreSQL 16 prête à l'emploi avec les valeurs par défaut ci-dessus.

## Endpoints publics (sans JWT)

```
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/verify?token=<token>
POST /api/v1/auth/resend-verification
```

Toutes les autres routes nécessitent un Bearer JWT valide.

## Création d'un nouvel endpoint

Suit cet ordre à chaque fois qu'on ajoute un nouveau endpoint REST.

### 1. Exceptions métier (`shared/exception/<domaine>/`)

Créer une exception par règle métier qui peut être violée. Étend `Exception`. Annoter avec `@Builder` Lombok pour exposer un builder avec `message`. Exemple : `PlaceNotFoundException`, `PlaceDuplicationException`.

### 2. Interface SPI (`domain/spi/`)

Déclarer les méthodes d'accès données dont le domaine a besoin. Paramètres annotés `@NonNull` (jspecify). Pas de dépendance vers JPA ou Spring ici.

### 3. Interface API (`domain/api/`)

Déclarer la méthode métier exposée au contrôleur. Les exceptions métier sont déclarées dans la signature (`throws ...`).

### 4. Service (`domain/service/<domaine>/`)

Implémente l'interface API. Injecte les SPI via constructeur (pas `@Autowired`). Annoter les méthodes avec `@Transactional`. Appliquer `@RateLimiter(name = "creationalRateLimiter")` sur les opérations créationnelles. La logique métier (validations, normalisations) est encapsulée dans des méthodes privées.

### 5. Repository (`infrastucture/repository/`)

Étend `JpaRepository`. Ajouter les méthodes dérivées Spring Data nécessaires (ex. `existsByOwnerAnd...`). Pas de `@Query` custom sauf si la méthode dérivée devient illisible.

### 6. Adapter (`infrastucture/adapter/`)

Implémente l'interface SPI. Annoté `@Component`. Injecte le repository via constructeur. Ajoute des `log.debug(...)` sur chaque méthode.

### 7. Modèles de présentation (`presentation/models/in/` et `out/`)

- `*Request` : record Java avec annotations Bean Validation (`@NotNull`, `@NotBlank`, `@Min`, etc.) et annotations Swagger `@Schema`. Utiliser `@NonNull` (jspecify) pour les champs obligatoires, `@Nullable` pour les optionnels.
- `*Response` : record Java, champs `@NonNull` / `@Nullable` selon la nullabilité du domaine. Annotations `@Schema` pour la doc.

Pour un endpoint PATCH, tous les champs du request sont `@Nullable` (seuls les champs fournis sont mis à jour).

### 8. Contrôleur (`presentation/endpoint/`)

- Annoté `@RestController`, `@RequestMapping("/api/v1/<ressource>")`
- Injecte l'interface API (jamais le service concret)
- `@PreAuthorize("hasAnyRole(...)")` sur chaque méthode avec les rôles autorisés
- `@AuthenticationPrincipal CustomUserDetails` pour récupérer l'utilisateur courant
- Catch les exceptions métier → les relancer via `ClientApiException.ofConflict()` / `ofBadRequest()` / `ofNotFound()` / `ofForbidden()`
- Annotations Swagger `@Operation`, `@ApiResponse` pour chaque code HTTP possible (200/201/400/401/403/404/409/429/500)

### 9. Test d'intégration (`src/test/java/.../integration/`)

- Étend `AbstractIntegrationTest`, nommé `<Ressource>ControllerIT`
- Sections organisées avec commentaires `// ✅ NOMINAL`, `// ❌ ERREURS DE VALIDATION`, `// ❌ ERREURS MÉTIER`, `// ⚠️ RATE LIMITING`
- Nommage des méthodes : `<action>_<contexte>_returns<HTTP_CODE>()`
- `@Sql` au niveau classe : `DELETE FROM ... WHERE user_id IN (...)` avec `BEFORE_TEST_METHOD` pour nettoyer avant chaque test
- `@Sql` au niveau méthode : INSERT des données nécessaires au test, également `BEFORE_TEST_METHOD` (s'exécute après le DELETE de classe)
- IDs de test : utiliser des valeurs fixes très grandes (`99999999998L`, `9000000010`) pour éviter les collisions avec les données de migration
- Réinitialiser le rate limiter dans `@BeforeEach` si `@RateLimiter` est en jeu sur le service testé