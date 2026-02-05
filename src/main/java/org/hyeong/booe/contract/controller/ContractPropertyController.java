package org.hyeong.booe.contract.controller;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.property.dto.BldRgstQueryDto;
import org.hyeong.booe.property.dto.response.DongHoSelectionResDto;
import org.hyeong.booe.property.service.PropertyUnitSelectionService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contracts/property")
public class ContractPropertyController {

    private final PropertyUnitSelectionService propertyUnitSelectionService;


    @PostMapping("/units")
    public Mono<DongHoSelectionResDto> getSelectableUnits(@RequestBody BldRgstQueryDto queryDto) {
        //return propertyUnitSelectionService.getSelectableDongHo(queryDto);
        return propertyUnitSelectionService.getSelectableDongHoWithTiming(queryDto);
    }
}
