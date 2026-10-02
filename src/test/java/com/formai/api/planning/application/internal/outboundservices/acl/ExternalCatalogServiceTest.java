package com.formai.api.planning.application.internal.outboundservices.acl;

import com.formai.api.planning.domain.model.valueobjects.MachineId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalCatalogServiceTest {

    @Test
    void shouldAnswerNotPublishedWhileTheMachineCatalogDoesNotExist() {
        var externalCatalogService = new ExternalCatalogService();

        assertThat(externalCatalogService.isMachinePublished(new MachineId(UUID.randomUUID()))).isFalse();
    }
}
