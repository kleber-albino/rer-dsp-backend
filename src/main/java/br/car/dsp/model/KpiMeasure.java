package br.car.dsp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "dsp", name = "kpi_measure")
@Getter
@Setter
public class KpiMeasure {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "area_of_interest_id", nullable = false)
	private AreaOfInterest areaOfInterest;

	@Column(name = "value", nullable = false, precision = 18, scale = 3)
	private BigDecimal value;

	@Column(name = "kpi_name", nullable = false)
	private String kpiName;
}
