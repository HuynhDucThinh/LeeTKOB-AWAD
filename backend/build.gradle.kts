plugins {
	java
	checkstyle
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "8.11.0"
}

group = "com.leetkob"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

val mapstructVersion = "1.6.3"
val lombokMapstructBindingVersion = "0.2.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("com.fasterxml.jackson.core:jackson-databind")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("com.h2database:h2")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-database-postgresql")

	// MapStruct
	implementation("org.mapstruct:mapstruct:$mapstructVersion")

	// Annotation Processors
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")
	annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

	// JWT (JJWT)
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// Google OAuth verification
	implementation("com.google.api-client:google-api-client:2.7.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")
	testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
}

checkstyle {
	toolVersion = "10.21.1"
	configFile = file("checkstyle.xml")
	isIgnoreFailures = false
	maxWarnings = 0
}

spotless {
	lineEndings = com.diffplug.spotless.LineEnding.UNIX
	java {
		googleJavaFormat("1.24.0")
		removeUnusedImports()
	}
}

springBoot {
	mainClass.set("com.leetkob.LeetKobApplication")
}

tasks.withType<Test> {
	useJUnitPlatform()
	filter {
		isFailOnNoMatchingTests = true
	}
	testLogging {
		events("passed", "skipped", "failed")
	}
}
