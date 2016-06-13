package com.cdphantom.suzaku.dao.impl;

import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Clob;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cdphantom.suzaku.dao.QuerySupport;
import com.cdphantom.suzaku.util.ClassHelper;
import com.cdphantom.suzaku.util.DateUtil;

public abstract class GenerateQL implements QuerySupport {
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(GenerateQL.class);
    
    protected static final Map<String, String> OPERAS = new HashMap<String, String>(12);

    private static final Map<String, String> ORACLE_DATE_FORMATS = new HashMap<String, String>(9);
    
    private static final Map<String, String> MYSQL_DATE_FORMATS = new HashMap<String, String>(9);

    static {
        ORACLE_DATE_FORMATS.put("^[0-9]{4}\\-[0-9]{1,2}\\-[0-9]{1,2}$", "YYYY-MM-DD");
        ORACLE_DATE_FORMATS.put("^[0-9]{4}\\-[0-9]{1,2}\\-[0-9]{1,2} [0-9]{1,2}\\:[0-9]{1,2}\\:[0-9]{1,2}$", "YYYY-MM-DD HH24:MI:SS");
        ORACLE_DATE_FORMATS.put("^[0-9]{4}\\-[0-9]{1,2}\\-[0-9]{1,2} [0-9]{1,2}\\:[0-9]{1,2}$", "YYYY-MM-DD HH24:MI");
        ORACLE_DATE_FORMATS.put("^[0-9]{4}\\-[0-9]{1,2}\\-[0-9]{1,2} [0-9]{1,2}$", "YYYY-MM-DD HH24");
        ORACLE_DATE_FORMATS.put("^[0-9]{4}\\/[0-9]{1,2}\\/[0-9]{1,2}$", "YYYY/MM/DD");
        ORACLE_DATE_FORMATS.put("^[0-9]{4}\\/[0-9]{1,2}\\/[0-9]{1,2} [0-9]{1,2}\\:[0-9]{1,2}\\:[0-9]{1,2}$", "YYYY/MM/DD HH24:MI:SS");
        ORACLE_DATE_FORMATS.put("^[0-9]{4}\\/[0-9]{1,2}\\/[0-9]{1,2} [0-9]{1,2}\\:[0-9]{1,2}$", "YYYY/MM/DD HH24:MI");
        ORACLE_DATE_FORMATS.put("^[0-9]{4}\\/[0-9]{1,2}\\/[0-9]{1,2} [0-9]{1,2}$", "YYYY/MM/DD HH24");
        ORACLE_DATE_FORMATS.put("^[0-9]{4}[0-9]{2}[0-9]{2}$", "YYYYMMDD");

        MYSQL_DATE_FORMATS.put("^[0-9]{4}\\-[0-9]{1,2}\\-[0-9]{1,2}$", "%Y-%m-%d");
        MYSQL_DATE_FORMATS.put("^[0-9]{4}\\-[0-9]{1,2}\\-[0-9]{1,2} [0-9]{1,2}\\:[0-9]{1,2}\\:[0-9]{1,2}$", "%Y-%m-%d %T");
        MYSQL_DATE_FORMATS.put("^[0-9]{4}\\-[0-9]{1,2}\\-[0-9]{1,2} [0-9]{1,2}\\:[0-9]{1,2}$", "%Y-%m-%d %H:%i");
        MYSQL_DATE_FORMATS.put("^[0-9]{4}\\-[0-9]{1,2}\\-[0-9]{1,2} [0-9]{1,2}$", "%Y-%m-%d %H");
        MYSQL_DATE_FORMATS.put("^[0-9]{4}\\/[0-9]{1,2}\\/[0-9]{1,2}$", "%Y/%m/%d");
        MYSQL_DATE_FORMATS.put("^[0-9]{4}\\/[0-9]{1,2}\\/[0-9]{1,2} [0-9]{1,2}\\:[0-9]{1,2}\\:[0-9]{1,2}$", "%Y/%m/%d %T");
        MYSQL_DATE_FORMATS.put("^[0-9]{4}\\/[0-9]{1,2}\\/[0-9]{1,2} [0-9]{1,2}\\:[0-9]{1,2}$", "%Y/%m/%d %H:%i");
        MYSQL_DATE_FORMATS.put("^[0-9]{4}\\/[0-9]{1,2}\\/[0-9]{1,2} [0-9]{1,2}$", "%Y/%m/%d %H");
        MYSQL_DATE_FORMATS.put("^[0-9]{4}[0-9]{2}[0-9]{2}$", "%Y%m%d");
        
        OPERAS.put(OPERA_LIKE, "like");// like
        OPERAS.put(OPERA_EQ, "=");// 等于
        OPERAS.put(OPERA_NE, "<>");// 不等于
        OPERAS.put(OPERA_GT, ">");// 大于
        OPERAS.put(OPERA_GE, ">=");// 大于等于
        OPERAS.put(OPERA_LT, "<");// 小于
        OPERAS.put(OPERA_LE, "<=");// 小于等于
        OPERAS.put(OPERA_NULL, "is null");// 空
        OPERAS.put(OPERA_NOTNULL, "is not null");// 非空
        OPERAS.put(OPERA_IN, "in");// 包含多值，值需要类型拼装，如字符串为：'v1','v2','v3';数字为:1,2,3
        OPERAS.put(OPERA_NOTIN, "not in");// 不包含多值
        /*
         * 多条件或查询，查询值的格式为“value1||value2”；
         * 如果为空使用“null”或者“not null”作为空或非空；
         * key为"_block"时直接传值为 [field1]='value1' or [field2]='value2'
         */
        OPERAS.put(OPERA_OR, "or");
    }

    /**
     * 根据传入的 PO 或 VO 及查询条件参数生成对应的 HQL 或 SQL 查询语句
     * @param resultType 结果类型，即 PO 或 VO 的类定义
     * @param params 查询参数：<pre>
     * 参数名支持以下格式： 
     * 1）变量名
     *      直接使用变量名，按照等于操作查询
     * 2）变量名_操作符 
     *      操作符为以下之一：
     *      like 对应sql中的'like' 
     *      eq 等于，对应sql中的'=' 
     *      lt 小于，对应sql中'<' 
     *      gt 大于，对应sql中'>' 
     *      ne 不等于，对应sql中的'<>' 
     *      le 小于等于，对应sql中的'<=' 
     *      ge 大于等于，对应sql中的'>='
     *      null 为空，对应sql中的'is null'
     *      not null 不为空，对应sql中的'is not null'
     *      in  包含多值，值需要类型拼装，如字符串为：'v1','v2','v3';数字为:1,2,3
     *      notin   不包含多值，值需要类型拼装，如字符串为：'v1','v2','v3';数字为:1,2,3
     *      or  或，多条件或查询，查询值的格式为“value1||value2”，如果为空使用“null”或者“not null”作为空或非空
     *      参数值中如使用了模糊查询字符'%'，系统将自动使用like进行查询（忽略参数名中设置的操作符）
     * 3)_block
     *      key为"_block"时直接传值为[field1]='value1' or [field2]='value2'，其中对象的属性名需要使用"[]"括起来
     *      "[]"中的值如果是VO中的主对象直接写属性名，如果是其他对象需要写"类名.属性名"
     * </pre>
     * @return 生成的查询语句
     */
    public String generateQueryLanguage(Class<?> resultType, Map<String, ?> params, Dialect dialect)
            throws RuntimeException {
        StringBuffer result = new StringBuffer();
        
        // 如果存在 PARAM_COUNT 参数只查询 count(xxx) 字段
        boolean isQueryCount = params.containsKey(PARAM_COUNT);
        String queryColumns = (isQueryCount ? getCountColumn(resultType, params.get(PARAM_COUNT)) : getQueryColumns(resultType));
        if(StringUtils.isNotBlank(queryColumns)) {
            result.append("select ");
            
            if(!isQueryCount) {
                Object distinct = params.get(PARAM_DISTINCT);
                if (distinct != null && castValue(distinct, Boolean.class)) {
                    result.append(" distinct ");
                }
            }
            
            result.append(queryColumns);
        }
        
        result.append(" from ").append(getQueryTableName(resultType, params, dialect));

        String queryCondition = getQueryCondition(resultType, params); // 查询条件
        if (StringUtils.isNotBlank(queryCondition)) {
            result.append(" where ").append(queryCondition);
        }

        if(params.containsKey(PARAM_ORDERBY)) { // 排序
            String orderBy = getQueryOrderby(params.get(PARAM_ORDERBY), resultType);
            if (StringUtils.isNotBlank(orderBy)) {
                result.append(" order by ").append(orderBy);
            }
        }
        
        return result.toString();
    }

    /**
     * 将指定的值转换为特定的数据类型
     * 
     * @param value 要进行转换的值
     * @param destType 目标数据类型，只支持简单的数据类型，不支持数组、集合、Map 及 JavaBean 等复杂类型
     * @param <T> 目标类型
     * @return 转换类型后的数值，如果传入的数值无法转换成目标类型，则抛出 {@link IllegalArgumentException}
     * @throws IllegalArgumentException 传入的数值无法转换成目标类型时抛出此异常
     * @throws NumberFormatException 目标类型是数字类型，但传入的数值无法转换成对应的数字时，抛出此异常
     */
    @SuppressWarnings("unchecked")
    public <T> T castValue(Object value, Class<T> destType) {
        if (value == null) {
            return null;
        }

        Class<?> valueType = value.getClass();
        if (destType.isAssignableFrom(valueType)) { // 参数值的类型与目标字段的类型相兼容
            return (T) value;
        }

        // 传入的数值是原生数据类型，或原生数据类型的包装类型，或字符串类型
        boolean isPrimitiveOrString = (ClassUtils.isPrimitiveOrWrapper(valueType) || CharSequence.class
                .isAssignableFrom(valueType));
        String trimedStrVal = StringUtils.trimToEmpty(value.toString());

        // boolean
        if (destType == boolean.class || destType == Boolean.class) {
            if (value instanceof Number) {
                return (T) BooleanUtils.toBooleanObject(((Number) value).intValue());
            }
            if (isPrimitiveOrString) {
                return (T) BooleanUtils.toBooleanObject(trimedStrVal);
            }
        }

        // char
        if ((destType == char.class || destType == Character.class) && isPrimitiveOrString
                && StringUtils.length(value.toString()) == 1) {
            return (T) Character.valueOf(value.toString().charAt(0));
        }

        // byte
        if ((destType == byte.class || destType == Byte.class)) {
            if (isPrimitiveOrString) {
                return (T) Byte.valueOf(trimedStrVal);
            }

            if (value instanceof BigInteger || value instanceof BigDecimal) {
                return (T) Byte.valueOf(((Number) value).byteValue());
            }
        }

        // short
        if (destType == short.class || destType == Short.class) {
            if (isPrimitiveOrString) {
                return (T) Short.valueOf(trimedStrVal);
            }

            if (value instanceof BigInteger || value instanceof BigDecimal) {
                return (T) Short.valueOf(((Number) value).shortValue());
            }
        }

        // int
        if (destType == int.class || destType == Integer.class) {
            if (isPrimitiveOrString) {
                return (T) Integer.valueOf(trimedStrVal);
            }

            if (value instanceof BigInteger || value instanceof BigDecimal) {
                return (T) Integer.valueOf(((Number) value).intValue());
            }
        }

        // long
        if (destType == long.class || destType == Long.class) {
            if (isPrimitiveOrString) {
                return (T) Long.valueOf(trimedStrVal);
            }

            if (value instanceof BigInteger || value instanceof BigDecimal) {
                return (T) Long.valueOf(((Number) value).longValue());
            }
        }

        // float
        if (destType == float.class || destType == Float.class) {
            if (isPrimitiveOrString) {
                return (T) Float.valueOf(trimedStrVal);
            }

            if (value instanceof BigInteger || value instanceof BigDecimal) {
                return (T) Float.valueOf(((Number) value).floatValue());
            }
        }

        // double
        if (destType == double.class || destType == Double.class) {
            if (isPrimitiveOrString) {
                return (T) Double.valueOf(trimedStrVal);
            }

            if (value instanceof BigInteger || value instanceof BigDecimal) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            }
        }

        // BigInteger
        if (destType == BigInteger.class && isPrimitiveOrString) {
            return (T) new BigInteger((String) trimedStrVal);
        }

        // BigDecimal
        if (destType == BigDecimal.class && isPrimitiveOrString) {
            return (T) new BigDecimal((String) trimedStrVal);
        }

        // String
        if (destType == String.class) {
            if (value instanceof Date) { // 日期
                return (T) DateFormatUtils.format((Date) value, DateUtil.DEFAULT_DATETIME_FORMAT);
            }
            if (value instanceof Calendar) { // 日历
                return (T) DateFormatUtils.format((Calendar) value, DateUtil.DEFAULT_DATETIME_FORMAT);
            }
            if (value instanceof Enum) { // 枚举
                return (T) ((Enum<?>) value).name();
            }
            if (value instanceof Clob) { // CLOB
                Clob clob = (Clob) value;
                // InputStream input = null;
                Reader input = null;
                try {
                    // input = clob.getAsciiStream();
                    input = clob.getCharacterStream();
                    return (T) IOUtils.toString(input);
                } catch (Exception e) {
                    LOGGER.warn("将 CLOB 类型的值 [{}]， 转换为 String 字符串时发生错误： {}", value, e);
                    return (T) value.toString();
                } finally {
                    IOUtils.closeQuietly(input);
                }
            }
            return (T) value.toString();
        }

        // Date or Calendar
        if (java.util.Date.class.isAssignableFrom(destType) || destType == Calendar.class) {
            Date date = null;

            if (value instanceof Date) {
                date = (Date) value;
            } else if (value instanceof Calendar) {
                date = ((Calendar) value).getTime();
            } else if (value instanceof Long) {
                date = new Date((Long) value);
            } else if (value instanceof BigDecimal) {
                date = new Date(((BigDecimal) value).longValue());
            } else if (value instanceof CharSequence) {
                try {
                    date = StringUtils.isBlank(trimedStrVal) ? null : DateUtils.parseDate(trimedStrVal,
                            DateUtil.SUPPORT_FORMATS);
                } catch (ParseException e) {
                    throw new IllegalArgumentException(
                            "字符串 [" + value
                                    + "] 不是一个有效的日期格式字符串，无法将其转换成日期对象，支持的日期格式请参见 gboat2.base.bridge.util.DateUtil.SUPPORT_FORMATS",
                            e);
                }
            }

            if (date != null) {
                if (java.util.Date.class == destType) {
                    return (T) date;
                }
                if (java.util.Calendar.class == destType) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    return (T) calendar;
                }
                if (java.sql.Date.class == destType) {
                    return (T) new java.sql.Date(date.getTime());
                }
                if (java.sql.Time.class == destType) {
                    return (T) new java.sql.Time(date.getTime());
                }
                if (java.sql.Timestamp.class == destType) {
                    return (T) new java.sql.Timestamp(date.getTime());
                }
            }
        }

        // Enum
        if (destType.isEnum()) { // 枚举 
            Enum<?>[] enums = (Enum<?>[]) destType.getEnumConstants();
            if (value instanceof CharSequence) {
                if (StringUtils.isBlank(trimedStrVal)) {
                    return null;
                }
                for (Enum<?> e : enums) {
                    if (e.name().equals(trimedStrVal)) {
                        return (T) e;
                    }
                }
                throw new IllegalArgumentException("字符串 [" + value + "] 无法转换为枚举 [" + destType.getName()
                        + "]： 没有名称与之匹配的枚举值");
            }

            if (value instanceof Number) {
                int ordinal = ((Number) value).intValue();
                for (Enum<?> e : enums) {
                    if (ordinal == e.ordinal()) {
                        return (T) e;
                    }
                }
                throw new IllegalArgumentException("数字 [" + value + "] 无法转换为枚举 [" + destType.getName()
                        + "]： 没有序号（ordinal）与之匹配的枚举值");
            }
        }

        throw new IllegalArgumentException("传入值 [" + value + "] 的类型为 [" + valueType.getName() + "]， 无法转换成有效的目标类型 ["
                + destType.getName() + "] 值");

    }
    
    /**
     * 根据查询结果的类定义获取要查询的字段， 即 “select ... from” 中的 “...” 部分
     * @param resultType 查询结果的类定义
     * @param params 查询条件
     */
    protected abstract String getQueryColumns(Class<?> resultType);

    /**
     * 获取查询语句中的 count(...)
     * @param resultType 查询结果的类定义
     * @param params 查询条件
     * @return 查询语句中的  count(...)
     */
    protected String getCountColumn(Class<?> resultType, Object countValue) {
        countValue = ObjectUtils.defaultIfNull(countValue, StringUtils.EMPTY);
        if (countValue instanceof Object[] && ((Object[]) countValue).length == 1) {
            countValue = ObjectUtils.defaultIfNull(((Object[]) countValue)[0], StringUtils.EMPTY);
        }

        // PARAM_COUNT 对应的值既不是字符串，也不是原生类型
        if (!(countValue instanceof CharSequence || ClassUtils.isPrimitiveOrWrapper(countValue.getClass()))) {
            throw new RuntimeException("查询 [" + resultType.getName() + "] 时传入的 [" + PARAM_COUNT + "] 参数的类型 ["
                    + countValue.getClass().getName() + "] 错误，对应的参数值为 [" + countValue + "]");
        }

        String fieldNameOfCount = StringUtils.defaultIfBlank(StringUtils.trim(countValue.toString()), "*");
        // count(*), count(true), count(false), count(1) 等
        if (fieldNameOfCount.matches("^(?i)(\\*|true|false|\\d+)$")) {
            return "count(" + fieldNameOfCount + ") as count_";
        }

        Field field = ClassHelper.getField(resultType, fieldNameOfCount);
        if (field == null) {
            throw new RuntimeException("查询 [" + resultType.getName() + "] 时，要使用 count() 进行统计查询的字段 ["
                    + fieldNameOfCount + "] 不存在");
        }
        return "count(" + getColumnName(field) + ") as " + field.getName() + "_count";
    }

    /**
     * 根据查询结果的类定义和查询参数获取生成的 HQL/SQL 语句中要查询的目标对象<br>
     * 即 “select ... from xxx where ...” 中的 “xxx”
     * 
     * @param resultType 查询结果的类定义
     * @param params 查询条件
     * @return 需要查询的目标对象，如：<br>
     *         gboat2.web.model.User 或 G2_T_USER User0_ left join G2_T_GROUP
     *         Group0_ on User0_.USER_ID = Group0_.USER_ID
     */
    protected abstract String getQueryTableName(Class<?> resultType, Map<String, ?> params, Dialect dialect);

    /**
     * 根据字段定义获取字段在 HQL/SQL 查询语句中的列名
     * 
     * @param field 字段定义
     * @return 字段在 HQL/SQL 查询语句中的列名：如： loginId 或 user0_.LOGIN_ID
     */
    protected abstract String getColumnName(Field field) throws RuntimeException;

    @SuppressWarnings("unchecked")
    protected String getQueryCondition(Class<?> resultType, Map<String, ?> params) {
        // PO、VO 类中所有的字段名称和字段定义的键值对
        Map<String, Field> fieldMap = ClassHelper.getFieldMap(resultType);
        // 存放 HQL 查询参数的 MAP
        Map<String, Object> namedParameters = new HashMap<String, Object>();

        StringBuffer result = new StringBuffer(); // 查询条件
        String key; // 参数的 Key
        Object value; // 参数的 Value
        String fieldNamesStr;
        String[] fieldNames;
        String operaSymble;
        Field field;
        String fieldClause; // 单个字段对应的查询语句
        StringBuffer clause; // 单个过滤条件对应的查询语句
        for (Entry<String, ?> entry : params.entrySet()) {
            key = entry.getKey();
            if (key == null || !key.startsWith(PARAM_PREFIX)) {
                continue; // key 不以下划线开头，则直接跳过（参数名称约定以 "_" 开头）
            }

            value = entry.getValue();
            if (value == null && !StringUtils.endsWithAny(key, PARAM_PREFIX + OPERA_NULL, PARAM_PREFIX + OPERA_NOTNULL)) {
                // 如果值为 null，而且 key 不是以 _null 或 _notnull 结束，则直接跳过
                LOGGER.warn("查询条件中 [{}] 的值为 null，已经被忽略。", key);
                continue;
            }

            clause = new StringBuffer();
            if (PARAM_BLOCK.equals(key)) { // 拼写好的 HQL 语句块
                fieldClause = processBlockValue(value, resultType);
                if (StringUtils.isNotBlank(fieldClause)) { // 用 () 将 block 传入的值包围起来
                    clause.append('(').append(fieldClause).append(')');
                }
            } else {
                fieldNamesStr = key.substring(PARAM_PREFIX.length()); // 移除掉最前面的“_”
                int lastIndex = fieldNamesStr.lastIndexOf(PARAM_PREFIX);
                if (lastIndex < 0) { // 没有指定过滤条件操作符
                    operaSymble = OPERA_EQ;
                } else {
                    operaSymble = fieldNamesStr.substring(lastIndex + PARAM_PREFIX.length());
                    fieldNamesStr = fieldNamesStr.substring(0, lastIndex);
                }

                fieldNames = fieldNamesStr.split(PARAM_SEPARATOR);
                int validCount = 0; // 有效的过滤条件个数
                for (String fieldName : fieldNames) {
                    field = fieldMap.get(fieldName);
                    if (field == null) { // 自动纠正字段名称的大小写拼写错误
                        field = ClassHelper.getFieldIgnoreCase(resultType, fieldName);
                        if (field == null) {
                            LOGGER.warn("类 [{}] 中不存在名称为 [{}] 的字段，查询条件已经忽略了对该字段进行解析。", resultType.getName(), fieldName);
                            continue;
                        }

                        LOGGER.info("类 [{}] 中字段 [{}] 的名称大小写拼写不正确，正确的拼写是 [{}]，请及时修正代码。", resultType.getName(),
                                fieldName);
                        fieldMap.put(field.getName(), field);
                    }

                    fieldClause = getFieldClause(field, value, operaSymble, namedParameters);
                    if (StringUtils.isNotBlank(fieldClause)) {
                        if (clause.length() > 0) {
                            clause.append(" or ");
                        }
                        clause.append('(').append(fieldClause).append(')');
                        validCount++;
                    }
                }
                if(validCount > 1) { // 如果同时对多个字段进行过滤，则将过滤条件用 () 包起来
                    clause.insert(0, '(').append(')');
                }
            }

            if (StringUtils.isNotBlank(clause)) {
                if (result.length() > 0) { // 多个条件使用 and 连接
                    result.append(" and ");
                }
                result.append(clause);
            }
        }
        // 将 HQL/SQL 语句中的参数添加到传入的 Map，供创建 Query 对象时设置参数值使用
        ((Map<String, Object>) params).putAll(namedParameters);
        return result.toString();
    }

    protected String getQueryOrderby(Object orderbyValue, Class<?> resultType) {
        if (orderbyValue == null) {
            throw new RuntimeException("参数 [" + PARAM_ORDERBY + "] 的值为 null，无效值！");
        }
        
        String result = null;
        String separator = ", ";
        if(orderbyValue instanceof CharSequence || orderbyValue instanceof Character) {
            result = orderbyValue.toString();
        } else if (orderbyValue instanceof CharSequence[]) {
            result = StringUtils.join((CharSequence[])orderbyValue, separator);
        } else if (orderbyValue instanceof Iterable) {
            result = StringUtils.join((Iterable<?>)orderbyValue, separator);
        } else if (orderbyValue instanceof Iterator) {
            result = StringUtils.join((Iterator<?>)orderbyValue, separator);
        } else if (orderbyValue instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) orderbyValue;
            if (!map.isEmpty()) {
                StringBuffer sb = new StringBuffer();
                for (Entry<?, ?> entry : map.entrySet()) {
                    if (sb.length() > 0) {
                        sb.append(separator);
                    }
                    
                    sb.append(entry.getKey());
                    if (entry.getValue() != null) {
                        sb.append(" ").append(entry.getValue());
                    }
                }
                result = sb.toString();
            }
        } else {
            throw new RuntimeException("查询 [" + resultType.getName() + "] 时传入的 [" + PARAM_ORDERBY + "] 参数的类型 ["
                    + orderbyValue.getClass().getName() + "] 错误，对应的参数值为 [" + orderbyValue + "]");
        }

        if (StringUtils.isBlank(result)) {
            throw new RuntimeException("参数 [" + PARAM_ORDERBY + "] 的值为空字符串，无效值！");
        }
        return result;
    }
    
    /**
     * 获取指定字段的过滤条件
     * @param field 字段定义
     * @param value 字段值
     * @param operaSymble 过滤条件操作符
     * @param namedParameters 存放 HQL 查询参数的 Map
     * @return 过滤条件查询语句
     */
    @SuppressWarnings("unchecked")
    protected String getFieldClause(Field field, Object value, String operaSymble, Map<String, Object> namedParameters) {
        operaSymble = StringUtils.defaultIfBlank(operaSymble, OPERA_EQ).trim();
        String opera = OPERAS.get(operaSymble);
        if (opera == null) {
            throw new RuntimeException("无效的操作 [" + operaSymble + "]");
        }

        if (OPERA_NULL.equals(operaSymble) || OPERA_NOTNULL.equals(operaSymble)) {
            return getColumnName(field) + " " + opera;
        }

        if (OPERA_OR.equals(operaSymble)) {
            String strVal = null;
            if (value instanceof String) {
                strVal = (String)value;
            } else if (value instanceof String[]) {
                String[] array = (String[]) value;
                if (array.length == 1 && array[0] != null) {
                    strVal = array[0];
                }
            }

            if (strVal != null) {
                if (strVal.contains("||")) { // 老版本中传入的值为 value1||value2 的格式
                    String[] array_0 = strVal.split("\\|\\|");
                    String[] array_1 = new String[array_0.length];
                    for (int i = 0; i < array_0.length; i++) {
                        /*
                         * 老版本中传入的参数值有的人给值加了单引号，有的没有给加，
                         * 即：有的人传入的值为 'value1'||'value2'，而有的人传入的值为 value1||value2 的格式
                         */
                        array_1[i] = (array_0[i].matches("^(?s)'.*'$") ? array_0[i].substring(1,
                                array_0[i].length() - 1) : array_0[i]);
                    }
                    value = array_1;
                }
            }
            return processOrCondition(field, value, 1, namedParameters, null);
        }

        if (OPERA_IN.equals(operaSymble) || OPERA_NOTIN.equals(operaSymble)) {
            String strVal = null;
            if (value instanceof String[]) {
                String[] array = (String[]) value;
                if (array.length == 1 && array[0] != null) {
                    strVal = array[0];
                }
            } else if (value instanceof CharSequence) {
                strVal = value.toString();
            }

            if (strVal != null) {
                strVal = strVal.trim();
                if (strVal.matches("^(?is)'.*'$")) {
                    // 老版本中传入的值为 'value1','value2' 的格式
                    value = strVal.substring(1, strVal.length() - 1).split("'\\s*,\\s*'");
                } else if (strVal.matches("^\\d[\\d\\s,.]*$")) {
                    // 老版本中传入的值为 x,y,z 的格式（数字列）
                    value = strVal.split("\\s*,\\s*");
                }
            }

            if (strVal != null && strVal.matches("^(?is)\\(?\\s*select\\b.+\\bfrom\\b.+$")) { // 子查询语句
                return getColumnName(field) + " " + opera + (strVal.charAt(0) == '(' ? strVal : "(" + strVal + ")");
            }
            return processInCondition(field, value, namedParameters, OPERA_IN.equals(operaSymble));
        }

        if (value instanceof String[] && ((String[]) value).length == 1 && StringUtils.isEmpty(((String[]) value)[0])) {
            LOGGER.info("查询 [" + field.getDeclaringClass().getName() + "] 时，过滤条件中字段 [" + field.getName() + "] 按 ["
                    + opera + "] 操作过滤时，其值为一个空字符串， 将直接忽略此查询条件（前台页面查询条件的文本框中啥也没有输入）。");
            return "";
        }
        
        Set<?> values = getParameterValues(field, value, OPERA_LIKE.equals(operaSymble));
        if (CollectionUtils.isEmpty(values)) {
            return "";
        }

        if (OPERA_LIKE.equals(operaSymble)) {
            if (field.getType() != String.class) {
                throw new RuntimeException("like 模糊查询只能用于 String 类型的字段");
            }

            return processLikeCondition(field, (Set<String>) values, namedParameters);
        } else {
            if (values.size() > 1) {
                throw new RuntimeException("操作符 [" + opera + "] 不支持多个参数值 " + values);
            }

            String parameterName = field.getName() + "_" + operaSymble + "_1_";
            namedParameters.put(parameterName, values.iterator().next());
            return getColumnName(field) + " " + opera + " :" + parameterName;
        }
    }

    /**
     * 处理查询参数中 {@link QuerySupport#PARAM_BLOCK} 参数的值
     * @param block HQL 或 SQL 语句块
     * @param 查询结果的类定义
     * @return 处理后的有效值
     */
    protected String processBlockValue(Object block, Class<?> recordType) {
        if (block instanceof CharSequence) {
            return block.toString();
        }

        if (block instanceof String[]) {
            String[] array = (String[]) block;
            if (array.length == 1 && StringUtils.isNotBlank(array[0])) {
                return array[0];
            }
            throw new RuntimeException("参数 [" + PARAM_BLOCK + "] 的值类型为 String[]，但其长度不为 1 或第 1 个元素的值为空，无效值！");
        }

        throw new RuntimeException("参数 [" + PARAM_BLOCK + "] 的值只支持 String 和 String[] 类型的值");
    }

    /**
     * 处理查询条件中的 or 关键字
     * 
     * @param field 字段定义
     * @param value 参数值
     * @param index 参数值的序号
     * @param params 查询条件中的参数键值对
     * @param orClause 已有的过滤条件语句
     * @return 查询条件语句
     */
    protected String processOrCondition(Field field, Object value, int index, Map<String, Object> params,
            StringBuffer orClause) {
        if (orClause == null) {
            orClause = new StringBuffer();
        }
        
        if (value instanceof Object[]) { // 数组
            for (Object obj : (Object[]) value) {
                processOrCondition(field, obj, index++, params, orClause);
            }
        } else if (value instanceof Iterable) { // 可迭代对象（集合等）
            for (Object obj : (Iterable<?>) value) {
                processOrCondition(field, obj, index++, params, orClause);
            }
        } else if (value instanceof Iterator) {
            Iterator<?> iter = (Iterator<?>) value;
            while (iter.hasNext()) {
                processOrCondition(field, iter.next(), index++, params, orClause);
            }
        } else {
            String trimedStrValue = (value == null ? "null" : StringUtils.trim(value.toString()));
            String fieldName = field.getName();

            if (orClause.length() > 0) {
                orClause.append(" or ");
            }
            orClause.append(getColumnName(field));

            if ("null".equalsIgnoreCase(trimedStrValue) || ("not null").equalsIgnoreCase(trimedStrValue)) {
                orClause.append(" is ").append(trimedStrValue);
            } else {
                String parameterName = fieldName + "_or_" + index + "_";
                params.put(parameterName, castValue(value, field.getType()));
                orClause.append(" = :").append(parameterName);
            }
        }
        return orClause.toString();
    }

    /**
     * 处理查询条件中的 in 关键字
     * 
     * @param field 字段定义
     * @param value 参数值
     * @param params 查询条件中的参数键值对
     * @param in 值为 true 表示是 in, 反之则表示是 not in
     * @return 查询条件语句
     */
    protected String processInCondition(Field field, Object value, Map<String, Object> params, boolean in) {
        boolean hasNullValue = false;
        String opera = (in ? OPERA_IN : OPERA_NOTIN);
        StringBuffer inClause = new StringBuffer();

        int index = 1;
        if (value instanceof Iterable) { // 可迭代对象（集合等）
            String fieldName = field.getName();
            inClause.append(getColumnName(field)).append(" ").append(OPERAS.get(opera)).append(" (");
            for (Object obj : (Iterable<?>) value) {
                if (obj == null) {
                    hasNullValue = true;
                    continue;
                }

                if (index > 1) {
                    inClause.append(", ");
                }
                String parameterName = fieldName + "_" + opera + "_" + index + "_";
                params.put(parameterName, castValue(obj, field.getType()));
                inClause.append(":").append(parameterName);
                index++;
            }

            if (index == 1) { // 没有任何有效参数值
                inClause = new StringBuffer();
            } else {
                inClause.append(")");
            }

            if (hasNullValue) {
                if (inClause.length() > 0) {
                    inClause.append(" or ");
                }
                inClause.append(fieldName).append(" ").append(OPERAS.get(in ? OPERA_NULL : OPERA_NOTNULL));
            }

            return inClause.toString();
        }

        if (value instanceof Object[]) { // 数组
            return processInCondition(field, Arrays.asList((Object[]) value), params, in);
        }

        if (value instanceof Iterator) { // Iterator
            Iterator<?> iter = (Iterator<?>) value;
            List<Object> list = new ArrayList<Object>();
            while (iter.hasNext()) {
                list.add(iter.next());
            }
            return processInCondition(field, list, params, in);
        }

        return processInCondition(field, Collections.singleton(value), params, in);
    }
    
    /**
     * 处理查询条件中的 like 关键字
     * 
     * @param field 字段定义
     * @param values 参数值
     * @param params 查询条件中的参数键值对
     * @return 查询条件语句
     */
    protected String processLikeCondition(Field field, Set<String> values, Map<String, Object> params) {
        boolean hasNullValue = false;
        List<String> excludeValues = Arrays.asList(new String[] { "", "%", "%%", "%null%" });
        StringBuffer likeClause = new StringBuffer();
        String columnName = getColumnName(field);
        int index = 1;
        for (Object val : values) {
            if (val == null) {
                hasNullValue = true;
                continue;
            }
            
            if (excludeValues.contains(val)) {
                continue;
            }

            if (!val.toString().contains("%")) {
                val = "%" + val + "%";
            }

            if (index > 1) {
                likeClause.append(" or ");
            }

            String parameterName = field.getName() + "_like_" + index + "_";
            params.put(parameterName, val);
            likeClause.append(columnName).append(" like :").append(parameterName);
        }
       
        if (hasNullValue) {
            if (likeClause.length() > 0) {
                likeClause.append(" or ");
            }
            likeClause.append(columnName).append(" ").append(OPERAS.get(OPERA_NULL));
        }
        return likeClause.toString();
    }

    /**
     * 根据字段定义及传入的参数值，将有效的参数值转换为一个 Set 集合
     * @param field 字段定义
     * @param value 传入的参数值
     * @param includeNull 是否允许返回结果中包含 <code>null</code>
     * @return 包含所有有效的参数值的 Set 集合
     */
    @SuppressWarnings("unchecked")
    protected <T> Set<T> getParameterValues(Field field, Object value, boolean includeNull) {
        Class<T> destType = (Class<T>) field.getType();
        Set<T> resultSet = new LinkedHashSet<T>();
        processParameterValue(value, destType, resultSet, includeNull);
        return resultSet;
    }

    /**
     * 将查询条件中的参数值转换成指定的目标类型
     * @param paramValue 查询条件中的参数值
     * @param destType 目标类型
     * @param resultSet 存放转换后的值的 Set 集合
     * @param includeNull 是否允许将 <code>null</code> 添加到 resultSet 集合中
     */
    protected <T> void processParameterValue(Object paramValue, Class<T> destType, Set<T> resultSet, boolean includeNull) {
        if (paramValue == null) {
            if(includeNull) {
                resultSet.add(null);
            }
            return;
        }

        if (paramValue instanceof CharSequence && StringUtils.isBlank(paramValue.toString())) {
            return;
        }

        if (paramValue instanceof Object[]) { // 数组
            for (Object obj : (Object[]) paramValue) {
                processParameterValue(obj, destType, resultSet, includeNull);
            }
        } else if (paramValue instanceof Iterable) { // 可迭代对象（集合等）
            for (Object obj : (Iterable<?>) paramValue) {
                processParameterValue(obj, destType, resultSet, includeNull);
            }
        } else if (paramValue instanceof Iterator) {
            Iterator<?> iter = (Iterator<?>) paramValue;
            while (iter.hasNext()) {
                processParameterValue(iter.next(), destType, resultSet, includeNull);
            }
        } else {
            T result = castValue(paramValue, destType);
            if (result != null) {
                resultSet.add(result);
            }
        }
    }

    /**
     * 判断指定的字段是否使用了 &#64;Transient 注解
     * 
     * @param field 字段定义
     * @return 字段或字段对应的 get、set 方法使用了 &#64;Transient 注解时返回 <code>true</code>，
     *         否则返回 <code>false</code>
     */
    protected boolean isTransient(Field field) {
        if (field.isAnnotationPresent(Transient.class)) {
            return true;
        }
        
        Class<?> declaringClass = field.getDeclaringClass();
        Method getMethod = ClassHelper.findGetMethod(declaringClass, field);
        if (getMethod != null && getMethod.isAnnotationPresent(Transient.class)) {
            return true;
        }
        
        Method setMethod = ClassHelper.findSetMethod(declaringClass, field);
        if (setMethod != null && setMethod.isAnnotationPresent(Transient.class)) {
            return true;
        }

        return false;
    }
    
    /**
     * 获取将指定的日期类型数据列转换为字符串的 SQL 语句
     * 
     * @param columnName 日期类型的数据库列名
     * @param patternOfJava 想要转换的日期格式在 Java 中表示方式，如： yyyy-MM-dd HH:mm:ss，
     *            请参见 {@link DateUtil#SUPPORT_FORMATS}
     * @return SQL 语句，如：<br>
     *         使用 Oracle 数据库时可能为 to_char(COLUMN_NAME, 'YYYY-MM-DD HH24:MI:SS')；<br>
     *         使用 MySQL 数据库时可能为 date_format(COLUMN_NAME,'%Y-%m-%d %T')
     */
    protected String getDate2StringSQL(String columnName, String patternOfJava, Dialect dialect) {
        String function; // 将日期转换为字符串的内置函数名称
        if (dialect instanceof Oracle8iDialect || dialect instanceof Oracle9iDialect) { // Oracle
            function = "to_char";
        } else if (dialect instanceof MySQLDialect) { // MySQL
            function = "date_format";
        } else {
            throw new RuntimeException("获取数据库的将日期转换成字符串的函数名称失败：Gboat 平台目前只支持 Oracle 和 MySQL 数据库");
        }
        return function + "(" + columnName + ", '"
                + getDateFormatePattern(DateFormatUtils.format(new Date(), patternOfJava), dialect) + "')";
    }

    /**
     * 获取将指定的日期字符串转换成数据库中的日期对象的 SQL 语句
     * 
     * @param dateString 日期字符串，如： 2014-12-01 或 2014-12-01 18:01:47
     * @return SQL 语句，如：<br>
     *         使用 Oracle 数据库时可能为 to_date(dateString, 'YYYY-MM-DD HH24:MI:SS')；<br>
     *         使用 MySQL 数据库时可能为 str_to_date(dateString,'%Y-%m-%d %T')
     */
    protected String getString2DateSQL(String dateString, Dialect dialect) {
        String function; // 将字符串转换为日期的内置函数名称
        if (dialect instanceof Oracle8iDialect || dialect instanceof Oracle9iDialect) { // Oracle
            function = "to_date";
        } else if (dialect instanceof MySQLDialect) { // MySQL
            function = "str_to_date";
        } else {
            throw new RuntimeException("获取数据库的将字符串转换成日期的函数名称失败：Gboat 平台目前只支持 Oracle 和 MySQL 数据库");
        }
        return function + "('" + dateString + "', '" + getDateFormatePattern(dateString, dialect) + "')";
    }

    /**
     * 获取数据库内置的查询当前系统时间的关键词
     * 
     * @return 数据库内置的查询当前系统时间的关键词，如： Oracle 返回 current_timestamp， MySQL 返回
     *         current_date
     */
    protected String getCurrentTimeFuncName(Dialect dialect) {
        return dialect.getCurrentTimestampSQLFunctionName();
    }

    /**
     * 根据数据库方言和示例日期字符串获取通过数据库内置的日期转字符串函数的日期格式
     * @param dateString 示例日期字符串，如：2014-10-18 12:06:53
     * @param dialect 数据库方言
     * @return 返回对应的日期格式，如： 使用 Oracle 数据库时可能返回 YYYY-MM-DD HH24:MI:SS， 使用 MySQL 数据库时可能返回 %Y-%m-%d %T
     */
    protected String getDateFormatePattern(String dateString, Dialect dialect) {
        if(dialect instanceof Oracle8iDialect || dialect instanceof Oracle9iDialect) { // Oracle
            return getDateFormatForOracle(dateString);
        }
   
        if(dialect instanceof MySQLDialect) { // MySQL
            return getDateFormatForMysql(dateString);
        }
         
        throw new RuntimeException("获取数据库内置的日期转字符串函数所需要的日期格式失败：Gboat 平台目前只支持 Oracle 和 MySQL 数据库");
    }

    /**
     * 根据日期字符串获取将日期字符串转换成 Oracle 日期的日期格式
     * @param value 日期字符串，如： <code>2014-12-01 12:03:45</code>
     * @return 转换成 Oracle 日期的日期格式
     */
    private String getDateFormatForOracle(String value) {
        for (Entry<String, String> e : GenerateQL.ORACLE_DATE_FORMATS.entrySet()) {
            if (Pattern.matches(e.getKey(), value)) {
                return e.getValue();
            }
        }
        throw new RuntimeException("invalid value format for Date : " + value);
    }


    /**
     * 根据日期字符串获取将日期字符串转换成 MySQL 日期的日期格式
     * @param value 日期字符串，如： <code>2014-12-01 12:03:45</code>
     * @return 转换成 MySQL 日期的日期格式
     */
    private String getDateFormatForMysql(String value) {
        for (Entry<String, String> e : GenerateQL.MYSQL_DATE_FORMATS.entrySet()) {
            if (Pattern.matches(e.getKey(), value)) {
                return e.getValue();
            }
        }
        throw new RuntimeException("invalid value format for Date : " + value);
    }
}
