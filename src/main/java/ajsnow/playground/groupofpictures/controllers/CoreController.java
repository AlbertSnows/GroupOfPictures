package ajsnow.playground.groupofpictures.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class CoreController {
	private static final Logger logger = LoggerFactory.getLogger(CoreController.class);
	@RequestMapping("/")
	public String default_route() {
		return "forward:/entry";
	}

	@RequestMapping("/entry")
	public ResponseEntity<Map<String, String>> entry () {
		logger.info("Hit entry endpoint!");
		var message = "API entry point! Relevant endpoints are under /videos, it has the information you're probably looking for.";
		return ResponseEntity.ok(Map.of("message", message));
	}

}
