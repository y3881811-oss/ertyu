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
        // 新增：用于拉取 APKEditor 等托管在 JitPack 的库
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MTFM"
include(":app")