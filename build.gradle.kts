plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "be.ucclesport"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("be.ucclesport.rus.MembershipsKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.helger.ubl:ph-ubl21:7.0.0")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:3.0.0")
    implementation("com.sun.xml.bind:jaxb-impl:3.0.0")

    implementation("ch.qos.logback:logback-core:1.4.7")
    implementation("org.slf4j:slf4j-api:2.0.5")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.20-RC")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}
