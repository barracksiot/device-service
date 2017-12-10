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

package io.barracks.deviceservice.model.operator.comparison;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyValidationTest {
    private static final String[] shouldPass = {
            "unitId",
            "firstSeen",
            "lastSeen",
            "customClientData",
            "customClientData.toto"
    };
    private static final String[] shouldFail = {
            "id",
            "userId",
            "event",
            "configuration",
            "lastEvent.id",
            "lastEvent.userId",
            "lastEvent.unitId",
            "lastEvent.channelId",
            "customClientData.",
            "customClientDataaaaaaaa",
            "lastEvent.customClientData",
            "configuration.id",
            "configuration.userId",
            "configuration.unitId",
            "configuration.channelId",
            "configuration.creationDate",
            "super"
    };

    @Test
    public void verifyKeyValidationPattern() {
        Logger logger = LoggerFactory.getLogger(KeyValidationTest.class);
        Pattern pattern = Pattern.compile(ComparisonOperator.KEY_PATTERN);
        for (String match : shouldPass) {
            logger.debug("Matching {} against pattern", match);
            assertThat(pattern.matcher(match).matches()).isTrue();
        }
        for (String match : shouldFail) {
            logger.debug("Not matching {} against pattern", match);
            assertThat(pattern.matcher(match).matches()).isFalse();
        }
    }
}
