package org.hyeong.booe.property.util;

import lombok.Getter;
import org.hyeong.booe.property.dto.response.Expos.ExposItemDto;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class AddressPatternAnalyzer {

    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    private final String prefix;
    private final String suffix;

    private AddressPatternAnalyzer(String prefix, String suffix) {
        this.prefix = (prefix == null) ? "" : prefix;
        this.suffix = (suffix == null) ? "" : suffix;
    }

    public static AddressPatternAnalyzer analyze(List<ExposItemDto> samples, boolean isDong) {
        String bestSample = selectBestSample(samples, isDong);
        return extractPattern(bestSample);
    }

    private static String selectBestSample(List<ExposItemDto> samples, boolean isDong) {
        if (samples == null || samples.isEmpty()) return "";

        return samples.stream()
                .map(item -> isDong ? item.getDongNm() : item.getHoNm())
                .filter(name -> name != null && name.matches(".*\\d+.*")) // 숫자가 포함된 것을 우선
                .findFirst()
                .orElse(isDong ? samples.get(0).getDongNm() : samples.get(0).getHoNm());
    }

    private static AddressPatternAnalyzer extractPattern(String sampleText) {
        if (sampleText == null || sampleText.isBlank()) {
            return new AddressPatternAnalyzer("", "");
        }

        Matcher matcher = DIGIT_PATTERN.matcher(sampleText);

        // 숫자형 패턴 (예: "제101동")
        if (matcher.find()) {
            return new AddressPatternAnalyzer(
                    sampleText.substring(0, matcher.start()),
                    sampleText.substring(matcher.end())
            );
        }

        // 문자형 패턴 (예: "A동")
        return extractCharacterPattern(sampleText);
    }

    private static AddressPatternAnalyzer extractCharacterPattern(String sampleText) {
        if (sampleText.length() > 1) {
            return new AddressPatternAnalyzer("", sampleText.substring(1));
        }
        return new AddressPatternAnalyzer("", "");
    }

    public String combine(String userInput) {
        if (userInput == null || userInput.isBlank()) return userInput;

        String result = userInput;
        if (!prefix.isEmpty() && !result.startsWith(prefix)) {
            result = prefix + result;
        }
        if (!suffix.isEmpty() && !result.endsWith(suffix)) {
            result = result + suffix;
        }
        return result;
    }
}
