plugins {
  id("java")
  id("com.gradleup.shadow") version "9.2.2"
}

var jarName = "nametagger-spigot"

group = "com.github.ieatglu3"
version = "1.0.0"

repositories {
  mavenCentral()
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
  maven("https://jitpack.io")
}

dependencies {
  implementation(project(":api"))
  implementation(project(":platform-util"))
  implementation("com.github.ieatglu3:bukec:v1.1.0")
  compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") {
    exclude(group = "net.md-5", module = "bungeecord-chat")
  }
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

  shadowJar {
    archiveBaseName = jarName
    version = project.version
    archiveClassifier = "shaded"
  }

  jar {
    archiveBaseName = jarName
    version = project.version
    dependsOn(shadowJar)
  }

  test {
    useJUnitPlatform()
  }

}