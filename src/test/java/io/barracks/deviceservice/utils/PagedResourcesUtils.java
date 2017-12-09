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

package io.barracks.deviceservice.utils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class PagedResourcesUtils {

    public static <T> PagedResources<T> buildPagedResources(Pageable pageable, List<T> items) {
        return new PagedResources<>(new ArrayList<>(items), new PagedResources.PageMetadata(pageable.getPageSize(), pageable.getPageNumber(), items.size()));
    }

    public static <T> PagedResources<Resource<T>> buildPagedResourcesFromPage(Page<T> page) {
        final List<Resource<T>> resources = page.getContent().stream().map(item -> new Resource<T>(item)).collect(toList());
        return new PagedResources<>(resources, new PagedResources.PageMetadata(page.getSize(), page.getNumber(), page.getTotalElements()));
    }

    public static <T> PagedResources<T> buildPagedResources(Pageable pageable, T... items) {
        return buildPagedResources(pageable, Arrays.asList(items));
    }

}
