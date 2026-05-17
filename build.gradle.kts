plugins {
    id("com.diffplug.spotless") version "8.5.1"
    id("org.sonarqube") version "7.2.3.7755"
}

group = "com.github.jgold5"
version = "1.0-SNAPSHOT"

sonar {
    properties {
        property("sonar.projectKey", "jgold5_tempest")
        property("sonar.organization", "jgold5")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "tempest-core/build/reports/jacoco/test/jacocoTestReport.xml"
        )
        property("sonar.java.binaries", "tempest-core/build/classes/java/main")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
    }

    spotless {
        java {
            googleJavaFormat()
        }
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {}

    tasks.named("check") {
        dependsOn(tasks.named("jacocoTestCoverageVerification"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        reports {
            xml.required = true
            html.required = true
        }
    }
}