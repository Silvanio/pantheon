package com.pantheon.service.dto;

import java.util.List;

public record SiteDocumentFolderContentsResponse(
        List<SiteDocumentProjectResponse> folders, List<SiteDocumentProjectAttachmentResponse> files) {
}
