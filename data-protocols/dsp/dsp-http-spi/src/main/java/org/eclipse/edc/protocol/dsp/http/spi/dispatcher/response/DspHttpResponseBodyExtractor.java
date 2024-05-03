/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.spi.dispatcher.response;

import okhttp3.ResponseBody;

/**
 * Extract the body from a http response body into a concrete type
 *
 * @param <R> the type of the body.
 */
@FunctionalInterface
public interface DspHttpResponseBodyExtractor<R> {

    DspHttpResponseBodyExtractor<Object> NOOP = r -> null;

    /**
     * Extract the body from the Response
     *
     * @param responseBody the Response.
     * @return the body.
     */
    R extractBody(ResponseBody responseBody);
}
