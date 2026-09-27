package com.formai.api.clients.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;

// weightHistory lists every recorded weight, oldest first.
public record BodyProfileResource(String goal,
                                  int heightCm,
                                  BigDecimal weightKg,
                                  String restrictions,
                                  List<BodyWeightRecordResource> weightHistory) { }
