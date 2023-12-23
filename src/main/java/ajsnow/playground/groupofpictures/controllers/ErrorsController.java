package ajsnow.playground.groupofpictures.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Controller
public class ErrorsController implements ErrorController {
	private static final Logger logger = LoggerFactory.getLogger(ErrorsController.class);
	private final ErrorAttributes errorAttributes;

	@Autowired
	public ErrorsController(ErrorAttributes errorAttributes
	) {
		this.errorAttributes = errorAttributes;
	}

	@RequestMapping("/error")
	public ResponseEntity<Map<String, Object>> handleError(WebRequest webRequest) {
		Map<String, Object> errorAttributesMap = errorAttributes.getErrorAttributes(
						webRequest, ErrorAttributeOptions.defaults());
		errorAttributesMap.put("message", "This is a intentional JSON error response. " +
						"If the HTTP status is 400, double check your request. " +
						"This response should also include an brief error description that may help ");
//		logger.info(errorAttributesMap.toString()); // hrmmm...
		HttpStatus status = HttpStatus.valueOf((int) errorAttributesMap.get("status"));
		return new ResponseEntity<>(errorAttributesMap, status);
	}

	public String getErrorPath() {
		return "/error";
	}
}
