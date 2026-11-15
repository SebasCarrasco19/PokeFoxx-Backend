
package com.pokefoxx.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Generar token JWT
    public String generarToken(UserDetails userDetails) {

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + jwtExpiration)
                )
                .signWith(getSigningKey())
                .compact();
    }

    // Extraer correo del token
    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    // Validar token
    public boolean validarToken(String token, UserDetails userDetails) {

        final String email = extraerEmail(token);

        return email.equals(userDetails.getUsername())
                && !tokenExpirado(token);
    }

    // Comprobar si el token expiró
    private boolean tokenExpirado(String token) {

        return extraerClaims(token)
                .getExpiration()
                .before(new Date());
    }

    // Extraer información del token
    private Claims extraerClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Obtener clave para firmar y verificar
    private SecretKey getSigningKey() {

        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
