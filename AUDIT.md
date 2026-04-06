# Audit Technique Complet — Speculix (fork de vernacular-vnc)

**Date :** 2026-04-06  
**Scope :** Analyse complète sans modification de code  
**Version auditée :** `1.15-SNAPSHOT` (commit `2433a64`)

---

## 1. Inventaire du code existant

### 1.1 Structure des packages

```
com.shinyhut.vernacular/
├── VernacularViewer.java              # GUI Swing de référence (451 lignes)
├── client/
│   ├── VernacularClient.java          # Client VNC principal, cycle de vie
│   ├── VernacularConfig.java          # Configuration (callbacks, encodings, FPS)
│   ├── VncSession.java                # État de session (streams, pixel format, locks)
│   ├── ClientEventHandler.java        # Événements clavier/souris, boucle FBU
│   ├── ServerEventHandler.java        # Boucle événements serveur (FBU, bell, clipboard)
│   ├── exceptions/                    # 10 exceptions typées (VncException base abstraite)
│   └── rendering/
│       ├── ColorDepth.java            # Enum 4 profondeurs (8 indexed, 8/16/24 true)
│       ├── Framebuffer.java           # Dispatch renderers, resize, paint
│       └── renderers/
│           ├── Renderer.java          # Interface commune
│           ├── RawRenderer.java       # Encoding RAW
│           ├── CopyRectRenderer.java  # Encoding COPYRECT
│           ├── RRERenderer.java       # Encoding RRE
│           ├── HextileRenderer.java   # Encoding HEXTILE (16×16 tiles)
│           ├── ZLibRenderer.java      # Encoding ZLIB (deflate → RAW)
│           ├── CursorRenderer.java    # Pseudo-encoding CURSOR
│           ├── PixelDecoder.java      # Décodage pixels (true color / indexed)
│           └── Pixel.java             # Container RGB simple
├── protocol/
│   ├── auth/
│   │   ├── SecurityHandler.java       # Interface auth
│   │   ├── NoSecurityHandler.java     # Auth NONE
│   │   ├── VncAuthenticationHandler.java    # VNC Auth (DES)
│   │   └── MsLogon2AuthenticationHandler.java # MS-Logon II (DH + DES)
│   ├── handshaking/
│   │   ├── Handshaker.java            # Orchestrateur handshake
│   │   ├── ProtocolVersionNegotiator.java  # Négocie RFB 3.3–3.8
│   │   └── SecurityTypeNegotiator.java     # Sélection auth
│   ├── initialization/
│   │   └── Initializer.java           # Post-handshake (pixel format, encodings)
│   └── messages/                      # 25 classes de messages RFB
└── utils/
    ├── ByteUtils.java                 # Manipulation bits/bytes
    └── KeySyms.java                   # Mapping clavier → keysyms VNC
```

**Total : 62 classes Java, 25 tests Groovy/Spock**

### 1.2 Qualité du code

| Critère | Évaluation |
|---------|-----------|
| **Lisibilité** | Bonne — code clair, méthodes courtes, nommage explicite |
| **Cohérence** | Bonne — style uniforme, packages bien découpés |
| **Patterns** | Strategy (Renderer), Observer (Consumer callbacks), Template (SecurityHandler) |
| **Javadoc** | Présente sur l'API publique (VernacularClient, VernacularConfig), absente ailleurs |
| **Zéro TODO/FIXME** | Aucun TODO ou FIXME dans tout le code |

### 1.3 Version Java cible

**Actuellement : Java 8 (source/target 1.8)**

| Pour | Contre |
|------|--------|
| Max compatibilité (Android, systèmes legacy) | Pas d'accès à `HttpClient` (Java 11), `Records` (Java 14), `Sealed classes` (Java 17) |
| Suffisant pour le code actuel | `java.util.zip.Inflater` API limitée vs Java 11+ |
| Dépendances OK en Java 8 | Pas de `var`, `switch expressions`, `text blocks` |

**Recommandation :** Monter à **Java 11 minimum** (LTS, fin de support étendu 2032). Java 17 si Android n'est pas une cible.

### 1.4 Qualité des tests (Spock/Groovy)

**Framework :** Spock 2.0 + Groovy 3.0.9

**Couverture par composant :**

| Composant | Tests | Couverture |
|-----------|-------|-----------|
| Messages RFB (encode/decode) | 18 specs | **Bonne** — chaque message a son test |
| Auth handlers | 3 specs | **Bonne** — NONE, VNC, MS-Logon II testés |
| Handshaking | 2 specs | **Correcte** — version + security negotiation |
| PixelDecoder | 1 spec | **Minimale** — true color + indexed |
| Renderers (RAW, RRE, Hextile, ZLib, CopyRect) | **0 specs** | **Absente** — aucun test de rendu |
| Framebuffer | **0 specs** | **Absente** — processUpdate, resize, paint non testés |
| VernacularClient | **0 specs** | **Absente** — cycle de vie non testé |
| ServerEventHandler / ClientEventHandler | **0 specs** | **Absente** — event loops non testées |
| Thread safety | **0 specs** | **Absente** |

**Verdict :** Tests de sérialisation/désérialisation corrects. **Zéro test d'intégration, zéro test de rendu, zéro test de concurrence.** La couverture réelle est estimée à ~30%.

---

## 2. Encodings RFB : supportés vs manquants

### 2.1 Encodings supportés

| Encoding | Code | Implémentation | État |
|----------|------|---------------|------|
| **RAW** | 0 | `RawRenderer` | Complet, pixel par pixel via `setRGB()` |
| **COPYRECT** | 1 | `CopyRectRenderer` | Complet |
| **RRE** | 2 | `RRERenderer` | Complet |
| **HEXTILE** | 5 | `HextileRenderer` | Complet, tiles 16×16, 5 sub-encodings |
| **ZLIB** | 6 | `ZLibRenderer` | Complet, délègue à RAW après inflate |

### 2.2 Pseudo-encodings supportés

| Pseudo-encoding | Code | État |
|-----------------|------|------|
| **DESKTOP_SIZE** | -223 | Complet — resize framebuffer |
| **CURSOR** | -239 | Complet — image curseur + bitmask transparence |
| **EXTENDED_CLIPBOARD** | 0xC0A1E5CE | Partiel — négocié, envoi client OK, réception serveur OK, mais pas de gestion REQUEST/PEEK/NOTIFY côté serveur |

### 2.3 Encodings manquants

| Encoding | Code | Complexité | Charge estimée | Notes |
|----------|------|-----------|---------------|-------|
| **TIGHT** | 7 | **Élevée** | **3–4 semaines** | Compression mixte (zlib + filtres + JPEG). Standard de facto pour TightVNC/TigerVNC. Prioritaire pour la performance. Nécessite gestion de 4 flux zlib indépendants + décodeur JPEG intégré |
| **ZRLE** | 16 | **Moyenne** | **1.5–2 semaines** | Zlib + Run-Length. Plus simple que TIGHT. Base sur ZLib existant + palette + RLE. Utilisé par défaut dans RealVNC |
| **ZSTD** | — | **Moyenne** | **1 semaine** | Extension non-standard (certains serveurs modernes). Remplacement drop-in de ZLIB avec meilleur ratio compression/CPU. Nécessite lib native (jni-zstd) |
| **H.264** | 50 | **Très élevée** | **4–6 semaines** | Décodage vidéo. Nécessite binding FFmpeg ou lib Java pure. Complexe mais crucial pour sessions haute résolution / haute fréquence |

---

## 3. Authentification : supportée vs manquante

### 3.1 Supportée

| Type | Code | Handler | Détails |
|------|------|---------|---------|
| **None** | 1 | `NoSecurityHandler` | Pas d'auth |
| **VNC Auth** | 2 | `VncAuthenticationHandler` | Challenge DES, mot de passe 8 chars max |
| **MS-Logon II** | 113 | `MsLogon2AuthenticationHandler` | Diffie-Hellman (31 bits) + DES-CBC |

**Priorité de sélection :** NONE > VNC > MS_LOGON_2 (codé en dur dans `SecurityTypeNegotiator`)

### 3.2 Manquante

| Type | Code(s) | Complexité | Charge estimée | Impact |
|------|---------|-----------|---------------|--------|
| **TLS / VeNCrypt** | 18, 19 | **Moyenne** | **2–3 semaines** | Wrapping SSL/TLS autour du socket, puis sous-négociation auth. Utilise `javax.net.ssl.SSLSocket`. **Critique** pour tout usage en réseau non-local |
| **ARD (Apple Remote Desktop)** | 30 | **Élevée** | **2–3 semaines** | Authentification propriétaire Apple avec Diffie-Hellman 2048 bits + AES-128. Nécessaire pour se connecter à macOS |
| **NLA (Network Level Auth / CredSSP)** | N/A | **Très élevée** | **4–6 semaines** | Spécifique Windows/RDP, pas standard RFB. Nécessite implémentation CredSSP (NTLM/Kerberos + TLS). Pertinent uniquement si cible Windows RDP-VNC |

### 3.3 Problèmes de sécurité

- **Aucun chiffrement du flux de données** — seul l'échange d'auth est protégé
- **DES est obsolète** — clé effective de 56 bits
- **DH 31 bits (MS-Logon II)** — trivialement cassable
- **Mots de passe limités à 8 caractères** en VNC Auth (limitation du protocole)

---

## 4. Dette technique

### 4.1 Thread safety — CRITIQUE

| Problème | Fichier | Lignes | Sévérité |
|----------|---------|--------|---------|
| `frame` (BufferedImage) non protégé — écrit par ServerEventHandler, lu par paint() | `Framebuffer.java` | 33, 59, 91–92 | **Critique** |
| `mouseX`, `mouseY` non volatile — écrits dans `moveMouse()`, lus dans `requestFramebufferUpdate()` | `ClientEventHandler.java` | 35–36, 90–92, 139–140 | **Élevé** |
| `lastFramebufferUpdateRequestTime` non volatile — même problème de visibilité | `ClientEventHandler.java` | 38, 125, 143 | **Élevé** |
| `protocolVersion`, `serverInit`, `pixelFormat` — setters/getters sans synchro | `VncSession.java` | 19–21, 48–70 | **Moyen** (écrit une seule fois à l'init, mais pas de happens-before garanti) |
| `lastFrame` dans VernacularViewer non volatile | `VernacularViewer.java` | 50 | **Moyen** |

### 4.2 Fuites de ressources

| Problème | Fichier | Lignes | Sévérité |
|----------|---------|--------|---------|
| `Graphics2D` jamais `dispose()` après `getGraphics()` | `HextileRenderer.java`, `RRERenderer.java`, `CopyRectRenderer.java`, `Framebuffer.java` | multiples | **Élevé** — fuite de ressources natives |
| `Inflater` jamais `end()` dans ZLibRenderer — fuite mémoire native | `ZLibRenderer.java` | 19 | **Élevé** |
| Socket non fermé si exception entre `new Socket()` et `createSession()` | `VernacularClient.java` | 52 | **Moyen** |
| Buffer 20 Mo alloué à chaque réception clipboard étendu | `ServerCutText.java` | 61 | **Moyen** — pression mémoire inutile |

### 4.3 Gestion des erreurs

| Problème | Fichier | Lignes | Sévérité |
|----------|---------|--------|---------|
| Exceptions avalées silencieusement (`catch ignored`) | `VncSession.java:113,117`, `ServerEventHandler.java:91`, `ClientEventHandler.java:81` | multiples | **Moyen** |
| `RuntimeException` lancée au lieu d'une exception typée | `ServerCutText.java` | 65 | **Moyen** |
| `eventLoop.join(1000)` — si le thread ne s'arrête pas en 1s, il est abandonné sans kill | `ServerEventHandler.java` | 89 | **Moyen** |
| Pas de timeout sur la connexion socket | `VernacularClient.java` | 52 | **Moyen** |

### 4.4 Code mort / patterns obsolètes

- **Aucun code mort** détecté
- **Aucun TODO/FIXME**
- **Travis CI** (`.travis.yml`) — Travis CI est effectivement abandonné, migrer vers GitHub Actions
- **Pas de `module-info.java`** — pas de support JPMS

---

## 5. Limitations fonctionnelles

### 5.1 Clipboard étendu — état réel

| Fonctionnalité | Client → Serveur | Serveur → Client |
|---------------|-----------------|-----------------|
| **Texte standard** (ISO-8859-1) | OK | OK |
| **Texte étendu** (UTF-8 compressé) | OK — `ClientCutTextExtendedClipboard` | OK — `ServerCutText.decodeExtendedMessageFormat()` |
| **Caps negotiation** | OK — `ClientCutTextCaps` | Partiel — reçu mais non exploité |
| **REQUEST** | Non implémenté | Non implémenté |
| **PEEK** | Non implémenté | Non implémenté |
| **NOTIFY** | Non implémenté | Non implémenté |
| **RTF / HTML / DIB / FILES** | Flags définis dans `MessageHeaderFlags` mais **jamais utilisés** | Non implémenté |

**Verdict :** Le clipboard étendu fonctionne pour du texte simple UTF-8. Les formats riches (RTF, HTML, fichiers) sont déclarés dans l'enum mais jamais implémentés.

### 5.2 Performance du rendu framebuffer

| Goulot d'étranglement | Impact | Fichier |
|-----------------------|--------|---------|
| `setRGB()` appelé pixel par pixel dans RawRenderer | **Critique** — ~2M appels pour un écran 1080p | `RawRenderer.java:34` |
| `new Color(r,g,b).getRGB()` crée un objet Color par pixel | **Élevé** — pression GC massive | `RawRenderer.java:34` |
| `frame.copyData(null)` copie tout le framebuffer à chaque paint() | **Élevé** — ~8 Mo copiés 30x/s pour du 1080p | `Framebuffer.java:73` |
| `getSubimage().copyData()` + `drawImage()` dans CopyRect — double copie | **Moyen** | `CopyRectRenderer.java:24–25` |
| Lecture séquentielle byte par byte dans PixelDecoder | **Moyen** — I/O overhead | `PixelDecoder.java:25–28` |
| Pas de dirty rectangle tracking — copie complète même pour 1 pixel changé | **Élevé** | `Framebuffer.java:69–76` |

**Estimation performance actuelle :** Utilisable en 8-bit indexed sur petits écrans. **Inutilisable en 24-bit 1080p à 30 FPS** — le rendering monopoliserait le CPU.

### 5.3 Compatibilité serveurs VNC

| Version RFB | Support | Notes |
|-------------|---------|-------|
| **3.3** | OK | Security type envoyé comme entier unique par le serveur |
| **3.7** | OK | Liste de security types, pas de message d'erreur auth |
| **3.8** | OK | Liste de security types + message d'erreur auth détaillé |
| **< 3.3** | Rejeté | `UnsupportedProtocolVersionException` |

**Compatibilité serveurs testable :**

| Serveur | Attendu |
|---------|---------|
| TightVNC | OK (VNC Auth + RAW/Hextile/CopyRect) |
| RealVNC | Partiel — pas de ZRLE (encoding par défaut de RealVNC) |
| TigerVNC | Partiel — pas de TIGHT (encoding par défaut) |
| macOS Screen Sharing | **KO** — nécessite ARD auth |
| Windows RDP via VNC proxy | Dépend du proxy VNC utilisé |
| LibVNCServer | OK |
| noVNC/websockify | **KO** — pas de WebSocket transport |

---

## 6. Ce qui manque pour un usage OculiX

OculiX automatise des interfaces graphiques via VNC. Voici les blocages identifiés :

### 6.1 Blocages critiques

| Blocage | Impact OculiX | Charge estimée |
|---------|--------------|---------------|
| **Performance rendering** — `setRGB()` pixel par pixel, copie complète à chaque frame | Latence inacceptable pour de l'automatisation temps réel en haute résolution | **2 semaines** — réécrire le pipeline avec `WritableRaster.setPixels()` / `DataBufferInt`, dirty rectangles |
| **Pas de TIGHT encoding** | Incompatible avec TigerVNC (serveur le plus courant en entreprise) | **3–4 semaines** |
| **Pas de ZRLE encoding** | Incompatible avec RealVNC | **1.5–2 semaines** |
| **Pas de TLS/VeNCrypt** | Impossible de se connecter à des serveurs sécurisés | **2–3 semaines** |
| **Thread safety** — race conditions sur le framebuffer | Crashes aléatoires en production, corruption d'image | **1 semaine** |

### 6.2 Blocages importants

| Blocage | Impact OculiX | Charge estimée |
|---------|--------------|---------------|
| **Pas d'API programmatique propre** — `VernacularClient` mélange Swing (KeyEvent AWT) et logique client | Couplage inutile avec AWT pour un usage headless | **1 semaine** — extraire une API headless |
| **Pas de reconnexion automatique** | Un drop réseau tue la session sans retry | **3 jours** |
| **Pas de timeout configurable** sur connect/read | Blocage infini si le serveur ne répond plus | **2 jours** |
| **Socket non exposé** — impossible de configurer TCP keepalive, timeout, buffer size | Pas de contrôle réseau fin | **2 jours** |
| **Fuites mémoire** (Inflater, Graphics2D) | Dégradation en usage longue durée (automatisation 24/7) | **2 jours** |
| **Clipboard étendu incomplet** | Limitation pour l'automatisation impliquant du copier/coller riche | **1 semaine** |

### 6.3 Améliorations souhaitables

| Amélioration | Impact OculiX | Charge estimée |
|-------------|--------------|---------------|
| **Screenshot API** — obtenir le framebuffer courant sans callback Consumer | Simplifie l'intégration pour OCR / comparaison d'images | **2 jours** |
| **Région d'intérêt (ROI)** — demander des updates partiels | Réduit la bande passante pour surveiller une zone précise | **3 jours** |
| **Event batching** — envoyer plusieurs events en une seule écriture réseau | Réduit la latence pour des séquences rapides clavier/souris | **3 jours** |
| **Métriques** — FPS réel, latence, bande passante | Monitoring en production | **3 jours** |
| **Support WebSocket** | Connexion via noVNC/websockify | **1 semaine** |

---

## Synthèse des charges estimées

| Catégorie | Charge totale estimée |
|-----------|---------------------|
| Corrections thread safety + fuites ressources | **1.5 semaines** |
| Réécriture pipeline rendering (performance) | **2 semaines** |
| Encoding TIGHT | **3–4 semaines** |
| Encoding ZRLE | **1.5–2 semaines** |
| TLS / VeNCrypt | **2–3 semaines** |
| API headless + reconnexion + timeouts | **2 semaines** |
| Tests (renderers, intégration, concurrence) | **2 semaines** |
| **Total MVP OculiX-ready** | **~14–17 semaines** (1 développeur) |

---

## Priorités recommandées

1. **P0 — Thread safety + fuites ressources** (pré-requis à tout le reste)
2. **P0 — Performance rendering** (sans ça, inutilisable en production)
3. **P1 — TIGHT encoding** (compatibilité TigerVNC)
4. **P1 — TLS/VeNCrypt** (sécurité réseau)
5. **P2 — ZRLE encoding** (compatibilité RealVNC)
6. **P2 — API headless + reconnexion**
7. **P3 — Tests d'intégration**
8. **P3 — Encodings avancés (H.264, ZSTD)**
