package com.cnpc.promoretail.replenishment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.replenishment.model.ReplenishmentList;
import com.cnpc.promoretail.replenishment.persistence.entity.ReplenishmentListEntity;
import com.cnpc.promoretail.replenishment.persistence.mapper.ReplenishmentListMapper;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisReplenishmentListRepository implements ReplenishmentListRepository {

    private final ReplenishmentListMapper mapper;

    public MybatisReplenishmentListRepository(ReplenishmentListMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ReplenishmentList save(ReplenishmentList list) {
        ReplenishmentListEntity existing = mapper.selectOne(new LambdaQueryWrapper<ReplenishmentListEntity>()
                .eq(ReplenishmentListEntity::getListId, list.listId()));
        ReplenishmentListEntity entity = toEntity(list);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            entity.setId(existing.getId());
            mapper.updateById(entity);
        }
        return list;
    }

    @Override
    public Optional<ReplenishmentList> findByListId(String listId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<ReplenishmentListEntity>()
                        .eq(ReplenishmentListEntity::getListId, listId)))
                .map(this::toList);
    }

    private ReplenishmentListEntity toEntity(ReplenishmentList list) {
        ReplenishmentListEntity entity = new ReplenishmentListEntity();
        entity.setListId(list.listId());
        entity.setListName(list.listName());
        entity.setStatus(list.status());
        entity.setItems(list.items());
        entity.setTotalItems(list.totalItems());
        entity.setCreatedBy(list.createdBy());
        entity.setCreatedAt(list.createdAt());
        entity.setUpdatedBy(list.updatedBy());
        entity.setUpdatedAt(list.updatedAt());
        return entity;
    }

    private ReplenishmentList toList(ReplenishmentListEntity entity) {
        return new ReplenishmentList(
                entity.getListId(),
                entity.getListName(),
                entity.getStatus(),
                entity.getItems(),
                entity.getTotalItems() == null ? 0 : entity.getTotalItems(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }
}
