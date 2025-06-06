/*
 * Copyright © 2025 rosestack.github.io
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
package io.github.rose.mybatis.encrypt;

import io.github.rose.mybatis.encrypt.util.Algorithm;
import io.github.rose.mybatis.encrypt.util.AlgorithmUtils;

/**
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since 0.0.1
 */
public class DefaultEncryptor implements IEncryptor {

    public String encrypt(Algorithm algorithm, String password, String plaintext, Object metaObject) {
        return AlgorithmUtils.resolve(algorithm, password, true, plaintext);
    }

    public String decrypt(Algorithm algorithm, String password, String plaintext, Object metaObject) {
        return AlgorithmUtils.resolve(algorithm, password, false, plaintext);
    }
}
