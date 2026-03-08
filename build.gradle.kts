plugins {
    java
    jacoco
    id("com.diffplug.spotless") version "6.23.0"
}

group = "com.github.jgold5"
version = "1.0-SNAPSHOT"

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