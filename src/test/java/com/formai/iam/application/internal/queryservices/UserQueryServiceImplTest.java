package com.formai.iam.application.internal.queryservices;

import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.commands.SignUpCommand;
import com.formai.iam.domain.model.queries.GetUserByIdQuery;
import com.formai.iam.domain.model.valueobjects.Email;
import com.formai.iam.domain.model.valueobjects.HashedPassword;
import com.formai.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserQueryServiceImpl queryService;

    @Test
    void shouldReturnUserWhenIdExists() {
        var user = User.registerTrainer(new SignUpCommand(new Email("trainer@formai.com"), "secret123", "Ana Trainer"),
                new HashedPassword("hashed"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(queryService.handle(new GetUserByIdQuery(user.getId()))).contains(user);
    }

    @Test
    void shouldReturnEmptyWhenIdDoesNotExist() {
        var id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(queryService.handle(new GetUserByIdQuery(id))).isEmpty();
    }
}
