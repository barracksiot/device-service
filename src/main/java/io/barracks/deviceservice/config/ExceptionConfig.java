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

package io.barracks.deviceservice.config;

import cz.jirutka.spring.exhandler.RestHandlerExceptionResolverBuilder;
import io.barracks.commons.configuration.ExceptionHandlingConfiguration;
import io.barracks.deviceservice.manager.exception.FilterNotFoundException;
import io.barracks.deviceservice.manager.exception.MatchingFilterNotFoundException;
import io.barracks.deviceservice.manager.exception.NoMatchingFilterFoundException;
import io.barracks.deviceservice.repository.exception.FilterCreationFailedException;
import io.barracks.deviceservice.rest.exception.BarracksQueryFormatException;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

@Configuration
public class ExceptionConfig extends ExceptionHandlingConfiguration {
    @Override
    public RestHandlerExceptionResolverBuilder restExceptionResolver() {
        return super.restExceptionResolver()
                .addErrorMessageHandler(BarracksQueryFormatException.class, HttpStatus.BAD_REQUEST)
                .addErrorMessageHandler(OperatorConversionException.class, HttpStatus.INTERNAL_SERVER_ERROR)
                .addErrorMessageHandler(DuplicateKeyException.class, HttpStatus.BAD_REQUEST)
                .addErrorMessageHandler(FilterCreationFailedException.class, HttpStatus.BAD_REQUEST)
                .addErrorMessageHandler(FilterNotFoundException.class, HttpStatus.NOT_FOUND)
                .addErrorMessageHandler(MatchingFilterNotFoundException.class, HttpStatus.INTERNAL_SERVER_ERROR)
                .addErrorMessageHandler(NoMatchingFilterFoundException.class, HttpStatus.NOT_FOUND);
    }

    @Override
    protected String getBaseName() {
        return "classpath:/io/barracks/deviceservice/configuration/messages";
    }
}
