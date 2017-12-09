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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Builder(toBuilder = true)
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Document(collection = "units")
@CompoundIndexes({
        @CompoundIndex(name = "userId_unitId_idx", def = "{'userId' : 1, 'unitId' : 1}", unique = true),
        @CompoundIndex(name = "segmentId_idx", def = "{'event.segmentId' : 1}"),
        @CompoundIndex(name = "userId_idx", def = "{'userId' : 1}")
})
public class Device {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    @Id
    @JsonIgnore
    private final String id;
    private final String userId;
    private final String unitId;
    private final Date firstSeen;
    @Field(value = "event")
    private final DeviceEvent lastEvent;
    private final DeviceConfiguration configuration;

    @JsonGetter("firstSeen")
    @JsonFormat(pattern = DATE_FORMAT)
    public Date getFirstSeen() {
        return firstSeen == null ? null : new Date(firstSeen.getTime());
    }

    @JsonIgnore
    public boolean hasPinged() {
        return getFirstSeen() == null;
    }
}
