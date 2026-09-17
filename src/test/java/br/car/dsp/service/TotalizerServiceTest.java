package br.car.dsp.service;

import br.car.dsp.dto.AreaOfInterestAggregate;
import br.car.dsp.dto.AreaOfInterestMeasuresConfigResponse;
import br.car.dsp.dto.CentroidWgs84Projection;
import br.car.dsp.dto.DetailByIdentifierResponse;
import br.car.dsp.dto.DetailFieldConfigResponse;
import br.car.dsp.dto.HomeDetailSearchConfigResponse;
import br.car.dsp.dto.HomeKpisConfigResponse;
import br.car.dsp.dto.InstallationConfigResponse;
import br.car.dsp.dto.KpiCardConfigResponse;
import br.car.dsp.dto.KpiTotalsProjection;
import br.car.dsp.dto.HomeScreenConfigResponse;
import br.car.dsp.dto.ScreensConfigResponse;
import br.car.dsp.dto.TotalizerFilterRequest;
import br.car.dsp.dto.TotalizerResponse;
import br.car.dsp.model.AreaOfInterest;
import br.car.dsp.model.TerritoryLevel2;
import br.car.dsp.model.TerritoryLevel3;
import br.car.dsp.repository.AreaOfInterestRepository;
import br.car.dsp.repository.KpiMeasureRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotalizerServiceTest {

	@Mock
	private AreaOfInterestRepository areaOfInterestRepository;

	@Mock
	private KpiMeasureRepository kpiMeasureRepository;

	@Mock
	private InstallationConfigService installationConfigService;

	@Mock
	private AreaOfInterestAttributeReader areaOfInterestAttributeReader;

	@InjectMocks
	private TotalizerService totalizerService;

	@BeforeEach
	void stubDefaultInstallationConfig() {
		lenient().when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithThemes(
						"Registered properties",
						"un.",
						"ha"
				));
		lenient().when(kpiMeasureRepository.sumByKpiNameAll()).thenReturn(List.of());
		lenient().when(kpiMeasureRepository.sumByKpiNameAndLevel2Ids(anyCollection())).thenReturn(List.of());
		lenient().when(kpiMeasureRepository.sumByKpiNameAndLevel3Ids(anyCollection())).thenReturn(List.of());
		lenient().when(areaOfInterestRepository.findCentroidWgs84(anyString()))
				.thenReturn(Optional.of(centroid(-15.75, -47.85)));
	}

	@Test
	void getTotalizers_WhenFilterNull_ShouldUseRealAreaOfInterestAggregate() {
		when(areaOfInterestRepository.aggregateAll())
				.thenReturn(aggregate(508L, new BigDecimal("160652.4")));
		when(kpiMeasureRepository.sumByKpiNameAll())
				.thenReturn(List.of(
						projection("theme_1", new BigDecimal("10.4")),
						projection("theme_2", new BigDecimal("20.6")),
						projection("theme_4", new BigDecimal("5"))
				));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(5, result.size());
		TotalizerResponse primary = result.getFirst();
		assertEquals(TotalizerService.CODE_AREA_OF_INTEREST, primary.code());
		assertEquals("Registered properties", primary.name());
		assertEquals(508.0, primary.value());
		assertEquals(160652.40, primary.subItemValue());
		assertEquals("un.", primary.unitOfMeasurement());
		assertEquals("ha", primary.subItemName());

		assertEquals(TotalizerService.CODE_THEME_1, result.get(1).code());
		assertEquals(10.40, result.get(1).value());
		assertEquals(TotalizerService.CODE_THEME_2, result.get(2).code());
		assertEquals(20.60, result.get(2).value());
		assertEquals(0.0, result.get(3).value());
		assertEquals(5.00, result.get(4).value());
		verify(areaOfInterestRepository).aggregateAll();
		verify(kpiMeasureRepository).sumByKpiNameAll();
	}

	@Test
	void getTotalizers_WhenStateProvided_ShouldAggregateByLevel2() {
		when(areaOfInterestRepository.aggregateByLevel2Ids(List.of("DF")))
				.thenReturn(aggregate(12L, new BigDecimal("100.6")));

		TotalizerFilterRequest filter = new TotalizerFilterRequest();
		filter.setLevel2Ids(List.of("DF"));

		List<TotalizerResponse> result = totalizerService.getTotalizers(filter);

		TotalizerResponse primary = result.getFirst();
		assertEquals(12.0, primary.value());
		assertEquals(100.60, primary.subItemValue());
		verify(areaOfInterestRepository).aggregateByLevel2Ids(List.of("DF"));
		verify(kpiMeasureRepository).sumByKpiNameAndLevel2Ids(List.of("DF"));
	}

	@Test
	void getTotalizers_WhenCitiesProvided_ShouldAggregateByLevel3() {
		when(areaOfInterestRepository.aggregateByLevel3Ids(anyCollection()))
				.thenReturn(aggregate(3L, new BigDecimal("45")));

		TotalizerFilterRequest filter = new TotalizerFilterRequest();
		filter.setLevel2Ids(List.of("ES"));
		filter.setLevel3Ids(List.of("3200607"));

		List<TotalizerResponse> result = totalizerService.getTotalizers(filter);

		TotalizerResponse primary = result.getFirst();
		assertEquals(3.0, primary.value());
		assertEquals(45.00, primary.subItemValue());
		verify(areaOfInterestRepository).aggregateByLevel3Ids(eq(List.of("3200607")));
		verify(kpiMeasureRepository).sumByKpiNameAndLevel3Ids(eq(List.of("3200607")));
	}

	@Test
	void getTotalizers_WhenConfigLabelChanges_ShouldUseLabelFromConfig() {
		when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithThemes(
						"Registered properties",
						"un.",
						"ha"
				));
		when(areaOfInterestRepository.aggregateAll())
				.thenReturn(aggregate(10L, new BigDecimal("20")));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		TotalizerResponse primary = result.getFirst();
		assertEquals("Registered properties", primary.name());
		assertEquals("un.", primary.unitOfMeasurement());
		assertEquals("ha", primary.subItemName());
	}

	@Test
	void getDetailByIdentifier_WhenKnown_ShouldReturnDetail() {
		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertEquals("DF-123", result.id());
		assertEquals("Distrito Federal", result.territory().level2().name());
		assertEquals("Brasília", result.territory().level3().name());
		assertEquals("DF", result.territory().level2().id());
		assertEquals("5300108", result.territory().level3().id());
		assertEquals("2020-01-10", result.registrationDate());
		assertEquals("2024-06-15", result.alterationDate());
		assertEquals(0, new BigDecimal("120.50").compareTo(result.area()));
		assertNotNull(result.latitude());
		assertNotNull(result.longitude());
		assertEquals(List.of(), result.otherIds());
		assertEquals(Map.of(), result.attributes());
		verify(areaOfInterestAttributeReader, never()).read(anyString(), anyCollection());
	}

	@Test
	void getDetailByIdentifier_WhenFieldsConfigured_ShouldResolveAttributesInOrder() {
		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));
		when(installationConfigService.getInstallationConfig()).thenReturn(installationConfigWithDetailFields(
				new DetailFieldConfigResponse("id", "Identifier"),
				new DetailFieldConfigResponse("nome", "Property name"),
				new DetailFieldConfigResponse("calculated.latitude", "Centroid latitude")
		));
		when(areaOfInterestAttributeReader.read("DF-123", List.of("nome")))
				.thenReturn(Map.of("nome", "Sample property"));

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertEquals(List.of("id", "nome", "calculated.latitude"), List.copyOf(result.attributes().keySet()));
		assertEquals("DF-123", result.attributes().get("id"));
		assertEquals("Sample property", result.attributes().get("nome"));
		assertEquals(result.latitude(), result.attributes().get("calculated.latitude"));
		verify(areaOfInterestAttributeReader).read("DF-123", List.of("nome"));
	}

	@Test
	void getDetailByIdentifier_WhenUnknown_ShouldThrowNotFound() {
		when(areaOfInterestRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> totalizerService.getDetailByIdentifier("UNKNOWN")
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	@Test
	void getDetailsByCoordinates_WhenNone_ShouldThrowNotFound() {
		when(areaOfInterestRepository.findIdsContainingPoint(-15.75, -47.85))
				.thenReturn(List.of());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> totalizerService.getDetailsByCoordinates(-15.75, -47.85)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	@Test
	void getDetailsByCoordinates_WhenSingle_ShouldReturnEmptyOtherIds() {
		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		when(areaOfInterestRepository.findIdsContainingPoint(-15.75, -47.85))
				.thenReturn(List.of("DF-123"));
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));

		DetailByIdentifierResponse result =
				totalizerService.getDetailsByCoordinates(-15.75, -47.85);

		assertEquals("DF-123", result.id());
		assertEquals(List.of(), result.otherIds());
	}

	@Test
	void getDetailsByCoordinates_WhenMultiple_ShouldReturnOtherIds() {
		when(areaOfInterestRepository.findIdsContainingPoint(-15.75, -47.85))
				.thenReturn(List.of("DF-123", "DF-456", "DF-789"));
		when(areaOfInterestRepository.findById(org.mockito.ArgumentMatchers.anyString()))
				.thenAnswer(invocation -> {
					String id = invocation.getArgument(0);
					AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
					areaOfInterest.setId(id);
					return Optional.of(areaOfInterest);
				});

		DetailByIdentifierResponse result =
				totalizerService.getDetailsByCoordinates(-15.75, -47.85);

		assertTrue(Set.of("DF-123", "DF-456", "DF-789").contains(result.id()));
		assertEquals(2, result.otherIds().size());
		assertFalse(result.otherIds().contains(result.id()));
		Set<String> allIds = new HashSet<>();
		allIds.add(result.id());
		allIds.addAll(result.otherIds());
		assertEquals(Set.of("DF-123", "DF-456", "DF-789"), allIds);
	}

	@Test
	void getDetailsByCoordinates_WhenNullParams_ShouldThrowNotFound() {
		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> totalizerService.getDetailsByCoordinates(null, -47.85)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	@Test
	void getTotalizers_WhenKpisMissing_ShouldUseFallbackPrimaryCardWithDefaultUnits() {
		when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithoutKpis(null));
		when(areaOfInterestRepository.aggregateAll())
				.thenReturn(aggregate(10L, new BigDecimal("25.4")));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(1, result.size());
		TotalizerResponse primary = result.getFirst();
		assertEquals(TotalizerService.CODE_AREA_OF_INTEREST, primary.code());
		assertEquals(TotalizerService.CODE_AREA_OF_INTEREST, primary.name());
		assertEquals(10.0, primary.value());
		assertEquals(25.40, primary.subItemValue());
		assertEquals(AreaOfInterestMeasuresConfigResponse.DEFAULT_LABEL, primary.unitOfMeasurement());
		assertEquals(AreaOfInterestMeasuresConfigResponse.DEFAULT_UNIT, primary.subItemName());
	}

	@Test
	void getTotalizers_WhenKpisCardsAreEmpty_ShouldUseFallbackPrimaryCard() {
		when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithEmptyKpiCards());
		when(areaOfInterestRepository.aggregateAll())
				.thenReturn(aggregate(3L, new BigDecimal("10")));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(1, result.size());
		assertEquals(TotalizerService.CODE_AREA_OF_INTEREST, result.getFirst().code());
		assertEquals(3.0, result.getFirst().value());
	}

	@Test
	void getTotalizers_WhenKpisMissing_ShouldUseAreaOfInterestUnitsFromInstallationConfig() {
		when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithoutKpis(
						new AreaOfInterestMeasuresConfigResponse("hectares", "ha")
				));
		when(areaOfInterestRepository.aggregateAll())
				.thenReturn(aggregate(1L, new BigDecimal("1")));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		TotalizerResponse primary = result.getFirst();
		assertEquals("ha", primary.unitOfMeasurement());
		assertEquals("hectares", primary.subItemName());
	}

	@Test
	void getDetailsByCoordinates_WhenSelectedIdMissing_ShouldThrowNotFound() {
		when(areaOfInterestRepository.findIdsContainingPoint(-15.75, -47.85))
				.thenReturn(List.of("DF-999"));
		when(areaOfInterestRepository.findById("DF-999")).thenReturn(Optional.empty());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> totalizerService.getDetailsByCoordinates(-15.75, -47.85)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	@Test
	void getTotalizers_WhenKpisCardsNull_ShouldUseFallbackPrimaryCard() {
		when(installationConfigService.getInstallationConfig())
				.thenReturn(new InstallationConfigResponse(
						List.of(),
						null,
						new HomeKpisConfigResponse(5, TotalizerService.CODE_AREA_OF_INTEREST, null),
						new AreaOfInterestMeasuresConfigResponse("ha", "ha"),
						null,
						null
				));
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(4L, new BigDecimal("8")));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(1, result.size());
		assertEquals(TotalizerService.CODE_AREA_OF_INTEREST, result.getFirst().code());
	}

	@Test
	void getDetailByIdentifier_WhenLevel2HasOnlyName_ShouldReturnPartialLevelRef() {
		TerritoryLevel2 level2 = new TerritoryLevel2();
		level2.setName("Distrito Federal");

		TerritoryLevel3 level3 = new TerritoryLevel3();
		level3.setId("5300108");
		level3.setName("Brasília");
		level3.setParent(level2);

		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		areaOfInterest.setTerritoryLevel3(level3);
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertNull(result.territory().level2().id());
		assertEquals("Distrito Federal", result.territory().level2().name());
	}

	@Test
	void getDetailByIdentifier_WhenNull_ShouldThrowNotFound() {
		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> totalizerService.getDetailByIdentifier(null)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	@Test
	void getDetailByIdentifier_WhenLevel2HasOnlyId_ShouldReturnPartialLevelRef() {
		TerritoryLevel2 level2 = new TerritoryLevel2();
		level2.setId("DF");

		TerritoryLevel3 level3 = new TerritoryLevel3();
		level3.setId("5300108");
		level3.setName("Brasília");
		level3.setParent(level2);

		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		areaOfInterest.setTerritoryLevel3(level3);
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertEquals("DF", result.territory().level2().id());
		assertNull(result.territory().level2().name());
	}

	@Test
	void getDetailByIdentifier_WhenCentroidLongitudeMissing_ShouldReturnNullCoordinates() {
		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));
		when(areaOfInterestRepository.findCentroidWgs84("DF-123"))
				.thenReturn(Optional.of(partialCentroid(-15.75, null)));

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertNull(result.latitude());
		assertNull(result.longitude());
	}

	@Test
	void getDetailsByCoordinates_WhenLongitudeNull_ShouldThrowNotFound() {
		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> totalizerService.getDetailsByCoordinates(-15.75, null)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	@Test
	void getTotalizers_WhenAggregateCountNullButAreaPresent_ShouldUseZeroCount() {
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(null, new BigDecimal("12.6")));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(0.0, result.getFirst().value());
		assertEquals(12.60, result.getFirst().subItemValue());
	}

	@Test
	void getTotalizers_WhenAggregateAreaNullButCountPresent_ShouldUseZeroArea() {
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(7L, null));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(7.0, result.getFirst().value());
		assertEquals(0.0, result.getFirst().subItemValue());
	}

	@Test
	void getTotalizers_WhenFilterHasNullIdLists_ShouldAggregateAll() {
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(2L, BigDecimal.ONE));

		TotalizerFilterRequest filter = new TotalizerFilterRequest();
		filter.setLevel2Ids(null);
		filter.setLevel3Ids(null);

		totalizerService.getTotalizers(filter);

		verify(areaOfInterestRepository).aggregateAll();
	}

	@Test
	void getTotalizers_WhenKpisCardsContainNullEntry_ShouldIgnoreNullCard() {
		List<KpiCardConfigResponse> cards = new ArrayList<>();
		cards.add(null);
		cards.add(card(TotalizerService.CODE_AREA_OF_INTEREST, "Primary", 1));
		when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithCustomCards(cards, 5));
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(1L, BigDecimal.ONE));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(1, result.size());
	}

	@Test
	void getDetailByIdentifier_WhenBlank_ShouldThrowNotFound() {
		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> totalizerService.getDetailByIdentifier("   ")
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	@Test
	void getDetailByIdentifier_WhenTerritoryLevel3Missing_ShouldReturnNullTerritoryRefs() {
		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		areaOfInterest.setTerritoryLevel3(null);
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertNull(result.territory().level2());
		assertNull(result.territory().level3());
	}

	@Test
	void getDetailByIdentifier_WhenLevel3WithoutParent_ShouldReturnOnlyLevel3() {
		TerritoryLevel3 level3 = new TerritoryLevel3();
		level3.setId("5300108");
		level3.setName("Brasília");

		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		areaOfInterest.setTerritoryLevel3(level3);
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertNull(result.territory().level2());
		assertEquals("5300108", result.territory().level3().id());
		assertEquals("Brasília", result.territory().level3().name());
	}

	@Test
	void getDetailByIdentifier_WhenDatesMissing_ShouldReturnNullDates() {
		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		areaOfInterest.setRegistrationDate(null);
		areaOfInterest.setAlterationDate(null);
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertNull(result.registrationDate());
		assertNull(result.alterationDate());
	}

	@Test
	void getDetailByIdentifier_WhenCentroidProjectionMissing_ShouldReturnNullCoordinates() {
		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));
		when(areaOfInterestRepository.findCentroidWgs84("DF-123")).thenReturn(Optional.empty());

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertNull(result.latitude());
		assertNull(result.longitude());
	}

	@Test
	void getDetailByIdentifier_WhenCentroidLatitudeMissing_ShouldReturnNullCoordinates() {
		AreaOfInterest areaOfInterest = buildSampleAreaOfInterest();
		when(areaOfInterestRepository.findById("DF-123")).thenReturn(Optional.of(areaOfInterest));
		when(areaOfInterestRepository.findCentroidWgs84("DF-123"))
				.thenReturn(Optional.of(partialCentroid(null, -47.85)));

		DetailByIdentifierResponse result = totalizerService.getDetailByIdentifier("DF-123");

		assertNull(result.latitude());
		assertNull(result.longitude());
	}

	@Test
	void getDetailsByCoordinates_WhenMatchingIdsNull_ShouldThrowNotFound() {
		when(areaOfInterestRepository.findIdsContainingPoint(-15.75, -47.85)).thenReturn(null);

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> totalizerService.getDetailsByCoordinates(-15.75, -47.85)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
	}

	@Test
	void getTotalizers_ShouldSkipNullAndBlankCards() {
		List<KpiCardConfigResponse> cards = new ArrayList<>();
		cards.add(null);
		cards.add(new KpiCardConfigResponse(" ", "Blank", "ha", null, null, 2, false, null));
		cards.add(new KpiCardConfigResponse(
				TotalizerService.CODE_AREA_OF_INTEREST,
				"Registered properties",
				"un.",
				"ha",
				null,
				1,
				true,
				null
		));
		when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithCustomCards(cards, 5));
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(4L, new BigDecimal("8")));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(1, result.size());
		assertEquals(TotalizerService.CODE_AREA_OF_INTEREST, result.getFirst().code());
	}

	@Test
	void getTotalizers_WhenMaxCardsIsZero_ShouldUseDefaultLimit() {
		List<KpiCardConfigResponse> cards = List.of(
				card(TotalizerService.CODE_AREA_OF_INTEREST, "Primary", 1),
				card(TotalizerService.CODE_THEME_1, "Theme 1", 2),
				card(TotalizerService.CODE_THEME_2, "Theme 2", 3),
				card(TotalizerService.CODE_THEME_3, "Theme 3", 4),
				card(TotalizerService.CODE_THEME_4, "Theme 4", 5),
				card("EXTRA_THEME", "Extra", 6)
		);
		when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithCustomCards(cards, 0));
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(1L, BigDecimal.ONE));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(5, result.size());
		assertTrue(result.stream().noneMatch(item -> "EXTRA_THEME".equals(item.code())));
	}

	@Test
	void getTotalizers_WhenCardOrderIsZero_ShouldSortAfterPositiveOrders() {
		List<KpiCardConfigResponse> cards = List.of(
				card(TotalizerService.CODE_THEME_1, "Theme later", 0),
				card(TotalizerService.CODE_AREA_OF_INTEREST, "Primary", 1)
		);
		when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithCustomCards(cards, 5));
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(1L, BigDecimal.ONE));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(TotalizerService.CODE_AREA_OF_INTEREST, result.getFirst().code());
		assertEquals(TotalizerService.CODE_THEME_1, result.get(1).code());
	}

	@Test
	void getTotalizers_WhenAggregateFieldsAreNull_ShouldReturnZerosForPrimaryCard() {
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(null, null));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		TotalizerResponse primary = result.getFirst();
		assertEquals(0.0, primary.value());
		assertEquals(0.0, primary.subItemValue());
	}

	@Test
	void getTotalizers_WhenThemeAggregateIsNull_ShouldReturnZeroForThemeCards() {
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(1L, BigDecimal.ONE));
		when(kpiMeasureRepository.sumByKpiNameAll()).thenReturn(null);

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(0.0, result.get(1).value());
	}

	@Test
	void getTotalizers_WhenThemeValuesAreNull_ShouldTreatAsZero() {
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(1L, BigDecimal.ONE));
		when(kpiMeasureRepository.sumByKpiNameAll()).thenReturn(List.of(
				projection("theme_1", null),
				projection("theme_2", null),
				projection("theme_3", null),
				projection("theme_4", null)
		));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(0.0, result.get(1).value());
		assertEquals(0.0, result.get(2).value());
	}

	@Test
	void getTotalizers_WhenUnknownThemeCode_ShouldReturnZero() {
		List<KpiCardConfigResponse> cards = List.of(
				card(TotalizerService.CODE_AREA_OF_INTEREST, "Primary", 1),
				card("UNKNOWN_THEME", "Unknown", 2)
		);
		when(installationConfigService.getInstallationConfig())
				.thenReturn(installationConfigWithCustomCards(cards, 5));
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(1L, BigDecimal.ONE));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(0.0, result.get(1).value());
	}

	@Test
	void getTotalizers_WhenInstallationConfigIsNull_ShouldUseFallbackDefaults() {
		when(installationConfigService.getInstallationConfig()).thenReturn(null);
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(2L, new BigDecimal("3")));

		List<TotalizerResponse> result = totalizerService.getTotalizers(null);

		assertEquals(1, result.size());
		assertEquals(AreaOfInterestMeasuresConfigResponse.DEFAULT_LABEL, result.getFirst().unitOfMeasurement());
	}

	@Test
	void getTotalizers_WhenFilterHasOnlyBlankIds_ShouldAggregateAll() {
		when(areaOfInterestRepository.aggregateAll()).thenReturn(aggregate(5L, new BigDecimal("10")));

		TotalizerFilterRequest filter = new TotalizerFilterRequest();
		List<String> level2Ids = new ArrayList<>();
		level2Ids.add(" ");
		level2Ids.add(null);
		level2Ids.add("");
		filter.setLevel2Ids(level2Ids);
		filter.setLevel3Ids(new ArrayList<>(List.of("  ")));

		totalizerService.getTotalizers(filter);

		verify(areaOfInterestRepository).aggregateAll();
	}

	private static InstallationConfigResponse installationConfigWithoutKpis(
			AreaOfInterestMeasuresConfigResponse areaOfInterest
	) {
		return new InstallationConfigResponse(
				List.of(),
				null,
				null,
				areaOfInterest,
				null,
				null
		);
	}

	private static InstallationConfigResponse installationConfigWithEmptyKpiCards() {
		return new InstallationConfigResponse(
				List.of(),
				null,
				new HomeKpisConfigResponse(5, TotalizerService.CODE_AREA_OF_INTEREST, List.of()),
				new AreaOfInterestMeasuresConfigResponse("ha", "ha"),
				null,
				null
		);
	}

	private static InstallationConfigResponse installationConfigWithThemes(
			String label,
			String unitOfMeasurement,
			String optionalLabel
	) {
		List<KpiCardConfigResponse> cards = List.of(
				new KpiCardConfigResponse(
						TotalizerService.CODE_AREA_OF_INTEREST,
						label,
						unitOfMeasurement,
						optionalLabel,
						"#CED6E5",
						1,
						true,
						null
				),
				new KpiCardConfigResponse(
						TotalizerService.CODE_THEME_1, "Theme 1", "ha", null, "#C1D2F2", 2, false, "theme_1"),
				new KpiCardConfigResponse(
						TotalizerService.CODE_THEME_2, "Theme 2", "ha", null, "#98B7EC", 3, false, "theme_2"),
				new KpiCardConfigResponse(
						TotalizerService.CODE_THEME_3, "Theme 3", "ha", null, "#97CCE3", 4, false, "theme_3"),
				new KpiCardConfigResponse(
						TotalizerService.CODE_THEME_4, "Theme 4", "ha", null, "#B6C3D9", 5, false, "theme_4")
		);
		return new InstallationConfigResponse(
				List.of(),
				null,
				new HomeKpisConfigResponse(5, TotalizerService.CODE_AREA_OF_INTEREST, cards),
				new AreaOfInterestMeasuresConfigResponse("ha", "ha"),
				null,
				null
		);
	}

	private static InstallationConfigResponse installationConfigWithDetailFields(
			DetailFieldConfigResponse... fields
	) {
		InstallationConfigResponse base = installationConfigWithThemes(
				"Registered properties",
				"un.",
				"ha"
		);
		return new InstallationConfigResponse(
				base.hierarchy(),
				new ScreensConfigResponse(
						new HomeScreenConfigResponse(
								"Browse registered data",
								List.of("level2", "level3"),
								null,
								null,
								null,
								null,
								null,
								new HomeDetailSearchConfigResponse(
										"Search details",
										"Area of interest data",
										"Registration date",
										"Alteration date",
										"Latitude",
										"Longitude",
										"Area",
										"Download features",
										List.of(fields)
								)
						),
						null
				),
				base.kpis(),
				base.areaOfInterest(),
				base.formats(),
				base.map()
		);
	}

	private static InstallationConfigResponse installationConfigWithCustomCards(
			List<KpiCardConfigResponse> cards,
			int maxCards
	) {
		return new InstallationConfigResponse(
				List.of(),
				null,
				new HomeKpisConfigResponse(maxCards, TotalizerService.CODE_AREA_OF_INTEREST, cards),
				new AreaOfInterestMeasuresConfigResponse("ha", "ha"),
				null,
				null
		);
	}

	private static KpiCardConfigResponse card(String code, String label, int order) {
		return new KpiCardConfigResponse(code, label, "ha", null, null, order, false, null);
	}

	private static KpiTotalsProjection projection(String kpiName, BigDecimal total) {
		return new KpiTotalsProjection() {
			@Override
			public String getKpiName() {
				return kpiName;
			}

			@Override
			public BigDecimal getTotal() {
				return total;
			}
		};
	}

	private static CentroidWgs84Projection partialCentroid(Double latitude, Double longitude) {
		return new CentroidWgs84Projection() {
			@Override
			public Double getLatitude() {
				return latitude;
			}

			@Override
			public Double getLongitude() {
				return longitude;
			}
		};
	}

	private static CentroidWgs84Projection centroid(double latitude, double longitude) {
		return new CentroidWgs84Projection() {
			@Override
			public Double getLatitude() {
				return latitude;
			}

			@Override
			public Double getLongitude() {
				return longitude;
			}
		};
	}

	private static AreaOfInterestAggregate aggregate(Long count, BigDecimal totalArea) {
		return new AreaOfInterestAggregate() {
			@Override
			public Long getCount() {
				return count;
			}

			@Override
			public BigDecimal getTotalArea() {
				return totalArea;
			}
		};
	}

	private static AreaOfInterest buildSampleAreaOfInterest() {
		TerritoryLevel2 level2 = new TerritoryLevel2();
		level2.setId("DF");
		level2.setName("Distrito Federal");

		TerritoryLevel3 level3 = new TerritoryLevel3();
		level3.setId("5300108");
		level3.setName("Brasília");
		level3.setParent(level2);

		GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 0);
		AreaOfInterest areaOfInterest = new AreaOfInterest();
		areaOfInterest.setId("DF-123");
		areaOfInterest.setRegistrationDate(OffsetDateTime.of(2020, 1, 10, 8, 30, 0, 0, ZoneOffset.UTC));
		areaOfInterest.setAlterationDate(OffsetDateTime.of(2024, 6, 15, 14, 0, 0, 0, ZoneOffset.UTC));
		areaOfInterest.setArea(new BigDecimal("120.50"));
		areaOfInterest.setTerritoryLevel3(level3);
		areaOfInterest.setCentroidCoordinates(
				factory.createPoint(new Coordinate(-47.85, -15.75))
		);
		return areaOfInterest;
	}
}
