package com.wasd.qlic.iam.infrastructure.tokens

import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.AccessToken
import com.wasd.qlic.iam.domain.services.TokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Infrastructure service: issues the access tokens (JWT signed with HS256)
 * that the API validates on every request to a protected resource.
 */
@Component
class JwtTokenProvider(
    private val jwtEncoder: JwtEncoder,
    @Value("\${qlic.security.jwt.expiration-minutes}") private val expirationMinutes: Long
) : TokenProvider {

    override fun issue(subscriber: Subscriber, account: Account): AccessToken {
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(Duration.ofMinutes(expirationMinutes))
        val claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(subscriber.id.toString())
            .claim("email", subscriber.email.value)
            .claim("accountId", account.id.toString())
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return AccessToken(value = token, expiresAt = expiresAt)
    }

    companion object {
        const val ISSUER = "qlic-backend-api"
    }
}
