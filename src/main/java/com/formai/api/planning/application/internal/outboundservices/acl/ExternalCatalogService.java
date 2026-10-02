package com.formai.api.planning.application.internal.outboundservices.acl;

import com.formai.api.planning.domain.model.valueobjects.MachineId;
import org.springframework.stereotype.Service;

// The catalog context (final increment, TB2) does not exist yet, so no machine is published
// and every link is rejected. Once it exists, this ACL delegates to CatalogContextFacade.
@Service
public class ExternalCatalogService {

    public boolean isMachinePublished(MachineId machineId) {
        return false;
    }
}
