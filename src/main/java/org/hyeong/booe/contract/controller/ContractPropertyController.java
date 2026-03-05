package org.hyeong.booe.contract.controller;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.property.dto.BldRgstQueryDto;
import org.hyeong.booe.property.dto.response.BuildingUnitResDto;
import org.hyeong.booe.property.service.PropertyUnitSelectionService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contracts/property")
public class ContractPropertyController {

    private final PropertyUnitSelectionService propertyUnitSelectionService;


    @PostMapping("/units")
    public Mono<BuildingUnitResDto> getSelectableUnits(@RequestBody BldRgstQueryDto queryDto) {
        //return propertyUnitSelectionService.getSelectableDongHo(queryDto);
        return propertyUnitSelectionService.getSelectableDongHoWithTiming(queryDto);
    }


//    @PostMapping("/property-info")
//    public Mono<String> getPropertyInfo(@RequestBody PropertyInfoReqDto reqDto) {
//
//    }
}
