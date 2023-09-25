import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "de.randombyte"
version = "2.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.illposed.osc:javaosc-core:0.8")
}

compose.desktop {
    application {
        mainClass = "de.randombyte.baustellalightcontrol.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "baustella-light-control"
            packageVersion = "2.0.0"
        }
    }
}
