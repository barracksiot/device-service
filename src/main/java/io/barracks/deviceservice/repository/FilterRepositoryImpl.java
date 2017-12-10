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

import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.repository.exception.FilterCreationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Repository
public class FilterRepositoryImpl implements FilterRepositoryCustom {

    private static final String USER_ID_KEY = "userId";
    private static final String NAME_KEY = "name";

    private final MongoOperations operations;

    @Autowired
    public FilterRepositoryImpl(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public Filter createFilter(Filter filter) {
        try {
            operations.insert(filter);
            return operations.findById(filter.getId(), Filter.class);
        } catch (DuplicateKeyException e) {
            throw new FilterCreationFailedException(filter, (com.mongodb.DuplicateKeyException) e.getCause()); // WARNING: we use implementation details of the exception...
        }
    }

    @Override
    public Page<Filter> getFiltersByUserId(String userId, Pageable pageable) {
        final Query query = query(where(USER_ID_KEY).is(userId));

        final long count = operations.count(query, Filter.class);
        final List<Filter> filters = operations.find(query.with(pageable), Filter.class);
        return new PageImpl<>(filters, pageable, count);
    }

    @Override
    public Optional<Filter> getFilterByUserIdAndName(String userId, String name) {
        final Query query = query(where(USER_ID_KEY).is(userId).and(NAME_KEY).is(name));
        return Optional.ofNullable(operations.findOne(query, Filter.class));
    }

    @Override
    public void deleteFilterByUserIdAndName(String userId, String name) {
        getFilterByUserIdAndName(userId, name).ifPresent(operations::remove);
    }

    @Override
    public Filter updateFilter(Filter filter, Operator query) {
        final Query mongoQuery = query(where(USER_ID_KEY).is(filter.getUserId()).and(NAME_KEY).is(filter.getName()));
        final Update update = Update.update("query", query);
        return operations.findAndModify(mongoQuery, update, new FindAndModifyOptions().returnNew(true), Filter.class);
    }
}
