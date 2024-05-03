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

package org.eclipse.edc.connector.dataplane.store.sql.schema.postgres;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.store.sql.schema.DataPlaneStatements;
import org.eclipse.edc.sql.lease.StatefulEntityMapping;

/**
 * Maps fields of a {@link DataFlow} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class DataPlaneMapping extends StatefulEntityMapping {

    public DataPlaneMapping(DataPlaneStatements statements) {
        super(statements);
    }
}
