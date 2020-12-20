plugins {
    id("org.jetbrains.kotlin.js") version "1.4.21"
}

group = "com.davidweitzenfeld"
version = "0.1.0"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.1")
    implementation(npm("gl-matrix", "3.3.0"))

    // Sourced from https://github.com/liorgonnen/kotlin-three-js-starter.
    implementation(project(":kt-decs:threejs_kt"))
    implementation(project(":kt-decs:statsjs_kt"))
}

kotlin {
    js {
        browser {
            webpackTask {
                cssSupport.enabled = true
            }

            runTask {
                cssSupport.enabled = true
            }

            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
        binaries.executable()
    }
}
