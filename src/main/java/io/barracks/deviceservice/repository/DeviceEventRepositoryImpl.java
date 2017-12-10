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
import io.barracks.deviceservice.model.DeviceEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
public class DeviceEventRepositoryImpl implements DeviceEventRepositoryCustom {

    private static final String USER_ID_KEY = "userId";
    private static final String UNIT_ID_KEY = "unitId";
    private static final String RECEPTION_DATE_KEY = "receptionDate";
    private static final String CHANGED_KEY = "changed";
    private final MongoOperations operations;

    @Autowired
    public DeviceEventRepositoryImpl(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public DataSet getSeenDeviceCount(String userId, OffsetDateTime start, OffsetDateTime end) {
        final ArrayList<Criteria> criterias = new ArrayList<>();
        criterias.add(where(USER_ID_KEY).is(userId));
        if (!start.equals(OffsetDateTime.MIN)) {
            criterias.add(where(RECEPTION_DATE_KEY).gte(start));
        }
        if (!end.equals(OffsetDateTime.MAX)) {
            criterias.add(where(RECEPTION_DATE_KEY).lte(end));
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(new Criteria().andOperator(criterias.toArray(new Criteria[0]))),
                Aggregation.group(UNIT_ID_KEY),
                Aggregation.group("_id").count().as("total"),
                Aggregation.group("total").count().as("total")
        );
        AggregationResults<DataSet> result = operations.aggregate(aggregation, DeviceEvent.class, DataSet.class);
        return result.getUniqueMappedResult();
    }

    @Override
    public Page<DeviceEvent> findByUserIdAndUnitId(String userId, String unitId, boolean onlyChanged, Pageable pageable) {
        Query query = query(where(USER_ID_KEY).is(userId).and(UNIT_ID_KEY).is(unitId));
        if (onlyChanged) {
            query.addCriteria(new Criteria().andOperator(where(CHANGED_KEY).exists(true), where(CHANGED_KEY).is(true)));
        }
        long count = operations.count(query, DeviceEvent.class);
        List<DeviceEvent> deviceEvents = operations.find(query.with(pageable), DeviceEvent.class);
        return new PageImpl<>(deviceEvents, pageable, count);
    }
}
