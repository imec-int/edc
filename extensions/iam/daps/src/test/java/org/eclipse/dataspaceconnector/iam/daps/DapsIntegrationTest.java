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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.daps;

import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.security.fs.FsCertificateResolver;
import org.eclipse.dataspaceconnector.security.fs.FsPrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultResponse;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@ExtendWith(EdcExtension.class)
class DapsIntegrationTest {

    private static final String AUDIENCE_IDS_CONNECTORS_ALL = "idsc:IDS_CONNECTORS_ALL";
    private static final String CLIENT_ID = "8B:DE:EE:4C:7C:2D:7B:93:51:2F:FD:A0:D3:53:4C:58:CA:9D:70:C2:keyid:CB:8C:C7:B6:85:79:A8:23:A6:CB:15:AB:17:50:2F:E6:65:43:5D:E8";
    private static final String CLIENT_KEYSTORE_KEY_ALIAS = "1";
    private static final String CLIENT_KEYSTORE_PASSWORD = "password";
    private static final String DAPS_URL = "http://localhost:4567";

    private final Map<String, String> configuration = Map.of(
            "edc.oauth.token.url", DAPS_URL + "/token",
            "edc.oauth.client.id", CLIENT_ID,
            "edc.oauth.provider.audience", AUDIENCE_IDS_CONNECTORS_ALL,
            "edc.oauth.provider.jwks.url", DAPS_URL + "/.well-known/jwks.json",
            "edc.oauth.public.key.alias", CLIENT_KEYSTORE_KEY_ALIAS,
            "edc.oauth.private.key.alias", CLIENT_KEYSTORE_KEY_ALIAS
    );

    @BeforeEach
    protected void before(EdcExtension extension) {
        KeyStore clientKeystore = readKeystoreFromResources("plugfest-keystore.p12", "PKCS12", CLIENT_KEYSTORE_PASSWORD);

        extension.registerSystemExtension(ConfigurationExtension.class, (ConfigurationExtension) configuration::get);
        extension.registerServiceMock(Vault.class, new MockVault());
        extension.registerServiceMock(PrivateKeyResolver.class, new FsPrivateKeyResolver(CLIENT_KEYSTORE_PASSWORD, clientKeystore));
        extension.registerServiceMock(CertificateResolver.class, new FsCertificateResolver(clientKeystore));
    }

    @Test
    void retrieveTokenAndValidate(IdentityService identityService) {
        TokenResult tokenResult = identityService.obtainClientCredentials("idsc:IDS_CONNECTOR_ATTRIBUTES_ALL");

        assertThat(tokenResult.success()).isTrue();

        VerificationResult verificationResult = identityService.verifyJwtToken(tokenResult.getToken(), AUDIENCE_IDS_CONNECTORS_ALL);

        assertThat(verificationResult.valid()).isTrue();
    }

    private static KeyStore readKeystoreFromResources(String fileName, String type, String password) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        Objects.requireNonNull(url);

        try {
            KeyStore ks = KeyStore.getInstance(type);
            FileInputStream fis = new FileInputStream(url.getFile());
            ks.load(fis, password.toCharArray());
            return ks;
        } catch (Exception e) {
            throw new EdcException("Failed to load keystore: " + e);
        }
    }

    protected static class MockVault implements Vault {
        private final Map<String, String> secrets = new ConcurrentHashMap<>();

        @Override
        public @Nullable
        String resolveSecret(String key) {
            return secrets.get(key);
        }

        @Override
        public VaultResponse storeSecret(String key, String value) {
            secrets.put(key, value);
            return VaultResponse.OK;
        }

        @Override
        public VaultResponse deleteSecret(String key) {
            secrets.remove(key);
            return VaultResponse.OK;
        }
    }
}
