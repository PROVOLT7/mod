plugins {
    java
}

group = "dev.vexvol"
version = "0.3.0-SNAPSHOT"

description = "VexVolMinigames - core VexVol mini-games and arena platform"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.onarandombox.com/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.130-stable")
    compileOnly("org.mvplugins.multiverse.core:multiverse-core:5.8.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
