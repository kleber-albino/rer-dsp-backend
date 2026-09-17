package br.car.dsp.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import br.car.dsp.model.TerritoryLevel2;
import br.car.dsp.model.TerritoryLevel3;
import br.car.dsp.repository.TerritoryLevel2Repository;
import br.car.dsp.repository.TerritoryLevel3Repository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DownloadFileNameBuilderTest {

	@Mock
	private TerritoryLevel2Repository level2Repository;

	@Mock
	private TerritoryLevel3Repository level3Repository;

	@InjectMocks
	private DownloadFileNameBuilder builder;

	@Test
	void build_ShouldUseThemeLevel2Level3Order() {
		TerritoryLevel2 level2 = new TerritoryLevel2();
		level2.setId("178");
		level2.setName("Ovar");

		TerritoryLevel3 level3 = new TerritoryLevel3();
		level3.setId("1068");
		level3.setName("Maceda");

		when(level2Repository.findById("178")).thenReturn(Optional.of(level2));
		when(level3Repository.findById("1068")).thenReturn(Optional.of(level3));

		assertEquals(
				"aeroportos_ovar_maceda.csv",
				builder.build("178", "1068", "Aeroportos", "csv")
		);
	}

	@Test
	void build_ShouldOmitLevel3WhenNotProvided() {
		TerritoryLevel2 level2 = new TerritoryLevel2();
		level2.setId("178");
		level2.setName("Ovar");

		when(level2Repository.findById("178")).thenReturn(Optional.of(level2));

		assertEquals("aeroportos_ovar.csv", builder.build("178", null, "Aeroportos", "csv"));
	}

	@Test
	void toFileSegment_ShouldSlugifyAccentsAndSpaces() {
		assertEquals("safety-area-0-300-m", DownloadFileNameBuilder.toFileSegment(
				"Safety aréa 0–300 m",
				"theme"
		));
	}

	@Test
	void toFileSegment_ShouldFallbackWhenValueIsBlank() {
		assertEquals("ovar", DownloadFileNameBuilder.toFileSegment("   ", "Ovar"));
	}

	@Test
	void normalizeExtension_ShouldStripLeadingDot() {
		assertEquals("csv", DownloadFileNameBuilder.normalizeExtension(".csv"));
		assertEquals("bin", DownloadFileNameBuilder.normalizeExtension(" "));
	}

	@Test
	void buildForAoi_ShouldUseThemeAndAoiSegments() {
		assertEquals(
				"demo-properties_demo-001.csv",
				builder.buildForAoi("DEMO-001", "Demo properties", "csv")
		);
	}

	@Test
	void buildBundleArchiveName_ShouldSuffixFeaturesZip() {
		assertEquals("demo-001_features.zip", builder.buildBundleArchiveName("DEMO-001"));
	}
}
