plugins {
    application
    id("com.gradleup.shadow") version "9.4.1"
}

application {
    mainClass.set("com.github.jgold5.tempest.cli.TempestCommand")
}

dependencies {
    implementation(project(":tempest-core"))
    implementation(libs.picocli)
    implementation(libs.hdrhistogram)
}