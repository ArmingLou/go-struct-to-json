plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "ind.arming"
version = "2.0.2"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2")
    type.set("GO")

    // required if Go language API is needed:
    plugins.set(listOf("org.jetbrains.plugins.go"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("203")
        untilBuild.set("241.*")
    }

    signPlugin {
//        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
//        privateKey.set(System.getenv("PRIVATE_KEY"))
        if (System.getenv("CERTIFICATE_CHAIN")!=null){
            certificateChainFile.set(file(System.getenv("CERTIFICATE_CHAIN")))
        }
        if(System.getenv("PRIVATE_KEY")!=null){
            privateKeyFile.set(file(System.getenv("PRIVATE_KEY")))
        }
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
