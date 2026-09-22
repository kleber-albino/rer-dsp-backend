package br.car.dsp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeoServerWfsClientTest {

	private static final String WFS_BASE_URL = "http://localhost:22669/geoserver/dsp/wfs";
	private static final String TYPE_NAME = "dsp:area-of-interest";
	private static final String CQL_FILTER = "territory_level_3_id IN ('5300108')";

	private MockRestServiceServer mockServer;
	private GeoServerWfsClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		client = new GeoServerWfsClient(new ObjectMapper(), builder.build());
	}

	@Test
	void buildLatestAttributeValueUrl_ShouldIncludeSortByCountAndPropertyName() {
		String url = GeoServerWfsClient.buildLatestAttributeValueUrl(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertTrue(url.contains("sortBy="));
		assertTrue(url.contains("updated_at"));
		assertTrue(url.contains("count=1"));
		assertTrue(url.contains("propertyName="));
		assertTrue(url.contains("outputFormat="));
		assertFalse(url.contains("resultType=hits"));
	}

	@Test
	void fetchLatestAttributeValue_ShouldParseGeoJsonAttribute() {
		String geoJson = """
				{
				  "type": "FeatureCollection",
				  "features": [
				    {
				      "type": "Feature",
				      "properties": {
				        "updated_at": "2024-06-15T10:30:00Z"
				      }
				    }
				  ]
				}
				""";

		mockServer.expect(requestTo(containsString("sortBy=")))
				.andExpect(requestTo(containsString("count=1")))
				.andExpect(requestTo(containsString("propertyName=")))
				.andRespond(withSuccess(geoJson, org.springframework.http.MediaType.APPLICATION_JSON));

		Optional<String> result = client.fetchLatestAttributeValue(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertTrue(result.isPresent());
		assertEquals("2024-06-15T10:30:00Z", result.get());
		mockServer.verify();
	}

	@Test
	void fetchLatestAttributeValue_ShouldReturnEmptyWhenNoFeatures() {
		String geoJson = """
				{
				  "type": "FeatureCollection",
				  "features": []
				}
				""";

		mockServer.expect(requestTo(containsString("area-of-interest")))
				.andRespond(withSuccess(geoJson, org.springframework.http.MediaType.APPLICATION_JSON));

		Optional<String> result = client.fetchLatestAttributeValue(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertTrue(result.isEmpty());
		mockServer.verify();
	}

	@Test
	void fetchLatestAttributeValue_ShouldReturnEmptyForImpossibleFilter() {
		Optional<String> result = client.fetchLatestAttributeValue(WFS_BASE_URL, TYPE_NAME, "1=0");

		assertTrue(result.isEmpty());
		mockServer.verify();
	}

	@Test
	void countFeatures_ShouldReturnZeroForImpossibleFilterWithoutHttpCall() {
		long result = client.countFeatures(WFS_BASE_URL, TYPE_NAME, "1=0");

		assertEquals(0L, result);
		mockServer.verify();
	}

	@Test
	void countFeatures_ShouldParseNumberMatchedFromJson() {
		String geoJson = """
				{
				  "type": "FeatureCollection",
				  "numberMatched": 42,
				  "features": []
				}
				""";

		mockServer.expect(requestTo(containsString("resultType=hits")))
				.andExpect(requestTo(containsString("CQL_FILTER=")))
				.andRespond(withSuccess(geoJson, org.springframework.http.MediaType.APPLICATION_JSON));

		long result = client.countFeatures(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertEquals(42L, result);
		mockServer.verify();
	}

	@Test
	void countFeatures_ShouldParseNumberMatchedFromXmlHitsResponse() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<wfs:FeatureCollection xmlns:wfs="http://www.opengis.net/wfs/2.0"
					numberMatched="17" numberReturned="0" />
				""";

		mockServer.expect(requestTo(containsString("resultType=hits")))
				.andRespond(withSuccess(xml, org.springframework.http.MediaType.APPLICATION_XML));

		long result = client.countFeatures(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertEquals(17L, result);
		mockServer.verify();
	}

	@Test
	void countFeatures_ShouldFallbackToFeaturesArraySizeWhenNumberMatchedIsMissing() {
		String geoJson = """
				{
				  "type": "FeatureCollection",
				  "features": [
				    { "type": "Feature" },
				    { "type": "Feature" },
				    { "type": "Feature" }
				  ]
				}
				""";

		mockServer.expect(requestTo(containsString("resultType=hits")))
				.andRespond(withSuccess(geoJson, org.springframework.http.MediaType.APPLICATION_JSON));

		long result = client.countFeatures(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertEquals(3L, result);
		mockServer.verify();
	}

	@Test
	void countFeatures_ShouldReturnZeroWhenResponseBodyIsBlank() {
		mockServer.expect(requestTo(containsString("resultType=hits")))
				.andRespond(withSuccess("", org.springframework.http.MediaType.APPLICATION_JSON));

		long result = client.countFeatures(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertEquals(0L, result);
		mockServer.verify();
	}

	@Test
	void countFeatures_ShouldReturnZeroWhenHttpRequestFails() {
		mockServer.expect(requestTo(containsString("resultType=hits")))
				.andRespond(withServerError());

		long result = client.countFeatures(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertEquals(0L, result);
		mockServer.verify();
	}

	@Test
	void countFeatures_ShouldReturnZeroWhenXmlHasNoNumberMatchedAttribute() {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<wfs:FeatureCollection xmlns:wfs="http://www.opengis.net/wfs/2.0" />
				""";

		mockServer.expect(requestTo(containsString("resultType=hits")))
				.andRespond(withSuccess(xml, org.springframework.http.MediaType.APPLICATION_XML));

		long result = client.countFeatures(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertEquals(0L, result);
		mockServer.verify();
	}

	@Test
	void countFeatures_ShouldReturnZeroWhenResponseJsonIsInvalid() {
		mockServer.expect(requestTo(containsString("resultType=hits")))
				.andRespond(withSuccess("{not-json", org.springframework.http.MediaType.APPLICATION_JSON));

		long result = client.countFeatures(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertEquals(0L, result);
		mockServer.verify();
	}

	@Test
	void countFeatures_ShouldBuildHitsRequestWithEncodedTypeNameAndCqlFilter() {
		String geoJson = """
				{
				  "type": "FeatureCollection",
				  "numberMatched": 1,
				  "features": []
				}
				""";

		mockServer.expect(requestTo(containsString("typeNames=dsp%3Aarea-of-interest")))
				.andExpect(requestTo(containsString("resultType=hits")))
				.andExpect(requestTo(containsString("CQL_FILTER=territory_level_3_id+IN+%28%275300108%27%29")))
				.andExpect(requestTo(containsString("version=2.0.0")))
				.andRespond(withSuccess(geoJson, org.springframework.http.MediaType.APPLICATION_JSON));

		long result = client.countFeatures(
				"http://localhost:22669/geoserver/dsp/wfs/",
				TYPE_NAME,
				CQL_FILTER
		);

		assertEquals(1L, result);
		mockServer.verify();
	}

	@Test
	void downloadGpkg_ShouldRequestGeoPackageOutput() {
		byte[] gpkgBytes = new byte[] { 1, 2, 3 };

		mockServer.expect(requestTo(containsString("outputFormat=gpkg")))
				.andExpect(requestTo(containsString("srsName=EPSG:4326")))
				.andExpect(requestTo(not(containsString("resultType=hits"))))
				.andRespond(withSuccess(gpkgBytes, MediaType.APPLICATION_OCTET_STREAM));

		byte[] result = client.downloadGpkg(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertArrayEquals(gpkgBytes, result);
		mockServer.verify();
	}

	@Test
	void downloadCsv_ShouldReturnCsvBytesWhenGeoServerRespondsWithContent() {
		byte[] csvBytes = "id,name\n1,test\n".getBytes();

		mockServer.expect(requestTo(containsString("outputFormat=csv")))
				.andExpect(requestTo(containsString("typeNames=dsp%3Aarea-of-interest")))
				.andExpect(requestTo(containsString("CQL_FILTER=")))
				.andRespond(withSuccess(new String(csvBytes), MediaType.parseMediaType("text/csv")));

		byte[] result = client.downloadCsv(WFS_BASE_URL, TYPE_NAME, CQL_FILTER);

		assertArrayEquals(csvBytes, result);
		mockServer.verify();
	}

	@Test
	void downloadCsv_ShouldBuildDownloadRequestWithoutHitsAndWithCsvFormat() {
		mockServer.expect(requestTo(containsString("outputFormat=csv")))
				.andExpect(requestTo(not(containsString("resultType=hits"))))
				.andExpect(requestTo(containsString("version=2.0.0")))
				.andExpect(requestTo(containsString("srsName=EPSG:4326")))
				.andExpect(requestTo(containsString("CQL_FILTER=territory_level_3_id+IN+%28%275300108%27%29")))
				.andRespond(withSuccess("id\n1\n", MediaType.parseMediaType("text/csv")));

		byte[] result = client.downloadCsv(
				"http://localhost:22669/geoserver/dsp/wfs/",
				TYPE_NAME,
				CQL_FILTER
		);

		assertTrue(result.length > 0);
		mockServer.verify();
	}

	@Test
	void downloadCsv_ShouldThrowNotFoundWhenResponseBodyIsEmpty() {
		mockServer.expect(requestTo(containsString("outputFormat=csv")))
				.andRespond(withSuccess("", MediaType.parseMediaType("text/csv")));

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> client.downloadCsv(WFS_BASE_URL, TYPE_NAME, CQL_FILTER)
		);

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
		assertEquals("File unavailable for download", exception.getReason());
		mockServer.verify();
	}

	@Test
	void downloadCsv_ShouldThrowBadGatewayWhenHttpRequestFails() {
		mockServer.expect(requestTo(containsString("outputFormat=csv")))
				.andRespond(withServerError());

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> client.downloadCsv(WFS_BASE_URL, TYPE_NAME, CQL_FILTER)
		);

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
		assertEquals("Failed to query GeoServer for download", exception.getReason());
		mockServer.verify();
	}
}
