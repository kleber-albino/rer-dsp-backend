package br.car.dsp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Generic area of interest (example: property).
 * Full geometry lives in the GeoServer exhibition database.
 */
@Entity
@Table(
		schema = "dsp",
		name = "area_of_interest",
		indexes = @Index(name = "idx_area_of_interest_territory_level_3_id", columnList = "territory_level_3_id")
)
@Getter
@Setter
public class AreaOfInterest {

	@Id
	@Column(name = "id", length = 255, nullable = false)
	private String id;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime registrationDate;

	@Column(name = "updated_at")
	private OffsetDateTime alterationDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(
			name = "territory_level_3_id",
			nullable = true,
			foreignKey = @ForeignKey(name = "fk_area_of_interest_territory_level_3")
	)
	private TerritoryLevel3 territoryLevel3;

	// Area calculated by the KPI migration job (unit = installationConfig.areaOfInterest).
	@Column(name = "area")
	private BigDecimal area;

	@Column(name = "boundary_box", columnDefinition = "geometry")
	private Polygon boundaryBox;

	@Column(name = "centroid_coordinates", columnDefinition = "geometry")
	private Point centroidCoordinates;
}
