package org.hyeong.booe.member.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.*;
import org.hyeong.booe.global.security.jwt.JwtProvider;
import org.hyeong.booe.global.security.jwt.RefreshTokenRedisService;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalAuthService {

    private final MemberRepository memberRepository;
    private final MemberCredentialRepository memberCredentialRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final MemberCodeGenerator memberCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;

    @Transactional
    public LocalSignupResDto signup(LocalSignupRequestDto reqDto) {
        validatePhoneVerification(reqDto.getVerificationId(), reqDto.getPhoneNum());
        checkDuplicatePhoneNumber(reqDto.getPhoneNum(), reqDto);
        Member member = createAccount(reqDto);
        return new LocalSignupResDto(member);
    }

    public LocalLoginResDto login(LocalLoginRequestDto requestDto) {
        MemberCredential credential = findCredential(requestDto.getLoginId());
        validatePassword(requestDto.getPassword(), credential.getPassword());

        Member member = credential.getMember();
        MemberProfile profile = findProfile(member);
        TokenResDto tokenResDto = generateToken(member);
        refreshTokenRedisService.save(member.getMemberCode(), tokenResDto.getRefreshToken());

        return LocalLoginResDto.of(member, profile, tokenResDto);
    }

    private MemberCredential findCredential(String loginId) {
        return memberCredentialRepository.findByLoginId(loginId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private MemberProfile findProfile(Member member) {
        return memberProfileRepository.findByMember(member)
                .orElseThrow(ProfileNotFoundException::new);
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidPasswordException();
        }
    }

    private TokenResDto generateToken(Member member) {
        String accessToken = jwtProvider.generateAccessToken(member.getMemberCode(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getMemberCode());
        return new TokenResDto(accessToken, refreshToken);
    }

    private Member createAccount(LocalSignupRequestDto reqDto) {
        validateDuplicateLoginId(reqDto.getLoginId());
        validatePasswordConfirm(reqDto.getPassword(), reqDto.getPasswordConfirm());

        Member member = Member.builder()
                .memberCode(memberCodeGenerator.generate()).build();
        memberRepository.save(member);

        memberProfileRepository.save(reqDto.toMemberProfile(member));
        String encodedPassword = passwordEncoder.encode(reqDto.getPassword());
        memberCredentialRepository.save(reqDto.toMemberCredential(member, encodedPassword));

        return member;
    }

    private void validateDuplicateLoginId(String loginId) {
        if (memberCredentialRepository.existsByLoginId(loginId)) {
            throw new DuplicateLoginIdException();
        }
    }

    private void validatePasswordConfirm(String password, String confirm) {
        if (!password.equals(confirm)) {
            throw new PasswordMismatchException();
        }
    }

    private void validatePhoneVerification(Long verificationId, String phoneNum) {
        PhoneVerification verification = phoneVerificationRepository.findById(verificationId)
                .orElseThrow(PhoneVerificationNotFoundException::new);
        if (!verification.isVerified()) throw new VerificationNotCompletedException();
        if (!verification.isSamePhoneNumber(phoneNum)) throw new VerificationPhoneMismatchException();
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
        return profile.getName().equals(dto.getName())
                && profile.getBirthDate().equals(dto.getBirthDate());
    }
}
