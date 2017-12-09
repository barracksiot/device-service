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

import io.barracks.deviceservice.manager.SegmentManager;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.Segment;
import io.barracks.deviceservice.rest.entity.SegmentStatus;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.util.List;

@RestController
@RequestMapping("/segments")
public class SegmentResource {
    private final PagedResourcesAssembler<Segment> segmentAssembler;
    private final PagedResourcesAssembler<Device> deviceAssembler;
    private final SegmentManager segmentManager;

    @Autowired
    public SegmentResource(
            PagedResourcesAssembler<Segment> assembler,
            PagedResourcesAssembler<Device> deviceAssembler,
            SegmentManager segmentManager
    ) {
        this.segmentAssembler = assembler;
        this.deviceAssembler = deviceAssembler;
        this.segmentManager = segmentManager;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(SegmentStatus.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                setValue(SegmentStatus.fromName(text));
            }
        });
    }

    @RequestMapping(method = RequestMethod.POST)
    public Segment createSegment(@Valid @RequestBody Segment segment) {
        return segmentManager.createSegment(segment);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{segmentId}")
    public Segment getSegmentById(@PathVariable("segmentId") String segmentId) {
        return segmentManager.getSegmentById(segmentId);
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/{segmentId}")
    public Segment updateSegment(@PathVariable("segmentId") String segmentId, @Valid @RequestBody Segment segment) {
        return segmentManager.updateSegment(segmentId, segment);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{segmentId}/devices")
    public PagedResources<Resource<Device>> getDevicesForSegment(@PathVariable("segmentId") String segmentId, @RequestParam(value = "versionId", required = false) String versionId, Pageable pageable) {
        if (StringUtils.isEmpty(versionId)) {
            return deviceAssembler.toResource(segmentManager.getDevicesBySegmentId(segmentId, pageable));
        } else {
            return deviceAssembler.toResource(segmentManager.getDevicesBySegmentIdAndVersionId(segmentId, versionId, pageable));
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "/other/devices")
    public PagedResources<Resource<Device>> getOtherDevicesForUser(@RequestParam("userId") String userId, @RequestParam(value = "versionId", required = false) String versionId, Pageable pageable) {
        if (StringUtils.isEmpty(versionId)) {
            return deviceAssembler.toResource(segmentManager.getOtherDevicesForUser(userId, pageable));
        } else {
            return deviceAssembler.toResource(segmentManager.getOtherDevicesForUserAndVersionId(userId, versionId, pageable));
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<Segment> getSegmentsByUserIdAndStatus(
            @NotBlank @RequestParam(value = "userId") String userId,
            @NotBlank @RequestParam(value = "status") SegmentStatus status) {
        return status == SegmentStatus.ACTIVE ? segmentManager.getActiveSegments(userId) : segmentManager.getInactiveSegments(userId);
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/order")
    public List<String> updateOrder(@RequestParam("userId") String userId, @RequestBody List<String> order) {
        return segmentManager.updateSegmentOrder(userId, order);
    }
}
