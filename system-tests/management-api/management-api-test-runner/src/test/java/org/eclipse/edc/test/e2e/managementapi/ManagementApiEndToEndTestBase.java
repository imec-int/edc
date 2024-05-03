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

package org.eclipse.edc.test.e2e.managementapi;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.query.Criterion;

import java.util.Arrays;
import java.util.Collection;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public abstract class ManagementApiEndToEndTestBase {

    public static final int PORT = getFreePort();
    public static final int PROTOCOL_PORT = getFreePort();

    protected final EdcRuntimeExtension runtime;

    public ManagementApiEndToEndTestBase(EdcRuntimeExtension runtime) {
        this.runtime = runtime;
    }

    protected RequestSpecification baseRequest() {
        return given()
                .port(PORT)
                .baseUri("http://localhost:%s/management".formatted(PORT))
                .when();
    }

    protected JsonObject query(Criterion... criteria) {
        var criteriaJson = Arrays.stream(criteria)
                .map(it -> {
                    JsonValue operandRight;
                    if (it.getOperandRight() instanceof Collection<?> collection) {
                        operandRight = Json.createArrayBuilder(collection).build();
                    } else {
                        operandRight = Json.createValue(it.getOperandRight().toString());
                    }
                    return createObjectBuilder()
                                    .add("operandLeft", it.getOperandLeft().toString())
                                    .add("operator", it.getOperator())
                                    .add("operandRight", operandRight)
                                    .build();
                        }
                ).collect(toJsonArray());

        return createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add(TYPE, "QuerySpec")
                .add("filterExpression", criteriaJson)
                .build();
    }
}
