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

import io.barracks.deviceservice.model.Segment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.List;

public class SegmentRepositoryImpl implements SegmentRepositoryCustom {
    public static final String USER_ID_KEY = "userId";
    public static final String SEGMENT_ID_KEY = "id";
    public static final String UPDATED_KEY = "updated";
    private final MongoOperations operations;

    @Autowired
    public SegmentRepositoryImpl(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public List<Segment> getSegmentsInIds(String userId, List<String> ids) {
        return getSegmentsByIds(userId, ids, true);
    }

    @Override
    public List<Segment> getSegmentsNotInIds(String userId, List<String> ids) {
        return getSegmentsByIds(userId, ids, false);
    }

    List<Segment> getSegmentsByIds(String userId, List<String> ids, boolean matchIds) {
        Criteria criteria = Criteria.where(USER_ID_KEY).is(userId);
        Query query;
        if (matchIds) {
            query = Query.query(criteria.and(SEGMENT_ID_KEY).in(ids));
        } else {
            query = Query.query(criteria.and(SEGMENT_ID_KEY).nin(ids))
                    .with(new Sort(new Sort.Order(Sort.Direction.DESC, UPDATED_KEY)));
        }
        List<Segment> results = operations.find(query, Segment.class);
        if (matchIds) {
            Segment[] sorted = new Segment[results.size()];
            for (Segment segment : results) {
                sorted[ids.indexOf(segment.getId())] = segment;
            }
            results = Arrays.asList(sorted);
        }
        return results;
    }
}
