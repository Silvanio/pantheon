package com.pantheon.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.pantheon.service.entity.DailyReportMaterialReceived;
public record MaterialReceivedResponse(UUID id, String materialName, String unit, BigDecimal quantity) {

    public static MaterialReceivedResponse from(DailyReportMaterialReceived received) {
        return new MaterialReceivedResponse(
                received.getId(), received.getMaterialName(), received.getUnit(), received.getQuantity());
    }
}
