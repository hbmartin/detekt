plugins {
    module
}

dependencies {
    api(projects.detektApi)
    testImplementation(projects.detektTestUtils)
    testImplementation(testFixtures(projects.detektApi))
    testImplementation(libs.mockk)

    constraints {
        testImplementation("net.bytebuddy:byte-buddy:1.11.0") {
            because("version 1.10.14 (pulled in by mockk 1.11.0) is not Java 16 compatible")
        }
    }
}
