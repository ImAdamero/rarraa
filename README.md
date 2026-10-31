# EasyBot (Fabric 1.21.11)

Zrodla odzyskane z jara, przemapowane na Yarn 1.21.11.
Dodane: Auto login, Auto AFK, motywy GUI.

## Budowanie lokalnie (Windows)
1. Zainstaluj JDK 21 lub nowszy (np. Temurin).
2. W folderze projektu: `gradlew.bat build`
3. Jar: `build\libs\EasyBot-1.0.0.jar` (bez `-sources`)
4. Do folderu mods wrzuc jara + Fabric API 0.141.6+1.21.11

## Budowanie na GitHubie
Wrzuc ZAWARTOSC folderu do repo (razem z folderem .github). Build ruszy sam,
jar bedzie w Actions -> Artifacts.

Jesli sa bledy kompilacji, wklej je do Claude'a (linie z "error:" i "symbol:").
