package org.hyeong.booe.property.service;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.property.api.BldRgstApiClient;
import org.hyeong.booe.property.api.ConstructionApiClient;
import org.hyeong.booe.property.dto.BldRgstQueryDto;
import org.hyeong.booe.property.dto.response.BldRgstAreaItem;
import org.hyeong.booe.property.dto.response.DongHoSelectionResDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyUnitSelectionService {

    private final BldRgstApiClient apiClient;

    public Mono<DongHoSelectionResDto> getSelectableDongHo(BldRgstQueryDto queryDto) {

        return apiClient.fetchAllAreaItems(queryDto)
                .map(this::groupByDongHo)
                .map(this::toResponseDto);
    }


    private Map<String, List<String>> groupByDongHo(List<BldRgstAreaItem> items) {

        Map<String, Set<String>> temp = new HashMap<>();

        for (BldRgstAreaItem item : items) {
            String dong = normalizeDong(item.getDongNm());
            String ho = normalizeHo(item.getHoNm());

            if (dong == null || ho == null) {
                continue;
            }
            if (!temp.containsKey(dong)) {
                temp.put(dong, new HashSet<>());
            }
            temp.get(dong).add(ho);
        }

        return temp.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().sorted().toList()));
    }

    /**
     * 동 정규화: 101, 제101동, 101동 → 101동
     */
    private String normalizeDong(String dongNm) {
        if (dongNm == null) return null;

        String trimmed = dongNm.trim();
        String number = trimmed.replaceAll("[^0-9]", "");
        if (!number.isEmpty()) {
            return number + "동";
        }
        if (trimmed.endsWith("동")) {
            return trimmed;
        }

        return trimmed + "동";
    }

    /**
     * 호 정규화: 1003호 → 1003
     */
    private String normalizeHo(String hoNm) {
        if (hoNm == null) return null;
        return hoNm.replaceAll("[^0-9]", "");
    }

    private DongHoSelectionResDto toResponseDto(
            Map<String, List<String>> grouped) {

        List<DongHoSelectionResDto.DongUnit> dongs =
                grouped.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> DongHoSelectionResDto.DongUnit.builder()
                                .dongName(e.getKey())
                                .hoList(e.getValue())
                                .build())
                        .toList();

        return DongHoSelectionResDto.builder()
                .dongs(dongs)
                .build();
    }
}
