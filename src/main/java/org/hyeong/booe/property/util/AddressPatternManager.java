package org.hyeong.booe.property.util;

import org.hyeong.booe.property.dto.response.Expos.ExposItemDto;
import org.hyeong.booe.property.dto.request.AreaReqDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddressPatternManager {

    public AreaReqDto castToApiRequest(List<ExposItemDto> items,
                                       String sigunguCd, String bjdongCd,
                                       String bun, String ji,
                                       String userDong, String userHo) {

        // 1. 숫자가 포함된 호 데이터를 가진 아이템 하나를 샘플로 추출 (거주 공간 추정)
        ExposItemDto sample = items.stream()
                .filter(item -> item.getHoNm().matches(".*\\d+.*"))
                .findFirst()
                .orElse(null);

        // 2. 샘플이 있다면 패턴 분석, 없으면 사용자 입력값 그대로 사용
        String finalDong = (sample != null) ? applyDongPattern(sample.getDongNm(), userDong) : userDong;
        String finalHo = (sample != null) ? applyHoPattern(sample.getHoNm(), userHo) : userHo;

        return AreaReqDto.builder()
                .sigunguCd(sigunguCd)
                .bjdongCd(bjdongCd)
                .bun(bun)
                .ji(ji)
                .dongNm(finalDong)
                .hoNm(finalHo)
                .numOfRows(30)
                .build();
    }

    // [메서드 추출] 동 명칭 패턴 적용 (제/동 접사 처리)
    private String applyDongPattern(String apiDong, String userDong) {
        String result = userDong;
        if (apiDong.contains("동")) result += "동";
        if (apiDong.startsWith("제")) result = "제" + result;
        return result;
    }

    // [메서드 추출] 호 명칭 패턴 적용 (접두사 배제하고 '호' 접미사만 판단)
    private String applyHoPattern(String apiHo, String userHo) {
        // prefix는 무시하고 숫자 뒤에 '호'가 붙어있는지만 확인
        return apiHo.contains("호") ? userHo + "호" : userHo;
    }

    private String formatToFourDigits(String value) {
        if (value == null || value.isBlank()) return "0000";
        if (value.length() == 4) return value;

        try {
            int number = Integer.parseInt(value);
            return String.format("%04d", number);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
