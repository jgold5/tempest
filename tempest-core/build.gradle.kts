dependencies {
    implementation(libs.hdrhistogram)
    implementation(libs.okhttp)
    testImplementation(libs.assertj)
    testImplementation(libs.logback)
    testImplementation(libs.wiremock)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val coveredClasses = fileTree(layout.buildDirectory.dir("classes/java/main")) {
    include(
        "com/github/jgold5/tempest/core/http/HttpRequestExecutor.class",
    )
}

tasks.named<JacocoReport>("jacocoTestReport") {
    classDirectories.setFrom(coveredClasses)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            limit {
                minimum = "0.30".toBigDecimal()
            }
        }
    }
}
