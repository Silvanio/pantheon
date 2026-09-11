package com.pantheon.service.storage;

import java.util.UUID;

/**
 * Object key layout: every file belonging to a daily report shares the same key prefix,
 * so the report's files behave like one folder even though object storage has no real
 * directory structure — see design.md "Storage key layout ('folders' per report)".
 */
public final class StorageKeys {

    private StorageKeys() {
    }

    public static String mediaKey(UUID constructionSiteId, UUID dailyReportId, UUID mediaId, String extension) {
        return "construction-sites/%s/daily-reports/%s/media/%s.%s"
                .formatted(constructionSiteId, dailyReportId, mediaId, extension);
    }

    public static String attachmentKey(
            UUID constructionSiteId, UUID dailyReportId, UUID attachmentId, String extension) {
        return "construction-sites/%s/daily-reports/%s/attachments/%s.%s"
                .formatted(constructionSiteId, dailyReportId, attachmentId, extension);
    }

    public static String companyLogoKey(UUID companyId, String extension) {
        return "companies/%s/logo.%s".formatted(companyId, extension);
    }

    public static String sitePhotoKey(UUID constructionSiteId, String extension) {
        return "construction-sites/%s/photo.%s".formatted(constructionSiteId, extension);
    }

    public static String siteDocumentProjectAttachmentKey(
            UUID constructionSiteId, UUID documentProjectId, UUID attachmentId, String extension) {
        return "construction-sites/%s/projects/%s/attachments/%s.%s"
                .formatted(constructionSiteId, documentProjectId, attachmentId, extension);
    }

    public static String materialDeliveryPhotoKey(UUID constructionSiteId, UUID materialId, UUID photoId, String extension) {
        return "construction-sites/%s/materials/%s/delivery-photos/%s.%s"
                .formatted(constructionSiteId, materialId, photoId, extension);
    }
}
