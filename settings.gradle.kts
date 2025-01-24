rootProject.name = "texty3"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url=uri("build/repository") } // used by flatpak-builder
    }
}
