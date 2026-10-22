# LUNA Android WireGuard

Application Android LUNA utilisant Kotlin, Jetpack Compose et exclusivement le moteur officiel WireGuard.

## Fonctions présentes

- Importation et exportation de profils WireGuard `.conf`.
- Validation des configurations avec la bibliothèque WireGuard.
- Permission VPN Android et démarrage/arrêt du tunnel par `GoBackend`.
- Récupération d'un profil depuis l'API avec un code d'activation à usage unique.
- Affichage des informations d'abonnement renvoyées par l'API.
- Workflow GitHub Actions pour compiler l'APK de debug et le publier comme artefact.

## Compilation

La CI utilise JDK 17 et Gradle 8.9. Le workflow est dans `.github/workflows/android-build.yml`. À chaque push sur `main`, il lance `gradle assembleDebug` et publie `luna-wireguard-debug-apk` dans l'onglet Actions.

Le projet utilise uniquement WireGuard standard sur UDP; il n'inclut pas Xray/VLESS/WSS/camouflage/SlowDNS.

## API

Le serveur d'API se trouve dans [luna-wireguard-server](https://github.com/abesskamer237/luna-wireguard-server). Le parcours client d'achat et de création de compte reste à compléter; la demande de configuration nécessite actuellement un code d'activation délivré pour un abonnement actif.

## Sécurité

Un profil WireGuard contient une clé privée. Ne le publie pas. Ne place jamais le jeton administratif de l'API ou les identifiants Monetbil dans l'application Android.
