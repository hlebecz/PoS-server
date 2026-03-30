plugins {
    java
    checkstyle
    id("com.diffplug.spotless") version "8.4.0"
}

group = "kurs.backend"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:7.2.5.Final")
    implementation("com.mysql:mysql-connector-j:9.6.0")
    implementation("org.postgresql:postgresql:42.7.3")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Auth
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.mindrot:jbcrypt:0.4")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

checkstyle {
    toolVersion = "10.18.2"
    configFile = rootProject.file("config/checkstyle/google_checks.xml")
}

spotless {
    java {
        googleJavaFormat("1.35.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        importOrder("java", "javax", "org", "com", "lombok")
        targetExclude("build/**")
    }

    format("gradle") {
        target("**/*.gradle", "**/*.gradle.kts")
        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }

    format("misc") {
        target("**/*.md", "**/*.properties", "**/*.yml", "**/*.yaml")
        trimTrailingWhitespace()
        indentWithSpaces(2)
        endWithNewline()
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "kurs.backend.server.Server"
    }
    from({
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.build {
    dependsOn(tasks.named("spotlessApply"))
}
