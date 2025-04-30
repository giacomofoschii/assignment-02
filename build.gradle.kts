plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.25.4")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.4")
    implementation("io.vertx:vertx-core:4.4.2")
    implementation("io.vertx:vertx-junit5:4.4.2")

    implementation("org.openjfx:javafx-base:21.0.2")
    implementation("org.openjfx:javafx-controls:21.0.2")
    implementation("org.openjfx:javafx-graphics:21.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}