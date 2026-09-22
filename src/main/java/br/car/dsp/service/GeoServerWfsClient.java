package br.car.dsp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class GeoServerWfsClient {

	public static final String LAST_UPDATE_ATTRIBUTE = "updated_at";

	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	@Autowired
	public GeoServerWfsClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.restClient = RestClient.create();
	}

	GeoServerWfsClient(ObjectMapper objectMapper, RestClient restClient) {
		this.objectMapper = objectMapper;
		this.restClient = restClient;
	}

	public long countFeatures(String wfsBaseUrl, String typeName, String cqlFilter) {
		if ("1=0".equals(cqlFilter)) {
			return 0L;
		}
		String url = buildGetFeatureUrl(
				wfsBaseUrl,
				typeName,
				null,
				cqlFilter,
				true
		);
		try {
			String body = restClient.get()
					.uri(URI.create(url))
					.retrieve()
					.body(String.class);
			if (body == null || body.isBlank()) {
				return 0L;
			}
			if (body.trim().startsWith("<")) {
				return parseNumberMatchedFromXml(body);
			}
			JsonNode root = objectMapper.readTree(body);
			if (root.hasNonNull("numberMatched")) {
				return root.get("numberMatched").asLong(0L);
			}
			if (root.has("features") && root.get("features").isArray()) {
				return root.get("features").size();
			}
			return 0L;
		} catch (Exception ex) {
			log.warn("WFS count failed for {}: {}", typeName, ex.getMessage());
			return 0L;
		}
	}

	public byte[] downloadCsv(String wfsBaseUrl, String typeName, String cqlFilter) {
		return download(wfsBaseUrl, typeName, cqlFilter, "csv");
	}

	public byte[] downloadGpkg(String wfsBaseUrl, String typeName, String cqlFilter) {
		return download(wfsBaseUrl, typeName, cqlFilter, "gpkg");
	}

	private byte[] download(String wfsBaseUrl, String typeName, String cqlFilter, String outputFormat) {
		String url = buildGetFeatureUrl(
				wfsBaseUrl,
				typeName,
				outputFormat,
				cqlFilter,
				false
		);
		try {
			byte[] body = restClient.get()
					.uri(URI.create(url))
					.retrieve()
					.body(byte[].class);
			if (body == null || body.length == 0) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File unavailable for download");
			}
			return body;
		} catch (ResponseStatusException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("WFS download failed for {}", typeName, ex);
			throw new ResponseStatusException(
					HttpStatus.BAD_GATEWAY,
					"Failed to query GeoServer for download",
					ex
			);
		}
	}

	public Optional<String> fetchLatestAttributeValue(String wfsBaseUrl, String typeName, String cqlFilter) {
		if ("1=0".equals(cqlFilter)) {
			return Optional.empty();
		}
		String url = buildLatestAttributeValueUrl(wfsBaseUrl, typeName, cqlFilter);
		try {
			String body = restClient.get()
					.uri(URI.create(url))
					.retrieve()
					.body(String.class);
			if (body == null || body.isBlank()) {
				return Optional.empty();
			}
			JsonNode root = objectMapper.readTree(body);
			JsonNode features = root.get("features");
			if (features == null || !features.isArray() || features.isEmpty()) {
				return Optional.empty();
			}
			JsonNode attribute = features.get(0).path("properties").path(LAST_UPDATE_ATTRIBUTE);
			if (attribute.isMissingNode() || attribute.isNull()) {
				return Optional.empty();
			}
			return Optional.of(attribute.asText());
		} catch (Exception ex) {
			log.warn("WFS last update fetch failed for {}: {}", typeName, ex.getMessage());
			return Optional.empty();
		}
	}

	private static String buildGetFeatureUrl(
			String wfsBaseUrl,
			String typeName,
			String outputFormat,
			String cqlFilter,
			boolean hitsOnly
	) {
		StringBuilder url = new StringBuilder(wfsBaseUrl.replaceAll("/$", ""));
		url.append("?service=WFS");
		url.append("&version=2.0.0");
		url.append("&request=GetFeature");
		url.append("&typeNames=").append(encode(typeName));
		if (outputFormat != null && !outputFormat.isBlank()) {
			url.append("&outputFormat=").append(encode(outputFormat));
		}
		url.append("&srsName=EPSG:4326");
		if (hitsOnly) {
			url.append("&resultType=hits");
		}
		if (cqlFilter != null && !cqlFilter.isBlank()) {
			url.append("&CQL_FILTER=").append(encode(cqlFilter));
		}
		return url.toString();
	}

	static String buildLatestAttributeValueUrl(String wfsBaseUrl, String typeName, String cqlFilter) {
		StringBuilder url = new StringBuilder(wfsBaseUrl.replaceAll("/$", ""));
		url.append("?service=WFS");
		url.append("&version=2.0.0");
		url.append("&request=GetFeature");
		url.append("&typeNames=").append(encode(typeName));
		url.append("&outputFormat=").append(encode("application/json"));
		url.append("&propertyName=").append(encode(LAST_UPDATE_ATTRIBUTE));
		url.append("&sortBy=").append(encode(LAST_UPDATE_ATTRIBUTE + " D"));
		url.append("&count=1");
		url.append("&srsName=EPSG:4326");
		if (cqlFilter != null && !cqlFilter.isBlank()) {
			url.append("&CQL_FILTER=").append(encode(cqlFilter));
		}
		return url.toString();
	}

	private static long parseNumberMatchedFromXml(String body) {
		int index = body.indexOf("numberMatched=\"");
		if (index < 0) {
			return 0L;
		}
		int start = index + "numberMatched=\"".length();
		int end = body.indexOf('"', start);
		if (end < 0) {
			return 0L;
		}
		try {
			return Long.parseLong(body.substring(start, end));
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
