/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.core;

import org.eclipse.edc.connector.core.agent.ParticipantAgentServiceImpl;
import org.eclipse.edc.connector.core.command.CommandHandlerRegistryImpl;
import org.eclipse.edc.connector.core.event.EventExecutorServiceContainer;
import org.eclipse.edc.connector.core.event.EventRouterImpl;
import org.eclipse.edc.connector.core.message.RemoteMessageDispatcherRegistryImpl;
import org.eclipse.edc.connector.core.validator.DataAddressValidatorRegistryImpl;
import org.eclipse.edc.connector.core.validator.JsonObjectValidatorRegistryImpl;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.policy.engine.PolicyEngineImpl;
import org.eclipse.edc.policy.engine.RuleBindingRegistryImpl;
import org.eclipse.edc.policy.engine.ScopeFilter;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.runtime.metamodel.annotation.BaseExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static org.eclipse.edc.spi.agent.ParticipantAgentService.DEFAULT_IDENTITY_CLAIM_KEY;

@BaseExtension
@Extension(value = CoreServicesExtension.NAME)
public class CoreServicesExtension implements ServiceExtension {

    public static final String NAME = "Core Services";

    private static final String DEFAULT_EDC_HOSTNAME = "localhost";

    @Setting(value = "Connector hostname, which e.g. is used in referer urls", defaultValue = DEFAULT_EDC_HOSTNAME)
    public static final String EDC_HOSTNAME = "edc.hostname";
    @Setting(value = "The name of the claim key used to determine the participant identity", defaultValue = DEFAULT_IDENTITY_CLAIM_KEY)
    public static final String EDC_AGENT_IDENTITY_KEY = "edc.agent.identity.key";

    @Inject
    private EventExecutorServiceContainer eventExecutorServiceContainer;

    @Inject(required = false)
    private TypeManager typeManager;

    private RuleBindingRegistry ruleBindingRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        ruleBindingRegistry = new RuleBindingRegistryImpl();
    }

    @Override
    public void shutdown() {
        ServiceExtension.super.shutdown();
    }

    @Override
    public void prepare() {
        PolicyRegistrationTypes.TYPES.forEach(typeManager()::registerTypes);
    }

    @Provider
    public TypeManager typeManager() {
        if (typeManager == null) {
            typeManager = new JacksonTypeManager();
        }
        return typeManager;
    }

    @Provider
    public Hostname hostname(ServiceExtensionContext context) {
        var hostname = context.getSetting(EDC_HOSTNAME, DEFAULT_EDC_HOSTNAME);
        if (DEFAULT_EDC_HOSTNAME.equals(hostname)) {
            context.getMonitor().warning(String.format("Settings: No setting found for key '%s'. Using default value '%s'", EDC_HOSTNAME, DEFAULT_EDC_HOSTNAME));
        }
        return () -> hostname;
    }

    @Provider
    public RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry() {
        return new RemoteMessageDispatcherRegistryImpl();
    }

    @Provider
    public CommandHandlerRegistry commandHandlerRegistry() {
        return new CommandHandlerRegistryImpl();
    }

    @Provider
    public ParticipantAgentService participantAgentService(ServiceExtensionContext context) {
        var identityKey = context.getSetting(EDC_AGENT_IDENTITY_KEY, DEFAULT_IDENTITY_CLAIM_KEY);
        return new ParticipantAgentServiceImpl(identityKey);
    }

    @Provider
    public RuleBindingRegistry ruleBindingRegistry() {
        return ruleBindingRegistry;
    }

    @Provider
    public PolicyEngine policyEngine() {
        var scopeFilter = new ScopeFilter(ruleBindingRegistry);
        return new PolicyEngineImpl(scopeFilter);
    }

    @Provider
    public EventRouter eventRouter(ServiceExtensionContext context) {
        return new EventRouterImpl(context.getMonitor(), eventExecutorServiceContainer.getExecutorService());
    }

    @Provider
    public TypeTransformerRegistry typeTransformerRegistry() {
        return new TypeTransformerRegistryImpl();
    }

    @Provider
    public JsonObjectValidatorRegistry jsonObjectValidator() {
        return new JsonObjectValidatorRegistryImpl();
    }

    @Provider
    public DataAddressValidatorRegistry dataAddressValidatorRegistry(ServiceExtensionContext context) {
        return new DataAddressValidatorRegistryImpl(context.getMonitor());
    }

    @Provider
    public CriterionOperatorRegistry criterionOperatorRegistry() {
        return CriterionOperatorRegistryImpl.ofDefaults();
    }

}

