/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.dataplane.flow;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.dataplane.proxy.ConsumerPullDataPlaneProxyResolver;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.response.StatusResult.failure;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;

@Deprecated(since = "0.5.1")
public class ConsumerPullTransferDataFlowController implements DataFlowController {

    private final DataPlaneSelectorService selectorService;
    private final ConsumerPullDataPlaneProxyResolver resolver;

    private final Set<String> transferTypes = Set.of("%s-%s".formatted("HttpData", PULL));

    public ConsumerPullTransferDataFlowController(DataPlaneSelectorService selectorService, ConsumerPullDataPlaneProxyResolver resolver) {
        this.selectorService = selectorService;
        this.resolver = resolver;
    }

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        // Backward compatibility: can handle if destination type is `HttpProxy` or the transfer type is `Http-PULL`
        return HTTP_PROXY.equals(transferProcess.getDestinationType()) ||
                (Optional.ofNullable(transferProcess.getTransferType()).map(transferTypes::contains).orElse(false));
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy) {
        var contentAddress = transferProcess.getContentDataAddress();

        return Optional.ofNullable(selectorService.select(contentAddress, destinationAddress(transferProcess)))
                .map(instance -> resolver.toDataAddress(transferProcess, contentAddress, instance)
                        .map(this::toResponse)
                        .map(StatusResult::success)
                        .orElse(failure -> failure(FATAL_ERROR, "Failed to generate proxy: " + failure.getFailureDetail())))
                .orElse(failure(FATAL_ERROR, format("Failed to find DataPlaneInstance for source/destination: %s/%s", contentAddress.getType(), HTTP_PROXY)));
    }

    @Override
    public StatusResult<Void> suspend(TransferProcess transferProcess) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        return StatusResult.success();
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        return transferTypes;
    }

    // Shim translation from "Http-PULL" to HttpProxy dataAddress
    private DataAddress destinationAddress(TransferProcess transferProcess) {
        if (transferTypes.contains(transferProcess.getDestinationType())) {
            var dadBuilder = DataAddress.Builder.newInstance();
            transferProcess.getDataDestination().getProperties().forEach(dadBuilder::property);
            return dadBuilder.type(HTTP_PROXY).build();
        } else {
            return transferProcess.getDataDestination();
        }
    }

    private DataFlowResponse toResponse(DataAddress address) {
        return DataFlowResponse.Builder.newInstance().dataAddress(address).build();
    }
}
