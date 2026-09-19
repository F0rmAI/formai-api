package studio.quedena.template.iam.domain.model.commands;

import studio.quedena.template.iam.domain.model.valueobjects.Email;

public record SignUpCommand(Email email, String rawPassword) { }
