/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 *    Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 */


plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":extensions:control-plane:api:management-api:asset-api"))
    implementation("io.acryl:datahub-client:0.10.5-5")
    implementation("org.apache.httpcomponents:httpclient:4.5")
    implementation("org.apache.httpcomponents:httpasyncclient:4.1.5")

}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}


