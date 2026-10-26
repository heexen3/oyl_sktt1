package cl.oyl.backend.config

import java.nio.file.Path
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Sirve las fotos subidas en /uploads/... */
@Configuration
class WebConfig(@Value("\${oyl.uploads-dir}") private val uploadsDir: String) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(Path.of(uploadsDir).toAbsolutePath().toUri().toString())
    }
}
