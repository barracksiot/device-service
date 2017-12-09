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

package io.barracks.deviceservice.model;

import com.fasterxml.jackson.annotation.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.hateoas.core.Relation;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Builder(toBuilder = true)
@ToString
@Getter
@EqualsAndHashCode(callSuper = false)
@SuppressFBWarnings
@CompoundIndexes({})
@JsonIgnoreProperties(ignoreUnknown = true)
@Relation(collectionRelation = "events")
public class DeviceEvent extends DeviceEventDocument {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    @Id
    @JsonIgnore
    private final String id;

    @NotBlank
    private final String unitId;

    @NotBlank
    private final String userId;

    @NotBlank
    private final String versionId;

    @JsonIgnore
    private final Date receptionDate;

    @NotNull
    private final Map<String, Object> additionalProperties;

    @JsonIgnore
    private final boolean changed;

    @JsonIgnore
    private final String segmentId;

    private final String deviceIP;

    @PersistenceConstructor
    private DeviceEvent(String id, String unitId, String userId, String versionId, Date receptionDate, Map<String, Object> additionalProperties, Boolean changed, String segmentId, String deviceIP) {
        this.id = id;
        this.unitId = unitId;
        this.userId = userId;
        this.versionId = versionId;
        this.receptionDate = receptionDate;
        this.additionalProperties = additionalProperties == null ? Collections.emptyMap() : new HashMap<>(additionalProperties);
        this.changed = changed != null && changed;
        this.segmentId = segmentId;
        this.deviceIP = deviceIP;
    }

    public static DeviceEvent create(String id, String unitId, String userId, String versionId, Date receptionDate, Map<String, Object> additionalProperties, Boolean changed, String segmentId, String deviceIP) {
        return new DeviceEvent(id, unitId, userId, versionId, receptionDate, additionalProperties, changed, segmentId, deviceIP);
    }

    @JsonCreator
    public static DeviceEvent fromJson(
            @JsonProperty("unitId") String unitId,
            @JsonProperty("userId") String userId,
            @JsonProperty("versionId") String versionId,
            @JsonProperty("additionalProperties") Map<String, Object> additionalProperties,
            @JsonProperty("deviceIP") String deviceIP){
        return new DeviceEvent(null, unitId, userId, versionId, null, additionalProperties, Boolean.FALSE, null, deviceIP);
    }

    @JsonGetter("id")
    public String getId() {
        return id;
    }

    @JsonGetter("receptionDate")
    @JsonFormat(pattern = DATE_FORMAT)
    public Date getReceptionDate() {
        return receptionDate == null ? null : new Date(receptionDate.getTime());
    }

    @JsonGetter("changed")
    public boolean isChanged() {
        return changed;
    }

    @JsonGetter("segmentId")
    public String getSegmentId() {
        return segmentId;
    }

    public Map<String, Object> getAdditionalProperties() {
        return new HashMap<>(additionalProperties);
    }
}