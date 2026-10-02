package com.formai.api.clients.interfaces.rest;

import com.formai.api.clients.domain.model.queries.GetClientProfileQuery;
import com.formai.api.clients.domain.services.ClientQueryService;
import com.formai.api.clients.interfaces.rest.transform.ClientAssemblerImpl;
import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static com.formai.api.clients.ClientsTestData.CLIENT_ID;
import static com.formai.api.clients.ClientsTestData.TRAINER_HOLDER_ID;
import static com.formai.api.clients.ClientsTestData.activeClient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientProfilesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ClientAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class ClientProfilesControllerTest {

    // A client's id is the id of their iam account: the JWT subject.
    private static final RequestPostProcessor CLIENT = user(CLIENT_ID.value().toString()).roles("CLIENT");
    private static final RequestPostProcessor TRAINER = user(TRAINER_HOLDER_ID).roles("TRAINER");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ClientQueryService clientQueryService;

    @Test
    void shouldReturnTheSignedInClientsNameAndEmail() throws Exception {
        when(clientQueryService.handle(new GetClientProfileQuery(CLIENT_ID))).thenReturn(Optional.of(activeClient()));

        mockMvc.perform(get("/api/v1/client-profiles/me").with(CLIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CLIENT_ID.value().toString()))
                .andExpect(jsonPath("$.fullName").value("Luis Ramos"))
                .andExpect(jsonPath("$.email").value("luis@formai.com"));
    }

    @Test
    void shouldReturn404WhenTheAccountHasNoClientRecord() throws Exception {
        when(clientQueryService.handle(new GetClientProfileQuery(CLIENT_ID))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/client-profiles/me").with(CLIENT))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403ForATrainer() throws Exception {
        mockMvc.perform(get("/api/v1/client-profiles/me").with(TRAINER))
                .andExpect(status().isForbidden());

        verifyNoInteractions(clientQueryService);
    }

    @Test
    void shouldReturn403WithoutASession() throws Exception {
        mockMvc.perform(get("/api/v1/client-profiles/me"))
                .andExpect(status().isForbidden());
    }
}
