package com.formai.iam.interfaces.rest;

import com.formai.iam.domain.services.UserCommandService;
import com.formai.iam.interfaces.rest.resources.AccountActivationResource;
import com.formai.iam.interfaces.rest.resources.CreateAccountActivationResource;
import com.formai.iam.interfaces.rest.transform.UserAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Clients", description = "Client account activation with the one-time code " +
        "their trainer shared with them.")
@RestController
@RequestMapping("/api/v1/account-activations")
public class AccountActivationsController {

    private final UserCommandService userCommandService;
    private final UserAssembler assembler;

    public AccountActivationsController(UserCommandService userCommandService, UserAssembler assembler) {
        this.userCommandService = userCommandService;
        this.assembler = assembler;
    }

    @Operation(summary = "Activate a client account",
            description = "Redeems a valid activation code, sets the client's password and records the " +
                    "personal data processing consent. The account becomes ACTIVE and can sign in from " +
                    "the mobile app.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account activated",
                    content = @Content(schema = @Schema(implementation = AccountActivationResource.class))),
            @ApiResponse(responseCode = "400", description = "Blank activation code or password",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "Invalid, expired or used activation code, consent " +
                    "not accepted, or password not between 8 and 128 characters long",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<AccountActivationResource> activate(@Valid @RequestBody CreateAccountActivationResource resource) {
        var command = assembler.toCommand(resource);
        var user = userCommandService.handle(command)
                .orElseThrow(() -> new IllegalStateException("Account activation should never return empty"));
        return new ResponseEntity<>(assembler.toActivationResource(user), HttpStatus.CREATED);
    }
}
