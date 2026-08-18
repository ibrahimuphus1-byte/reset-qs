pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.lsposed.org/") }   // ✅ Official LSPosed repo
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.lsposed.org/") }   // ✅ Same repo for dependencies
    }
}
rootProject.name = "QS Columns & Restart Tile Module"
include(":app")
