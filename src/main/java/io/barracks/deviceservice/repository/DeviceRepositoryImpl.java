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

import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceConfiguration;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.model.operator.OperatorConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class DeviceRepositoryImpl implements DeviceRepositoryCustom {
    private static final String EVENT_KEY = "event";
    private static final String UNIT_ID_KEY = "unitId";
    private static final String USER_ID_KEY = "userId";
    private static final String CONFIGURATION_KEY = "configuration";
    private static final String VERSION_ID_KEY = "event.versionId";
    private static final String RECEPTION_DATE_KEY = "receptionDate";
    private static final String FIRST_SEEN_KEY = "firstSeen";
    private static final String SEGMENT_ID_KEY = "event.segmentId";
    private final MongoOperations operations;

    @Autowired
    public DeviceRepositoryImpl(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public Device updateConfiguration(String userId, String unitId, DeviceConfiguration configuration) {
        return updateDocument(userId, unitId, CONFIGURATION_KEY, configuration);
    }

    @Override
    public Device updateDeviceEvent(String userId, String unitId, DeviceEvent event) {
        return updateDocument(userId, unitId, EVENT_KEY, event);
    }

    @Override
    public Device updateFirstSeen(String userId, String unitId, Date creationDate) {
        return updateDocument(userId, unitId, FIRST_SEEN_KEY, creationDate);
    }

    <T> Device updateDocument(String userId, String unitId, String documentKey, T value) {
        return operations.findAndModify(
                Query.query(where(USER_ID_KEY).is(userId).and(UNIT_ID_KEY).is(unitId)),
                Update.update(documentKey, value),
                new FindAndModifyOptions().upsert(true).returnNew(true),
                Device.class
        );
    }

    @Override
    public DataSet getDevicesCountPerVersionId(String userId) {
        Criteria criteria = where(USER_ID_KEY).is(userId).and(EVENT_KEY).exists(true);
        Query query = Query.query(criteria);
        List versionIds = operations.getCollection(Device.class.getDeclaredAnnotation(Document.class).collection())
                .distinct(VERSION_ID_KEY, query.getQueryObject());
        DataSet.Builder builder = DataSet.builder();
        long total = 0;
        for (Object version : versionIds) {
            Criteria countCriteria = where(USER_ID_KEY).is(userId).and(EVENT_KEY).exists(true).and(VERSION_ID_KEY).is(version);
            Query countQuery = Query.query(countCriteria);
            long count = operations.count(countQuery, Device.class);
            total += count;
            builder.value((String) version, BigDecimal.valueOf(count));
        }
        return builder.total(BigDecimal.valueOf(total)).build();
    }

    @Override
    public DataSet getLastSeenDeviceCount(String userId, OffsetDateTime start, OffsetDateTime end) {
        ArrayList<Criteria> criteria = new ArrayList<>();
        criteria.add(where(USER_ID_KEY).is(userId));
        criteria.add(where(EVENT_KEY).exists(true));
        if (!start.equals(OffsetDateTime.MIN)) {
            criteria.add(where(EVENT_KEY + "." + RECEPTION_DATE_KEY).gte(Date.from(start.toInstant())));
        }
        if (!end.equals(OffsetDateTime.MAX)) {
            criteria.add(where(EVENT_KEY + "." + RECEPTION_DATE_KEY).lte(Date.from(end.toInstant())));
        }
        Query query = Query.query(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        return DataSet.builder().total(BigDecimal.valueOf(operations.count(query, Device.class))).build();
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
    public Page<Device> findByUserId(String userId, Optional<Operator> searchFilter, Pageable pageable) {
        final Query query = query(where(USER_ID_KEY).is(userId));
        if (searchFilter.isPresent()) {
            query.addCriteria(OperatorConverter.toMongoCriteria(searchFilter.get()));
        }
        final long count = operations.count(query, Device.class);
        final List<Device> devices = operations.find(query.with(pageable), Device.class);
        return new PageImpl<>(devices, pageable, count);
    }

    @Override
    public Page<Device> findBySegmentId(String segmentId, Pageable pageable) {
        final Query query = query(where(SEGMENT_ID_KEY).is(segmentId));
        final long count = operations.count(query, Device.class);
        final List<Device> devices = operations.find(query.with(pageable), Device.class);
        return new PageImpl<>(devices, pageable, count);
    }

    @Override
    public Page<Device> findDevicesNotIn(String userId, List<String> segmentIds, Pageable pageable) {
        final Query query = query(
                new Criteria().andOperator(
                        where(USER_ID_KEY).is(userId),
                        where(EVENT_KEY).exists(true),
                        new Criteria().orOperator(
                                where(SEGMENT_ID_KEY).nin(segmentIds),
                                where(SEGMENT_ID_KEY).exists(false)
                        )
                )
        );
        final long count = operations.count(query, Device.class);
        final List<Device> configurations = operations.find(query.with(pageable), Device.class);
        return new PageImpl<>(configurations, pageable, count);
    }

    @Override
    public Page<Device> findBySegmentIdAndVersionId(String segmentId, String versionId, Pageable pageable) {
        final Query query = query(where(SEGMENT_ID_KEY).is(segmentId).and(VERSION_ID_KEY).is(versionId));
        final long count = operations.count(query, Device.class);
        final List<Device> devices = operations.find(query.with(pageable), Device.class);
        return new PageImpl<>(devices, pageable, count);
    }

    @Override
    public Page<Device> findForUserIdAndVersionIdAndNotSegmentIds(String userId, String versionId, List<String> segmentIds, Pageable pageable) {
        final Query query = query(
                new Criteria().andOperator(
                        where(USER_ID_KEY).is(userId),
                        where(EVENT_KEY).exists(true),
                        where(VERSION_ID_KEY).is(versionId),
                        new Criteria().orOperator(
                                where(SEGMENT_ID_KEY).nin(segmentIds),
                                where(SEGMENT_ID_KEY).exists(false)
                        )
                )
        );
        final long count = operations.count(query, Device.class);
        final List<Device> configurations = operations.find(query.with(pageable), Device.class);
        return new PageImpl<>(configurations, pageable, count);
    }
}
