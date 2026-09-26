package com.formai.iam.interfaces.rest;

import com.formai.iam.domain.services.UserCommandService;
import com.formai.iam.interfaces.rest.resources.CreatePasswordResetRequestResource;
import com.formai.iam.interfaces.rest.resources.PasswordResetRequestResource;
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

@Tag(name = "Password Recovery", description = "Recover account access: request a one-time reset link " +
        "by email, then set a new password with its token.")
@RestController
@RequestMapping("/api/v1/password-reset-requests")
public class PasswordResetRequestsController {

    private final UserCommandService userCommandService;
    private final UserAssembler assembler;

    public PasswordResetRequestsController(UserCommandService userCommandService, UserAssembler assembler) {
        this.userCommandService = userCommandService;
        this.assembler = assembler;
    }

    @Operation(summary = "Request a password reset link",
            description = "Issues a one-time reset link valid for 30 minutes when an active account exists " +
                    "for the email. Always answers with the same message, whether the account exists or " +
                    "not, to prevent user enumeration.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request accepted",
                    content = @Content(schema = @Schema(implementation = PasswordResetRequestResource.class))),
            @ApiResponse(responseCode = "400", description = "Malformed email", content = @Content)
    })
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<PasswordResetRequestResource> request(@Valid @RequestBody CreatePasswordResetRequestResource resource) {
        userCommandService.handle(assembler.toCommand(resource));
        return new ResponseEntity<>(assembler.toPasswordResetRequestResource(), HttpStatus.CREATED);
    }
}
