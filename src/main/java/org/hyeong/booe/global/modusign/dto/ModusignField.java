package org.hyeong.booe.global.modusign.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * PDF 내 서명·텍스트 입력 필드 위치 정의
 * page: 1-based
 * x, y: 좌측 상단 기준 포인트 (PDF pt 단위, A4 = 595×842)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModusignField {

    private final String type;      // SIGNATURE | TEXT
    private final int page;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final String label;     // TEXT 필드 레이블 (nullable)

    public static ModusignField signature(int page, double x, double y) {
        return ModusignField.builder()
                .type("SIGNATURE")
                .page(page)
                .x(x).y(y)
                .width(160).height(60)
                .build();
    }

    public static ModusignField ssnInput(int page, double x, double y) {
        return ModusignField.builder()
                .type("TEXT")
                .page(page)
                .x(x).y(y)
                .width(220).height(30)
                .label("주민등록번호")
                .build();
    }
}
