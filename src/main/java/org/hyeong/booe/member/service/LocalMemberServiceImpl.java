package org.hyeong.booe.member.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.*;
import org.hyeong.booe.global.security.jwt.JwtProvider;
import org.hyeong.booe.global.security.jwt.TokenResDto;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberCredential;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.MemberStatus;
import org.hyeong.booe.member.dto.req.LocalLoginRequestDto;
import org.hyeong.booe.member.dto.req.LocalSignupRequestDto;
import org.hyeong.booe.member.dto.res.LocalLoginResDto;
import org.hyeong.booe.member.dto.res.LocalSignupResDto;
import org.hyeong.booe.member.repository.MemberCredentialRepository;
import org.hyeong.booe.member.repository.MemberProfileRepository;
import org.hyeong.booe.member.repository.MemberRepository;
import org.hyeong.booe.member.util.MemberCodeGenerator;
import org.hyeong.booe.verification.domain.PhoneVerification;
import org.hyeong.booe.verification.repository.PhoneVerificationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class LocalMemberServiceImpl {

    private final MemberRepository memberRepository;
    private final MemberCredentialRepository memberCredentialRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberCodeGenerator memberCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;


    @Transactional
    public LocalSignupResDto signup(LocalSignupRequestDto reqDto) {

        validatePhoneVerification(reqDto.getVerificationId(), reqDto.getPhoneNum());
        checkDuplicatePhoneNumber(reqDto.getPhoneNum(), reqDto);

        Member member = createAccount(reqDto);

        return new LocalSignupResDto(member);
    }

    public LocalLoginResDto login(LocalLoginRequestDto requestDto) {

        MemberCredential credential = memberCredentialRepository.findByLoginId(requestDto.getLoginId())
                .orElseThrow(MemberNotFoundException::new);

        if (!passwordEncoder.matches(requestDto.getPassword(), credential.getPassword())) {
            throw new InvalidPasswordException();
        }
        Member member = credential.getMember();

        MemberProfile profile = memberProfileRepository.findByMember(member)
                .orElseThrow(ProfileNotFoundException::new);

        TokenResDto tokenResDto = generateToken(member);

        return LocalLoginResDto.of(member, profile, tokenResDto);
    }

    private TokenResDto generateToken(Member member) {

        String accessToken = jwtProvider.generateAccessToken(member.getMemberCode(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getMemberCode());

        return new TokenResDto(accessToken, refreshToken);
    }

    private Member createAccount(LocalSignupRequestDto reqDto) {

        validateDuplicateLoginId(reqDto.getLoginId());
        validatePassword(reqDto.getPassword(), reqDto.getPasswordConfirm());

        Member member = Member.builder()
                .memberCode(memberCodeGenerator.generate()).build();
        memberRepository.save(member);

        MemberProfile memberProfile = reqDto.toMemberProfile(member);
        memberProfileRepository.save(memberProfile);

        String encodedPassword = passwordEncoder.encode(reqDto.getPassword());

        MemberCredential memberCredential = reqDto.toMemberCredential(member, encodedPassword);
        memberCredentialRepository.save(memberCredential);

        return member;
    }


    // 👇 private 분리
    private void validateDuplicateLoginId(String loginId) {
        if (memberCredentialRepository.existsByLoginId(loginId)) {
            throw new DuplicateLoginIdException();
        }
    }

    private void validatePassword(String password, String confirm) {
        if (!password.equals(confirm)) {
            throw new PasswordMismatchException();
        }
    }

    private void validatePhoneVerification(Long verificationId, String phoneNum) {
        PhoneVerification verification = phoneVerificationRepository.findById(verificationId)
                .orElseThrow(PhoneVerificationNotFoundException::new);

        if (!verification.isVerified()) {
            throw new VerificationNotCompletedException();
        }

        if (!verification.isSamePhoneNumber(phoneNum)) {
            throw new VerificationPhoneMismatchException();
        }
    }

    private void checkDuplicatePhoneNumber(String phoneNum, LocalSignupRequestDto dto) {

        memberProfileRepository.findByPhoneNumberAndMemberStatus(phoneNum, MemberStatus.ACTIVE)
                .ifPresent(existingProfile -> {
                    if (isSamePerson(existingProfile, dto)) {
                        throw new AlreadyRegisteredMemberException();
                    }
                    log.info("Phone number ownership transferred from member {} to new signup",
                            existingProfile.getMember().getId());
                    existingProfile.clearPhoneNumber();
                });

    }


    private boolean isSamePerson(MemberProfile profile, LocalSignupRequestDto dto) {
        return profile.getName().equals(dto.getName()) &&
                profile.getBirthDate().equals(dto.getBirthDate());
    }
}
