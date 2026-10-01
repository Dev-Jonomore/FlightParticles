plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven {
        name = "eldonexus"
        url = uri("https://eldonexus.de/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3-pre-2.build.0-alpha")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("net.strokkur.commands:annotations-paper:2.3.1")
    annotationProcessor("net.strokkur.commands:processor-paper:2.3.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        minecraftVersion("26.3")
        jvmArgs("-Xms2G", "-Xmx2G")
        downloadPlugins {
            url("https://download.luckperms.net/1672/bukkit/loader/LuckPerms-Bukkit-5.5.85.jar")
        }
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
