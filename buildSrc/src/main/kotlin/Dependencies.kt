/*
 * Copyright 2019-2022 Chair of Geoinformatics, Technical University of Munich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


object DependencyVersions {

    // standard libraries
    const val kotlin = "1.6.10"
    const val coroutines = "1.6.0"
    const val arrow = "1.0.1"
    const val result = "4.0.0"

    // testing
    const val junit = "5.8.2"
    const val assertj = "3.22.0"
    const val mockk = "1.12.2"

    // logging
    const val kotlinLogging = "2.1.21"
    const val slf4jSimple = "1.7.33"

    // documentation
    const val orchid = "0.21.1"

    // object creation
    const val jakartaActivationApi = "2.0.1"
    const val jakartaXmlBindApi = "3.0.1"
    const val jaxb = "3.0.2"

    // object mapping
    const val mapstruct = "1.4.2.Final"

    // io
    const val clikt = "3.4.0"
    const val mordant = "1.2.1"
    const val commonsIO = "2.11.0"
    const val commonsCSV = "1.9.0"
    const val commonsLang = "3.12.0"
    const val emojiJava = "5.1.1"

    // math
    const val guava = "31.0.1-jre"
    const val commonsMath = "3.6.1"
    const val joml = "1.10.3"
    const val poly2tri = "0.1.2"

    // geo
    const val proj4 = "1.1.4"
    const val citygml4j = "eacbbc4954"
}

object Dependencies {
    // standard libraries
    const val kotlinStandardLibrary = "stdlib-jdk8:${DependencyVersions.kotlin}"
    const val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${DependencyVersions.coroutines}"
    const val arrowCore = "io.arrow-kt:arrow-core:${DependencyVersions.arrow}"
    const val result = "com.github.kittinunf.result:result:${DependencyVersions.result}"

    // testing
    const val junit = "org.junit.jupiter:junit-jupiter:${DependencyVersions.junit}"
    const val assertj = "org.assertj:assertj-core:${DependencyVersions.assertj}"
    const val mockk = "io.mockk:mockk:${DependencyVersions.mockk}"

    // logging
    const val kotlinLogging = "io.github.microutils:kotlin-logging:${DependencyVersions.kotlinLogging}"
    const val slf4jSimple = "org.slf4j:slf4j-simple:${DependencyVersions.slf4jSimple}"

    // object creation
    const val jakartaActivationApi = "jakarta.activation:jakarta.activation-api:${DependencyVersions.jakartaActivationApi}"
    const val jakartaXmlBindApi = "jakarta.xml.bind:jakarta.xml.bind-api:${DependencyVersions.jakartaXmlBindApi}"
    const val sunJaxbImpl = "com.sun.xml.bind:jaxb-impl:${DependencyVersions.jaxb}"
    const val jaxbRuntime = "org.glassfish.jaxb:jaxb-runtime:${DependencyVersions.jaxb}"
    const val jaxbXjc = "org.glassfish.jaxb:jaxb-xjc:${DependencyVersions.jaxb}"

    // object mapping
    const val mapstruct = "org.mapstruct:mapstruct:${DependencyVersions.mapstruct}"
    const val mapstructProcessor = "org.mapstruct:mapstruct-processor:${DependencyVersions.mapstruct}"

    // io
    const val clikt = "com.github.ajalt.clikt:clikt:${DependencyVersions.clikt}"
    const val mordant = "com.github.ajalt:mordant:${DependencyVersions.mordant}"
    const val commonsIO = "commons-io:commons-io:${DependencyVersions.commonsIO}"
    const val commonsCSV = "org.apache.commons:commons-csv:${DependencyVersions.commonsCSV}"
    const val commonsLang = "org.apache.commons:commons-lang3:${DependencyVersions.commonsLang}"
    const val emojiJava = "com.vdurmont:emoji-java:${DependencyVersions.emojiJava}"

    // math
    const val guava = "com.google.guava:guava:${DependencyVersions.guava}"
    const val commonsMath = "org.apache.commons:commons-math3:${DependencyVersions.commonsMath}"
    const val joml = "org.joml:joml:${DependencyVersions.joml}"
    const val poly2tri = "org.orbisgis:poly2tri-core:${DependencyVersions.poly2tri}"

    // geo
    const val proj4 = "org.locationtech.proj4j:proj4j:${DependencyVersions.proj4}"
    const val citygml4j = "com.github.citygml4j:citygml4j:${DependencyVersions.citygml4j}"
}
