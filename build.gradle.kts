plugins {
	id("java")
	id("application")
    id("io.github.jwharm.flatpak-gradle-generator") version "1.5.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

group = "ca.footeware.java"
version = "1.0.0"

repositories {
	mavenCentral()
	gradlePluginPortal()
	maven { url=uri("build/repository") } // used by flatpak-builder
}

dependencies {
    implementation("io.github.jwharm.javagi:adw:0.11.2")
}

application {
    mainClass = "texty3.Main"
    applicationDefaultJvmArgs += "--enable-native-access=ALL-UNNAMED"
    applicationDefaultJvmArgs += "-Djava.library.path=/usr/lib64:/lib64:/lib:/usr/lib:/lib/x86_64-linux-gnu"
}

tasks.register("compileResources") {
    exec {
        workingDir("src/main/resources/")
        commandLine("glib-compile-resources", "texty3.gresource.xml")
    }
}

tasks.named("compileJava") {
    dependsOn("compileResources")
}

tasks.register<org.gradle.jvm.tasks.Jar>("uberJar") {
    archiveClassifier = "uber"

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    
    manifest {
		attributes["Main-Class"] = "texty3.Main"
	}
}

tasks.named<org.gradle.jvm.tasks.Jar>("uberJar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// Task to generate a file with all dependency urls for the offline flatpak build
tasks.flatpakGradleGenerator {
    outputFile.set(file("$rootDir/flatpak/maven-dependencies.json"))
    downloadDirectory.set("build/repository")
}

//tasks.named("installDist") {
//    destinationDirectory = file("/opt/texty3")
//}
