package org.hyeong.booe.member.domain.type;

public enum OauthProviderType {

    KAKAO("kakao"),
    NAVER("naver"),
    APPLE("apple");

    private final String key;

    OauthProviderType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
