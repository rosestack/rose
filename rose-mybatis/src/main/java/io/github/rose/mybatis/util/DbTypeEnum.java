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
package io.github.rose.mybatis.util;

import com.baomidou.mybatisplus.annotation.DbType;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * 针对 MyBatis Plus 的 {@link DbType} 增强，补充更多信息
 */
public enum DbTypeEnum {

    /**
     * H2
     * <p>
     * 注意：H2 不支持 find_in_set 函数
     */
    H2(DbType.H2, "H2", ""),

    /**
     * MySQL
     */
    MY_SQL(DbType.MYSQL, "MySQL", "FIND_IN_SET('#{value}', #{column}) <> 0"),

    /**
     * Oracle
     */
    ORACLE(DbType.ORACLE, "Oracle", "FIND_IN_SET('#{value}', #{column}) <> 0"),

    /**
     * PostgreSQL
     * <p>
     * 华为 openGauss 使用 ProductName 与 PostgreSQL 相同
     */
    POSTGRE_SQL(DbType.POSTGRE_SQL, "PostgreSQL", "POSITION('#{value}' IN #{column}) <> 0"),

    /**
     * SQL Server
     */
    SQL_SERVER(
            DbType.SQL_SERVER, "Microsoft SQL Server", "CHARINDEX(',' + #{value} + ',', ',' + #{column} + ',') <> 0"),
    /**
     * SQL Server 2005
     */
    SQL_SERVER2005(
            DbType.SQL_SERVER2005,
            "Microsoft SQL Server 2005",
            "CHARINDEX(',' + #{value} + ',', ',' + #{column} + ',') <> 0"),

    /**
     * 达梦
     */
    DM(DbType.DM, "DM DBMS", "FIND_IN_SET('#{value}', #{column}) <> 0"),

    /**
     * 人大金仓
     */
    KINGBASE_ES(DbType.KINGBASE_ES, "KingbaseES", "POSITION('#{value}' IN #{column}) <> 0"),
    ;

    public static final Map<String, DbTypeEnum> MAP_BY_NAME =
            Arrays.stream(values()).collect(Collectors.toMap(DbTypeEnum::getProductName, Function.identity()));

    public static final Map<DbType, DbTypeEnum> MAP_BY_MP =
            Arrays.stream(values()).collect(Collectors.toMap(DbTypeEnum::getMpDbType, Function.identity()));

    /**
     * MyBatis Plus 类型
     */
    private final DbType mpDbType;

    /**
     * 数据库产品名
     */
    private final String productName;

    /**
     * SQL FIND_IN_SET 模板
     */
    private final String findInSetTemplate;

    DbTypeEnum(DbType mpDbType, String productName, String findInSetTemplate) {
        this.mpDbType = mpDbType;
        this.productName = productName;
        this.findInSetTemplate = findInSetTemplate;
    }

    public static DbType find(String databaseProductName) {
        if (StringUtils.isBlank(databaseProductName)) {
            return null;
        }
        return MAP_BY_NAME.get(databaseProductName).getMpDbType();
    }

    public static String getFindInSetTemplate(DbType dbType) {
        return Optional.of(MAP_BY_MP.get(dbType).getFindInSetTemplate())
                .orElseThrow(() -> new IllegalArgumentException("FIND_IN_SET not supported"));
    }

    public DbType getMpDbType() {
        return mpDbType;
    }

    public String getProductName() {
        return productName;
    }

    public String getFindInSetTemplate() {
        return findInSetTemplate;
    }
}
