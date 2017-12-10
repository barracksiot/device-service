/*
 * MIT License
 *
 * Copyright (c) 2017 Barracks Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.barracks.deviceservice.repository;

import com.mongodb.WriteResult;
import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.model.operator.OperatorConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
public class DeviceRepositoryImpl implements DeviceRepositoryCustom {

    private static final String LAST_EVENT_KEY = "lastEvent";
    private static final String USER_ID_KEY = "userId";
    private static final String UNIT_ID_KEY = "unitId";
    private static final String RECEPTION_DATE_KEY = "lastEvent.receptionDate";

    private final MongoOperations operations;

    @Autowired
    public DeviceRepositoryImpl(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public boolean updateLastEvent(String userId, String unitId, DeviceEvent event) {
        final Device existingDevice = findDeviceByUserIdAndUnitId(userId, unitId);
        final boolean isNew = createDeviceIfAbsent(userId, unitId, existingDevice);
        event.getReceptionDate().ifPresent(date -> updateDeviceLastEvent(userId, unitId, event, date));

        return isNew;
    }

    private Device findDeviceByUserIdAndUnitId(String userId, String unitId) {
        return operations.findOne(
                query(
                        where(USER_ID_KEY).is(userId)
                                .and(UNIT_ID_KEY).is(unitId)
                ), Device.class
        );
    }

    private boolean createDeviceIfAbsent(String userId, String unitId, Device existingDevice) {
        final boolean isNew = !Optional.ofNullable(existingDevice).isPresent();

        Optional.ofNullable(existingDevice)
                .orElseGet(() -> {
                    final Device newDevice = Device.builder().userId(userId).unitId(unitId).build();
                    operations.save(newDevice);
                    return newDevice;
                });
        return isNew;
    }

    private WriteResult updateDeviceLastEvent(String userId, String unitId, DeviceEvent event, OffsetDateTime date) {
        return operations.updateFirst(
                query(
                        new Criteria().andOperator(
                                where(USER_ID_KEY).is(userId),
                                where(UNIT_ID_KEY).is(unitId),
                                new Criteria().orOperator(
                                        where(LAST_EVENT_KEY).exists(false),
                                        where(RECEPTION_DATE_KEY).lte(date)
                                )
                        )
                ),
                Update.update(LAST_EVENT_KEY, event),
                Device.class
        );
    }

    @Override
    public DataSet getLastSeenDeviceCount(String userId, OffsetDateTime start, OffsetDateTime end) {
        final ArrayList<Criteria> criteria = new ArrayList<>();
        criteria.add(where(USER_ID_KEY).is(userId));
        criteria.add(where(LAST_EVENT_KEY).exists(true));
        if (!start.equals(OffsetDateTime.MIN)) {
            criteria.add(where(RECEPTION_DATE_KEY).gte(start));
        }
        if (!end.equals(OffsetDateTime.MAX)) {
            criteria.add(where(RECEPTION_DATE_KEY).lte(end));
        }
        final Query query = query(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        final BigDecimal total = BigDecimal.valueOf(operations.count(query, Device.class));
        return DataSet.builder().total(total).build();
    }

    @Override
    public DataSet getDeviceCountPerUserId() {
        final List<?> userIds = operations.getCollection(operations.getCollectionName(Device.class)).distinct(USER_ID_KEY);
        final Map<String, BigDecimal> values = userIds.stream()
                .collect(Collectors.toMap(
                        id -> (String) id,
                        id -> BigDecimal.valueOf(operations.count(query(where(USER_ID_KEY).is(id)), Device.class))
                ));
        final BigDecimal total = values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return DataSet.builder().values(values).total(total).build();
    }

    @Override
    public Page<Device> findByUserId(String userId, @Nullable Operator searchFilter, Pageable pageable) {
        final Query query = query(where(USER_ID_KEY).is(userId));
        Optional.ofNullable(searchFilter).ifPresent(operator ->
                query.addCriteria(OperatorConverter.toMongoCriteria(operator))
        );
        final long count = operations.count(query, Device.class);
        final List<Device> devices = operations.find(query.with(pageable), Device.class);
        return new PageImpl<>(devices, pageable, count);
    }

}
