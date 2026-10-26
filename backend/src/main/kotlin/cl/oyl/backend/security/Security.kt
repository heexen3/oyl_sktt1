package cl.oyl.backend.security

import cl.oyl.backend.domain.Role
import cl.oyl.backend.domain.UserAccount
import com.nimbusds.jose.jwk.source.ImmutableSecret
import java.time.Duration
import java.time.Instant
import javax.crypto.spec.SecretKeySpec
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.stereotype.Component

@ConfigurationProperties("oyl.jwt")
data class JwtProperties(val secret: String, val ttlHours: Long = 12)

@Configuration
@EnableMethodSecurity
class SecurityConfig(private val props: JwtProperties) {
    private val key get() = SecretKeySpec(props.secret.toByteArray(), "HmacSHA256")

    init { require(props.secret.length >= 32) { "oyl.jwt.secret debe tener al menos 32 caracteres" } }

    @Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
    @Bean fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(key))
    @Bean fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { h -> h.frameOptions { it.sameOrigin() } } // consola H2
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/api/v1/auth/**", "/h2-console/**", "/actuator/health").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/stores/**", "/uploads/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMINISTRADOR")
                it.requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMINISTRADOR")
                it.requestMatchers(HttpMethod.PATCH, "/api/v1/products/**", "/api/v1/orders/*/status").hasRole("ADMINISTRADOR")
                it.requestMatchers("/api/v1/inventory/**").hasRole("ADMINISTRADOR")
                it.anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .oauth2ResourceServer { rs -> rs.jwt { it.jwtAuthenticationConverter(::toAuthentication) } }
        return http.build()
    }

    private fun toAuthentication(jwt: Jwt): AbstractAuthenticationToken {
        val role = jwt.getClaimAsString("role")
        return JwtAuthenticationToken(jwt, listOfNotNull(role?.let { SimpleGrantedAuthority("ROLE_$it") }), jwt.subject!!)
    }
}

@Component
class TokenService(private val encoder: JwtEncoder, private val props: JwtProperties) {
    fun issue(user: UserAccount): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer("oyl-backend")
            .subject(user.id.toString())
            .issuedAt(now)
            .expiresAt(now.plus(Duration.ofHours(props.ttlHours)))
            .claim("role", user.role.name)
            .claim("alias", user.alias)
            .build()
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).tokenValue
    }
}

/** Usuario autenticado del request actual (id + rol tomados del JWT). */
data class CurrentUser(val id: Long, val role: Role) {
    val isAdmin get() = role == Role.ADMINISTRADOR

    companion object {
        fun get(): CurrentUser {
            val auth = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
            return CurrentUser(auth.token.subject!!.toLong(), Role.valueOf(auth.token.getClaimAsString("role")!!))
        }
    }
}
