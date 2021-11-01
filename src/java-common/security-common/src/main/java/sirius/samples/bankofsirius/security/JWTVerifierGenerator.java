/*
 * Copyright 2020, Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sirius.samples.bankofsirius.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JWTVerifierGenerator {
    private static final Logger LOGGER =
        LoggerFactory.getLogger(JWTVerifierGenerator.class);

    @Bean(name = "verifier")
    @ConditionalOnProperty(value = "jwt.account.authentication.enabled",
            matchIfMissing = true, havingValue = "true")
    public JWTVerifier generateJWTVerifier(
            @Value("${PUB_KEY_PATH}") final String publicKeyPath) throws IOException {
        // load public key from file
        final Path publicKeyFile = Paths.get(publicKeyPath);
        final String keyContents = new String(Files.readAllBytes(publicKeyFile));

        try {
            final String keyStr = keyContents.replaceFirst("-----BEGIN PUBLIC KEY-----", "")
                    .replaceFirst("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            final byte[] keyBytes = Base64.getDecoder().decode(keyStr);
            final KeyFactory kf = KeyFactory.getInstance("RSA");
            final X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(keyBytes);
            final RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(keySpecX509);

            // Initialize JWT verifier.
            final Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            final JWTVerifier verifier = JWT.require(algorithm).build();
            LOGGER.debug("Generated JWT token verifier [algorithm={},publicKeyPath={}]",
                    algorithm.getName(), publicKeyFile);
            return verifier;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            final String msg = String.format("Cannot generate JWT key [path=%s]",
                    publicKeyFile);
            throw new GenerateKeyException(msg, e);
        }
    }
}
