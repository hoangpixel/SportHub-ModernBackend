package com.sporthub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ==============================
    // TẠO JWT
    // ==============================
        public String generateToken(
                UserDetails userDetails,
                Long userId) {

        Date now = new Date();

        Date expiration =
                new Date(
                        now.getTime() + jwtExpiration
                );

        List<String> authorities =
                userDetails.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        List<String> roles =
                authorities.stream()
                        .filter(a ->
                                a.startsWith("ROLE_")
                        )
                        .toList();

        List<String> permissions =
                authorities.stream()
                        .filter(a ->
                                !a.startsWith("ROLE_")
                        )
                        .toList();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
        }

    // ==============================
    // LẤY USERNAME TỪ JWT
    // ==============================
    public String extractUsername(String token) {
        return extractAllClaims(token)
                .getSubject();
    }

    // ==============================
    // KIỂM TRA TOKEN
    // ==============================
    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        String username =
                extractUsername(token);

        return username.equals(
                userDetails.getUsername()
        ) && !isTokenExpired(token);
    }

    // ==============================
    // KIỂM TRA HẾT HẠN
    // ==============================
    private boolean isTokenExpired(String token) {

        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    // ==============================
    // ĐỌC CLAIMS
    // ==============================
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ==============================
    // SECRET KEY
    // ==============================
    private SecretKey getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(jwtSecret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}