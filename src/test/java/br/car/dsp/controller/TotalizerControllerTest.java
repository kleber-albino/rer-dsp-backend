package br.car.dsp.controller;

import br.car.dsp.dto.DetailByIdentifierResponse;
import br.car.dsp.dto.TerritoryLevelRefResponse;
import br.car.dsp.dto.TerritoryLevelsResponse;
import br.car.dsp.dto.TotalizerFilterRequest;
import br.car.dsp.dto.TotalizerResponse;
import br.car.dsp.service.TotalizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotalizerControllerTest {

	@Mock
	private TotalizerService totalizerService;

	@InjectMocks
	private TotalizerController totalizerController;

	private TotalizerFilterRequest filter;
	private List<TotalizerResponse> totalizers;
	private DetailByIdentifierResponse detail;

	@BeforeEach
	void setUp() {
		filter = new TotalizerFilterRequest();
		filter.setLevel2Ids(List.of("DF"));

		totalizers = List.of(
				new TotalizerResponse("Imóveis cadastrados", "AREA_OF_INTEREST", 10.0, "ha", 20.0, "un.")
		);

		detail = new DetailByIdentifierResponse(
				"DF123456789012",
				"-15.79",
				"-47.88",
				new TerritoryLevelsResponse(
						new TerritoryLevelRefResponse("DF", "Distrito Federal"),
						new TerritoryLevelRefResponse("5300108", "Brasília")
				),
				"2020-01-10",
				"2024-06-15",
				new BigDecimal("120.50"),
				List.of(),
				Map.of()
		);
	}

	@Test
	void getTotalizers_ShouldDelegateToService() {
		when(totalizerService.getTotalizers(filter)).thenReturn(totalizers);

		List<TotalizerResponse> result = totalizerController.getTotalizers(filter);

		assertEquals(totalizers, result);
		verify(totalizerService).getTotalizers(filter);
	}

	@Test
	void getDetailsByIdentifier_ShouldDelegateToService() {
		when(totalizerService.getDetailByIdentifier("DF123456789012")).thenReturn(detail);

		DetailByIdentifierResponse result =
				totalizerController.getDetailsByIdentifier("DF123456789012");

		assertEquals(detail, result);
		verify(totalizerService).getDetailByIdentifier("DF123456789012");
	}

	@Test
	void getDetailsByCoordinates_ShouldDelegateToService() {
		when(totalizerService.getDetailsByCoordinates(-15.79, -47.88)).thenReturn(detail);

		DetailByIdentifierResponse result =
				totalizerController.getDetailsByCoordinates(-15.79, -47.88);

		assertEquals(detail, result);
		verify(totalizerService).getDetailsByCoordinates(-15.79, -47.88);
	}
}
