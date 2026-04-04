package org.hyeong.booe.global.modusign;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.global.modusign.dto.CreateDocumentRequest;
import org.hyeong.booe.global.modusign.dto.CreateDocumentResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModusignApiClient {

    private final WebClient modusignWebClient;
    private final ObjectMapper objectMapper;

    /**
     * 모두사인에 계약서 PDF와 참여자 정보를 전달하여 서명 요청 문서를 생성합니다.
     *
     * @return 생성된 모두사인 문서 ID
     */
    public String createDocument(CreateDocumentRequest request, byte[] pdfBytes) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        try {
            builder.part("document", objectMapper.writeValueAsString(request), MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            throw new RuntimeException("모두사인 요청 직렬화 실패", e);
        }

        ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return "contract.pdf";
            }
        };
        builder.part("file", pdfResource, MediaType.APPLICATION_PDF);

        CreateDocumentResponse response = modusignWebClient.post()
                .uri("/documents")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(CreateDocumentResponse.class)
                .doOnError(e -> log.error("[Modusign] 문서 생성 실패: {}", e.getMessage()))
                .block();

        if (response == null || response.getId() == null) {
            throw new RuntimeException("모두사인 문서 생성 응답이 비어있습니다.");
        }

        log.info("[Modusign] 문서 생성 성공 - documentId={}", response.getId());
        return response.getId();
    }
}
