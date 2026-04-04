package org.hyeong.booe.global.modusign.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ModusignParticipant {

    private final String name;
    private final ModusignSigningMethod signingMethod;
    private final String locale;
    private final int signingOrder;
    private final List<ModusignField> fields;

    public static ModusignParticipant of(String name, String mobile, int order, List<ModusignField> fields) {
        return ModusignParticipant.builder()
                .name(name)
                .signingMethod(ModusignSigningMethod.kakao(mobile))
                .locale("ko")
                .signingOrder(order)
                .fields(fields)
                .build();
    }
}
