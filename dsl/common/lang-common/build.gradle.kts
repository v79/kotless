import tanvd.kosogor.proxy.publishJar

group = rootProject.group
version = rootProject.version


plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.50" apply true
}

dependencies {
    api("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", "0.13.0")

    api(project(":model"))

    api("com.amazonaws", "aws-lambda-java-core", "1.2.0")

    api("org.slf4j", "slf4j-log4j12", "1.7.25")
    implementation("log4j", "log4j", "1.2.17")
    implementation("com.amazonaws", "aws-lambda-java-log4j", "1.0.0")
}

publishJar {
    bintray {
        username = "tanvd"
        repository = "io.kotless"
        info {
            description = "Kotless Lang Common"
            githubRepo = "https://github.com/JetBrains/kotless"
            vcsUrl = "https://github.com/JetBrains/kotless"
            labels.addAll(listOf("kotlin", "serverless", "web", "devops", "faas", "lambda"))
        }
    }
}

