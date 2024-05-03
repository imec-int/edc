/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials.spi.validation;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;

import java.util.Collection;

/**
 * A list of trusted VC issuers
 */
public interface TrustedIssuerRegistry {

    void addIssuer(Issuer issuer);

    Issuer getById(String id);

    Collection<Issuer> getTrustedIssuers();
}

