# Minecraft 26.2 and 26.3

This independent Gradle build produces Fabric and NeoForge versions of ImagineBook.
It uses Java 25 and the unobfuscated Minecraft APIs. The original Stonecutter build
at the repository root still handles Minecraft 1.20–1.21.

From this directory, with a JDK 25 installation selected:

```powershell
.\gradlew.bat collectJars
```

On Linux/macOS, use `bash ./gradlew collectJars`.
The four installable JARs are collected in `../build/libs/26.x/`.
Individual builds use `:26.2-fabric:build`, `:26.3-fabric:build`,
`:26.2-neoforge:build`, or `:26.3-neoforge:build`.
Each target also produces a sources JAR in its own `build/libs/` directory.

Fabric requires Fabric Loader 0.20.1 or newer and Fabric API for the matching game
version. NeoForge builds use 26.2.0.93 and 26.3.0.100 respectively.
Install only the JAR matching both your game version and loader. The mod runs on
the client; a server installation is unnecessary. WebP ImageIO is bundled.

The shared port is in `src/main/java`; loader entrypoints and metadata are in
`src/fabric` and `src/neoforge`. The small rendering adapters in `src/26.2` and
`src/26.3` handle the rendering backend's package changes. Textures and translations come from the original
`../src/main/resources/assets` directory.

Pasting into the book's text area now replaces the selection normally and flows
overflow onto new pages, preserving existing later pages, whitespace, and Unicode
characters. It respects vanilla's 1024-character/14-line page limits and 100-page
book limit. If the entire paste cannot fit, the book is left unchanged and an
error is shown. Pasting into the image URL and numeric fields keeps their usual
behavior. Pagination tests run as part of every target's `build` task.

Validate that the book, text-field, and rendering mixins transform against each vanilla game:

```powershell
.\gradlew.bat :26.2-fabric:runMixinSmoke :26.3-fabric:runMixinSmoke -x downloadAssets
```

The development-only smoke mod exits before Minecraft opens a window and prints
`IMAGINEBOOK_SMOKE_PASSED`. It also checks the image pipeline allows mirrored images.
This checks Mixin transformation, not interactive
rendering or network image downloads. It is excluded from installable JARs.
