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
import io.barracks.deviceservice.repository.document.DeviceEventDocument;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.hateoas.core.Relation;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Optional;

@Relation(collectionRelation = "events")
@CompoundIndexes({})
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@PersistenceConstructor))
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class DeviceEvent extends DeviceEventDocument {

    @Id
    @JsonIgnore
    private final String id;

    private final String userId;

    private final String unitId;

    @JsonIgnore
    private final boolean changed;

    @Valid
    @NotNull
    private final DeviceRequest request;

    @Valid
    @NotNull
    private final DeviceResponse response;

    @JsonIgnore
    @CreatedDate
    private final OffsetDateTime receptionDate;

    @JsonCreator
    public static DeviceEvent fromJson() {
        return builder().build();
    }

    @JsonProperty("receptionDate")
    public Optional<OffsetDateTime> getReceptionDate() {
        return Optional.ofNullable(receptionDate).map(date -> OffsetDateTime.ofInstant(date.toInstant(), date.getOffset()));
    }

    @JsonProperty("changed")
    public boolean isChanged() {
        return changed;
    }
}
