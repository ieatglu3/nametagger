plugins {
  id("java")
  id("maven-publish")
}

var projectGroup = "com.github.ieatglu3"
var projectVersion = "1.0.0"
var projectArtifactId = "nametagger"

group = projectGroup
version = projectVersion

repositories {
  mavenCentral()
  maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
  compileOnly("com.github.retrooper:packetevents-api:2.11.1")
  compileOnly("net.kyori:adventure-api:4.26.1")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
  sourceCompatibility = JavaVersion.toVersion(21)
}

tasks {
  withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 10
  }
  jar {
    archiveBaseName = projectArtifactId
    version = project.version
  }
  test {
    useJUnitPlatform()
  }
}