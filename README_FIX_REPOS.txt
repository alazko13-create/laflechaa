Cambios aplicados para compilar en GitHub Actions con menos fallos:

1. Se eliminaron repositorios inseguros e inestables (http, mirrors Nexus externos, jcenter).
2. Se dejaron solo repositorios estables:
   - google()
   - mavenCentral()
3. Se ajustó GitHub Actions para usar Java 11 en lugar de Java 17,
   lo cual es más compatible con:
   - Android Gradle Plugin 4.2.2
   - Gradle 6.7.1
4. El workflow sigue generando el APK Debug y lo sube como artifact.

Ruta esperada del APK:
app/build/outputs/apk/debug/app-debug.apk
