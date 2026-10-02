package com.formai.api.iam.interfaces.rest;

import com.formai.api.iam.domain.exceptions.InvalidActivationCodeException;
import com.formai.api.iam.domain.model.queries.GetUsableActivationCodeQuery;
import com.formai.api.iam.domain.services.UserQueryService;
import com.formai.api.iam.interfaces.rest.resources.ActivationCodeVerificationResource;
import com.formai.api.iam.interfaces.rest.resources.CreateActivationCodeVerificationResource;
import com.formai.api.iam.interfaces.rest.transform.UserAssembler;
import com.formai.api.shared.interfaces.rest.ApiTags;
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

@Tag(name = ApiTags.ACCOUNT_ACCESS, description = ApiTags.ACCOUNT_ACCESS_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/activation-code-verifications")
public class ActivationCodeVerificationsController {

    private final UserQueryService userQueryService;
    private final UserAssembler assembler;

    public ActivationCodeVerificationsController(UserQueryService userQueryService, UserAssembler assembler) {
        this.userQueryService = userQueryService;
        this.assembler = assembler;
    }

    // The code travels in the body, not in the path, so it never lands in access logs.
    @Operation(summary = "Verify an activation code",
            description = "Checks that the activation code exists and has not expired, before the client " +
                    "sets a password. It does not redeem the code: the account is activated with " +
                    "POST /account-activations.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The code can be used — returns when it expires",
                    content = @Content(schema = @Schema(implementation = ActivationCodeVerificationResource.class))),
            @ApiResponse(responseCode = "400", description = "Blank activation code", content = @Content),
            @ApiResponse(responseCode = "422", description = "Invalid, expired or used activation code",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<ActivationCodeVerificationResource> verify(
            @Valid @RequestBody CreateActivationCodeVerificationResource resource) {
        var activationCode = userQueryService.handle(new GetUsableActivationCodeQuery(resource.activationCode()))
                .orElseThrow(InvalidActivationCodeException::new);
        return new ResponseEntity<>(assembler.toVerificationResource(activationCode), HttpStatus.CREATED);
    }
}
