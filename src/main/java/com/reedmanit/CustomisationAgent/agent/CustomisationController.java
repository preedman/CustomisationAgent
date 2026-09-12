package com.reedmanit.CustomisationAgent.agent;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customisations")
public class CustomisationController {

    private static final Logger log = LoggerFactory.getLogger(CustomisationController.class);

    private final CustomisationAgentService customisationAgentService;

    public CustomisationController(CustomisationAgentService customisationAgentService) {
        this.customisationAgentService = customisationAgentService;
    }

    @PostMapping
    public ResponseEntity<CustomisationResponse> customise(@RequestBody CustomisationRequest request) {
        log.info("Received customisation request: specification='{}', acceptanceCriteria='{}'",
                request.specification(), request.acceptanceCriteria());
        String result = customisationAgentService.executeWithAi(
                request.specification(),
                request.acceptanceCriteria()
        );
        log.debug("Customisation request processed successfully with result: {}", result);

        return ResponseEntity.ok(new CustomisationResponse(result));
    }

    public record CustomisationRequest(
            String specification,
            String acceptanceCriteria
    ) {
    }

    public record CustomisationResponse(
            String result
    ) {
    }
}


