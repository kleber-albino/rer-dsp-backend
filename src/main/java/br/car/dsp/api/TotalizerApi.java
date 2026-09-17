package br.car.dsp.api;

import br.car.dsp.dto.DetailByIdentifierResponse;
import br.car.dsp.dto.TotalizerFilterRequest;
import br.car.dsp.dto.TotalizerResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Totalizers and area-of-interest detail by identifier or coordinates.
 * Path {@code getDeatilsByIdentifier} keeps the historical typo for API compatibility.
 */
@RequestMapping("/totalizer")
public interface TotalizerApi {

	@PostMapping(
			value = "/getTotalizers",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	List<TotalizerResponse> getTotalizers(@RequestBody TotalizerFilterRequest filter);

	@GetMapping(value = "/getDeatilsByIdentifier/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
	DetailByIdentifierResponse getDetailsByIdentifier(@PathVariable("identifier") String identifier);

	@GetMapping(value = "/getDetailsByCoordinates", produces = MediaType.APPLICATION_JSON_VALUE)
	DetailByIdentifierResponse getDetailsByCoordinates(
			@RequestParam("lat") Double lat,
			@RequestParam("lng") Double lng
	);
}
