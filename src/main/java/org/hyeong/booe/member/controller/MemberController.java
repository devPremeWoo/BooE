package org.hyeong.booe.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hyeong.booe.global.details.CustomUserDetails;
import org.hyeong.booe.member.dto.req.MemberUpdateReqDto;
import org.hyeong.booe.member.dto.res.MemberInfoResDto;
import org.hyeong.booe.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<MemberInfoResDto> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(memberService.getMyInfo(userDetails.getMemberId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateMyInfo(
            @RequestBody @Valid MemberUpdateReqDto reqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.updateMyInfo(userDetails.getMemberId(), reqDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.withdraw(userDetails.getMemberId());
        return ResponseEntity.ok().build();
    }
}
