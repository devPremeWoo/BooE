package org.hyeong.booe.auth.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.member.domain.type.OauthProviderType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OauthSignupTempInfo {

    private OauthProviderType providerType;
    private String providerUserId;
}
