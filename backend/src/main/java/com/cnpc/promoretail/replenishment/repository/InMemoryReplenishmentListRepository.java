package com.cnpc.promoretail.replenishment.repository;

import com.cnpc.promoretail.replenishment.model.ReplenishmentList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryReplenishmentListRepository implements ReplenishmentListRepository {

    private final ConcurrentMap<String, ReplenishmentList> lists = new ConcurrentHashMap<>();

    @Override
    public ReplenishmentList save(ReplenishmentList list) {
        lists.put(list.listId(), list);
        return list;
    }

    @Override
    public Optional<ReplenishmentList> findByListId(String listId) {
        return Optional.ofNullable(lists.get(listId));
    }
}
