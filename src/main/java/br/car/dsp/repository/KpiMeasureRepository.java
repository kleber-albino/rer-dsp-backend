package br.car.dsp.repository;

import br.car.dsp.dto.KpiTotalsProjection;
import br.car.dsp.model.KpiMeasure;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KpiMeasureRepository extends JpaRepository<KpiMeasure, Long> {

	@Query(value = """
			SELECT
				k.kpi_name AS kpiName,
				COALESCE(SUM(k.value), 0) AS total
			FROM dsp.kpi_measure k
			GROUP BY k.kpi_name
			""", nativeQuery = true)
	List<KpiTotalsProjection> sumByKpiNameAll();

	@Query(value = """
			SELECT
				k.kpi_name AS kpiName,
				COALESCE(SUM(k.value), 0) AS total
			FROM dsp.kpi_measure k
			JOIN dsp.area_of_interest a ON a.id = k.area_of_interest_id
			JOIN dsp.territory_level_3 l3 ON l3.id = a.territory_level_3_id
			WHERE l3.parent_id IN (:level2Ids)
			GROUP BY k.kpi_name
			""", nativeQuery = true)
	List<KpiTotalsProjection> sumByKpiNameAndLevel2Ids(@Param("level2Ids") Collection<String> level2Ids);

	@Query(value = """
			SELECT
				k.kpi_name AS kpiName,
				COALESCE(SUM(k.value), 0) AS total
			FROM dsp.kpi_measure k
			JOIN dsp.area_of_interest a ON a.id = k.area_of_interest_id
			WHERE a.territory_level_3_id IN (:level3Ids)
			GROUP BY k.kpi_name
			""", nativeQuery = true)
	List<KpiTotalsProjection> sumByKpiNameAndLevel3Ids(@Param("level3Ids") Collection<String> level3Ids);
}
