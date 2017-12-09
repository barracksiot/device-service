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
import io.barracks.deviceservice.model.operator.Operator;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Date;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@Document(collection = "segments")
@CompoundIndex(name = "userId_name_idx", def = "{'userId' : 1, 'name' : 1}", unique = true)
public class Segment {
    @Id
    @JsonIgnore
    private final String id;
    @NotBlank
    private final String userId;
    @NotNull
    @Length(max = 50, min = 1)
    @Pattern(regexp = "(?!^[Oo]ther$)\\w+")
    private final String name;
    @NotNull
    @Valid
    private final Operator query;
    @JsonIgnore
    @LastModifiedDate
    private Date updated;

    @JsonCreator
    public static Segment fromJson(
            @JsonProperty("userId") String userId,
            @JsonProperty("name") String name,
            @JsonProperty("query") Operator query) {
        return Segment.builder()
                .userId(userId)
                .name(name)
                .query(query)
                .build();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonGetter("id")
    public String getId() {
        return id;
    }
}