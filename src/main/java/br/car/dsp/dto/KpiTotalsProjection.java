package br.car.dsp.dto;

import java.math.BigDecimal;

/**
 * Aggregated KPI total grouped by {@code kpi_name}.
 */
public interface KpiTotalsProjection {

	String getKpiName();

	BigDecimal getTotal();
}
