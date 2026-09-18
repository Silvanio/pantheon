package com.pantheon.service.service;

import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.exception.CompanyNotFoundException;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.repository.CompanyRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.storage.StorageService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Resolves the company/site branding (logo, company name, project name) shared by every PDF this
 * service generates, and renders it as one HTML header fragment — see {@code purchase-requests}
 * and {@code daily-report-media-and-signoff}'s PDF requirements.
 */
@Service
public class PdfBrandingService {

    /**
     * Base CSS shared by every generated PDF, appended alongside each document's own rules.
     * Mostly neutral grays for a clean, uncluttered layout, with the product's blue
     * ({@code pantheon-web}'s {@code blueprint} token) reserved for a few deliberate accents
     * (header rule, section labels, table header fill, totals) rather than large filled blocks —
     * the balance asked for after the all-blue first pass and the all-gray second pass.
     */
    public static final String STYLE = "@page{margin:30px 36px;}"
            + "*{box-sizing:border-box;}"
            + "body{font-family:Helvetica,Arial,sans-serif;font-size:9.5px;color:#33373d;line-height:1.5;}"
            + "h1{font-size:15px;font-weight:600;margin:14px 0 2px;color:#17191d;}"
            + "h2{font-size:8.5px;font-weight:700;text-transform:uppercase;letter-spacing:0.05em;color:#2148d6;"
            + "margin:16px 0 6px;padding-bottom:3px;border-bottom:1px solid #e3e5e8;}"
            + "table.data{width:100%;border-collapse:collapse;margin-top:2px;}"
            + "table.data th{background:#eef4ff;color:#1c39ac;font-size:8px;font-weight:700;text-transform:uppercase;"
            + "letter-spacing:0.03em;text-align:left;padding:6px 10px;border-bottom:1.5px solid #b8d0ff;}"
            + "table.data td{padding:6px 10px;font-size:9.5px;color:#33373d;border-bottom:1px solid #edeeef;}"
            + "table.data tbody tr:last-child td{border-bottom:none;}"
            + ".muted{color:#9aa1ab;} .italic{font-style:italic;}"
            + ".right{text-align:right;}"
            + ".total-line{margin-top:8px;padding-top:8px;border-top:1.5px solid #b8d0ff;text-align:right;"
            + "font-size:11px;font-weight:700;color:#1c39ac;}"
            + ".total-line .label{font-size:8px;font-weight:600;text-transform:uppercase;letter-spacing:0.04em;"
            + "color:#9aa1ab;margin-right:10px;}"
            + ".meta-card{background:#f7f9ff;border:1px solid #d9e6ff;border-left:3px solid #2148d6;border-radius:4px;"
            + "padding:9px 13px;margin:9px 0;}"
            + ".meta-row{padding:1.5px 0;font-size:9px;} .meta-row .label{color:#5f6874;display:inline-block;min-width:104px;}"
            + ".meta-row .value{color:#17191d;font-weight:600;}"
            + ".badge{display:inline-block;background:#eef4ff;color:#1c39ac;border-radius:3px;padding:2px 8px;"
            + "font-size:7.5px;font-weight:700;text-transform:uppercase;letter-spacing:0.03em;white-space:nowrap;}"
            + ".pdf-header{width:100%;padding-bottom:12px;margin-bottom:2px;border-bottom:3px solid #2148d6;}"
            + ".pdf-header td{border:none;padding:0;vertical-align:middle;}"
            + ".pdf-logo-cell{width:44px;}"
            + ".pdf-logo{max-width:36px;max-height:36px;}"
            + ".pdf-company-name{font-size:12px;font-weight:700;color:#17191d;}"
            + ".pdf-site-name{font-size:9px;color:#2148d6;margin-top:1px;}"
            + ".footer-note{margin-top:20px;padding-top:8px;border-top:1px solid #ececed;font-size:7.5px;color:#b5b9bf;}";

    private final ConstructionSiteRepository siteRepository;
    private final CompanyRepository companyRepository;
    private final StorageService storageService;

    public PdfBrandingService(
            ConstructionSiteRepository siteRepository, CompanyRepository companyRepository,
            StorageService storageService) {
        this.siteRepository = siteRepository;
        this.companyRepository = companyRepository;
        this.storageService = storageService;
    }

    public record Branding(String companyName, String siteName, String logoDataUri) {
    }

    public Branding resolve(UUID constructionSiteId) {
        ConstructionSite site = siteRepository.findById(constructionSiteId)
                .orElseThrow(() -> new ConstructionSiteNotFoundException(constructionSiteId));
        Company company = companyRepository.findById(site.getCompanyId())
                .orElseThrow(() -> new CompanyNotFoundException(site.getCompanyId()));

        String companyName = company.getTradeName() != null && !company.getTradeName().isBlank()
                ? company.getTradeName()
                : company.getName();

        String logoDataUri = null;
        if (company.getLogoObjectKey() != null) {
            byte[] bytes = storageService.getObject(company.getLogoObjectKey());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            logoDataUri = "data:" + sniffImageContentType(bytes) + ";base64," + base64;
        }

        return new Branding(companyName, site.getName(), logoDataUri);
    }

    public String renderHeaderHtml(Branding branding) {
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"pdf-header\"><tr>");
        if (branding.logoDataUri() != null) {
            html.append("<td class=\"pdf-logo-cell\"><img class=\"pdf-logo\" src=\"")
                    .append(branding.logoDataUri()).append("\" /></td>");
        }
        html.append("<td><div class=\"pdf-company-name\">").append(escape(branding.companyName())).append("</div>")
                .append("<div class=\"pdf-site-name\">").append(escape(branding.siteName())).append("</div></td>");
        html.append("</tr></table>");
        return html.toString();
    }

    /**
     * The declared MIME type in a data: URI is what openhtmltopdf uses to pick an image decoder —
     * a mismatch (e.g. a PNG stored under a ".bin"/misdetected extension) makes the image silently
     * fail to render with no visible error. Sniffing the real format from its magic bytes, rather
     * than trusting the storage key's file extension, avoids that whole class of bug.
     */
    private String sniffImageContentType(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        if (looksLikeSvg(bytes)) {
            return "image/svg+xml";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private boolean looksLikeSvg(byte[] bytes) {
        int length = Math.min(bytes.length, 300);
        String prefix = new String(bytes, 0, length, StandardCharsets.UTF_8).stripLeading().toLowerCase(Locale.ROOT);
        return prefix.startsWith("<svg") || (prefix.startsWith("<?xml") && prefix.contains("<svg"));
    }

    private String escape(String value) {
        if (value == null) {
            return "—";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
