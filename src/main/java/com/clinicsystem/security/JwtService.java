package com.clinicsystem.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    public static final String ACCESS_TOKEN_TYPE = "ACCESS";
    public static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private static final String TYPE_CLAIM = "token_type";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails, ACCESS_TOKEN_TYPE, expirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, REFRESH_TOKEN_TYPE, refreshExpirationMs);
    }

    private String buildToken(UserDetails userDetails, String type, long ttl) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(TYPE_CLAIM, type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(key())
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isValid(token, userDetails, ACCESS_TOKEN_TYPE);
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        return isValid(token, userDetails, REFRESH_TOKEN_TYPE);
    }

    private boolean isValid(String token, UserDetails userDetails, String expectedType) {
        try {
            Claims claims = parse(token);
            return expectedType.equals(claims.get(TYPE_CLAIM, String.class))
                    && claims.getSubject().equals(userDetails.getUsername())
                    && !isExpired(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isExpired(Claims claims) {
        Date exp = claims.getExpiration();
        return exp == null || exp.before(new Date());
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }
}
