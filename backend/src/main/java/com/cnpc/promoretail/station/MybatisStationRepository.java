package com.cnpc.promoretail.station;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.station.model.Station;
import com.cnpc.promoretail.station.model.StationQuery;
import com.cnpc.promoretail.station.persistence.StationEntity;
import com.cnpc.promoretail.station.persistence.StationMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisStationRepository implements StationRepository {

    private final StationMapper mapper;

    public MybatisStationRepository(StationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Station> findByQuery(StationQuery query) {
        StationQuery effectiveQuery = query == null ? new StationQuery(null, null, null, null) : query;
        LambdaQueryWrapper<StationEntity> wrapper = new LambdaQueryWrapper<StationEntity>()
                .orderByAsc(StationEntity::getStationCode);
        if (effectiveQuery.city() != null) {
            wrapper.eq(StationEntity::getCity, effectiveQuery.city());
        }
        if (effectiveQuery.district() != null) {
            wrapper.eq(StationEntity::getDistrict, effectiveQuery.district());
        }
        if (effectiveQuery.stationType() != null) {
            wrapper.eq(StationEntity::getStationType, effectiveQuery.stationType());
        }
        return mapper.selectList(wrapper).stream()
                .map(StationEntity::toStation)
                .filter(station -> matchesScope(effectiveQuery.salesScope(), station))
                .toList();
    }

    @Override
    public Optional<Station> findByStationCode(String stationCode) {
        if (stationCode == null || stationCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<StationEntity>()
                        .eq(StationEntity::getStationCode, stationCode.trim())
                        .last("limit 1")))
                .map(StationEntity::toStation);
    }

    private boolean matchesScope(String expectedScope, Station station) {
        if (expectedScope == null) {
            return true;
        }
        return station.salesScope().stream()
                .anyMatch(scope -> expectedScope.equalsIgnoreCase(scope));
    }
}
