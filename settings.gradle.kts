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

        // Meta Wearables Device Access Toolkit — hosted on GitHub Packages.
        // Requires a GitHub personal access token (classic) with read:packages scope.
        // Set GITHUB_TOKEN in your environment or add github_token=<token> to local.properties.
        maven {
            url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
            credentials {
                val localProps = java.util.Properties().also { props ->
                    val f = file("local.properties")
                    if (f.exists()) props.load(f.inputStream())
                }
                username = localProps.getProperty("github_username")
                    ?: System.getenv("GITHUB_USERNAME") ?: ""
                password = localProps.getProperty("github_token")
                    ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}

rootProject.name = "MetaAR"
include(":app")
