pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SfmScanner"

include(":app")

include(":core:core-common")
include(":core:core-ui")
include(":core:core-storage")

include(":features:feature-splash")
include(":features:feature-selection")
include(":features:feature-form")
include(":features:feature-scan")
include(":features:feature-upload")

include(":data:data-camera")
include(":data:data-ar")
include(":data:data-firebase")
