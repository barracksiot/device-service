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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Optional;

@Builder(toBuilder = true)
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@PersistenceConstructor))

@Document(collection = "devices_v2")
@CompoundIndexes({
        @CompoundIndex(name = "userId_unitId_idx", def = "{ 'userId' : 1, 'unitId' : 1 }", unique = true),
        @CompoundIndex(name = "userId_idx", def = "{ 'userId' : 1 }")
})

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {

    @Id
    @JsonIgnore
    private final String id;

    private final String userId;

    private final String unitId;

    @JsonIgnore
    @CreatedDate
    private final OffsetDateTime firstSeen;

    @Valid
    private final DeviceEvent lastEvent;

    @JsonProperty("firstSeen")
    public Optional<OffsetDateTime> getFirstSeen() {
        return Optional.ofNullable(firstSeen).map(firstSeen -> OffsetDateTime.ofInstant(firstSeen.toInstant(), firstSeen.getOffset()));
    }

    public Optional<DeviceEvent> getLastEvent() {
        return Optional.ofNullable(lastEvent).map(lastEvent -> lastEvent.toBuilder().build());
    }

    @JsonProperty("lastEvent")
    private DeviceEvent _getLastEvent() {
        return getLastEvent().orElse(null);
    }
}
