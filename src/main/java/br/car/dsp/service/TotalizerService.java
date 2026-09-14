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
import br.car.dsp.dto.TerritoryLevelRefResponse;
import br.car.dsp.dto.TerritoryLevelsResponse;
import br.car.dsp.dto.TotalizerFilterRequest;
import br.car.dsp.dto.TotalizerResponse;
import br.car.dsp.model.AreaOfInterest;
import br.car.dsp.model.TerritoryLevel2;
import br.car.dsp.model.TerritoryLevel3;
import br.car.dsp.repository.AreaOfInterestRepository;
import br.car.dsp.repository.KpiMeasureRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TotalizerService {

	public static final String CODE_AREA_OF_INTEREST = "AREA_OF_INTEREST";
	public static final String CODE_THEME_1 = "THEME_1";
	public static final String CODE_THEME_2 = "THEME_2";
	public static final String CODE_THEME_3 = "THEME_3";
	public static final String CODE_THEME_4 = "THEME_4";

	private static final int DEFAULT_MAX_CARDS = 5;

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final Set<String> CANONICAL_AOI_DETAIL_FIELDS = Set.of(
			"id",
			"created_at",
			"updated_at",
			"area"
	);

	private static final Set<String> CALCULATED_AOI_DETAIL_FIELDS = Set.of(
			"calculated.latitude",
			"calculated.longitude",
			"calculated.territory_level_2_name",
			"calculated.territory_level_3_name"
	);

	private final AreaOfInterestRepository areaOfInterestRepository;
	private final KpiMeasureRepository kpiMeasureRepository;
	private final InstallationConfigService installationConfigService;
	private final AreaOfInterestAttributeReader areaOfInterestAttributeReader;

	@Transactional(readOnly = true)
	public List<TotalizerResponse> getTotalizers(TotalizerFilterRequest filter) {
		List<String> level2Ids = filter != null ? filter.getLevel2Ids() : List.of();
		List<String> level3Ids = filter != null ? filter.getLevel3Ids() : List.of();

		InstallationConfigResponse config = installationConfigService.getInstallationConfig();
		List<KpiCardConfigResponse> cards = resolveCards(config);
		AreaOfInterestAggregate aggregate = resolveAggregate(level2Ids, level3Ids);
		Map<String, BigDecimal> kpiTotals = needsKpiTotals(cards)
				? resolveKpiTotals(level2Ids, level3Ids)
				: Map.of();

		List<TotalizerResponse> totalizers = new ArrayList<>(cards.size());
		for (KpiCardConfigResponse card : cards) {
			if (card == null || card.code() == null || card.code().isBlank()) {
				continue;
			}
			totalizers.add(toTotalizer(card, aggregate, kpiTotals));
		}
		return totalizers;
	}

	@Transactional(readOnly = true)
	public DetailByIdentifierResponse getDetailByIdentifier(String identifier) {
		if (identifier == null || identifier.isBlank()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Identifier not found");
		}

		AreaOfInterest areaOfInterest = areaOfInterestRepository.findById(identifier.trim())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Identifier not found"
				));

		Centroid centroid = resolveCentroid(areaOfInterestRepository.findCentroidWgs84(areaOfInterest.getId()));
		return toDetailResponse(areaOfInterest, centroid, List.of(), detailFields());
	}

	@Transactional(readOnly = true)
	public DetailByIdentifierResponse getDetailsByCoordinates(Double latitude, Double longitude) {
		if (latitude == null || longitude == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Identifier not found");
		}

		List<String> matchingIds = areaOfInterestRepository.findIdsContainingPoint(latitude, longitude);
		if (matchingIds == null || matchingIds.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Identifier not found");
		}

		List<String> shuffledIds = new ArrayList<>(matchingIds);
		Collections.shuffle(shuffledIds);

		String selectedId = shuffledIds.getFirst();
		List<String> otherIds = shuffledIds.stream().skip(1).toList();

		AreaOfInterest areaOfInterest = areaOfInterestRepository.findById(selectedId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Identifier not found"
				));

		Centroid centroid = resolveCentroid(areaOfInterestRepository.findCentroidWgs84(areaOfInterest.getId()));
		return toDetailResponse(areaOfInterest, centroid, otherIds, detailFields());
	}

	private DetailByIdentifierResponse toDetailResponse(
			AreaOfInterest areaOfInterest,
			Centroid centroid,
			List<String> otherIds,
			List<DetailFieldConfigResponse> fields
	) {
		TerritoryLevel3 level3 = areaOfInterest.getTerritoryLevel3();
		TerritoryLevel2 level2 = level3 != null ? level3.getParent() : null;

		return new DetailByIdentifierResponse(
				areaOfInterest.getId(),
				centroid.latitude(),
				centroid.longitude(),
				new TerritoryLevelsResponse(
						toLevelRef(level2 != null ? level2.getId() : null,
								level2 != null ? level2.getName() : null),
						toLevelRef(level3 != null ? level3.getId() : null,
								level3 != null ? level3.getName() : null)
				),
				formatDate(areaOfInterest.getRegistrationDate()),
				formatDate(areaOfInterest.getAlterationDate()),
				areaOfInterest.getArea(),
				otherIds != null ? otherIds : List.of(),
				resolveAttributes(areaOfInterest, centroid, level2, level3, fields)
		);
	}

	private List<DetailFieldConfigResponse> detailFields() {
		InstallationConfigResponse config = installationConfigService.getInstallationConfig();
		ScreensConfigResponse screens = config != null ? config.screens() : null;
		HomeScreenConfigResponse home = screens != null ? screens.home() : null;
		HomeDetailSearchConfigResponse detail = home != null ? home.detail() : null;
		if (detail == null || detail.fields() == null || detail.fields().isEmpty()) {
			return List.of();
		}
		return detail.fields();
	}

	private Map<String, Object> resolveAttributes(
			AreaOfInterest areaOfInterest,
			Centroid centroid,
			TerritoryLevel2 level2,
			TerritoryLevel3 level3,
			List<DetailFieldConfigResponse> fields
	) {
		if (fields == null || fields.isEmpty()) {
			return Map.of();
		}

		List<String> extras = fields.stream()
				.map(DetailFieldConfigResponse::field)
				.filter(TotalizerService::isExtraColumn)
				.toList();
		Map<String, Object> extraValues = extras.isEmpty()
				? Map.of()
				: areaOfInterestAttributeReader.read(areaOfInterest.getId(), extras);

		LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
		for (DetailFieldConfigResponse item : fields) {
			String field = item != null ? item.field() : null;
			if (field == null || field.isBlank()) {
				continue;
			}
			if (field.startsWith("calculated.") && !CALCULATED_AOI_DETAIL_FIELDS.contains(field)) {
				continue;
			}
			attributes.put(field, attributeValue(
					field,
					areaOfInterest,
					centroid,
					level2,
					level3,
					extraValues
			));
		}
		return attributes;
	}

	private static Object attributeValue(
			String field,
			AreaOfInterest areaOfInterest,
			Centroid centroid,
			TerritoryLevel2 level2,
			TerritoryLevel3 level3,
			Map<String, Object> extraValues
	) {
		return switch (field) {
			case "id" -> areaOfInterest.getId();
			case "created_at" -> formatDate(areaOfInterest.getRegistrationDate());
			case "updated_at" -> formatDate(areaOfInterest.getAlterationDate());
			case "area" -> areaOfInterest.getArea();
			case "calculated.latitude" -> centroid.latitude();
			case "calculated.longitude" -> centroid.longitude();
			case "calculated.territory_level_2_name" -> level2 != null ? level2.getName() : null;
			case "calculated.territory_level_3_name" -> level3 != null ? level3.getName() : null;
			default -> extraValues.get(field);
		};
	}

	private static boolean isExtraColumn(String field) {
		if (field == null || field.isBlank() || field.startsWith("calculated.")) {
			return false;
		}
		return !CANONICAL_AOI_DETAIL_FIELDS.contains(field);
	}

	private static boolean needsKpiTotals(List<KpiCardConfigResponse> cards) {
		return cards.stream().anyMatch(card -> card.layer() != null && !card.layer().isBlank());
	}

	private List<KpiCardConfigResponse> resolveCards(InstallationConfigResponse config) {
		HomeKpisConfigResponse kpis = config != null ? config.kpis() : null;
		if (kpis == null || kpis.cards() == null || kpis.cards().isEmpty()) {
			return List.of(fallbackPrimaryCard(config));
		}

		int maxCards = kpis.maxCards() > 0 ? Math.min(kpis.maxCards(), DEFAULT_MAX_CARDS) : DEFAULT_MAX_CARDS;
		return kpis.cards().stream()
				.filter(card -> card != null && card.code() != null && !card.code().isBlank())
				.sorted(Comparator.comparingInt(card -> card.order() > 0 ? card.order() : Integer.MAX_VALUE))
				.limit(maxCards)
				.toList();
	}

	private static KpiCardConfigResponse fallbackPrimaryCard(InstallationConfigResponse config) {
		AreaOfInterestMeasuresConfigResponse measures = config != null && config.areaOfInterest() != null
				? config.areaOfInterest()
				: AreaOfInterestMeasuresConfigResponse.defaults();
		return new KpiCardConfigResponse(
				CODE_AREA_OF_INTEREST,
				CODE_AREA_OF_INTEREST,
				measures.areaUnitLabel(),
				measures.areaUnit(),
				null,
				1,
				true,
				null
		);
	}

	private TotalizerResponse toTotalizer(
			KpiCardConfigResponse card,
			AreaOfInterestAggregate aggregate,
			Map<String, BigDecimal> kpiTotals
	) {
		String code = card.code().trim();
		if (CODE_AREA_OF_INTEREST.equals(code)) {
			return toAreaOfInterestTotalizer(aggregate, card);
		}
		BigDecimal themeSum = kpiTotalForCard(card, kpiTotals);
		double value = themeSum.setScale(2, RoundingMode.HALF_UP).doubleValue();
		return new TotalizerResponse(
				card.label(),
				code,
				value,
				null,
				null,
				card.unitOfMeasurement()
		);
	}

	private static TotalizerResponse toAreaOfInterestTotalizer(
			AreaOfInterestAggregate aggregate,
			KpiCardConfigResponse card
	) {
		long count = aggregate != null && aggregate.getCount() != null
				? aggregate.getCount()
				: 0L;
		BigDecimal totalArea = aggregate != null && aggregate.getTotalArea() != null
				? aggregate.getTotalArea()
				: BigDecimal.ZERO;
		double areaSum = totalArea.setScale(2, RoundingMode.HALF_UP).doubleValue();
		return new TotalizerResponse(
				card.label(),
				CODE_AREA_OF_INTEREST,
				(double) count,
				card.optionalLabel(),
				areaSum,
				card.unitOfMeasurement()
		);
	}

	private static BigDecimal kpiTotalForCard(KpiCardConfigResponse card, Map<String, BigDecimal> kpiTotals) {
		if (card == null || card.layer() == null || card.layer().isBlank() || kpiTotals == null) {
			return BigDecimal.ZERO;
		}
		return nullToZero(kpiTotals.get(card.layer().trim()));
	}

	private static BigDecimal nullToZero(BigDecimal value) {
		return value != null ? value : BigDecimal.ZERO;
	}

	private AreaOfInterestAggregate resolveAggregate(List<String> level2Ids, List<String> level3Ids) {
		List<String> normalizedLevel3Ids = normalizeIds(level3Ids);
		if (!normalizedLevel3Ids.isEmpty()) {
			return areaOfInterestRepository.aggregateByLevel3Ids(normalizedLevel3Ids);
		}
		List<String> normalizedLevel2Ids = normalizeIds(level2Ids);
		if (!normalizedLevel2Ids.isEmpty()) {
			return areaOfInterestRepository.aggregateByLevel2Ids(normalizedLevel2Ids);
		}
		return areaOfInterestRepository.aggregateAll();
	}

	private Map<String, BigDecimal> resolveKpiTotals(List<String> level2Ids, List<String> level3Ids) {
		List<KpiTotalsProjection> projections;
		List<String> normalizedLevel3Ids = normalizeIds(level3Ids);
		if (!normalizedLevel3Ids.isEmpty()) {
			projections = kpiMeasureRepository.sumByKpiNameAndLevel3Ids(normalizedLevel3Ids);
		} else {
			List<String> normalizedLevel2Ids = normalizeIds(level2Ids);
			if (!normalizedLevel2Ids.isEmpty()) {
				projections = kpiMeasureRepository.sumByKpiNameAndLevel2Ids(normalizedLevel2Ids);
			} else {
				projections = kpiMeasureRepository.sumByKpiNameAll();
			}
		}
		Map<String, BigDecimal> totals = new LinkedHashMap<>();
		if (projections != null) {
			for (KpiTotalsProjection projection : projections) {
				if (projection == null || projection.getKpiName() == null) {
					continue;
				}
				totals.put(projection.getKpiName(), nullToZero(projection.getTotal()));
			}
		}
		return totals;
	}

	private static List<String> normalizeIds(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return ids.stream()
				.filter(id -> id != null && !id.isBlank())
				.map(String::trim)
				.toList();
	}

	private static TerritoryLevelRefResponse toLevelRef(String id, String name) {
		if (id == null && name == null) {
			return null;
		}
		return new TerritoryLevelRefResponse(id, name);
	}

	private static String formatDate(OffsetDateTime value) {
		if (value == null) {
			return null;
		}
		return value.toLocalDate().format(ISO_DATE);
	}

	private static Centroid resolveCentroid(Optional<CentroidWgs84Projection> projection) {
		if (projection.isEmpty()) {
			return Centroid.empty();
		}
		CentroidWgs84Projection centroid = projection.get();
		if (centroid.getLatitude() == null || centroid.getLongitude() == null) {
			return Centroid.empty();
		}
		return new Centroid(
				formatCoordinate(centroid.getLatitude()),
				formatCoordinate(centroid.getLongitude())
		);
	}

	private static String formatCoordinate(double value) {
		return BigDecimal.valueOf(value)
				.setScale(6, RoundingMode.HALF_UP)
				.stripTrailingZeros()
				.toPlainString();
	}

	private record Centroid(String latitude, String longitude) {
		static Centroid empty() {
			return new Centroid(null, null);
		}
	}
}
