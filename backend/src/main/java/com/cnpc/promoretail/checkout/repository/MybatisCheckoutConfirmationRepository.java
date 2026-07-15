package com.cnpc.promoretail.checkout.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.checkout.model.CheckoutConfirmation;
import com.cnpc.promoretail.checkout.persistence.entity.CheckoutConfirmationEntity;
import com.cnpc.promoretail.checkout.persistence.mapper.CheckoutConfirmationMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisCheckoutConfirmationRepository implements CheckoutConfirmationRepository {

    private final CheckoutConfirmationMapper mapper;

    public MybatisCheckoutConfirmationRepository(CheckoutConfirmationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CheckoutConfirmation save(CheckoutConfirmation confirmation) {
        mapper.insert(toEntity(confirmation));
        return confirmation;
    }

    @Override
    public Optional<CheckoutConfirmation> findByConfirmationId(String confirmationId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<CheckoutConfirmationEntity>()
                        .eq(CheckoutConfirmationEntity::getConfirmationId, confirmationId)))
                .map(this::toConfirmation);
    }

    @Override
    public List<CheckoutConfirmation> findByCalculationId(String calculationId) {
        return mapper.selectList(new LambdaQueryWrapper<CheckoutConfirmationEntity>()
                        .eq(CheckoutConfirmationEntity::getCalculationId, calculationId)
                        .orderByAsc(CheckoutConfirmationEntity::getConfirmedAt))
                .stream()
                .map(this::toConfirmation)
                .toList();
    }

    private CheckoutConfirmationEntity toEntity(CheckoutConfirmation confirmation) {
        CheckoutConfirmationEntity entity = new CheckoutConfirmationEntity();
        entity.setConfirmationId(confirmation.confirmationId());
        entity.setCalculationId(confirmation.calculationId());
        entity.setSelectedCandidateId(confirmation.selectedCandidateId());
        entity.setSelectedCandidateSnapshot(confirmation.selectedCandidateSnapshot());
        entity.setOperatorId(confirmation.operatorId());
        entity.setOperatorName(confirmation.operatorName());
        entity.setSkipped(confirmation.skipped());
        entity.setConfirmedAt(confirmation.confirmedAt());
        entity.setCreatedAt(confirmation.createdAt());
        entity.setUpdatedAt(confirmation.updatedAt());
        return entity;
    }

    private CheckoutConfirmation toConfirmation(CheckoutConfirmationEntity entity) {
        return new CheckoutConfirmation(
                entity.getConfirmationId(),
                entity.getCalculationId(),
                entity.getSelectedCandidateId(),
                entity.getSelectedCandidateSnapshot(),
                entity.getOperatorId(),
                entity.getOperatorName(),
                Boolean.TRUE.equals(entity.getSkipped()),
                entity.getConfirmedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
