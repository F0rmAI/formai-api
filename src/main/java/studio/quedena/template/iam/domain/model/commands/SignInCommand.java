package studio.quedena.template.iam.domain.model.commands;

import studio.quedena.template.iam.domain.model.valueobjects.Email;

public record SignInCommand(Email email, String rawPassword) { }
