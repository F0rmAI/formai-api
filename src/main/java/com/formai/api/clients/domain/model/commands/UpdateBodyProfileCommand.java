package com.formai.api.clients.domain.model.commands;

import com.formai.api.clients.domain.model.valueobjects.BodyWeight;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.Height;
import com.formai.api.clients.domain.model.valueobjects.TrainingGoal;

public record UpdateBodyProfileCommand(ClientId clientId,
                                       String holderId,
                                       TrainingGoal goal,
                                       Height height,
                                       BodyWeight weight,
                                       String restrictions) { }
