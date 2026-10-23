# LUNA Android WireGuard

Application Android LUNA utilisant Kotlin, Jetpack Compose et exclusivement le moteur officiel WireGuard.

## Fonctions présentes

- Interface sombre LUNA personnalisée, avec onglets Accueil, Activité, Serveurs, Réglages et Aide.
- Vue d’accueil avec état de tunnel, bouton de connexion et cartes de configuration.
- Sélecteur de transports WireGuard UDP, WSS, Camouflage et SlowDNS ; seuls les modes avec un chemin client/serveur complet peuvent démarrer le tunnel.
- Importation et exportation de profils WireGuard `.conf`.
- Validation des configurations avec la bibliothèque WireGuard.
- Permission VPN Android et démarrage/arrêt du tunnel par `GoBackend`.
- Récupération d'un profil depuis l'API avec un code d'activation à usage unique.
- Affichage des informations d'abonnement renvoyées par l'API.
- Workflow GitHub Actions pour compiler l'APK de debug et le publier comme artefact.

## Compilation

La CI utilise JDK 17 et Gradle 8.9. Le workflow est dans `.github/workflows/android-build.yml`. À chaque push sur `main`, il lance `gradle testDebugUnitTest assembleDebug` (tests unitaires puis APK debug) et publie `luna-wireguard-debug-apk` dans l'onglet Actions.

Le moteur WireGuard reste standard. Le mode WSS utilise un relais UDP-local vers WebSocket TLS authentifié, et le mode Camouflage ajoute un cadrage LUNA indépendant avec padding variable. Ils ne sont proposés pour un profil que si le serveur renvoie les deux URL et le jeton de transport par appareil. Ces deux modes restent expérimentaux tant qu’un test réel Android-vers-VPS (handshake, trafic bidirectionnel, reconnexion et révocation) n’a pas été validé. SlowDNS reste indisponible. Aucun de ces modes n’est compatible avec le protocole propriétaire de Geny.

## API

URL par défaut : `https://apis.pagnol.xyz`. Le serveur d'API se trouve dans [luna-wireguard-server](https://github.com/abesskamer237/luna-wireguard-server). Le parcours client d'achat et de création de compte reste à compléter; la demande de configuration nécessite actuellement un code d'activation délivré pour un abonnement actif.

## Liens d’activation LUNA

L’application accepte les liens `luna://enroll?api=...&token=...`. Le lien transporte uniquement une URL d’API HTTPS et un code d’enrôlement aléatoire à usage unique (43 caractères), pas une configuration VPN ni une clé privée. Il préremplit les champs mais ne déclenche aucune requête automatiquement. L’utilisateur doit vérifier le domaine HTTPS puis appuyer sur « Obtenir une configuration ». Les codes expirés ou déjà utilisés sont rejetés par l’API. N’inclus jamais la clé privée WireGuard ou le jeton administrateur dans une URI.

## Sécurité

Un profil WireGuard contient une clé privée. Ne le publie pas. Ne place jamais le jeton administratif de l'API ou les identifiants Monetbil dans l'application Android.
