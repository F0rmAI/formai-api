package com.formai.api.tracking.application.internal.commandservices;

import com.formai.api.tracking.application.internal.outboundservices.acl.ExternalPlanningService;
import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.commands.EndActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutineCommand;
import com.formai.api.tracking.domain.model.commands.SyncActiveRoutinesOfRoutineCommand;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.repositories.ActiveRoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.ROUTINE_ID;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.activeRoutine;
import static com.formai.api.tracking.TrackingTestData.plannedRoutine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveRoutineCommandServiceImplTest {

    @Mock
    ActiveRoutineRepository activeRoutineRepository;

    @Mock
    ExternalPlanningService externalPlanningService;

    @InjectMocks
    ActiveRoutineCommandServiceImpl commandService;

    private void savesReturnTheRoutine() {
        when(activeRoutineRepository.save(any(ActiveRoutine.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void shouldCreateTheLocalCopyOnFirstSync() {
        // Arrange
        when(externalPlanningService.fetchActiveRoutine(CLIENT_ID)).thenReturn(Optional.of(plannedRoutine(1)));
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());
        savesReturnTheRoutine();

        // Act
        var routine = commandService.handle(new SyncActiveRoutineCommand(CLIENT_ID));

        // Assert
        assertThat(routine).hasValueSatisfying(synced -> {
            assertThat(synced.getClientId()).isEqualTo(CLIENT_ID);
            assertThat(synced.getVersion()).isEqualTo(1);
        });
    }

    @Test
    void shouldSyncEveryClientFollowingTheRoutine() {
        // Arrange
        var otherClient = new ClientId(UUID.randomUUID());
        var first = activeRoutine();
        var second = activeRoutine();
        second.setClientId(otherClient);
        when(activeRoutineRepository.findAllByRoutineId(ROUTINE_ID)).thenReturn(List.of(first, second));
        when(externalPlanningService.fetchActiveRoutine(any(ClientId.class))).thenReturn(Optional.of(plannedRoutine(2)));
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(first));
        when(activeRoutineRepository.findByClientId(otherClient)).thenReturn(Optional.of(second));
        savesReturnTheRoutine();

        // Act
        commandService.handle(new SyncActiveRoutinesOfRoutineCommand(ROUTINE_ID));

        // Assert
        assertThat(first.getVersion()).isEqualTo(2);
        assertThat(second.getVersion()).isEqualTo(2);
    }

    @Test
    void shouldUpdateTheExistingCopyToTheNewVersion() {
        var existing = activeRoutine();
        when(externalPlanningService.fetchActiveRoutine(CLIENT_ID)).thenReturn(Optional.of(plannedRoutine(2)));
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));
        savesReturnTheRoutine();

        var routine = commandService.handle(new SyncActiveRoutineCommand(CLIENT_ID));

        assertThat(routine).containsSame(existing);
        assertThat(existing.getVersion()).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyWhenPlanningHasNoRoutineForTheClient() {
        when(externalPlanningService.fetchActiveRoutine(CLIENT_ID)).thenReturn(Optional.empty());

        assertThat(commandService.handle(new SyncActiveRoutineCommand(CLIENT_ID))).isEmpty();
        verify(activeRoutineRepository, never()).save(any());
    }

    @Test
    void shouldEndTheRoutineWhenPlanningNoLongerAssignsOne() {
        var routine = activeRoutine();
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(routine));
        when(externalPlanningService.fetchActiveRoutine(CLIENT_ID)).thenReturn(Optional.empty());

        commandService.handle(new EndActiveRoutineCommand(CLIENT_ID, TODAY));

        assertThat(routine.getEndDate()).isEqualTo(TODAY);
        verify(activeRoutineRepository).save(routine);
    }

    @Test
    void shouldResyncInsteadOfEndingWhenANewRoutineWasAlreadyAssigned() {
        var routine = activeRoutine();
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(routine));
        when(externalPlanningService.fetchActiveRoutine(CLIENT_ID)).thenReturn(Optional.of(plannedRoutine(3)));

        commandService.handle(new EndActiveRoutineCommand(CLIENT_ID, TODAY));

        assertThat(routine.getEndDate()).isNull();
        assertThat(routine.getVersion()).isEqualTo(3);
        verify(activeRoutineRepository).save(routine);
    }

    @Test
    void shouldIgnoreEndingWhenTheClientHasNoLocalRoutine() {
        when(activeRoutineRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        commandService.handle(new EndActiveRoutineCommand(CLIENT_ID, TODAY));

        verify(activeRoutineRepository, never()).save(any());
    }
}
