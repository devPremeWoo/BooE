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

            String fileName = "contract-" + contractId + ".pdf";
            Path filePath = dir.resolve(fileName);
            Files.write(filePath, pdfBytes);

            log.info("[PDF] 저장 완료 - {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("PDF 저장 실패", e);
        }
    }
}
