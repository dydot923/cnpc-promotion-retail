package com.cnpc.promoretail.checkout.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.checkout.model.CheckoutCalculationRecord;
import com.cnpc.promoretail.checkout.persistence.entity.CheckoutCalculationRecordEntity;
import com.cnpc.promoretail.checkout.persistence.mapper.CheckoutCalculationRecordMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisCheckoutCalculationRecordRepository implements CheckoutCalculationRecordRepository {

    private final CheckoutCalculationRecordMapper mapper;

    public MybatisCheckoutCalculationRecordRepository(CheckoutCalculationRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CheckoutCalculationRecord save(CheckoutCalculationRecord record) {
        mapper.insert(toEntity(record));
        return record;
    }

    @Override
    public Optional<CheckoutCalculationRecord> findByCalculationId(String calculationId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<CheckoutCalculationRecordEntity>()
                        .eq(CheckoutCalculationRecordEntity::getCalculationId, calculationId)))
                .map(this::toRecord);
    }

    @Override
    public List<CheckoutCalculationRecord> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<CheckoutCalculationRecordEntity>()
                        .orderByAsc(CheckoutCalculationRecordEntity::getCreatedAt))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private CheckoutCalculationRecordEntity toEntity(CheckoutCalculationRecord record) {
        CheckoutCalculationRecordEntity entity = new CheckoutCalculationRecordEntity();
        entity.setCalculationId(record.calculationId());
        entity.setRequestSnapshot(record.requestSnapshot());
        entity.setResultSnapshot(record.resultSnapshot());
        entity.setRuleVersionIds(record.ruleVersionIds());
        entity.setCreatedAt(record.createdAt());
        return entity;
    }

    private CheckoutCalculationRecord toRecord(CheckoutCalculationRecordEntity entity) {
        return new CheckoutCalculationRecord(
                entity.getCalculationId(),
                entity.getRequestSnapshot(),
                entity.getResultSnapshot(),
                entity.getRuleVersionIds(),
                entity.getCreatedAt()
        );
    }
}
