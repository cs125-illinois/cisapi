import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.4.10"
    kotlin("jvm") version kotlinVersion
    id("org.jmailen.kotlinter") version "3.2.0"
    id("com.github.ben-manes.versions") version "0.33.0"
    id("io.gitlab.arturbosch.detekt") version "1.14.1"
}
repositories {
    jcenter()
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.11.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")
    implementation("com.fasterxml.woodstox:woodstox-core:6.2.1")
    implementation("com.github.kittinunf.fuel:fuel:2.3.0")

    testImplementation("io.kotest:kotest-runner-junit5:4.2.6")
}
tasks.withType<KotlinCompile> {
    val javaVersion = JavaVersion.VERSION_1_8.toString()
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    kotlinOptions {
        jvmTarget = javaVersion
    }
}
tasks.test {
    useJUnitPlatform()
    enableAssertions = true
}
tasks.dependencyUpdates {
    resolutionStrategy {
        componentSelection {
            all {
                if (listOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea", "eap").any { qualifier ->
                    candidate.version.matches(Regex("(?i).*[.-]$qualifier[.\\d-+]*"))
                }) {
                    reject("Release candidate")
                }
            }
        }
    }
    gradleReleaseChannel = "current"
}

detekt {
    input = files("src/main/kotlin")
    buildUponDefaultConfig = true
}
tasks.check {
    dependsOn("detekt")
}
