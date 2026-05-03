# Design : PATCH /api/v1/users/{id}/role

**Date** : 2026-05-03  
**Branche** : feature/add_get_bookings_graphql  
**Statut** : approuvé

---

## Contexte

Permettre aux administrateurs de gérer les rôles des utilisateurs via un endpoint REST dédié. Le `SUPER_ADMIN` a un accès complet, l'`ADMIN` est
limité aux utilisateurs avec les rôles `USER` ou `OWNER` uniquement.

---

## Endpoint

```
PATCH /api/v1/users/{id}/role
Authorization: Bearer <JWT>
Roles autorisés : ADMIN, SUPER_ADMIN
```

### Corps de la requête

```json
{
  "role": "OWNER",
  "action": "ADD"
}
```

- `role` : valeur de `RoleEnum` (`USER`, `OWNER`, `ADMIN`, `SUPER_ADMIN`)
- `action` : valeur de `RoleActionEnum` (`ADD`, `REMOVE`)

### Réponse succès

**200 OK** — retourne le `UserAdminResponse` avec les rôles mis à jour.

---

## Nouveaux fichiers

| Fichier                                                   | Type      | Description                                |
|-----------------------------------------------------------|-----------|--------------------------------------------|
| `domain/models/RoleActionEnum.java`                       | enum      | `ADD`, `REMOVE`                            |
| `presentation/models/in/UpdateUserRoleRequest.java`       | record    | `role: RoleEnum`, `action: RoleActionEnum` |
| `shared/exception/user/RoleAlreadyAssignedException.java` | exception | ADD sur rôle déjà présent                  |
| `shared/exception/user/RoleNotAssignedException.java`     | exception | REMOVE sur rôle absent                     |

---

## Modifications de fichiers existants

| Fichier                                                        | Changement                                                                         |
|----------------------------------------------------------------|------------------------------------------------------------------------------------|
| `domain/spi/UserSpi`                                           | + `removeRoleFromUser(Long userId, Long roleId)`                                   |
| `infrastucture/repository/UserRepository`                      | + requête DELETE native `removeRoleFromUser`                                       |
| `infrastucture/adapter/UserAdapter`                            | implémente `removeRoleFromUser`                                                    |
| `domain/api/UserApi`                                           | + `updateUserRole(Long id, UpdateUserRoleRequest, User currentUser)` avec `throws` |
| `domain/service/user/UserService`                              | implémente `updateUserRole`                                                        |
| `presentation/endpoint/UsersController`                        | + `PATCH /{id}/role`                                                               |
| `src/test/resources/db/migration/V1000__insert_users_test.sql` | + utilisateur OWNER et SUPER_ADMIN de test                                         |
| `integration/UserControllerIT`                                 | + cas de test                                                                      |

---

## Logique métier (UserService.updateUserRole)

```
1. Charger l'utilisateur cible par id
   → UserNotFoundException (404) si introuvable

2. Charger le rôle en base via RoleSpi.findByName(request.role())
   → RoleNotFoundException (404) si introuvable

3. Vérifications d'autorisation :
   a. Si currentUser est SUPER_ADMIN ET request.role() == SUPER_ADMIN
      ET currentUser.id == target.id → UserActionForbiddenException (403)
      [Un SUPER_ADMIN ne peut pas modifier son propre rôle SUPER_ADMIN]
   
   b. Si currentUser est ADMIN (pas SUPER_ADMIN) :
      - Si la cible possède ADMIN ou SUPER_ADMIN → UserActionForbiddenException (403)
      - Si request.role() est ADMIN ou SUPER_ADMIN → UserActionForbiddenException (403)

4. Si action == ADD :
   - Si la cible possède déjà le rôle → RoleAlreadyAssignedException (409)
   - Sinon : userSpi.addRoleToUser(target.id, role.id)

5. Si action == REMOVE :
   - Si la cible ne possède pas le rôle → RoleNotAssignedException (400)
   - Sinon : userSpi.removeRoleFromUser(target.id, role.id)

6. Recharger l'utilisateur et retourner UserAdminResponse
```

---

## Codes HTTP retournés

| Code | Situation                                                   |
|------|-------------------------------------------------------------|
| 200  | Succès                                                      |
| 400  | `action == REMOVE` et rôle absent de la cible               |
| 401  | Token JWT manquant ou invalide                              |
| 403  | Permissions insuffisantes (Spring Security ou règle métier) |
| 404  | Utilisateur ou rôle introuvable                             |
| 409  | `action == ADD` et rôle déjà assigné                        |
| 429  | Rate limiting                                               |
| 500  | Erreur serveur                                              |

---

## Tests d'intégration (UserControllerIT)

### Fixtures de test supplémentaires (V1000)

- `99999999997` — `test.owner@gmail.com` avec rôle `OWNER`
- `99999999996` — `test.super.admin@gmail.com` avec rôle `SUPER_ADMIN`

### Cas nominaux (✅ NOMINAL)

| Test                               | Acteur      | Cible               | Action       | Résultat |
|------------------------------------|-------------|---------------------|--------------|----------|
| SUPER_ADMIN ajoute OWNER à un USER | SUPER_ADMIN | USER (99999999998)  | ADD OWNER    | 200      |
| SUPER_ADMIN retire USER d'un USER  | SUPER_ADMIN | USER (99999999998)  | REMOVE USER  | 200      |
| ADMIN ajoute OWNER à un USER       | ADMIN       | USER (99999999998)  | ADD OWNER    | 200      |
| ADMIN retire USER d'un OWNER       | ADMIN       | OWNER (99999999997) | REMOVE OWNER | 200      |

### Erreurs de validation (❌ VALIDATION)

| Test                   | Situation      | Code |
|------------------------|----------------|------|
| Champ `role` absent    | Body incomplet | 400  |
| Champ `action` absent  | Body incomplet | 400  |
| Valeur `role` invalide | Enum inconnu   | 400  |

### Erreurs métier (❌ MÉTIER)

| Test                                       | Situation                     | Code |
|--------------------------------------------|-------------------------------|------|
| ADD rôle déjà présent                      | USER tente ADD USER           | 409  |
| REMOVE rôle absent                         | USER n'a pas OWNER            | 400  |
| Utilisateur cible introuvable              | id inconnu                    | 404  |
| ADMIN cible un ADMIN                       | Cible protégée                | 403  |
| ADMIN cible un SUPER_ADMIN                 | Cible protégée                | 403  |
| ADMIN assigne rôle ADMIN                   | Rôle interdit pour ADMIN      | 403  |
| SUPER_ADMIN retire SUPER_ADMIN de lui-même | Auto-modification SUPER_ADMIN | 403  |
| USER tente l'accès                         | Rôle insuffisant              | 403  |
| OWNER tente l'accès                        | Rôle insuffisant              | 403  |
