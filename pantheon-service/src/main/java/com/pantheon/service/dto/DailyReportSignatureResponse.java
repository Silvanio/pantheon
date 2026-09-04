package com.pantheon.service.dto;

import java.time.Instant;
import java.util.UUID;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.DailyReportSignature;
public record DailyReportSignatureResponse(
        UUID id, UUID membershipId, ConstructionFunction function, Instant signedAt) {

    public static DailyReportSignatureResponse from(DailyReportSignature signature) {
        return new DailyReportSignatureResponse(
                signature.getId(), signature.getMembershipId(), signature.getFunction(), signature.getSignedAt());
    }
}
