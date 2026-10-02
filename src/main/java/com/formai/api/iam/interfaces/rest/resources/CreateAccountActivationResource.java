package com.formai.api.iam.interfaces.rest.resources;

import com.formai.api.iam.domain.model.valueobjects.ConsentAcceptance;
import com.formai.api.iam.domain.model.valueobjects.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountActivationResource(
        @NotBlank String activationCode,
        @NotBlank @Size(max = 320) @jakarta.validation.constraints.Email(regexp = Email.FORMAT) String email,
        @NotBlank String password,
        boolean consentAccepted,
        @NotBlank @Size(max = ConsentAcceptance.MAX_VERSION_LENGTH) String consentVersion
) { }
