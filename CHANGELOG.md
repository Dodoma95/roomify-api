# Changelog

> NOTE : Le format est basé sur [Keep a Changelog], et ce projet adhère à [Semantic Versioning].

Toutes les modifications notables apportées à ce projet seront documentées dans ce fichier.

## [unreleased]

## [1.4.0]

- Fix de l'ajout du rôle OWNER à un utilisateur lorsqu'il crée une Place

## [1.3.0]

- Ajout query GraphQL "place" pour retourner les informations d'une place à partir de son id
- Quelques montées de version dixit dependabot

## [1.2.0]

- Ajout endpoints de management des bookings et des indisponibilités des places.
- Amélioration des events envoie de mail et templates
- Amélioration de la documentation
- Ajout d'un filtre sur les dates recherchées query graphql "places"
- Ajout nouvelle query "availableSlots" pour retourner les disponibilités d'une place sur un mois donné

## [1.1.0]

- Ajout endpoints POST place, PATCH place, DELETE place et search places GraphQL

## [1.0.1]

- Ajout endpoint PATCH user

## [1.0.0]

- Ajout endpoints DELETE user

[Unreleased]: https://github.com/Dodoma95/roomify-api/compare/1.4.0...develop

[1.4.0]: https://github.com/Dodoma95/roomify-api/compare/1.3.0...1.4.0

[1.3.0]: https://github.com/Dodoma95/roomify-api/compare/1.2.0...1.3.0

[1.2.0]: https://github.com/Dodoma95/roomify-api/compare/1.1.0...1.2.0

[1.1.0]: https://github.com/Dodoma95/roomify-api/compare/1.0.1...1.1.0

[1.0.1]: https://github.com/Dodoma95/roomify-api/compare/1.0.0...1.0.1

[1.0.0]: https://github.com/Dodoma95/roomify-api/tree/roomify-api-1.0.0

[Keep a Changelog]: https://keepachangelog.com/fr/1.0.0/ "CHANGELOG Template et bonnes pratiques"

[Semantic Versioning]: https://semver.org/lang/fr/ "Bonnes pratique de la Gestion de Version"