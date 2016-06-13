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
        OPERAS.put(OPERA_EQ, "=");// ����
        OPERAS.put(OPERA_NE, "<>");// ������
        OPERAS.put(OPERA_GT, ">");// ����
        OPERAS.put(OPERA_GE, ">=");// ���ڵ���
        OPERAS.put(OPERA_LT, "<");// С��
        OPERAS.put(OPERA_LE, "<=");// С�ڵ���
        OPERAS.put(OPERA_NULL, "is null");// ��
        OPERAS.put(OPERA_NOTNULL, "is not null");// �ǿ�
        OPERAS.put(OPERA_IN, "in");// ������ֵ��ֵ��Ҫ����ƴװ�����ַ���Ϊ��'v1','v2','v3';����Ϊ:1,2,3
        OPERAS.put(OPERA_NOTIN, "not in");// ��������ֵ
        /*
         * ���������ѯ����ѯֵ�ĸ�ʽΪ��value1||value2����
         * ���Ϊ��ʹ�á�null�����ߡ�not null����Ϊ�ջ�ǿգ�
         * keyΪ"_block"ʱֱ�Ӵ�ֵΪ [field1]='value1' or [field2]='value2'
         */
        OPERAS.put(OPERA_OR, "or");
    }

    /**
     * ���ݴ���� PO �� VO ����ѯ�����������ɶ�Ӧ�� HQL �� SQL ��ѯ���
     * @param resultType ������ͣ��� PO �� VO ���ඨ��
     * @param params ��ѯ������<pre>
     * ������֧�����¸�ʽ�� 
     * 1��������
     *      ֱ��ʹ�ñ����������յ��ڲ�����ѯ
     * 2��������_������ 
     *      ������Ϊ����֮һ��
     *      like ��Ӧsql�е�'like' 
     *      eq ���ڣ���Ӧsql�е�'=' 
     *      lt С�ڣ���Ӧsql��'<' 
     *      gt ���ڣ���Ӧsql��'>' 
     *      ne �����ڣ���Ӧsql�е�'<>' 
     *      le С�ڵ��ڣ���Ӧsql�е�'<=' 
     *      ge ���ڵ��ڣ���Ӧsql�е�'>='
     *      null Ϊ�գ���Ӧsql�е�'is null'
     *      not null ��Ϊ�գ���Ӧsql�е�'is not null'
     *      in  ������ֵ��ֵ��Ҫ����ƴװ�����ַ���Ϊ��'v1','v2','v3';����Ϊ:1,2,3
     *      notin   ��������ֵ��ֵ��Ҫ����ƴװ�����ַ���Ϊ��'v1','v2','v3';����Ϊ:1,2,3
     *      or  �򣬶��������ѯ����ѯֵ�ĸ�ʽΪ��value1||value2�������Ϊ��ʹ�á�null�����ߡ�not null����Ϊ�ջ�ǿ�
     *      ����ֵ����ʹ����ģ����ѯ�ַ�'%'��ϵͳ���Զ�ʹ��like���в�ѯ�����Բ����������õĲ�������
     * 3)_block
     *      keyΪ"_block"ʱֱ�Ӵ�ֵΪ[field1]='value1' or [field2]='value2'�����ж������������Ҫʹ��"[]"������
     *      "[]"�е�ֵ�����VO�е�������ֱ��д�����������������������Ҫд"����.������"
     * </pre>
     * @return ���ɵĲ�ѯ���
     */
    public String generateQueryLanguage(Class<?> resultType, Map<String, ?> params, Dialect dialect)
            throws RuntimeException {
        StringBuffer result = new StringBuffer();
        
        // ������� PARAM_COUNT ����ֻ��ѯ count(xxx) �ֶ�
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

        String queryCondition = getQueryCondition(resultType, params); // ��ѯ����
        if (StringUtils.isNotBlank(queryCondition)) {
            result.append(" where ").append(queryCondition);
        }

        if(params.containsKey(PARAM_ORDERBY)) { // ����
            String orderBy = getQueryOrderby(params.get(PARAM_ORDERBY), resultType);
            if (StringUtils.isNotBlank(orderBy)) {
                result.append(" order by ").append(orderBy);
            }
        }
        
        return result.toString();
    }

    /**
     * ��ָ����ֵת��Ϊ�ض�����������
     * 
     * @param value Ҫ����ת����ֵ
     * @param destType Ŀ���������ͣ�ֻ֧�ּ򵥵��������ͣ���֧�����顢���ϡ�Map �� JavaBean �ȸ�������
     * @param <T> Ŀ������
     * @return ת�����ͺ����ֵ������������ֵ�޷�ת����Ŀ�����ͣ����׳� {@link IllegalArgumentException}
     * @throws IllegalArgumentException �������ֵ�޷�ת����Ŀ������ʱ�׳����쳣
     * @throws NumberFormatException Ŀ���������������ͣ����������ֵ�޷�ת���ɶ�Ӧ������ʱ���׳����쳣
     */
    @SuppressWarnings("unchecked")
    public <T> T castValue(Object value, Class<T> destType) {
        if (value == null) {
            return null;
        }

        Class<?> valueType = value.getClass();
        if (destType.isAssignableFrom(valueType)) { // ����ֵ��������Ŀ���ֶε����������
            return (T) value;
        }

        // �������ֵ��ԭ���������ͣ���ԭ���������͵İ�װ���ͣ����ַ�������
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
            if (value instanceof Date) { // ����
                return (T) DateFormatUtils.format((Date) value, DateUtil.DEFAULT_DATETIME_FORMAT);
            }
            if (value instanceof Calendar) { // ����
                return (T) DateFormatUtils.format((Calendar) value, DateUtil.DEFAULT_DATETIME_FORMAT);
            }
            if (value instanceof Enum) { // ö��
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
                    LOGGER.warn("�� CLOB ���͵�ֵ [{}]�� ת��Ϊ String �ַ���ʱ�������� {}", value, e);
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
                            "�ַ��� [" + value
                                    + "] ����һ����Ч�����ڸ�ʽ�ַ������޷�����ת�������ڶ���֧�ֵ����ڸ�ʽ��μ� gboat2.base.bridge.util.DateUtil.SUPPORT_FORMATS",
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
        if (destType.isEnum()) { // ö�� 
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
                throw new IllegalArgumentException("�ַ��� [" + value + "] �޷�ת��Ϊö�� [" + destType.getName()
                        + "]�� û��������֮ƥ���ö��ֵ");
            }

            if (value instanceof Number) {
                int ordinal = ((Number) value).intValue();
                for (Enum<?> e : enums) {
                    if (ordinal == e.ordinal()) {
                        return (T) e;
                    }
                }
                throw new IllegalArgumentException("���� [" + value + "] �޷�ת��Ϊö�� [" + destType.getName()
                        + "]�� û����ţ�ordinal����֮ƥ���ö��ֵ");
            }
        }

        throw new IllegalArgumentException("����ֵ [" + value + "] ������Ϊ [" + valueType.getName() + "]�� �޷�ת������Ч��Ŀ������ ["
                + destType.getName() + "] ֵ");

    }
    
    /**
     * ���ݲ�ѯ������ඨ���ȡҪ��ѯ���ֶΣ� �� ��select ... from�� �е� ��...�� ����
     * @param resultType ��ѯ������ඨ��
     * @param params ��ѯ����
     */
    protected abstract String getQueryColumns(Class<?> resultType);

    /**
     * ��ȡ��ѯ����е� count(...)
     * @param resultType ��ѯ������ඨ��
     * @param params ��ѯ����
     * @return ��ѯ����е�  count(...)
     */
    protected String getCountColumn(Class<?> resultType, Object countValue) {
        countValue = ObjectUtils.defaultIfNull(countValue, StringUtils.EMPTY);
        if (countValue instanceof Object[] && ((Object[]) countValue).length == 1) {
            countValue = ObjectUtils.defaultIfNull(((Object[]) countValue)[0], StringUtils.EMPTY);
        }

        // PARAM_COUNT ��Ӧ��ֵ�Ȳ����ַ�����Ҳ����ԭ������
        if (!(countValue instanceof CharSequence || ClassUtils.isPrimitiveOrWrapper(countValue.getClass()))) {
            throw new RuntimeException("��ѯ [" + resultType.getName() + "] ʱ����� [" + PARAM_COUNT + "] ���������� ["
                    + countValue.getClass().getName() + "] ���󣬶�Ӧ�Ĳ���ֵΪ [" + countValue + "]");
        }

        String fieldNameOfCount = StringUtils.defaultIfBlank(StringUtils.trim(countValue.toString()), "*");
        // count(*), count(true), count(false), count(1) ��
        if (fieldNameOfCount.matches("^(?i)(\\*|true|false|\\d+)$")) {
            return "count(" + fieldNameOfCount + ") as count_";
        }

        Field field = ClassHelper.getField(resultType, fieldNameOfCount);
        if (field == null) {
            throw new RuntimeException("��ѯ [" + resultType.getName() + "] ʱ��Ҫʹ�� count() ����ͳ�Ʋ�ѯ���ֶ� ["
                    + fieldNameOfCount + "] ������");
        }
        return "count(" + getColumnName(field) + ") as " + field.getName() + "_count";
    }

    /**
     * ���ݲ�ѯ������ඨ��Ͳ�ѯ������ȡ���ɵ� HQL/SQL �����Ҫ��ѯ��Ŀ�����<br>
     * �� ��select ... from xxx where ...�� �е� ��xxx��
     * 
     * @param resultType ��ѯ������ඨ��
     * @param params ��ѯ����
     * @return ��Ҫ��ѯ��Ŀ������磺<br>
     *         gboat2.web.model.User �� G2_T_USER User0_ left join G2_T_GROUP
     *         Group0_ on User0_.USER_ID = Group0_.USER_ID
     */
    protected abstract String getQueryTableName(Class<?> resultType, Map<String, ?> params, Dialect dialect);

    /**
     * �����ֶζ����ȡ�ֶ��� HQL/SQL ��ѯ����е�����
     * 
     * @param field �ֶζ���
     * @return �ֶ��� HQL/SQL ��ѯ����е��������磺 loginId �� user0_.LOGIN_ID
     */
    protected abstract String getColumnName(Field field) throws RuntimeException;

    @SuppressWarnings("unchecked")
    protected String getQueryCondition(Class<?> resultType, Map<String, ?> params) {
        // PO��VO �������е��ֶ����ƺ��ֶζ���ļ�ֵ��
        Map<String, Field> fieldMap = ClassHelper.getFieldMap(resultType);
        // ��� HQL ��ѯ������ MAP
        Map<String, Object> namedParameters = new HashMap<String, Object>();

        StringBuffer result = new StringBuffer(); // ��ѯ����
        String key; // ������ Key
        Object value; // ������ Value
        String fieldNamesStr;
        String[] fieldNames;
        String operaSymble;
        Field field;
        String fieldClause; // �����ֶζ�Ӧ�Ĳ�ѯ���
        StringBuffer clause; // ��������������Ӧ�Ĳ�ѯ���
        for (Entry<String, ?> entry : params.entrySet()) {
            key = entry.getKey();
            if (key == null || !key.startsWith(PARAM_PREFIX)) {
                continue; // key �����»��߿�ͷ����ֱ����������������Լ���� "_" ��ͷ��
            }

            value = entry.getValue();
            if (value == null && !StringUtils.endsWithAny(key, PARAM_PREFIX + OPERA_NULL, PARAM_PREFIX + OPERA_NOTNULL)) {
                // ���ֵΪ null������ key ������ _null �� _notnull ��������ֱ������
                LOGGER.warn("��ѯ������ [{}] ��ֵΪ null���Ѿ������ԡ�", key);
                continue;
            }

            clause = new StringBuffer();
            if (PARAM_BLOCK.equals(key)) { // ƴд�õ� HQL ����
                fieldClause = processBlockValue(value, resultType);
                if (StringUtils.isNotBlank(fieldClause)) { // �� () �� block �����ֵ��Χ����
                    clause.append('(').append(fieldClause).append(')');
                }
            } else {
                fieldNamesStr = key.substring(PARAM_PREFIX.length()); // �Ƴ�����ǰ��ġ�_��
                int lastIndex = fieldNamesStr.lastIndexOf(PARAM_PREFIX);
                if (lastIndex < 0) { // û��ָ����������������
                    operaSymble = OPERA_EQ;
                } else {
                    operaSymble = fieldNamesStr.substring(lastIndex + PARAM_PREFIX.length());
                    fieldNamesStr = fieldNamesStr.substring(0, lastIndex);
                }

                fieldNames = fieldNamesStr.split(PARAM_SEPARATOR);
                int validCount = 0; // ��Ч�Ĺ�����������
                for (String fieldName : fieldNames) {
                    field = fieldMap.get(fieldName);
                    if (field == null) { // �Զ������ֶ����ƵĴ�Сдƴд����
                        field = ClassHelper.getFieldIgnoreCase(resultType, fieldName);
                        if (field == null) {
                            LOGGER.warn("�� [{}] �в���������Ϊ [{}] ���ֶΣ���ѯ�����Ѿ������˶Ը��ֶν��н�����", resultType.getName(), fieldName);
                            continue;
                        }

                        LOGGER.info("�� [{}] ���ֶ� [{}] �����ƴ�Сдƴд����ȷ����ȷ��ƴд�� [{}]���뼰ʱ�������롣", resultType.getName(),
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
                if(validCount > 1) { // ���ͬʱ�Զ���ֶν��й��ˣ��򽫹��������� () ������
                    clause.insert(0, '(').append(')');
                }
            }

            if (StringUtils.isNotBlank(clause)) {
                if (result.length() > 0) { // �������ʹ�� and ����
                    result.append(" and ");
                }
                result.append(clause);
            }
        }
        // �� HQL/SQL ����еĲ�����ӵ������ Map�������� Query ����ʱ���ò���ֵʹ��
        ((Map<String, Object>) params).putAll(namedParameters);
        return result.toString();
    }

    protected String getQueryOrderby(Object orderbyValue, Class<?> resultType) {
        if (orderbyValue == null) {
            throw new RuntimeException("���� [" + PARAM_ORDERBY + "] ��ֵΪ null����Чֵ��");
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
            throw new RuntimeException("��ѯ [" + resultType.getName() + "] ʱ����� [" + PARAM_ORDERBY + "] ���������� ["
                    + orderbyValue.getClass().getName() + "] ���󣬶�Ӧ�Ĳ���ֵΪ [" + orderbyValue + "]");
        }

        if (StringUtils.isBlank(result)) {
            throw new RuntimeException("���� [" + PARAM_ORDERBY + "] ��ֵΪ���ַ�������Чֵ��");
        }
        return result;
    }
    
    /**
     * ��ȡָ���ֶεĹ�������
     * @param field �ֶζ���
     * @param value �ֶ�ֵ
     * @param operaSymble ��������������
     * @param namedParameters ��� HQL ��ѯ������ Map
     * @return ����������ѯ���
     */
    @SuppressWarnings("unchecked")
    protected String getFieldClause(Field field, Object value, String operaSymble, Map<String, Object> namedParameters) {
        operaSymble = StringUtils.defaultIfBlank(operaSymble, OPERA_EQ).trim();
        String opera = OPERAS.get(operaSymble);
        if (opera == null) {
            throw new RuntimeException("��Ч�Ĳ��� [" + operaSymble + "]");
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
                if (strVal.contains("||")) { // �ϰ汾�д����ֵΪ value1||value2 �ĸ�ʽ
                    String[] array_0 = strVal.split("\\|\\|");
                    String[] array_1 = new String[array_0.length];
                    for (int i = 0; i < array_0.length; i++) {
                        /*
                         * �ϰ汾�д���Ĳ���ֵ�е��˸�ֵ���˵����ţ��е�û�и��ӣ�
                         * �����е��˴����ֵΪ 'value1'||'value2'�����е��˴����ֵΪ value1||value2 �ĸ�ʽ
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
                    // �ϰ汾�д����ֵΪ 'value1','value2' �ĸ�ʽ
                    value = strVal.substring(1, strVal.length() - 1).split("'\\s*,\\s*'");
                } else if (strVal.matches("^\\d[\\d\\s,.]*$")) {
                    // �ϰ汾�д����ֵΪ x,y,z �ĸ�ʽ�������У�
                    value = strVal.split("\\s*,\\s*");
                }
            }

            if (strVal != null && strVal.matches("^(?is)\\(?\\s*select\\b.+\\bfrom\\b.+$")) { // �Ӳ�ѯ���
                return getColumnName(field) + " " + opera + (strVal.charAt(0) == '(' ? strVal : "(" + strVal + ")");
            }
            return processInCondition(field, value, namedParameters, OPERA_IN.equals(operaSymble));
        }

        if (value instanceof String[] && ((String[]) value).length == 1 && StringUtils.isEmpty(((String[]) value)[0])) {
            LOGGER.info("��ѯ [" + field.getDeclaringClass().getName() + "] ʱ�������������ֶ� [" + field.getName() + "] �� ["
                    + opera + "] ��������ʱ����ֵΪһ�����ַ����� ��ֱ�Ӻ��Դ˲�ѯ������ǰ̨ҳ���ѯ�������ı�����ɶҲû�����룩��");
            return "";
        }
        
        Set<?> values = getParameterValues(field, value, OPERA_LIKE.equals(operaSymble));
        if (CollectionUtils.isEmpty(values)) {
            return "";
        }

        if (OPERA_LIKE.equals(operaSymble)) {
            if (field.getType() != String.class) {
                throw new RuntimeException("like ģ����ѯֻ������ String ���͵��ֶ�");
            }

            return processLikeCondition(field, (Set<String>) values, namedParameters);
        } else {
            if (values.size() > 1) {
                throw new RuntimeException("������ [" + opera + "] ��֧�ֶ������ֵ " + values);
            }

            String parameterName = field.getName() + "_" + operaSymble + "_1_";
            namedParameters.put(parameterName, values.iterator().next());
            return getColumnName(field) + " " + opera + " :" + parameterName;
        }
    }

    /**
     * �����ѯ������ {@link QuerySupport#PARAM_BLOCK} ������ֵ
     * @param block HQL �� SQL ����
     * @param ��ѯ������ඨ��
     * @return ��������Чֵ
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
            throw new RuntimeException("���� [" + PARAM_BLOCK + "] ��ֵ����Ϊ String[]�����䳤�Ȳ�Ϊ 1 ��� 1 ��Ԫ�ص�ֵΪ�գ���Чֵ��");
        }

        throw new RuntimeException("���� [" + PARAM_BLOCK + "] ��ֵֻ֧�� String �� String[] ���͵�ֵ");
    }

    /**
     * �����ѯ�����е� or �ؼ���
     * 
     * @param field �ֶζ���
     * @param value ����ֵ
     * @param index ����ֵ�����
     * @param params ��ѯ�����еĲ�����ֵ��
     * @param orClause ���еĹ����������
     * @return ��ѯ�������
     */
    protected String processOrCondition(Field field, Object value, int index, Map<String, Object> params,
            StringBuffer orClause) {
        if (orClause == null) {
            orClause = new StringBuffer();
        }
        
        if (value instanceof Object[]) { // ����
            for (Object obj : (Object[]) value) {
                processOrCondition(field, obj, index++, params, orClause);
            }
        } else if (value instanceof Iterable) { // �ɵ������󣨼��ϵȣ�
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
     * �����ѯ�����е� in �ؼ���
     * 
     * @param field �ֶζ���
     * @param value ����ֵ
     * @param params ��ѯ�����еĲ�����ֵ��
     * @param in ֵΪ true ��ʾ�� in, ��֮���ʾ�� not in
     * @return ��ѯ�������
     */
    protected String processInCondition(Field field, Object value, Map<String, Object> params, boolean in) {
        boolean hasNullValue = false;
        String opera = (in ? OPERA_IN : OPERA_NOTIN);
        StringBuffer inClause = new StringBuffer();

        int index = 1;
        if (value instanceof Iterable) { // �ɵ������󣨼��ϵȣ�
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

            if (index == 1) { // û���κ���Ч����ֵ
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

        if (value instanceof Object[]) { // ����
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
     * �����ѯ�����е� like �ؼ���
     * 
     * @param field �ֶζ���
     * @param values ����ֵ
     * @param params ��ѯ�����еĲ�����ֵ��
     * @return ��ѯ�������
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
     * �����ֶζ��弰����Ĳ���ֵ������Ч�Ĳ���ֵת��Ϊһ�� Set ����
     * @param field �ֶζ���
     * @param value ����Ĳ���ֵ
     * @param includeNull �Ƿ������ؽ���а��� <code>null</code>
     * @return ����������Ч�Ĳ���ֵ�� Set ����
     */
    @SuppressWarnings("unchecked")
    protected <T> Set<T> getParameterValues(Field field, Object value, boolean includeNull) {
        Class<T> destType = (Class<T>) field.getType();
        Set<T> resultSet = new LinkedHashSet<T>();
        processParameterValue(value, destType, resultSet, includeNull);
        return resultSet;
    }

    /**
     * ����ѯ�����еĲ���ֵת����ָ����Ŀ������
     * @param paramValue ��ѯ�����еĲ���ֵ
     * @param destType Ŀ������
     * @param resultSet ���ת�����ֵ�� Set ����
     * @param includeNull �Ƿ����� <code>null</code> ��ӵ� resultSet ������
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

        if (paramValue instanceof Object[]) { // ����
            for (Object obj : (Object[]) paramValue) {
                processParameterValue(obj, destType, resultSet, includeNull);
            }
        } else if (paramValue instanceof Iterable) { // �ɵ������󣨼��ϵȣ�
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
     * �ж�ָ�����ֶ��Ƿ�ʹ���� &#64;Transient ע��
     * 
     * @param field �ֶζ���
     * @return �ֶλ��ֶζ�Ӧ�� get��set ����ʹ���� &#64;Transient ע��ʱ���� <code>true</code>��
     *         ���򷵻� <code>false</code>
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
     * ��ȡ��ָ������������������ת��Ϊ�ַ����� SQL ���
     * 
     * @param columnName �������͵����ݿ�����
     * @param patternOfJava ��Ҫת�������ڸ�ʽ�� Java �б�ʾ��ʽ���磺 yyyy-MM-dd HH:mm:ss��
     *            ��μ� {@link DateUtil#SUPPORT_FORMATS}
     * @return SQL ��䣬�磺<br>
     *         ʹ�� Oracle ���ݿ�ʱ����Ϊ to_char(COLUMN_NAME, 'YYYY-MM-DD HH24:MI:SS')��<br>
     *         ʹ�� MySQL ���ݿ�ʱ����Ϊ date_format(COLUMN_NAME,'%Y-%m-%d %T')
     */
    protected String getDate2StringSQL(String columnName, String patternOfJava, Dialect dialect) {
        String function; // ������ת��Ϊ�ַ��������ú�������
        if (dialect instanceof Oracle8iDialect || dialect instanceof Oracle9iDialect) { // Oracle
            function = "to_char";
        } else if (dialect instanceof MySQLDialect) { // MySQL
            function = "date_format";
        } else {
            throw new RuntimeException("��ȡ���ݿ�Ľ�����ת�����ַ����ĺ�������ʧ�ܣ�Gboat ƽ̨Ŀǰֻ֧�� Oracle �� MySQL ���ݿ�");
        }
        return function + "(" + columnName + ", '"
                + getDateFormatePattern(DateFormatUtils.format(new Date(), patternOfJava), dialect) + "')";
    }

    /**
     * ��ȡ��ָ���������ַ���ת�������ݿ��е����ڶ���� SQL ���
     * 
     * @param dateString �����ַ������磺 2014-12-01 �� 2014-12-01 18:01:47
     * @return SQL ��䣬�磺<br>
     *         ʹ�� Oracle ���ݿ�ʱ����Ϊ to_date(dateString, 'YYYY-MM-DD HH24:MI:SS')��<br>
     *         ʹ�� MySQL ���ݿ�ʱ����Ϊ str_to_date(dateString,'%Y-%m-%d %T')
     */
    protected String getString2DateSQL(String dateString, Dialect dialect) {
        String function; // ���ַ���ת��Ϊ���ڵ����ú�������
        if (dialect instanceof Oracle8iDialect || dialect instanceof Oracle9iDialect) { // Oracle
            function = "to_date";
        } else if (dialect instanceof MySQLDialect) { // MySQL
            function = "str_to_date";
        } else {
            throw new RuntimeException("��ȡ���ݿ�Ľ��ַ���ת�������ڵĺ�������ʧ�ܣ�Gboat ƽ̨Ŀǰֻ֧�� Oracle �� MySQL ���ݿ�");
        }
        return function + "('" + dateString + "', '" + getDateFormatePattern(dateString, dialect) + "')";
    }

    /**
     * ��ȡ���ݿ����õĲ�ѯ��ǰϵͳʱ��Ĺؼ���
     * 
     * @return ���ݿ����õĲ�ѯ��ǰϵͳʱ��Ĺؼ��ʣ��磺 Oracle ���� current_timestamp�� MySQL ����
     *         current_date
     */
    protected String getCurrentTimeFuncName(Dialect dialect) {
        return dialect.getCurrentTimestampSQLFunctionName();
    }

    /**
     * �������ݿⷽ�Ժ�ʾ�������ַ�����ȡͨ�����ݿ����õ�����ת�ַ������������ڸ�ʽ
     * @param dateString ʾ�������ַ������磺2014-10-18 12:06:53
     * @param dialect ���ݿⷽ��
     * @return ���ض�Ӧ�����ڸ�ʽ���磺 ʹ�� Oracle ���ݿ�ʱ���ܷ��� YYYY-MM-DD HH24:MI:SS�� ʹ�� MySQL ���ݿ�ʱ���ܷ��� %Y-%m-%d %T
     */
    protected String getDateFormatePattern(String dateString, Dialect dialect) {
        if(dialect instanceof Oracle8iDialect || dialect instanceof Oracle9iDialect) { // Oracle
            return getDateFormatForOracle(dateString);
        }
   
        if(dialect instanceof MySQLDialect) { // MySQL
            return getDateFormatForMysql(dateString);
        }
         
        throw new RuntimeException("��ȡ���ݿ����õ�����ת�ַ�����������Ҫ�����ڸ�ʽʧ�ܣ�Gboat ƽ̨Ŀǰֻ֧�� Oracle �� MySQL ���ݿ�");
    }

    /**
     * ���������ַ�����ȡ�������ַ���ת���� Oracle ���ڵ����ڸ�ʽ
     * @param value �����ַ������磺 <code>2014-12-01 12:03:45</code>
     * @return ת���� Oracle ���ڵ����ڸ�ʽ
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
     * ���������ַ�����ȡ�������ַ���ת���� MySQL ���ڵ����ڸ�ʽ
     * @param value �����ַ������磺 <code>2014-12-01 12:03:45</code>
     * @return ת���� MySQL ���ڵ����ڸ�ʽ
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
