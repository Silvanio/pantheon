package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.FornecedorRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Fornecedor;
import com.pantheon.service.entity.FornecedorPaymentMethod;
import com.pantheon.service.exception.CnpjPrefixTooShortException;
import com.pantheon.service.exception.InvalidCpfCnpjException;
import com.pantheon.service.exception.PixKeyRequiredException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.FornecedorRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock
    private FornecedorRepository fornecedorRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    private FornecedorService service;

    private UUID siteId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        service = new FornecedorService(fornecedorRepository, siteRepository, siteAccessService);
        siteId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(new ConstructionSite(
                siteId, companyId, "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now())));
    }

    @Test
    void findOrCreateCreatesNewFornecedorForUnseenCnpj() {
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "11222333000181")).thenReturn(Optional.empty());
        when(fornecedorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Fornecedor result = service.findOrCreate(
                companyId, UUID.randomUUID(),
                new FornecedorRequest("11222333000181", "Nova Empresa", null, null, null, null, null));

        assertThat(result.getCnpj()).isEqualTo("11222333000181");
        assertThat(result.getName()).isEqualTo("Nova Empresa");
        verify(fornecedorRepository).save(any());
    }

    @Test
    void findOrCreateReusesExistingFornecedorForKnownCnpj() {
        Fornecedor existing = new Fornecedor(
                UUID.randomUUID(), companyId, "11222333000181", "Empresa Original", null, null, null, null, null,
                UUID.randomUUID(), Instant.now());
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "11222333000181")).thenReturn(Optional.of(existing));

        Fornecedor result = service.findOrCreate(
                companyId, UUID.randomUUID(),
                new FornecedorRequest(
                        "11222333000181", "Nome Diferente Ignorado", null, null, null, FornecedorPaymentMethod.PIX,
                        "chave-ignorada"));

        assertThat(result).isEqualTo(existing);
        assertThat(result.getName()).isEqualTo("Empresa Original");
        assertThat(result.getPaymentMethod()).isNull();
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void findOrCreateAcceptsAValidCpfAsTheDocument() {
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "11144477735")).thenReturn(Optional.empty());
        when(fornecedorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Fornecedor result = service.findOrCreate(
                companyId, UUID.randomUUID(), new FornecedorRequest("11144477735", "Pessoa Física", null, null, null, null, null));

        assertThat(result.getCnpj()).isEqualTo("11144477735");
    }

    @Test
    void findOrCreateRejectsInvalidCheckDigits() {
        assertThatThrownBy(() -> service.findOrCreate(
                companyId, UUID.randomUUID(), new FornecedorRequest("11144477736", "Empresa", null, null, null, null, null)))
                .isInstanceOf(InvalidCpfCnpjException.class);
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void findOrCreateRejectsDocumentWithNeitherCpfNorCnpjLength() {
        assertThatThrownBy(() -> service.findOrCreate(
                companyId, UUID.randomUUID(), new FornecedorRequest("12345", "Empresa", null, null, null, null, null)))
                .isInstanceOf(InvalidCpfCnpjException.class);
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void findOrCreateAlwaysCreatesFreshFornecedorWhenNoDocumentGiven() {
        when(fornecedorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Fornecedor first = service.findOrCreate(
                companyId, UUID.randomUUID(), new FornecedorRequest(null, "Sem Documento", null, null, null, null, null));
        Fornecedor second = service.findOrCreate(
                companyId, UUID.randomUUID(), new FornecedorRequest("", "Sem Documento", null, null, null, null, null));

        assertThat(first.getCnpj()).isNull();
        assertThat(second.getCnpj()).isEmpty();
        assertThat(first.getId()).isNotEqualTo(second.getId());
        verify(fornecedorRepository, never()).findByCompanyIdAndCnpj(any(), any());
    }

    @Test
    void searchReturnsMatchesByPrefix() {
        Fornecedor match = new Fornecedor(
                UUID.randomUUID(), companyId, "11222333000181", "Empresa", null, null, null, null, null,
                UUID.randomUUID(), Instant.now());
        when(fornecedorRepository.findByCompanyIdAndCnpjStartingWithOrderByNameAsc(companyId, "12345"))
                .thenReturn(List.of(match));

        List<Fornecedor> result = service.searchByCnpjPrefix(siteId, UUID.randomUUID(), "12345");

        assertThat(result).containsExactly(match);
    }

    @Test
    void searchOnlyReturnsFornecedoresFromTheSitesOwnCompany() {
        UUID otherCompanyId = UUID.randomUUID();
        // A matching Fornecedor would exist under a DIFFERENT company for the same prefix; the
        // site's own company has none. The search must never leak the other company's record.
        // Deliberately not stubbing otherCompanyId's call: it must never happen, and Mockito's
        // default answer for an unstubbed method returning a List is an empty list anyway.
        when(fornecedorRepository.findByCompanyIdAndCnpjStartingWithOrderByNameAsc(companyId, "11222"))
                .thenReturn(List.of());

        List<Fornecedor> result = service.searchByCnpjPrefix(siteId, UUID.randomUUID(), "11222");

        assertThat(result).isEmpty();
        verify(fornecedorRepository).findByCompanyIdAndCnpjStartingWithOrderByNameAsc(companyId, "11222");
        verify(fornecedorRepository, never())
                .findByCompanyIdAndCnpjStartingWithOrderByNameAsc(eq(otherCompanyId), any());
    }

    @Test
    void findOrCreateRejectsPixWithoutKey() {
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "11222333000181")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOrCreate(
                companyId, UUID.randomUUID(),
                new FornecedorRequest("11222333000181", "Empresa", null, null, null, FornecedorPaymentMethod.PIX, " ")))
                .isInstanceOf(PixKeyRequiredException.class);
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void findOrCreatePersistsPixMethodAndKey() {
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "11222333000181")).thenReturn(Optional.empty());
        when(fornecedorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Fornecedor result = service.findOrCreate(
                companyId, UUID.randomUUID(),
                new FornecedorRequest(
                        "11222333000181", "Empresa", null, null, null, FornecedorPaymentMethod.PIX, "chave@pix.com"));

        assertThat(result.getPaymentMethod()).isEqualTo(FornecedorPaymentMethod.PIX);
        assertThat(result.getPixKey()).isEqualTo("chave@pix.com");
    }

    @Test
    void findOrCreateDiscardsPixKeyForNonPixMethod() {
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "11222333000181")).thenReturn(Optional.empty());
        when(fornecedorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Fornecedor result = service.findOrCreate(
                companyId, UUID.randomUUID(),
                new FornecedorRequest(
                        "11222333000181", "Empresa", null, null, null, FornecedorPaymentMethod.DINHEIRO,
                        "chave-nao-deveria-ficar"));

        assertThat(result.getPaymentMethod()).isEqualTo(FornecedorPaymentMethod.DINHEIRO);
        assertThat(result.getPixKey()).isNull();
    }

    @Test
    void searchRejectsPrefixShorterThanFiveCharacters() {
        assertThatThrownBy(() -> service.searchByCnpjPrefix(siteId, UUID.randomUUID(), "1234"))
                .isInstanceOf(CnpjPrefixTooShortException.class);
    }

    @Test
    void sameCnpjUnderDifferentCompaniesStaysSeparate() {
        UUID otherCompanyId = UUID.randomUUID();
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "11222333000181")).thenReturn(Optional.empty());
        when(fornecedorRepository.findByCompanyIdAndCnpj(otherCompanyId, "11222333000181")).thenReturn(Optional.empty());
        when(fornecedorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FornecedorRequest request = new FornecedorRequest("11222333000181", "Empresa", null, null, null, null, null);
        Fornecedor first = service.findOrCreate(companyId, UUID.randomUUID(), request);
        Fornecedor second = service.findOrCreate(otherCompanyId, UUID.randomUUID(), request);

        assertThat(first.getCompanyId()).isEqualTo(companyId);
        assertThat(second.getCompanyId()).isEqualTo(otherCompanyId);
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }
}
