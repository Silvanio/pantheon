package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.repository.CompanyRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.storage.StorageService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PdfBrandingServiceTest {

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StorageService storageService;

    private PdfBrandingService service;

    private UUID siteId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        service = new PdfBrandingService(siteRepository, companyRepository, storageService);
        siteId = UUID.randomUUID();
        companyId = UUID.randomUUID();
    }

    private ConstructionSite site() {
        return new ConstructionSite(
                siteId, companyId, "Obra Central", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }

    @Test
    void resolveUsesTradeNameWhenSet() {
        Company company = new Company(companyId, "Nome Cadastro", UUID.randomUUID(), Instant.now());
        company.completeProfile("Razao Social LTDA", "Nome Fantasia", "12345678000199", "Endereco", null, Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        PdfBrandingService.Branding branding = service.resolve(siteId);

        assertThat(branding.companyName()).isEqualTo("Nome Fantasia");
        assertThat(branding.siteName()).isEqualTo("Obra Central");
        assertThat(branding.logoDataUri()).isNull();
    }

    @Test
    void resolveFallsBackToBareNameWithoutTradeName() {
        Company company = new Company(companyId, "Nome Cadastro", UUID.randomUUID(), Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        PdfBrandingService.Branding branding = service.resolve(siteId);

        assertThat(branding.companyName()).isEqualTo("Nome Cadastro");
    }

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0};

    @Test
    void resolveDetectsPngFromItsRealBytesRegardlessOfStorageKeyExtension() {
        Company company = new Company(companyId, "Nome Cadastro", UUID.randomUUID(), Instant.now());
        // Regression: the storage key's extension used to be trusted for content-type, which broke
        // whenever it didn't match the actual bytes (e.g. an original upload with no extension,
        // stored as "logo.bin"). The real format must be sniffed from the bytes themselves.
        company.completeProfile(null, null, null, null, "companies/" + companyId + "/logo.bin", Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(storageService.getObject(eq("companies/" + companyId + "/logo.bin"))).thenReturn(PNG_MAGIC);

        PdfBrandingService.Branding branding = service.resolve(siteId);

        assertThat(branding.logoDataUri())
                .isEqualTo("data:image/png;base64," + Base64.getEncoder().encodeToString(PNG_MAGIC));
    }

    @Test
    void resolveDetectsJpegFromItsRealBytes() {
        Company company = new Company(companyId, "Nome Cadastro", UUID.randomUUID(), Instant.now());
        company.completeProfile(null, null, null, null, "companies/" + companyId + "/logo.jpg", Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(storageService.getObject(eq("companies/" + companyId + "/logo.jpg"))).thenReturn(JPEG_MAGIC);

        PdfBrandingService.Branding branding = service.resolve(siteId);

        assertThat(branding.logoDataUri()).startsWith("data:image/jpeg;base64,");
    }

    @Test
    void resolveDetectsSvgFromItsRealBytes() {
        // Regression: a company logo uploaded as SVG (the Company-settings file picker accepts any
        // image/* type) used to fall through to the "image/jpeg" default, which openhtmltopdf's
        // decoder can't parse from SVG XML — the logo silently failed to render in every PDF.
        Company company = new Company(companyId, "Nome Cadastro", UUID.randomUUID(), Instant.now());
        company.completeProfile(null, null, null, null, "companies/" + companyId + "/logo.svg", Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        byte[] svgBytes = "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle r=\"5\"/></svg>"
                .getBytes(StandardCharsets.UTF_8);
        when(storageService.getObject(eq("companies/" + companyId + "/logo.svg"))).thenReturn(svgBytes);

        PdfBrandingService.Branding branding = service.resolve(siteId);

        assertThat(branding.logoDataUri()).startsWith("data:image/svg+xml;base64,");
    }

    @Test
    void resolveDetectsSvgWithLeadingXmlDeclaration() {
        Company company = new Company(companyId, "Nome Cadastro", UUID.randomUUID(), Instant.now());
        company.completeProfile(null, null, null, null, "companies/" + companyId + "/logo.svg", Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site()));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        byte[] svgBytes = ("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"><circle r=\"5\"/></svg>")
                .getBytes(StandardCharsets.UTF_8);
        when(storageService.getObject(eq("companies/" + companyId + "/logo.svg"))).thenReturn(svgBytes);

        PdfBrandingService.Branding branding = service.resolve(siteId);

        assertThat(branding.logoDataUri()).startsWith("data:image/svg+xml;base64,");
    }

    @Test
    void renderHeaderHtmlIncludesNamesAndLogoWhenPresent() {
        PdfBrandingService.Branding branding =
                new PdfBrandingService.Branding("Empresa X", "Obra Y", "data:image/png;base64,QQ==");

        String html = service.renderHeaderHtml(branding);

        assertThat(html).contains("Empresa X").contains("Obra Y");
        assertThat(html).contains("<img");
        assertThat(html).contains("data:image/png;base64,QQ==");
    }

    @Test
    void renderHeaderHtmlOmitsImageWhenNoLogo() {
        PdfBrandingService.Branding branding = new PdfBrandingService.Branding("Empresa X", "Obra Y", null);

        String html = service.renderHeaderHtml(branding);

        assertThat(html).contains("Empresa X").contains("Obra Y");
        assertThat(html).doesNotContain("<img");
    }
}
