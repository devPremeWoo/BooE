package org.hyeong.booe.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hyeong.booe.common.ApiResponse;
import org.hyeong.booe.common.code.SuccessCode;
import org.hyeong.booe.global.details.CustomUserDetails;
import org.hyeong.booe.member.dto.req.LocalLoginRequestDto;
import org.hyeong.booe.member.dto.req.LocalSignupRequestDto;
import org.hyeong.booe.member.dto.req.RefreshReqDto;
import org.hyeong.booe.member.dto.res.LocalLoginResDto;
import org.hyeong.booe.member.dto.res.LocalSignupResDto;
import org.hyeong.booe.member.service.auth.LocalAuthService;
import org.hyeong.booe.member.service.MemberService;
import org.hyeong.booe.global.security.jwt.TokenResDto;
import org.hyeong.booe.global.security.jwt.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final LocalAuthService localAuthService;
    private final TokenService tokenService;
    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<LocalSignupResDto> signup(
            @RequestBody @Valid LocalSignupRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(localAuthService.signup(requestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid LocalLoginRequestDto reqDto) {
        LocalLoginResDto resDto = localAuthService.login(reqDto);

        return ResponseEntity
                .status(SuccessCode.LOGIN_SUCCESS.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.LOGIN_SUCCESS, resDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResDto> refresh(
            @RequestBody @Valid RefreshReqDto reqDto) {
        TokenResDto tokenResDto = tokenService.refresh(reqDto.getRefreshToken());
        return ResponseEntity.ok(tokenResDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.logout(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
