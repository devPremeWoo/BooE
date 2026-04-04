package org.hyeong.booe.global.modusign.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateDocumentResponse {

    private String id;          // 모두사인 문서 ID
    private String status;      // 문서 상태
    private String title;
}
