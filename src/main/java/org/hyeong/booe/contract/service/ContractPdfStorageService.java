package org.hyeong.booe.contract.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class ContractPdfStorageService {

    private final String storagePath;

    public ContractPdfStorageService(@Value("${payment.pdf-storage-path:./pdf-storage}") String storagePath) {
        this.storagePath = storagePath;
    }

    public String save(Long contractId, byte[] pdfBytes) {
        try {
            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);

            Path filePath = dir.resolve(fileNameFor(contractId));
            Files.write(filePath, pdfBytes);

            log.info("[PDF] 저장 완료 - {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("PDF 저장 실패", e);
        }
    }

    /**
     * 저장될(또는 곧 저장될) PDF의 결정적 경로를 반환.
     * PDF 생성이 비동기로 분리된 뒤에도 응답 시점에 경로를 미리 알려주기 위함 — 파일은 곧 생성됨.
     */
    public String pathFor(Long contractId) {
        return Paths.get(storagePath).resolve(fileNameFor(contractId)).toString();
    }

    private String fileNameFor(Long contractId) {
        return "contract-" + contractId + ".pdf";
    }
}
