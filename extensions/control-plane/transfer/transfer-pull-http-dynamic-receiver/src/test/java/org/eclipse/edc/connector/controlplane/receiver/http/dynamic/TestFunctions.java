/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.receiver.http.dynamic;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.connector.controlplane.receiver.http.dynamic.HttpDynamicEndpointDataReferenceReceiver.HTTP_RECEIVER_ENDPOINT;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.HTTP_DATA_TYPE;

public class TestFunctions {

    public static TransferProcess createTransferProcess(String id, Map<String, Object> properties) {
        return TransferProcess.Builder.newInstance()
                .id(id)
                .state(TransferProcessStates.STARTED.code())
                .type(TransferProcess.Type.CONSUMER)
                .contentDataAddress(DataAddress.Builder.newInstance()
                        .type(HTTP_DATA_TYPE)
                        .property(BASE_URL, "http://localhost:8080/test")
                        .build())
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("HttpProxy")
                        .build())
                .privateProperties(properties)
                .build();
    }

    public static TransferProcess createTransferProcess(String id) {
        return createTransferProcess(id, new HashMap<>());
    }

    public static Map<String, Object> transferProperties(String url) {
        return new HashMap<>(Map.of(HTTP_RECEIVER_ENDPOINT, url));
    }

}
