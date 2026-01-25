package org.hyeong.booe.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hyeong.booe.common.ApiResponse;
import org.hyeong.booe.common.code.SuccessCode;
import org.hyeong.booe.member.dto.req.LocalLoginRequestDto;
import org.hyeong.booe.member.dto.req.LocalSignupRequestDto;
import org.hyeong.booe.member.dto.res.LocalLoginResDto;
import org.hyeong.booe.member.dto.res.LocalSignupResDto;
import org.hyeong.booe.member.service.LocalMemberServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {

    private final LocalMemberServiceImpl memberService;

    @PostMapping("/signup")
    public ResponseEntity<LocalSignupResDto> signup(@RequestBody @Valid LocalSignupRequestDto requestDto) {

        LocalSignupResDto result = memberService.signup(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LocalLoginRequestDto reqDto) {
        LocalLoginResDto resDto = memberService.login(reqDto);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", resDto.token().getAccessToken());

        return ResponseEntity
                .status(SuccessCode.LOGIN_SUCCESS.getHttpStatus())
                .headers(headers)
                .body(ApiResponse.success(SuccessCode.LOGIN_SUCCESS, resDto));
    }
}
