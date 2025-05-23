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
package io.github.rose.mybatis.functional;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since 0.0.1
 */
@SuppressWarnings("unchecked")
public abstract class EntityOperations {

    public static <T> EntityUpdater<T> doUpdate(BaseMapper<T> baseMapper) {
        return new EntityUpdater<>(baseMapper);
    }

    public static <T> EntityCreator<T> doCreate(BaseMapper<T> baseMapper) {
        return new EntityCreator(baseMapper);
    }
}
