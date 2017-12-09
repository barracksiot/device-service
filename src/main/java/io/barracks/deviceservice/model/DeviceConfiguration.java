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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.util.Date;

@Builder
@SuppressFBWarnings
@Getter
@ToString
@EqualsAndHashCode
@CompoundIndexes({})
public class DeviceConfiguration extends DeviceConfigurationDocument {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    @Id
    @JsonIgnore
    private final String id;
    @JsonIgnore
    private final String userId;
    @JsonIgnore
    private final String unitId;
    @JsonIgnore
    private final Date creationDate;

    @PersistenceConstructor
    public DeviceConfiguration(String id, String userId, String unitId, Date creationDate) {
        this.id = id;
        this.userId = userId;
        this.unitId = unitId;
        this.creationDate = creationDate;
    }

    @JsonCreator
    public static DeviceConfiguration fromJson(
    ) {
        return DeviceConfiguration.builder()
                .build();
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    @JsonProperty
    public String getUserId() {
        return userId;
    }

    @JsonProperty
    public String getUnitId() {
        return unitId;
    }

    @JsonProperty
    @JsonFormat(pattern = DATE_FORMAT)
    public Date getCreationDate() {
        return creationDate == null ? null : new Date(creationDate.getTime());
    }
}
