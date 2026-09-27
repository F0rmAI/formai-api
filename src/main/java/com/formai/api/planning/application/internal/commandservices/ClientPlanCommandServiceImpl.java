package com.formai.api.planning.application.internal.commandservices;

import com.formai.api.planning.application.internal.outboundservices.acl.ExternalClientsService;
import com.formai.api.planning.domain.exceptions.AssigneeNotFoundException;
import com.formai.api.planning.domain.exceptions.ClientNotAssignableException;
import com.formai.api.planning.domain.exceptions.RoutineNotFoundException;
import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.CloseAssignmentCommand;
import com.formai.api.planning.domain.model.entities.Assignment;
import com.formai.api.planning.domain.model.events.AssignmentClosed;
import com.formai.api.planning.domain.model.events.RoutineAssigned;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.repositories.ClientPlanRepository;
import com.formai.api.planning.domain.repositories.RoutineRepository;
import com.formai.api.planning.domain.services.ClientPlanCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClientPlanCommandServiceImpl implements ClientPlanCommandService {

    private final ClientPlanRepository clientPlanRepository;
    private final RoutineRepository routineRepository;
    private final ExternalClientsService externalClientsService;
    private final ApplicationEventPublisher eventPublisher;

    public ClientPlanCommandServiceImpl(ClientPlanRepository clientPlanRepository,
                                        RoutineRepository routineRepository,
                                        ExternalClientsService externalClientsService,
                                        ApplicationEventPublisher eventPublisher) {
        this.clientPlanRepository = clientPlanRepository;
        this.routineRepository = routineRepository;
        this.externalClientsService = externalClientsService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Optional<ClientPlan> handle(AssignRoutineCommand command) {
        routineRepository.findByIdAndHolderId(command.routineId(), command.holderId())
                .orElseThrow(RoutineNotFoundException::new);
        var isActive = externalClientsService.isActiveClientOfTrainer(command.clientId(), command.holderId())
                .orElseThrow(AssigneeNotFoundException::new);
        if (!isActive) {
            throw new ClientNotAssignableException();
        }
        var plan = clientPlanRepository.findByClientId(command.clientId())
                .orElseGet(() -> ClientPlan.startFor(command.clientId(), command.holderId()));
        var previous = plan.currentAssignment();
        plan.assign(command);
        var saved = clientPlanRepository.save(plan);

        previous.ifPresent(closed -> publishClosed(command.clientId(), closed));
        eventPublisher.publishEvent(new RoutineAssigned(command.clientId().value(), command.routineId().value(),
                command.startDate()));
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public void handle(CloseAssignmentCommand command) {
        clientPlanRepository.findByClientId(command.clientId()).ifPresent(plan -> {
            var current = plan.currentAssignment();
            plan.closeCurrentAssignment(command.endDate());
            clientPlanRepository.save(plan);
            current.ifPresent(closed -> publishClosed(command.clientId(), closed));
        });
    }

    private void publishClosed(ClientId clientId, Assignment closed) {
        eventPublisher.publishEvent(new AssignmentClosed(clientId.value(), closed.getRoutineId().value(),
                closed.getPeriod().endDate()));
    }
}
