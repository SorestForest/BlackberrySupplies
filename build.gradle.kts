plugins {
    id("java")
}

group = "ru.SorestForest"
version = "1.0-SNAPSHOT"

tasks.withType<ProcessResources> {
    filteringCharset = "UTF-8"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
tasks.withType<Test> {
    systemProperty("file.encoding","UTF-8")
}


repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("net.dv8tion:JDA:6.0.0-preview_DEV")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}