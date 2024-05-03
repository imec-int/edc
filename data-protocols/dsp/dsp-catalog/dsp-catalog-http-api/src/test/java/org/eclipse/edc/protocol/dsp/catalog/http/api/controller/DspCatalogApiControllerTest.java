/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.http.api.controller;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.ResponseDecorator;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.DATASET_REQUEST;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class DspCatalogApiControllerTest extends RestControllerTestBase {

    private final TypeTransformerRegistry transformerRegistry = mock();
    private final CatalogProtocolService service = mock();
    private final DspRequestHandler dspRequestHandler = mock();
    private final ContinuationTokenManager continuationTokenManager = mock();

    @Nested
    class RequestCatalog {

        @Test
        void shouldCreateResource() {
            var requestBody = createObjectBuilder().add(TYPE, DSPACE_TYPE_CATALOG_REQUEST_MESSAGE).build();
            var catalog = createObjectBuilder().add(JsonLdKeywords.TYPE, "catalog").build();
            when(transformerRegistry.transform(any(Catalog.class), eq(JsonObject.class))).thenReturn(Result.success(catalog));
            when(dspRequestHandler.createResource(any(), any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());
            when(continuationTokenManager.createResponseDecorator(any())).thenReturn(mock());

            baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post(CATALOG_REQUEST)
                    .then()
                    .statusCode(200)
                    .contentType(JSON);

            var captor = ArgumentCaptor.forClass(PostDspRequest.class);
            verify(dspRequestHandler).createResource(captor.capture(), isA(ResponseDecorator.class));
            var request = captor.getValue();
            assertThat(request.getInputClass()).isEqualTo(CatalogRequestMessage.class);
            assertThat(request.getResultClass()).isEqualTo(Catalog.class);
            assertThat(request.getExpectedMessageType()).isEqualTo(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE);
            assertThat(request.getProcessId()).isNull();
            assertThat(request.getToken()).isEqualTo("auth");
            assertThat(request.getMessage()).isEqualTo(requestBody);
            verify(continuationTokenManager).createResponseDecorator("http://localhost:%d/catalog/request".formatted(port));
        }

        @Test
        void shouldApplyContinuationToken_whenPassed() {
            var requestBody = createObjectBuilder().add(TYPE, DSPACE_TYPE_CATALOG_REQUEST_MESSAGE).build();
            var catalog = createObjectBuilder().add(JsonLdKeywords.TYPE, "catalog").build();
            when(transformerRegistry.transform(any(Catalog.class), eq(JsonObject.class))).thenReturn(Result.success(catalog));
            when(dspRequestHandler.createResource(any(), any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());
            when(continuationTokenManager.createResponseDecorator(any())).thenReturn(mock());
            var enrichedRequestBody = createObjectBuilder(requestBody).add("query", Json.createObjectBuilder()).build();
            when(continuationTokenManager.applyQueryFromToken(any(), any())).thenReturn(Result.success(enrichedRequestBody));

            baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post(CATALOG_REQUEST + "?continuationToken=pagination-token")
                    .then()
                    .statusCode(200)
                    .contentType(JSON);

            var captor = ArgumentCaptor.forClass(PostDspRequest.class);
            verify(dspRequestHandler).createResource(captor.capture(), isA(ResponseDecorator.class));
            var request = captor.getValue();
            assertThat(request.getMessage()).isSameAs(enrichedRequestBody);
            verify(continuationTokenManager).applyQueryFromToken(requestBody, "pagination-token");
        }

        @Test
        void shouldReturnBadRequest_whenContinuationTokenIsNotValid() {
            var requestBody = createObjectBuilder().add(TYPE, DSPACE_TYPE_CATALOG_REQUEST_MESSAGE).build();
            when(continuationTokenManager.applyQueryFromToken(any(), any())).thenReturn(Result.failure("error"));

            baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post(CATALOG_REQUEST + "?continuationToken=pagination-token")
                    .then()
                    .statusCode(400);

            verifyNoInteractions(dspRequestHandler, transformerRegistry);
        }
    }

    @Test
    void getDataset_shouldGetResource() {
        when(dspRequestHandler.getResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON).build());

        baseRequest()
                .get(DATASET_REQUEST + "/datasetId")
                .then()
                .statusCode(200)
                .contentType(JSON);

        var captor = ArgumentCaptor.forClass(GetDspRequest.class);
        verify(dspRequestHandler).getResource(captor.capture());
        var request = captor.getValue();
        assertThat(request.getToken()).isEqualTo("auth");
        assertThat(request.getResultClass()).isEqualTo(Dataset.class);
        assertThat(request.getId()).isEqualTo("datasetId");
        assertThat(request.getErrorType()).isNotNull();
    }

    @Override
    protected Object controller() {
        return new DspCatalogApiController(service, dspRequestHandler, continuationTokenManager);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, "auth")
                .when();
    }

}
