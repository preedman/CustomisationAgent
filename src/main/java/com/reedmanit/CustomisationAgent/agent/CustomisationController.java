package com.reedmanit.CustomisationAgent.agent;



import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customisations")
public class CustomisationController {

    private final CustomisationAgentService customisationAgentService;

    public CustomisationController(CustomisationAgentService customisationAgentService) {
        this.customisationAgentService = customisationAgentService;
    }

    @PostMapping
    public ResponseEntity<CustomisationResponse> customise(@RequestBody CustomisationRequest request) {
        String result = customisationAgentService.executeWithAi(
                request.specification(),
                request.acceptanceCriteria()
        );

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


