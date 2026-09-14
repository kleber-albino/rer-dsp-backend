package br.car.dsp.controller;

import br.car.dsp.dto.AreaOfInterestMeasuresConfigResponse;
import br.car.dsp.dto.FormatsConfigResponse;
import br.car.dsp.dto.HierarchyLevelConfigResponse;
import br.car.dsp.dto.HomeKpisConfigResponse;
import br.car.dsp.dto.InstallationConfigResponse;
import br.car.dsp.dto.KpiCardConfigResponse;
import br.car.dsp.dto.HomeDetailSearchConfigResponse;
import br.car.dsp.dto.HomeScreenConfigResponse;
import br.car.dsp.dto.ScreenConfigResponse;
import br.car.dsp.dto.ScreensConfigResponse;
import br.car.dsp.service.InstallationConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigControllerTest {

	private static final String PRIMARY_KPI_CODE = "AREA_OF_INTEREST";

	@Mock
	private InstallationConfigService installationConfigService;

	@InjectMocks
	private ConfigController configController;

	@Test
	void getInstallationConfig_ShouldDelegateToService() {
		InstallationConfigResponse expected = new InstallationConfigResponse(
				List.of(new HierarchyLevelConfigResponse("level1", "Level 1", "Select level 1", 1)),
				new ScreensConfigResponse(
						new HomeScreenConfigResponse(
								"Browse registered data",
								List.of("level2", "level3"),
								null,
								null,
								null,
								null,
								null,
								null
						),
						new ScreenConfigResponse(
								"Download public data",
								List.of("level1", "level2", "level3"),
								null,
								null,
								null,
								null,
								null
						)
				),
				new HomeKpisConfigResponse(
						5,
						PRIMARY_KPI_CODE,
						List.of(new KpiCardConfigResponse(
								PRIMARY_KPI_CODE,
								"Registered properties",
								"un.",
								"ha",
								"#CED6E5",
								1,
								true,
								null
						))
				),
				new AreaOfInterestMeasuresConfigResponse("ha", "ha"),
				FormatsConfigResponse.defaults(),
				null
		);
		when(installationConfigService.getInstallationConfig()).thenReturn(expected);

		InstallationConfigResponse result = configController.getInstallationConfig();

		assertEquals(expected, result);
		assertEquals(List.of("level2", "level3"), result.screens().home().hierarchyKeys());
		assertEquals(List.of("level1", "level2", "level3"), result.screens().downloads().hierarchyKeys());
		assertEquals(5, result.kpis().maxCards());
		assertEquals(PRIMARY_KPI_CODE, result.kpis().primaryCode());
		assertEquals(PRIMARY_KPI_CODE, result.kpis().cards().getFirst().code());
		assertEquals("ha", result.areaOfInterest().areaUnit());
		assertEquals("ha", result.areaOfInterest().areaUnitLabel());
		assertEquals("yyyy-MM-dd", result.formats().date());
		verify(installationConfigService).getInstallationConfig();
	}
}
