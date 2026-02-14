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
        maven("https://packages.jetbrains.team/maven/p/koog/maven")
        google()
        mavenCentral()
    }
}

rootProject.name = "Chiiugo"
include(":app")
