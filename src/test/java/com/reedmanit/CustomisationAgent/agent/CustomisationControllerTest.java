package com.reedmanit.CustomisationAgent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomisationController.class)
public class CustomisationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomisationAgentService customisationAgentService;

    @Test
    void shouldSubmitCustomisationRequestAndReturnResult() throws Exception {
        String specification = "Add a due date field to tasks.";
        String acceptanceCriteria = "Tasks can be created with a due date and tests pass.";
        String expectedResult = "CUSTOMISATION_COMPLETED: Due date field added successfully.";

        when(customisationAgentService.executeWithAi(specification, acceptanceCriteria))
                .thenReturn(expectedResult);

        mockMvc.perform(post("/api/customisations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specification": "Add a due date field to tasks.",
                                  "acceptanceCriteria": "Tasks can be created with a due date and tests pass."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(expectedResult));

        verify(customisationAgentService).executeWithAi(specification, acceptanceCriteria);
    }

}
