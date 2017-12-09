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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.barracks.commons.test.JsonResourceLoader;
import io.barracks.deviceservice.manager.exception.DeviceNotFoundException;
import io.barracks.deviceservice.model.*;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.repository.*;
import io.barracks.deviceservice.utils.DeviceEventUtils;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static io.barracks.deviceservice.utils.DeviceEventUtils.getDeviceEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceManagerTest {
    @Mock
    private DeviceEventRepository deviceEventRepository;
    @Mock
    private DeviceConfigurationRepository deviceConfigurationRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private SegmentRepository segmentRepository;
    @Mock
    private SegmentOrderRepository segmentOrderRepository;
    private DeviceManager deviceManager;

    @Before
    public void setUp() throws Exception {
        final DeviceManager manager = new DeviceManager(deviceEventRepository, deviceConfigurationRepository, deviceRepository, segmentRepository, segmentOrderRepository);
        deviceManager = spy(manager);
        reset(deviceManager, deviceEventRepository, deviceConfigurationRepository, deviceRepository, segmentRepository, segmentOrderRepository);
    }

    @Test
    public void saveDeviceEvent_whenFirstSeen_shouldUpdateFirstSeen() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final Date receptionDate = new Date(123456789000L);
        final DeviceEvent source = getDeviceEvent();
        final DeviceConfiguration configuration = DeviceConfiguration.builder() // Simple configuration
                .build();
        final Device originalDevice = Device.builder() // Device with a simple configuration
                .configuration(configuration)
                .build();
        final Device updatedDevice = originalDevice.toBuilder() // Updated device should include firstSeen
                .firstSeen(receptionDate)
                .build();
        final DeviceEvent processedEvent = DeviceEvent.builder() // Source request should be assigned date
                .userId(source.getUserId())
                .unitId(source.getUnitId())
                .versionId(source.getVersionId())
                .deviceIP(source.getDeviceIP())
                .receptionDate(receptionDate)
                .additionalProperties(source.getAdditionalProperties())
                .build();
        final DeviceEvent toSave = processedEvent.toBuilder() // Final version should have changed and operator correctly set
                .changed(true)
                .segmentId(segmentId)
                .build();
        final DeviceEvent saved = getDeviceEvent();

        doReturn(receptionDate).when(deviceManager).createReceptionDate();
        doReturn(originalDevice).when(deviceManager).getOrCreateDevice(source.getUserId(), source.getUnitId());
        doReturn(updatedDevice).when(deviceRepository).updateFirstSeen(source.getUserId(), source.getUnitId(), receptionDate);
        doReturn(Optional.of(segmentId)).when(deviceManager).getExclusiveSegmentId(updatedDevice, processedEvent);
        doReturn(true).when(deviceManager).hasChanged(source, segmentId, null);
        doReturn(saved).when(deviceEventRepository).save(toSave);

        // When
        final DeviceEvent result = deviceManager.saveDeviceEvent(source);

        // Then
        verify(deviceManager).createReceptionDate();
        verify(deviceManager).getOrCreateDevice(source.getUserId(), source.getUnitId());
        verify(deviceRepository).updateFirstSeen(source.getUserId(), source.getUnitId(), receptionDate);
        verify(deviceManager).getExclusiveSegmentId(updatedDevice, processedEvent);
        verify(deviceManager).hasChanged(source, segmentId, null);
        verify(deviceEventRepository).save(toSave);
        assertThat(result).isEqualTo(saved);
    }

    @Test
    public void saveDeviceEvent_whenNotFirstSeen_shouldNotUpdateFirstSeen() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final Date receptionDate = new Date(123456789000L);
        final DeviceEvent source = getDeviceEvent();
        final DeviceConfiguration configuration = DeviceConfiguration.builder() // Simple configuration
                .build();
        final Device originalDevice = Device.builder() // Device with a simple configuration
                .configuration(configuration)
                .firstSeen(new Date(1L))
                .build();
        final DeviceEvent processedEvent = DeviceEvent.builder() // Source request should be assigned date
                .userId(source.getUserId())
                .unitId(source.getUnitId())
                .versionId(source.getVersionId())
                .deviceIP(source.getDeviceIP())
                .receptionDate(receptionDate)
                .additionalProperties(source.getAdditionalProperties())
                .build();
        final DeviceEvent toSave = processedEvent.toBuilder() // Final version should have changed and operator correctly set
                .changed(true)
                .segmentId(segmentId)
                .build();
        final DeviceEvent saved = toSave.toBuilder() // Saved event will get and ID
                .id(UUID.randomUUID().toString())
                .build();

        doReturn(receptionDate).when(deviceManager).createReceptionDate();
        doReturn(originalDevice).when(deviceManager).getOrCreateDevice(source.getUserId(), source.getUnitId());
        doReturn(Optional.of(segmentId)).when(deviceManager).getExclusiveSegmentId(originalDevice, processedEvent);
        doReturn(true).when(deviceManager).hasChanged(source, segmentId, null);
        doReturn(saved).when(deviceEventRepository).save(toSave);

        // When
        final DeviceEvent result = deviceManager.saveDeviceEvent(source);

        // Then
        verify(deviceManager).createReceptionDate();
        verify(deviceManager).getOrCreateDevice(source.getUserId(), source.getUnitId());
        verify(deviceManager).getExclusiveSegmentId(originalDevice, processedEvent);
        verify(deviceManager).hasChanged(source, segmentId, null);
        verify(deviceEventRepository).save(toSave);
        assertThat(result).isEqualTo(saved);
    }

    @Test
    public void getExclusiveSegmentForDevice_whenMatches_shouldReturnIdForFirstMatchingSegment() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Device device = Device.builder()
                .userId(userId)
                .lastEvent(DeviceEvent.builder().id(UUID.randomUUID().toString()).build())
                .build();
        final DeviceEvent event = DeviceEvent.builder()
                .id(UUID.randomUUID().toString())
                .build();
        final JsonNode expectedDevice = new ObjectMapper().valueToTree(Device.builder()
                .userId(userId)
                .lastEvent(event)
                .build());

        final List<String> segmentIds = new LinkedList<>();
        final List<Segment> segments = new LinkedList<>();
        final Operator nonMatchingExpression = mock(Operator.class);
        doReturn(false).when(nonMatchingExpression).matches(expectedDevice);
        final Operator matchingExpression = mock(Operator.class);
        doReturn(true).when(matchingExpression).matches(expectedDevice);
        for (int i = 0; i < 10; i++) {
            final String segmentId = UUID.randomUUID().toString();
            final Segment segment = mock(Segment.class);
            when(segment.getId()).thenReturn(segmentId);
            when(segment.getQuery()).thenReturn(i < 3 ? nonMatchingExpression : matchingExpression);
            segmentIds.add(segmentId);
            segments.add(segment);
        }

        final String expectedUUID = segments.get(3).getId();
        final SegmentOrder segmentOrder = SegmentOrder.builder().segmentIds(segmentIds).build();
        doReturn(segmentOrder).when(segmentOrderRepository).findByUserId(userId);
        doReturn(segments).when(segmentRepository).getSegmentsInIds(userId, segmentIds);

        // When
        final Optional<String> result = deviceManager.getExclusiveSegmentId(device, event);

        // Then
        verify(segmentOrderRepository).findByUserId(userId);
        verify(segmentRepository).getSegmentsInIds(userId, segmentIds);
        verify(nonMatchingExpression, Mockito.times(3)).matches(expectedDevice);
        verify(matchingExpression, Mockito.times(1)).matches(expectedDevice);
        assertThat(result).contains(expectedUUID);
    }

    @Test
    public void getExclusiveSegmentForDevice_whenNotMatching_shouldCheckAllSegmentsAndReturnNull() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Device device = Device.builder()
                .userId(userId)
                .lastEvent(DeviceEvent.builder().id(UUID.randomUUID().toString()).build())
                .build();
        final DeviceEvent event = DeviceEvent.builder()
                .id(UUID.randomUUID().toString())
                .build();
        final JsonNode expectedDevice = new ObjectMapper().valueToTree(Device.builder()
                .userId(userId)
                .lastEvent(event)
                .build());

        final List<String> segmentIds = new LinkedList<>();
        final List<Segment> segments = new LinkedList<>();
        final Operator nonMatchingExpression = mock(Operator.class);
        doReturn(false).when(nonMatchingExpression).matches(expectedDevice);
        for (int i = 0; i < 10; i++) {
            final String segmentId = UUID.randomUUID().toString();
            final Segment segment = mock(Segment.class);
            when(segment.getId()).thenReturn(segmentId);
            when(segment.getQuery()).thenReturn(nonMatchingExpression);
            segmentIds.add(segmentId);
            segments.add(segment);
        }
        final SegmentOrder segmentOrder = SegmentOrder.builder().segmentIds(segmentIds).build();
        doReturn(segmentOrder).when(segmentOrderRepository).findByUserId(userId);
        doReturn(segments).when(segmentRepository).getSegmentsInIds(userId, segmentIds);

        // When
        final Optional<String> result = deviceManager.getExclusiveSegmentId(device, event);

        // Then
        verify(segmentOrderRepository).findByUserId(userId);
        verify(segmentRepository).getSegmentsInIds(userId, segmentIds);
        verify(nonMatchingExpression, Mockito.times(segments.size())).matches(expectedDevice);
        assertThat(result).isNotPresent();
    }

    @Test
    public void hasChanged_whenPreviousIsNull_shouldReturnTrue() {
        // Given

        // When
        final boolean result = deviceManager.hasChanged(null, null, null);

        // Then
        assertTrue(result);
    }

    @Test
    public void hasChanged_whenLatestExistsAndMatchesReceivedEvent_shouldReturnFalse() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final String deviceIP = UUID.randomUUID().toString();
        final Map<String, Object> properties = ImmutableMap.of("key", "value");
        final DeviceEvent latest = DeviceEvent.create(
                UUID.randomUUID().toString(), unitId, userId, versionId, new Date(123456789000L), properties, true, segmentId, deviceIP);
        final DeviceEvent source = DeviceEvent.fromJson(unitId, userId, versionId, properties, deviceIP);

        // When
        final boolean result = deviceManager.hasChanged(source, segmentId, latest);

        // Then
        assertFalse(result);
    }

    @Test
    public void hasChanged_whenLatestExistsHasDifferentVersionId_shouldReturnTrue() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final String deviceIP = UUID.randomUUID().toString();
        final Map<String, Object> properties = ImmutableMap.of("key", "value");
        final DeviceEvent latest = DeviceEvent.create(
                UUID.randomUUID().toString(), unitId, userId, UUID.randomUUID().toString(), new Date(123456789000L), properties, true, segmentId, deviceIP);
        final DeviceEvent source = DeviceEvent.fromJson(unitId, userId, versionId, properties, deviceIP);

        // When
        final boolean result = deviceManager.hasChanged(source, segmentId, latest);

        // Then
        assertTrue(result);
    }

    @Test
    public void hasChanged_whenLatestExistsHasDifferentProperties_shouldReturnTrue() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final String deviceIP = UUID.randomUUID().toString();
        final Map<String, Object> properties = ImmutableMap.of("key", "value");
        final DeviceEvent latest = DeviceEvent.create(
                UUID.randomUUID().toString(), unitId, userId, versionId, new Date(123456789000L), null, true, segmentId, deviceIP);
        final DeviceEvent source = DeviceEvent.fromJson(unitId, userId, versionId, properties, deviceIP);

        // When
        final boolean result = deviceManager.hasChanged(source, segmentId, latest);

        // Then
        assertTrue(result);
    }

    @Test
    public void hasChanged_whenLatestExistsHasDifferentSegmentId_shouldReturnTrue() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final String deviceIP = UUID.randomUUID().toString();
        final Map<String, Object> properties = ImmutableMap.of("key", "value");
        final DeviceEvent latest = DeviceEvent.create(
                UUID.randomUUID().toString(), unitId, userId, versionId, new Date(123456789000L), properties, true, UUID.randomUUID().toString(), deviceIP);
        final DeviceEvent source = DeviceEvent.fromJson(unitId, userId, versionId, properties, deviceIP);

        // When
        final boolean result = deviceManager.hasChanged(source, segmentId, latest);

        // Then
        assertTrue(result);
    }

    @Test
    public void getDevice_whenConfigurationAlreadyPresent_shouldReturnConfiguration() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration conf = DeviceConfiguration.builder().build();
        final Device device = Device.builder().userId(userId).unitId(unitId).configuration(conf).build();
        doReturn(Optional.of(device)).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);

        // When
        final Device result = deviceManager.getOrCreateDevice(userId, unitId);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        assertEquals(result, device);
    }

    @Test
    public void getDevice_whenConfigurationNotSet_shouldCreateConfigurationAndUpdateUnit() {
        // Given
        final Optional<Device> device = Optional.empty();
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Date creationDate = new Date(123456789000L);
        final DeviceConfiguration defaultConfiguration = DeviceConfiguration.builder()
                .userId(userId)
                .unitId(unitId)
                .creationDate(creationDate)
                .build();
        final Device expected = Device.builder().configuration(defaultConfiguration).build();
        doReturn(device).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        doReturn(creationDate).when(deviceManager).createReceptionDate();
        doReturn(defaultConfiguration).when(deviceConfigurationRepository).save(defaultConfiguration);
        doReturn(expected).when(deviceRepository).updateConfiguration(userId, unitId, defaultConfiguration);

        // When
        final Device result = deviceManager.getOrCreateDevice(userId, unitId);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        verify(deviceManager).createReceptionDate();
        verify(deviceConfigurationRepository).save(defaultConfiguration);
        verify(deviceRepository).updateConfiguration(userId, unitId, defaultConfiguration);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void saveUnitConfiguration_whenSucceeds_shouldSaveAndUpdateAndReturnTheCreatedConfiguration() {
        // Given
        final Date date = new Date(123456789000L);
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration requestConfiguration = DeviceConfiguration.builder()
                .build();
        final DeviceConfiguration toBeSavedConfiguration = DeviceConfiguration.builder()
                .unitId(unitId)
                .userId(userId)
                .creationDate(date)
                .build();
        final DeviceConfiguration savedConfiguration = DeviceConfiguration.builder()
                .unitId(unitId)
                .userId(userId)
                .id(UUID.randomUUID().toString())
                .build();
        doReturn(date).when(deviceManager).createReceptionDate();
        doReturn(savedConfiguration).when(deviceConfigurationRepository).save(toBeSavedConfiguration);

        // When
        DeviceConfiguration result = deviceManager.saveDeviceConfiguration(userId, unitId, requestConfiguration);

        // Then
        verify(deviceManager).createReceptionDate();
        verify(deviceConfigurationRepository).save(toBeSavedConfiguration);
        verify(deviceRepository).updateConfiguration(userId, unitId, savedConfiguration);
        assertThat(result).isEqualTo(savedConfiguration);
    }

    @Test
    public void getDeviceEventsByUnitId_shouldForwardCallToTheRepository() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page<DeviceEvent> page = new PageImpl<>(Collections.emptyList());
        final boolean changed = true;
        doReturn(page).when(deviceEventRepository).findByUserIdAndUnitId(userId, unitId, changed, pageable);

        // When
        final Page<DeviceEvent> result = deviceManager.getDeviceEventsByUnitId(userId, unitId, changed, pageable);

        // Then
        verify(deviceEventRepository).findByUserIdAndUnitId(userId, unitId, changed, pageable);
        assertThat(result).isEqualTo(page);
    }


    @Test
    public void getDevicesByUserId_shouldForwardCallToTheRepositoryWithEmptyQuery_whenNoQueryGiven() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page<Device> page = new PageImpl<>(Collections.emptyList());
        doReturn(page).when(deviceRepository).findByUserId(userId, Optional.empty(), pageable);

        // When
        final Page<Device> result = deviceManager.getDevicesByUserId(userId, Optional.empty(), pageable);

        // Then
        verify(deviceRepository).findByUserId(userId, Optional.empty(), pageable);
        assertThat(result).isEqualTo(page);
    }

    @Test
    public void getDevicesByUserId_shouldForwardCallToTheRepositoryWithQueryObject_whenValidQueryGiven() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final JSONObject json = JsonResourceLoader.getJsonFromResource(getClass(), "query");
        final Pageable pageable = new PageRequest(0, 10);
        final Page<Device> page = new PageImpl<>(Collections.emptyList());
        final Operator expected = new ObjectMapper().readValue(json.toJSONString(), Operator.class);
        doReturn(page).when(deviceRepository).findByUserId(userId, Optional.of(expected), pageable);

        // When
        final Page<Device> result = deviceManager.getDevicesByUserId(userId, Optional.of(expected), pageable);

        // Then
        verify(deviceRepository).findByUserId(userId, Optional.of(expected), pageable);
        assertThat(result).isEqualTo(page);
    }

    @Test
    public void getDeviceByUserIdAndUnitId_whenDeviceExists_shouldReturnUnit() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration configuration = DeviceConfiguration.builder()
                .build();
        final DeviceEvent event = DeviceEvent.builder()
                .userId(userId)
                .unitId(unitId)
                .versionId(UUID.randomUUID().toString())
                .receptionDate(new Date(1234567890L))
                .build();
        final Device device = Device.builder()
                .unitId(unitId)
                .userId(userId)
                .configuration(configuration)
                .lastEvent(event)
                .build();
        doReturn(Optional.of(device)).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);

        // When
        Device result = deviceManager.getDeviceByUserIdAndUnitId(userId, unitId);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        assertThat(result).isEqualTo(device);
    }

    @Test
    public void getDeviceByUserIdAndUnitId_whenUnitDoesNotExist_shouldThrowException() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        doReturn(Optional.empty()).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);

        // Then when
        assertThatExceptionOfType(DeviceNotFoundException.class)
                .isThrownBy(() -> deviceManager.getDeviceByUserIdAndUnitId(userId, unitId));
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
    }

}
