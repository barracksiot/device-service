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

package io.barracks.deviceservice.rest;

import io.barracks.deviceservice.manager.FilterManager;
import io.barracks.deviceservice.model.DeviceRequest;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/owners/{userId}/devices/{unitId}/match")
public class MatchResource {

    private final FilterManager filterManager;

    @Autowired
    public MatchResource(FilterManager filterManager) {
        this.filterManager = filterManager;
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    public Object matchEvent(
            @PathVariable("userId") @NotBlank String userId,
            @PathVariable("unitId") @NotBlank String unitId,
            @RequestParam("filter") @NotEmpty List<String> filters,
            @RequestParam(value = "first", required = false, defaultValue = "true") boolean first,
            @RequestBody @Valid DeviceRequest event
    ) {
        if (first) {
            return filterManager.findFirstMatchingFilter(userId, unitId, filters, event);
        } else {
            return filterManager.findMatchingFilters(userId, unitId, filters, event);
        }
    }
}
