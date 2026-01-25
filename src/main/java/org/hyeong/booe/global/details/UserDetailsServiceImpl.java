package org.hyeong.booe.global.details;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.MemberNotFoundException;
import org.hyeong.booe.member.domain.MemberCredential;
import org.hyeong.booe.member.repository.MemberCredentialRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MemberCredentialRepository memberCredentialRepository;

    @Override
    public UserDetails loadUserByUsername(String memberCode) throws UsernameNotFoundException {

        MemberCredential credential = memberCredentialRepository.findByMemberCodeWithMember(memberCode)
                .orElseThrow(MemberNotFoundException::new); // M001

        return new CustomUserDetails(credential.getMember(), credential);
    }
}
