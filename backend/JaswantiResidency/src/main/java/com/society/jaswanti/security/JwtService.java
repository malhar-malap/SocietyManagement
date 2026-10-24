package com.society.jaswanti.security;

import com.society.jaswanti.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:900000}") // 15 mins default
    private long jwtExpiration;

    @Value("${jwt.refresh-token.expiration:604800000}") // 7 days default
    private long refreshExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(User user) {
        return generateToken(new HashMap<>(), user);
    }

    public String generateToken(Map<String, Object> extraClaims, User user) {
        extraClaims.put("typ", "access");
        extraClaims.put("token_version", user.getTokenVersion());
        return buildToken(extraClaims, user, jwtExpiration);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "refresh");
        claims.put("token_version", user.getTokenVersion());
        return buildToken(claims, user, refreshExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, User user, long expiration) {
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", "ROLE_" + user.getRole().name());
        
        return Jwts.builder()
                .claims(extraClaims)
                .subject(user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("typ", String.class));
    }

    public Integer extractTokenVersion(String token) {
        return extractClaim(token, claims -> claims.get("token_version", Integer.class));
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }
        String typ = extractTokenType(token);
        if (!"access".equals(typ)) {
            return false;
        }
        if (!userDetails.isEnabled()) {
            return false;
        }
        if (userDetails instanceof User user) {
            Integer version = extractTokenVersion(token);
            if (version == null || version != user.getTokenVersion()) {
                return false;
            }
        }
        return true;
    }

    public boolean isRefreshTokenValid(String token, User user) {
        final String username = extractUsername(token);
        if (!username.equals(user.getUsername()) || isTokenExpired(token)) {
            return false;
        }
        String typ = extractTokenType(token);
        if (!"refresh".equals(typ)) {
            return false;
        }
        if (!user.isEnabled()) {
            return false;
        }
        Integer version = extractTokenVersion(token);
        if (version == null || version != user.getTokenVersion()) {
            return false;
        }
        return true;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isAccessTokenValid(token, userDetails);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
