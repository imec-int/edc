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

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.test.e2e.managementapi.Runtimes.inMemoryRuntime;
import static org.eclipse.edc.test.e2e.managementapi.Runtimes.postgresRuntime;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

public class CatalogApiEndToEndTest {

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        public static final EdcRuntimeExtension RUNTIME = inMemoryRuntime();

        InMemory() {
            super(RUNTIME);
        }

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASE = context -> createDatabase("runtime");

        @RegisterExtension
        public static final EdcRuntimeExtension RUNTIME = postgresRuntime();

        Postgres() {
            super(RUNTIME);
        }
    }

    abstract static class Tests extends ManagementApiEndToEndTestBase {
        // requests the catalog to itself, to save another connector.
        private final String providerUrl = "http://localhost:" + PROTOCOL_PORT + "/protocol";

        Tests(EdcRuntimeExtension runtime) {
            super(runtime);
        }

        @Test
        void requestCatalog_shouldReturnCatalog_withoutQuerySpec() {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", providerUrl)
                    .add("protocol", "dataspace-protocol-http")
                    .build();

            baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v2/catalog/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("dcat:Catalog"));
        }

        @Test
        void requestCatalog_shouldReturnCatalog_withQuerySpec() {
            var assetIndex = runtime.getContext().getService(AssetIndex.class);
            var policyDefinitionStore = runtime.getContext().getService(PolicyDefinitionStore.class);
            var contractDefinitionStore = runtime.getContext().getService(ContractDefinitionStore.class);

            var policyId = UUID.randomUUID().toString();

            var cd = ContractDefinition.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .contractPolicyId(policyId)
                    .accessPolicyId(policyId)
                    .build();

            var policy = Policy.Builder.newInstance()
                    .build();

            policyDefinitionStore.create(PolicyDefinition.Builder.newInstance().id(policyId).policy(policy).build());
            contractDefinitionStore.save(cd);

            assetIndex.create(createAsset("id-1", "test-type").build());
            assetIndex.create(createAsset("id-2", "test-type").build());

            var criteria = createArrayBuilder()
                    .add(createObjectBuilder()
                            .add(TYPE, "Criterion")
                            .add("operandLeft", EDC_NAMESPACE + "id")
                            .add("operator", "=")
                            .add("operandRight", "id-2")
                            .build()
                    )
                    .build();

            var querySpec = createObjectBuilder()
                    .add(TYPE, "QuerySpec")
                    .add("filterExpression", criteria)
                    .add("limit", 1);

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "CatalogRequest")
                    .add("counterPartyAddress", providerUrl)
                    .add("protocol", "dataspace-protocol-http")
                    .add("querySpec", querySpec)
                    .build();

            baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v2/catalog/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("dcat:Catalog"))
                    .body("'dcat:dataset'.id", is("id-2"));
        }

        @Test
        void getDataset_shouldReturnDataset() {
            var dataPlaneInstance = DataPlaneInstance.Builder.newInstance().url("http://localhost/any")
                    .allowedDestType("any").allowedSourceType("test-type").allowedTransferType("any").build();
            runtime.getContext().getService(DataPlaneInstanceStore.class).create(dataPlaneInstance);

            var assetIndex = runtime.getContext().getService(AssetIndex.class);
            assetIndex.create(createAsset("asset-id", "test-type").build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "DatasetRequest")
                    .add(ID, "asset-id")
                    .add("counterPartyAddress", providerUrl)
                    .add("protocol", "dataspace-protocol-http")
                    .build();

            baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v2/catalog/dataset/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("asset-id"))
                    .body(TYPE, is("dcat:Dataset"))
                    .body("'dcat:distribution'.'dcat:accessService'.@id", notNullValue());
        }

        private Asset.Builder createAsset(String id, String sourceType) {
            return Asset.Builder.newInstance()
                    .dataAddress(DataAddress.Builder.newInstance().type(sourceType).build())
                    .id(id);
        }

    }

}
