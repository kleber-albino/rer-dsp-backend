package br.car.dsp.dto;

/**
 * Mirrors Consulta Pública Totalizer.
 */
public record TotalizerResponse(
		String name,
		String code,
		Double value,
		String subItemName,
		Double subItemValue,
		String unitOfMeasurement
) {
}
