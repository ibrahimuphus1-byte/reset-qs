pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo.xposed.info/repo") }   // ✅ Adds Xposed API
    }
}
rootProject.name = "QS Columns & Restart Tile Module"
include(":app")
