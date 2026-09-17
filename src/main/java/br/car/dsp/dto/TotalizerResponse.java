package br.car.dsp.dto;

public record TotalizerResponse(
		String name,
		String code,
		Double value,
		String subItemName,
		Double subItemValue,
		String unitOfMeasurement
) {
}
