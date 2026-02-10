import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.resourcefactory.paper.paperPluginYaml

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev").version("2.0.0-beta.19")
    id("xyz.jpenilla.resource-factory-bukkit-convention").version("1.3.0")
    // id("xyz.jpenilla.run-paper") version("3.0.0-beta.2")
    id("com.gradleup.shadow").version("9.1.0")
}

group = "com.gmail.takenokoii78"
version = "1.0-SNAPSHOT"
description = "ブロックID及びブロック状態を全列挙するデータパックを生成するためのプラグイン"

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")

    implementation(files(
        "../GenericPluginCore/build/libs/GenericPluginCore-1.0-SNAPSHOT.jar"
    ))
}

tasks {
    compileJava {
        options.release = 21
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
    }

    shadowJar {
        mergeServiceFiles()
    }

    withType<Jar>().configureEach {
        doLast {
            println("Jar file was generated at: ${archiveFile.get().asFile.absolutePath}")
        }
    }

    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-unchecked"))
    }
}

bukkitPluginYaml {
    name = project.name
    description = project.description
    version = "${project.version}"
    apiVersion = "1.21.11"
    authors.add("Takenoko-II")
    main = "com.gmail.takenokoii78.blockpropertyaccessor.BlockPropertyAccessor"
}

paperPluginYaml {
    name = project.name
    description = project.description
    version = "${project.version}"
    apiVersion = "1.21.11"
    authors.add("Takenoko-II")
    main = "com.gmail.takenokoii78.blockpropertyaccessor.BlockPropertyAccessor"
}
