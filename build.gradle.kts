plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
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
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("net.strokkur.commands:annotations-paper:2.1.4")
    annotationProcessor("net.strokkur.commands:processor-paper:2.1.4")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G")
        downloadPlugins {
            url("https://download.luckperms.net/1658/bukkit/loader/LuckPerms-Bukkit-5.5.71.jar")
        }
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
