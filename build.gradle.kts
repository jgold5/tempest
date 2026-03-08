plugins {
    java
    jacoco
    id("com.diffplug.spotless") version "6.23.0"
    id("org.sonarqube") version "4.4.1.3373"
}

group = "com.github.jgold5"
version = "1.0-SNAPSHOT"

sonar {
    properties {
        property("sonar.projectKey", "jgold5_tempest")
        property("sonar.organization", "jgold5")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths",
            "tempest-core/build/reports/jacoco/test/jacocoTestReport.xml")
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

    tasks.test {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        violationRules {
            rule {
                limit {
                    minimum = "0.00".toBigDecimal()
                }
            }
        }
    }

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