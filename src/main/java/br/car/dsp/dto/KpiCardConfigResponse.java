package br.car.dsp.dto;

/**
 * Structural definition of a KPI card (labels/units/colors).
 * Numeric values come from totalizers; labels come from here.
 */
public record KpiCardConfigResponse(
		String code,
		String label,
		String unitOfMeasurement,
		String optionalLabel,
		String accentColor,
		int order,
		boolean required,
		String layer
) {
}
