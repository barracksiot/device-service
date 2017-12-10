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

package io.barracks.deviceservice.model.json;

import io.barracks.deviceservice.model.Package;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import static io.barracks.deviceservice.utils.PackageUtils.getPackage;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class PackageJsonTest {

    @Autowired
    private JacksonTester<Package> json;

    @Value("classpath:io/barracks/deviceservice/package.json")
    private Resource packageRef;

    @Test
    public void serializeShouldFillReferenceAndVersion() throws Exception {
        // Given
        final Package source = getPackage();

        // When
        final JsonContent<Package> result = json.write(source);

        // Then
        assertThat(result).extractingJsonPathStringValue("reference").isEqualTo(source.getReference());
        assertThat(result).extractingJsonPathStringValue("version").isEqualTo(source.getVersion());
    }

    @Test
    public void deserializeShouldFillReferenceAndVersion() throws Exception {
        // Given
        final Package expected = Package.builder()
                .reference("io.barracks.reference")
                .version("0.0.1")
                .build();

        // When
        final ObjectContent<Package> result = json.read(packageRef);

        // Then
        assertThat(result).isEqualTo(expected);
    }
}
