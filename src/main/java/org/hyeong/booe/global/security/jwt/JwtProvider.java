package org.hyeong.booe.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.member.domain.type.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProvider {

    private final UserDetailsService userDetailsService;

    // Header key 값 (name)
    public static final String AUTHORIZATION_HEADER = "Authorization";

    // Token 식별자
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String jwtSecretKey;

    private Key key;
    private JwtParser jwtParser;

    //@Value("${jwt.access_token_time")
    //private Long ACCESS_TOKEN_TIME;
    private final long accessTokenExpTime = 1000L * 60 * 60;
    private final long refreshTokenExpTime = 1000L * 60 * 60 * 24 * 7;



    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {

        if (jwtSecretKey == null || jwtSecretKey.isEmpty()) {
            throw new IllegalStateException("JWT secret key is not configured!");
        }

        byte[] bytes = Base64.getDecoder().decode(jwtSecretKey);
        key = Keys.hmacShaKeyFor(bytes);

        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(key)
                .build();

    }

    public String generateAccessToken(String memberCode, Role role) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpTime);

        return BEARER_PREFIX +
                Jwts.builder().setSubject(memberCode)
                        .claim("role", role.name())
                        .setIssuedAt(now)
                        .setExpiration(expiry)
                        .signWith(key, signatureAlgorithm)
                        .compact();
    }

    public String generateRefreshToken(String memberCode) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpTime);

        return Jwts.builder().setSubject(memberCode)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, signatureAlgorithm)
                .compact();

    }

    public Authentication getAuthentication(String accessToken) {

        Claims claims = jwtParser.parseClaimsJws(accessToken).getBody();
        String memberId = claims.getSubject();

        String role = claims.get("role", String.class);
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        // 3. UserDetails 생성 (실무에서 자주 사용)
        UserDetails userDetails = userDetailsService.loadUserByUsername(memberId);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    public boolean validateToken(String token) {

        try {
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Expired");
        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("Invalid JWT token");
        }

        return false;
    }

    public Claims getClaims(String token) {
        return jwtParser.parseClaimsJws(token)
                .getBody();
    }

    public String getId(String token) {
        return getClaims(token).getId();
    }

}
