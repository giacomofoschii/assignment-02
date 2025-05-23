plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.swing")
}

application {
    mainClass.set("reactive.DependencyAnalyser")
    applicationDefaultJvmArgs = listOf(
        "--add-modules=javafx.controls,javafx.fxml,javafx.graphics",
        "--add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.swing",
        "--add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED",
        "--add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED",
        "--add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED"
    )
}

tasks.register<JavaExec>("runSimulationAnalyser") {
    group = "application"
    description = "Esegue la classe SimulationAnalyser"
    mainClass.set("asynchronous.SimulationAnalyser")
    classpath = sourceSets["main"].runtimeClasspath
}

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.25.4")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.4")
    implementation("io.vertx:vertx-core:4.4.2")

    implementation("org.graphstream:gs-core:2.0")
    implementation("org.graphstream:gs-ui-javafx:2.0")
    implementation("org.graphstream:gs-algo:2.0")

    implementation("io.reactivex.rxjava3:rxjava:3.1.6")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}