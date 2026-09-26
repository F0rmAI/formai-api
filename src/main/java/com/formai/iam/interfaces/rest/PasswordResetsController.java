package com.formai.iam.interfaces.rest;

import com.formai.iam.domain.services.UserCommandService;
import com.formai.iam.interfaces.rest.resources.CreatePasswordResetResource;
import com.formai.iam.interfaces.rest.resources.PasswordResetResource;
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

@Tag(name = "Password Recovery", description = "Set a new password with a one-time reset link token.")
@RestController
@RequestMapping("/api/v1/password-resets")
public class PasswordResetsController {

    private final UserCommandService userCommandService;
    private final UserAssembler assembler;

    public PasswordResetsController(UserCommandService userCommandService, UserAssembler assembler) {
        this.userCommandService = userCommandService;
        this.assembler = assembler;
    }

    @Operation(summary = "Reset the password",
            description = "Redeems a valid, unused reset token and replaces the account password.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Password reset",
                    content = @Content(schema = @Schema(implementation = PasswordResetResource.class))),
            @ApiResponse(responseCode = "400", description = "Blank token or password", content = @Content),
            @ApiResponse(responseCode = "422", description = "Invalid, expired or used token, or password not " +
                    "between 8 and 128 characters long — request a new link",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<PasswordResetResource> reset(@Valid @RequestBody CreatePasswordResetResource resource) {
        userCommandService.handle(assembler.toCommand(resource));
        return new ResponseEntity<>(assembler.toPasswordResetResource(), HttpStatus.CREATED);
    }
}
