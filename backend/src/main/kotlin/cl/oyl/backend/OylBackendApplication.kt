package cl.oyl.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class OylBackendApplication

fun main(args: Array<String>) {
    runApplication<OylBackendApplication>(*args)
}
