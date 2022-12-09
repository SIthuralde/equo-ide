# <image align="left" src="../.github/equo_logo.svg"> Equo IDE for Gradle

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.equo.ide?color=blue&label=gradle%20plugin%20portal)](https://plugins.gradle.org/plugin/dev.equo.ide)
[![Changelog](https://img.shields.io/badge/changelog-here-blue)](CHANGELOG.md)
[![Javadoc](https://img.shields.io/badge/javadoc-here-blue)](https://javadoc.io/doc/dev.equo.ide/equo-ide-gradle-plugin)

**PUBLIC BETA! Try it out, but it's not production ready yet. [Join our mailing list](https://equo.dev/ide) to be notified when it's ready.**

- a build plugin for Gradle and Maven
- downloads, configures, and launches an instance of the Eclipse IDE
- ensures that all of your devs have a zero-effort and perfectly repeatable IDE setup process

Use it like this with `gradlew equoIde`

```gradle
plugins {
  id 'dev.equo.ide' version '{{ latest version at top of page }}'
}
equoIde { // launch with gradlew equoIde
  release '4.26'
}
```

## Limitations

- Java 11+
- Gradle 6.0+
- Eclipse JDT 2022-09 (4.25) or later, though it might work on earlier versions too.