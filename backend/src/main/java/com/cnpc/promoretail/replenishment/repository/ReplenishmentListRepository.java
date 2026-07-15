package com.cnpc.promoretail.replenishment.repository;

import com.cnpc.promoretail.replenishment.model.ReplenishmentList;
import java.util.Optional;

public interface ReplenishmentListRepository {

    ReplenishmentList save(ReplenishmentList list);

    Optional<ReplenishmentList> findByListId(String listId);
}
