package br.car.dsp.api;

import br.car.dsp.dto.DownloadItemResponse;
import br.car.dsp.dto.DownloadSearchRequest;
import br.car.dsp.dto.DownloadThemeResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Theme file catalog and download via GeoServer WFS.
 */
@RequestMapping("/downloads")
public interface DownloadApi {

	@GetMapping(value = "/themes", produces = MediaType.APPLICATION_JSON_VALUE)
	List<DownloadThemeResponse> getThemes();

	@PostMapping(
			value = "/search",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	List<DownloadItemResponse> search(@RequestBody DownloadSearchRequest request);

	@GetMapping(value = "/file")
	ResponseEntity<byte[]> downloadFile(
			@RequestParam String level2,
			@RequestParam(required = false) String level3,
			@RequestParam String theme,
			@RequestParam String format
	);

	@GetMapping(value = "/features-bundle")
	ResponseEntity<byte[]> downloadFeaturesBundle(
			@RequestParam String aoiId,
			@RequestParam String format
	);
}
