package com.formai.api.clients.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;

public record BodyProfileResource(String goal,
                                  int heightCm,
                                  BigDecimal weightKg,
                                  String restrictions,
                                  List<BodyWeightRecordResource> weightHistory) { }
