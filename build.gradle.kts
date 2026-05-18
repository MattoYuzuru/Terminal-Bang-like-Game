plugins {
    application
}

group = "com.github.mattoyudzuru"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.github.mattoyudzuru.terminalbang.app.TerminalBangApplication"
}

dependencies {
    implementation("org.apache.sshd:sshd-core:2.17.1")
    implementation("org.apache.sshd:sshd-common:2.17.1")
    implementation("org.jline:jline:3.30.13")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-core:12.6.1")
    implementation("org.flywaydb:flyway-database-postgresql:12.6.1")
    implementation("org.postgresql:postgresql:42.7.8")
    implementation("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(platform("org.junit:junit-bom:5.14.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:4.0.0-M1")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}
