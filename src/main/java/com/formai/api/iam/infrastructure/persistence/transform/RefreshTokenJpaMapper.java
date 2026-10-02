package com.formai.api.iam.infrastructure.persistence.transform;

import com.formai.api.iam.domain.model.aggregates.RefreshToken;
import com.formai.api.iam.infrastructure.persistence.entities.RefreshTokenJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenJpaMapper {

    RefreshTokenJpaEntity toEntity(RefreshToken refreshToken);

    RefreshToken toDomain(RefreshTokenJpaEntity entity);
}
