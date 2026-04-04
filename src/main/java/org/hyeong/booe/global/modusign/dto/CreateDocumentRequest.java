package org.hyeong.booe.global.modusign.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateDocumentRequest {

    private final String title;
    private final List<ModusignParticipant> participants;
}
