import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.4"
}

group = "com.kivojenko.plugin.scheduled"
version = "1.1.0"

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
        bundledPlugin("com.intellij.properties")
        bundledPlugin("org.jetbrains.plugins.yaml")
        // YAML needs JSON, which the test sandbox only loads when listed
        bundledPlugin("com.intellij.modules.json")
        // Optional: reads Quartz triggers from a data source in the Database tool window
        bundledPlugin("com.intellij.database")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
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

    runIde {
        // Opens the demo project: @Scheduled methods, Quartz jobs and crons from application.yml
        args(layout.projectDirectory.dir("src/test/demo").asFile.absolutePath)
    }
}


intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
        }
    }
}