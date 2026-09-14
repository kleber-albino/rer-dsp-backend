package br.car.dsp.repository;

import br.car.dsp.dto.AreaOfInterestAggregate;
import br.car.dsp.dto.CentroidWgs84Projection;
import br.car.dsp.model.AreaOfInterest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AreaOfInterestRepository extends JpaRepository<AreaOfInterest, String> {

	@EntityGraph(attributePaths = {"territoryLevel3", "territoryLevel3.parent"})
	@Override
	Optional<AreaOfInterest> findById(String id);

	@Query(value = """
			SELECT
				public.ST_Y(public.ST_Transform(a.centroid_coordinates, 4326)) AS latitude,
				public.ST_X(public.ST_Transform(a.centroid_coordinates, 4326)) AS longitude
			FROM dsp.area_of_interest a
			WHERE a.id = :id
			  AND a.centroid_coordinates IS NOT NULL
			  AND public.ST_SRID(a.centroid_coordinates) > 0
			""", nativeQuery = true)
	Optional<CentroidWgs84Projection> findCentroidWgs84(@Param("id") String id);

	@Query(value = """
			SELECT a.id
			FROM dsp.area_of_interest a
			WHERE a.boundary_box IS NOT NULL
			  AND public.ST_SRID(a.boundary_box) > 0
			  AND public.ST_Contains(
			        a.boundary_box,
			        public.ST_Transform(
			          public.ST_SetSRID(
			            public.ST_MakePoint(:longitude, :latitude),
			            4326
			          ),
			          public.ST_SRID(a.boundary_box)
			        )
			      )
			ORDER BY a.area ASC NULLS LAST, a.id ASC
			""", nativeQuery = true)
	List<String> findIdsContainingPoint(
			@Param("latitude") Double latitude,
			@Param("longitude") Double longitude
	);

	@Query(value = """
			SELECT
				COUNT(1) AS count,
				COALESCE(SUM(a.area), 0) AS totalArea
			FROM dsp.area_of_interest a
			""", nativeQuery = true)
	AreaOfInterestAggregate aggregateAll();

	@Query(value = """
			SELECT
				COUNT(1) AS count,
				COALESCE(SUM(a.area), 0) AS totalArea
			FROM dsp.area_of_interest a
			JOIN dsp.territory_level_3 l3 ON l3.id = a.territory_level_3_id
			WHERE l3.parent_id IN (:level2Ids)
			""", nativeQuery = true)
	AreaOfInterestAggregate aggregateByLevel2Ids(@Param("level2Ids") Collection<String> level2Ids);

	@Query(value = """
			SELECT
				COUNT(1) AS count,
				COALESCE(SUM(a.area), 0) AS totalArea
			FROM dsp.area_of_interest a
			WHERE a.territory_level_3_id IN (:level3Ids)
			""", nativeQuery = true)
	AreaOfInterestAggregate aggregateByLevel3Ids(@Param("level3Ids") Collection<String> level3Ids);

	@Query("""
			SELECT a.id
			FROM AreaOfInterest a
			WHERE a.territoryLevel3.id = :level3Id
			""")
	List<String> findIdsByTerritoryLevel3Id(@Param("level3Id") String level3Id);

	@Query("""
			SELECT a.id
			FROM AreaOfInterest a
			JOIN a.territoryLevel3 level3
			WHERE level3.parent.id = :level2Id
			""")
	List<String> findIdsByTerritoryLevel2Id(@Param("level2Id") String level2Id);
}
