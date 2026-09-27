package com.formai.api.clients.interfaces.rest;

import com.formai.api.clients.domain.exceptions.ActivationCodeNotRenewableException;
import com.formai.api.clients.domain.exceptions.ClientAlreadyRegisteredException;
import com.formai.api.clients.domain.exceptions.ClientEmailUnavailableException;
import com.formai.api.clients.domain.model.commands.DeactivateClientCommand;
import com.formai.api.clients.domain.model.commands.RegisterClientCommand;
import com.formai.api.clients.domain.model.commands.RenewActivationCodeCommand;
import com.formai.api.clients.domain.model.commands.UpdateBodyProfileCommand;
import com.formai.api.clients.domain.model.queries.GetClientByIdQuery;
import com.formai.api.clients.domain.model.queries.GetClientsQuery;
import com.formai.api.clients.domain.model.valueobjects.ClientPage;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.RegisteredClient;
import com.formai.api.clients.domain.services.ClientCommandService;
import com.formai.api.clients.domain.services.ClientQueryService;
import com.formai.api.clients.interfaces.rest.transform.ClientAssemblerImpl;
import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.formai.api.clients.ClientsTestData.CLIENT_ID;
import static com.formai.api.clients.ClientsTestData.TODAY;
import static com.formai.api.clients.ClientsTestData.TRAINER_HOLDER_ID;
import static com.formai.api.clients.ClientsTestData.activeClient;
import static com.formai.api.clients.ClientsTestData.bodyProfileCommand;
import static com.formai.api.clients.ClientsTestData.invitedClient;
import static com.formai.api.clients.ClientsTestData.ticket;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ClientAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
@WithMockUser(username = TRAINER_HOLDER_ID, roles = "TRAINER")
class ClientsControllerTest {

    private static final String LUIS_BODY = "{\"fullName\":\"Luis Ramos\",\"email\":\"Luis@FormAI.com\"}";
    private static final String PROFILE_BODY =
            "{\"goal\":\"Hypertrophy\",\"heightCm\":%d,\"weightKg\":%s,\"restrictions\":\"Left knee injury\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ClientCommandService clientCommandService;

    @MockitoBean
    ClientQueryService clientQueryService;

    // --- Register ------------------------------------------------------------------------

    @Test
    void shouldReturn201WithTheActivationCodeOnScreen() throws Exception {
        when(clientCommandService.handle(argThat((RegisterClientCommand command) ->
                command.holderId().equals(TRAINER_HOLDER_ID) && command.email().value().equals("luis@formai.com"))))
                .thenReturn(Optional.of(new RegisteredClient(invitedClient(), ticket())));

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(LUIS_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CLIENT_ID.value().toString()))
                .andExpect(jsonPath("$.status").value("INVITED"))
                .andExpect(jsonPath("$.activationCode").value("ABCD2345"))
                .andExpect(jsonPath("$.activationCodeExpiresAt").value("2026-10-04T12:00:00Z"));
    }

    @Test
    void shouldReturn400ForAnInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Luis Ramos\",\"email\":\"luis@formai\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(clientCommandService);
    }

    @Test
    void shouldReturn409ForAClientAlreadyInTheList() throws Exception {
        when(clientCommandService.handle(any(RegisterClientCommand.class)))
                .thenThrow(new ClientAlreadyRegisteredException("luis@formai.com"));

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(LUIS_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn409ForAnEmailThatIsNotAvailable() throws Exception {
        when(clientCommandService.handle(any(RegisterClientCommand.class)))
                .thenThrow(new ClientEmailUnavailableException());

        mockMvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(LUIS_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This email is not available"));
    }

    // --- List and read -------------------------------------------------------------------

    @Test
    void shouldListTheClientsSearchedAndFilteredByStatus() throws Exception {
        when(clientQueryService.handle(argThat((GetClientsQuery query) ->
                query.holderId().equals(TRAINER_HOLDER_ID) && query.search().equals(Optional.of("lu"))
                        && query.status().equals(Optional.of(ClientStatus.ACTIVE)))))
                .thenReturn(new ClientPage(List.of(activeClient()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/clients").param("search", "lu").param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Luis Ramos"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn400ForAnUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/v1/clients").param("status", "DELETED"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForAClientOfAnotherTrainer() throws Exception {
        when(clientQueryService.handle(any(GetClientByIdQuery.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clients/" + CLIENT_ID.value()))
                .andExpect(status().isNotFound());
    }

    // --- Deactivation and codes ----------------------------------------------------------

    @Test
    void shouldReturn201WhenAClientIsDeactivated() throws Exception {
        var client = activeClient();
        client.deactivate();
        when(clientCommandService.handle(new DeactivateClientCommand(CLIENT_ID, TRAINER_HOLDER_ID)))
                .thenReturn(Optional.of(client));

        mockMvc.perform(post("/api/v1/clients/" + CLIENT_ID.value() + "/deactivations"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void shouldReturn201WithANewActivationCode() throws Exception {
        when(clientCommandService.handle(new RenewActivationCodeCommand(CLIENT_ID, TRAINER_HOLDER_ID)))
                .thenReturn(Optional.of(ticket()));

        mockMvc.perform(post("/api/v1/clients/" + CLIENT_ID.value() + "/activation-codes"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(CLIENT_ID.value().toString()))
                .andExpect(jsonPath("$.activationCode").value("ABCD2345"));
    }

    @Test
    void shouldReturn409WhenTheClientAlreadyActivatedTheirAccount() throws Exception {
        when(clientCommandService.handle(any(RenewActivationCodeCommand.class)))
                .thenThrow(new ActivationCodeNotRenewableException());

        mockMvc.perform(post("/api/v1/clients/" + CLIENT_ID.value() + "/activation-codes"))
                .andExpect(status().isConflict());
    }

    // --- Body profile --------------------------------------------------------------------

    @Test
    void shouldSaveTheBodyProfileAndReturnItsWeightHistory() throws Exception {
        var client = activeClient();
        client.updateBodyProfile(bodyProfileCommand("80.5"), TODAY);
        when(clientCommandService.handle(argThat((UpdateBodyProfileCommand command) ->
                command.height().centimeters() == 175 && command.weight().kilograms().compareTo(
                        new BigDecimal("80.5")) == 0)))
                .thenReturn(Optional.of(client));

        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/body-profile")
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE_BODY.formatted(175, "80.5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("Hypertrophy"))
                .andExpect(jsonPath("$.heightCm").value(175))
                .andExpect(jsonPath("$.weightHistory[0].recordedOn").value(TODAY.toString()));
    }

    @Test
    void shouldReturn422NamingTheFieldForAHeightOutOfRange() throws Exception {
        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/body-profile")
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE_BODY.formatted(260, "80.5")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("heightCm"));

        verifyNoInteractions(clientCommandService);
    }

    @Test
    void shouldReturn422NamingTheFieldForAWeightOfZero() throws Exception {
        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/body-profile")
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE_BODY.formatted(175, "0")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("weightKg"));
    }

    @Test
    void shouldReturn404WhenTheClientHasNoBodyProfileYet() throws Exception {
        when(clientQueryService.handle(new GetClientByIdQuery(CLIENT_ID, TRAINER_HOLDER_ID)))
                .thenReturn(Optional.of(activeClient()));

        mockMvc.perform(get("/api/v1/clients/" + CLIENT_ID.value() + "/body-profile"))
                .andExpect(status().isNotFound());
    }

    // --- Access --------------------------------------------------------------------------

    @Test
    @WithAnonymousUser
    void shouldRejectARequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isForbidden());
    }

    // Client data belongs to the trainer's web app: a client's token is not enough (FR-002).
    @Test
    @WithMockUser(username = TRAINER_HOLDER_ID, roles = "CLIENT")
    void shouldReturn403ForAClientToken() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(clientQueryService);
    }
}
