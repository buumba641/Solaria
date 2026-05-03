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
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://maven.pkg.github.com/solana-mobile/mobile-wallet-adapter")
            content {
                includeGroup("com.solanamobile")
            }
        }
    }
}

rootProject.name = "Solaria"
include(":app")
