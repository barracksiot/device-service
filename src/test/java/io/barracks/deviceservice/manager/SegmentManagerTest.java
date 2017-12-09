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

package io.barracks.deviceservice.manager;

import io.barracks.deviceservice.manager.exception.InvalidSegmentOrderException;
import io.barracks.deviceservice.manager.exception.SegmentNotFoundException;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.Segment;
import io.barracks.deviceservice.model.SegmentOrder;
import io.barracks.deviceservice.repository.DeviceRepository;
import io.barracks.deviceservice.repository.SegmentOrderRepository;
import io.barracks.deviceservice.repository.SegmentRepository;
import io.barracks.deviceservice.utils.DeviceUtils;
import io.barracks.deviceservice.utils.SegmentOrderUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SegmentManagerTest {
    @Mock
    private SegmentRepository segmentRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private SegmentOrderRepository orderRepository;
    private SegmentManager segmentManager;


    @Before
    public void setUp() throws Exception {
        final SegmentManager manager = new SegmentManager(segmentRepository, deviceRepository, orderRepository);
        segmentManager = spy(manager);
        reset(segmentManager, segmentRepository, deviceRepository, orderRepository);
    }

    @Test
    public void createSegment_shouldCallRepository_andReturnSegment() {
        // Given
        Segment segment = Segment.builder().userId(UUID.randomUUID().toString()).build();
        Segment expected = segment.toBuilder().id(UUID.randomUUID().toString()).build();
        doReturn(expected).when(segmentRepository).insert(segment);

        // When
        final Segment result = segmentManager.createSegment(segment);

        // Then
        verify(segmentRepository).insert(segment);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getSegmentById_shouldCallRepository_andReturnSegment() {
        // Given
        final String id = UUID.randomUUID().toString();
        Segment expected = Segment.builder()
                .id(id)
                .userId(UUID.randomUUID().toString())
                .build();
        doReturn(expected).when(segmentRepository).findOne(id);

        // When
        final Segment result = segmentManager.getSegmentById(id);

        // Then
        verify(segmentRepository).findOne(id);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getSegmentById_whenNoSegment_shouldThrowException() {
        // Given
        final String id = UUID.randomUUID().toString();
        doReturn(null).when(segmentRepository).findOne(id);

        // Then / When
        assertThatExceptionOfType(SegmentNotFoundException.class)
                .isThrownBy(() -> segmentManager.getSegmentById(id));
        verify(segmentRepository).findOne(id);
    }

    @Test
    public void updateSegment_shouldCallRepository_andReturnSegment() {
        // Given
        final String id = UUID.randomUUID().toString();
        final Segment update = Segment.builder()
                .id(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .name(UUID.randomUUID().toString())
                .build();
        final Segment expected = update.toBuilder()
                .id(id)
                .build();
        doReturn(expected).when(segmentRepository).save(update.toBuilder().id(id).build());

        // When
        final Segment result = segmentManager.updateSegment(id, update);

        // Then
        verify(segmentRepository).save(update.toBuilder().id(id).build());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDevicesBySegmentId_shouldReturnRepositoryResults() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page expected = new PageImpl<Device>(Collections.emptyList());
        doReturn(expected).when(deviceRepository).findBySegmentId(segmentId, pageable);

        // When
        final Page result = segmentManager.getDevicesBySegmentId(segmentId, pageable);

        // Then
        verify(deviceRepository).findBySegmentId(segmentId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getOtherDevicesForUser_shouldGetSegmentOrder_andReturnRepositoryResults() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page expected = new PageImpl<Device>(Collections.emptyList());
        final List<String> segmentIds = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(SegmentOrder.builder().segmentIds(segmentIds).build()).when(orderRepository).findByUserId(userId);
        doReturn(expected).when(deviceRepository).findDevicesNotIn(userId, segmentIds, pageable);

        // When
        final Page result = segmentManager.getOtherDevicesForUser(userId, pageable);

        // Then
        verify(orderRepository).findByUserId(userId);
        verify(deviceRepository).findDevicesNotIn(userId, segmentIds, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDevicesBySegmentIdAndVersionId_shouldCallRepository_andReturnResults() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page expected = new PageImpl<>(Collections.singletonList(DeviceUtils.getDevice()));
        doReturn(expected).when(deviceRepository).findBySegmentIdAndVersionId(segmentId, versionId, pageable);

        // When
        final Page result = segmentManager.getDevicesBySegmentIdAndVersionId(segmentId, versionId, pageable);

        // Then
        verify(deviceRepository).findBySegmentIdAndVersionId(segmentId, versionId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDevicesOtherDevicesForUserAndVersionId_shouldGetOrder_andReturnRepositoryResults() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final SegmentOrder order = SegmentOrderUtils.getSegmentOrder();
        final Page expected = new PageImpl<>(Collections.singletonList(DeviceUtils.getDevice()));
        doReturn(order).when(orderRepository).findByUserId(userId);
        doReturn(expected).when(deviceRepository).findForUserIdAndVersionIdAndNotSegmentIds(userId, versionId, order.getSegmentIds(), pageable);

        // When
        final Page result = segmentManager.getOtherDevicesForUserAndVersionId(userId, versionId, pageable);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getActiveSegments_shouldGetActiveIds_andReturnMatchingSegments() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final SegmentOrder order = SegmentOrder.builder().userId(userId).build();
        final List<Segment> segments = Collections.emptyList();
        doReturn(order).when(orderRepository).findByUserId(userId);
        doReturn(segments).when(segmentRepository).getSegmentsInIds(userId, order.getSegmentIds());

        // When
        final List<Segment> result = segmentManager.getActiveSegments(userId);

        // Then
        verify(orderRepository).findByUserId(userId);
        verify(segmentRepository).getSegmentsInIds(userId, order.getSegmentIds());
        assertThat(result).isEqualTo(segments);
    }

    @Test
    public void getInactiveSegments_shouldGetActiveIds_andReturnNonMatchingSegments() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final SegmentOrder order = SegmentOrder.builder().userId(userId).build();
        final List<Segment> segments = Collections.emptyList();
        doReturn(order).when(orderRepository).findByUserId(userId);
        doReturn(segments).when(segmentRepository).getSegmentsNotInIds(userId, order.getSegmentIds());

        // When
        final List<Segment> result = segmentManager.getInactiveSegments(userId);

        // Then
        verify(orderRepository).findByUserId(userId);
        verify(segmentRepository).getSegmentsNotInIds(userId, order.getSegmentIds());
        assertThat(result).isEqualTo(segments);
    }

    @Test
    public void updateSegmentOrder_whenAllSegmentsBelongToUser_shouldUpdateOrder_andReturnOrder() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<Segment> segments = Arrays.asList(Segment.builder().build(), Segment.builder().build());
        final List<String> order = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final List<String> expected = Collections.emptyList();
        final SegmentOrder segmentOrder = SegmentOrder.builder().segmentIds(expected).build();
        doReturn(segments).when(segmentRepository).getSegmentsInIds(userId, order);
        doReturn(segmentOrder).when(orderRepository).updateOrder(userId, order);

        // When
        final List<String> result = segmentManager.updateSegmentOrder(userId, order);

        // Then
        verify(segmentRepository).getSegmentsInIds(userId, order);
        verify(orderRepository).updateOrder(userId, order);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void updateSegmentOrder_whenAllSegmentsDontBelongToUser_shouldThrowException() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<Segment> segments = Collections.singletonList(Segment.builder().build());
        final List<String> order = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(segments).when(segmentRepository).getSegmentsInIds(userId, order);

        // Then When
        assertThatExceptionOfType(InvalidSegmentOrderException.class)
                .isThrownBy(() -> segmentManager.updateSegmentOrder(userId, order));
        verify(segmentRepository).getSegmentsInIds(userId, order);
    }
}
