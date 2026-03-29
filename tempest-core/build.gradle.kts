dependencies {
    implementation(libs.hdrhistogram)
    implementation(libs.okhttp)
    implementation(libs.logback)
    testImplementation(libs.assertj)
    testImplementation(libs.wiremock)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val coveredClasses = fileTree(layout.buildDirectory.dir("classes/java/main")) {
    include(
        "com/github/jgold5/tempest/core/http/HttpRequestExecutor.class",
        "com/github/jgold5/tempest/core/metrics/MetricsRecorder.class",
        "com/github/jgold5/tempest/core/ratelimit/TokenBucket.class",
        "com/github/jgold5/tempest/core/LoadGenerator.class",
    )
}

tasks.named<JacocoReport>("jacocoTestReport") {
    classDirectories.setFrom(coveredClasses)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    classDirectories.setFrom(coveredClasses)
    violationRules {
        rule {
            limit {
                minimum = "0.30".toBigDecimal()
            }
        }
    }
}