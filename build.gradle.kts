plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.4"
}

group = "com.kivojenko.plugin.scheduled"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2025.2.3")
        pluginVerifier()
        plugin("PsiViewer:252.23892.248")
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.modules.java")
    }

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("com.cronutils:cron-utils:9.2.1")
}


java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}


tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}


intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "231"
        }
    }
}