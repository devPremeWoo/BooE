package org.hyeong.booe.contract.controller;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.dto.req.DownPaymentConfirmReqDto;
import org.hyeong.booe.contract.dto.req.PropertyInfoReqDto;
import org.hyeong.booe.contract.dto.req.ReviewRequestDto;
import org.hyeong.booe.contract.dto.res.ContractResDto;
import org.hyeong.booe.contract.dto.res.PropertyInfoResDto;
import org.hyeong.booe.contract.service.ContractService;
import org.hyeong.booe.global.details.CustomUserDetails;
import org.hyeong.booe.property.dto.request.BuildingInfoReqDto;
import org.hyeong.booe.property.dto.response.BuildingUnitResDto;
import org.hyeong.booe.property.dto.response.LandRatioDto;
import org.hyeong.booe.property.dto.response.LandResDto;
import org.hyeong.booe.property.service.LandInfoService;
import org.hyeong.booe.property.service.PropertyCompositeService;
import org.hyeong.booe.property.service.PropertyUnitSelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/contracts")
public class ContractPropertyController {

    private final PropertyUnitSelectionService propertyUnitSelectionService;
    private final LandInfoService landInfoService;
    private final PropertyCompositeService propertyCompositeService;
    private final ContractService contractService;

    @GetMapping("/contract")
    public String contract() {
        return "contracts/monthlyContractDoc";
    }

    @PostMapping("/units")
    public Mono<BuildingUnitResDto> getSelectableUnits(@RequestBody BuildingInfoReqDto queryDto) {
        //return propertyUnitSelectionService.getSelectableDongHo(queryDto);
        return propertyUnitSelectionService.getSelectableDongHoWithTiming(queryDto);
    }

    @PostMapping("/property-info")
    public Mono<ResponseEntity<PropertyInfoResDto>> getPropertyInfo(@RequestBody PropertyInfoReqDto reqDto) {

        return propertyCompositeService.getCompositePropertyInfo(reqDto)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/draft")
    public ResponseEntity<Long> saveDraft(@RequestBody @Validated(ContractBaseReqDto.TempSave.class) ContractBaseReqDto reqDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long savedId = contractService.save(reqDto, userDetails.getMemberId());
        return ResponseEntity
                .created(URI.create("/contracts/" + savedId))
                .body(savedId);
    }

    @PostMapping("/review-request")
    public ResponseEntity<Void> requestReview(@RequestBody @Validated ReviewRequestDto reqDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        contractService.requestReview(reqDto, userDetails.getMemberId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{contractId}")
    public ResponseEntity<ContractResDto> getContract(@PathVariable Long contractId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(contractService.getContract(contractId, userDetails.getMemberId()));
    }

    @PostMapping("/{contractId}/lessee-submit")
    public ResponseEntity<Void> submitLesseeInfo(@PathVariable Long contractId,
                                                  @RequestBody @Validated(ContractBaseReqDto.LesseeSubmit.class) ContractBaseReqDto reqDto,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        contractService.submitLesseeInfo(contractId, reqDto, userDetails.getMemberId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{contractId}/down-payment-confirm")
    public ResponseEntity<Void> confirmDownPayment(@PathVariable Long contractId,
                                                    @RequestBody DownPaymentConfirmReqDto reqDto,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        contractService.confirmDownPayment(contractId, reqDto, userDetails.getMemberId());
        return ResponseEntity.ok().build();
    }





//    @PostMapping("/test/land-ratio-service")
//    public Mono<ResponseEntity<LandRatioDto>> testLandRatioService(@RequestBody PropertyInfoReqDto reqDto) {
//
//        return landInfoService.getLandRatioInfo(reqDto)
//                .map(result -> {
//
//                    return ResponseEntity.ok(result);
//                })
//                .doOnError(e -> log.error("[TEST-SERVICE] 서비스 로직 중 에러 발생: {}", e.getMessage()));
//                // 에러 발생 시 테스트 중단되지 않도록 빈 값 반환 (로그 확인용)
//
//    }


}
