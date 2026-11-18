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

        @Value("${jwt.refresh-expiration:604800000}")
        private long refreshExpiration;

        // Generar token normal
        public String generarToken(UserDetails userDetails) {

                return Jwts.builder()
                                .subject(userDetails.getUsername())
                                .claim("tipo", "access")
                                .issuedAt(new Date())
                                .expiration(
                                                new Date(
                                                                System.currentTimeMillis()
                                                                                + jwtExpiration))
                                .signWith(getSigningKey())
                                .compact();
        }

        // Generar refresh token
        public String generarRefreshToken(UserDetails userDetails) {

                return Jwts.builder()
                                .subject(userDetails.getUsername())
                                .claim("tipo", "refresh")
                                .issuedAt(new Date())
                                .expiration(
                                                new Date(
                                                                System.currentTimeMillis()
                                                                                + refreshExpiration))
                                .signWith(getSigningKey())
                                .compact();
        }

        // Extraer correo del token
        public String extraerEmail(String token) {

                return extraerClaims(token)
                                .getSubject();
        }

        // Validar token normal
        public boolean validarToken(
                        String token,
                        UserDetails userDetails) {

                final String email = extraerEmail(token);

                String tipo = extraerClaims(token)
                                .get("tipo", String.class);

                return email.equals(userDetails.getUsername())
                                && "access".equals(tipo)
                                && !tokenExpirado(token);
        }

        // Validar refresh token
        public boolean validarRefreshToken(
                        String token,
                        UserDetails userDetails) {

                final String email = extraerEmail(token);

                String tipo = extraerClaims(token)
                                .get("tipo", String.class);

                return email.equals(
                                userDetails.getUsername())
                                && "refresh".equals(tipo)
                                && !tokenExpirado(token);
        }

        // Comprobar si el token expiro
        private boolean tokenExpirado(String token) {

                return extraerClaims(token)
                                .getExpiration()
                                .before(new Date());
        }

        // Extraer informacion del token
        private Claims extraerClaims(String token) {

                return Jwts.parser()
                                .verifyWith(getSigningKey())
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
        }

        // Obtener clave para firmar y verificar
        private SecretKey getSigningKey() {

                byte[] keyBytes = Decoders.BASE64.decode(
                                jwtSecret);

                return Keys.hmacShaKeyFor(
                                keyBytes);
        }
}