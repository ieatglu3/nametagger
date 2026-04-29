plugins {
  id("java")
}

var projectGroup = "com.github.ieatglu3.nametagger"
var projectVersion = "1.0.0"
var projectArtifactId = "nametagger-platform-util"

group = projectGroup
version = projectVersion

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":nametagger-api"))
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