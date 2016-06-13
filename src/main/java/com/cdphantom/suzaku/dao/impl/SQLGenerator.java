package com.cdphantom.suzaku.dao.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.dialect.Dialect;

import com.cdphantom.suzaku.annotation.Relation;
import com.cdphantom.suzaku.annotation.Relations;
import com.cdphantom.suzaku.util.ClassHelper;
import com.cdphantom.suzaku.util.DateUtil;
import com.cdphantom.suzaku.annotation.Relation.DefaultBase;

public class SQLGenerator extends GenerateQL {

    private static final Map<String, String> TABLE_ALIASES = new HashMap<String, String>();

    /**
     * ��ȡ PO �� VO ����Ҫ��ѯ���������ֶ�����ӳ���ϵ
     * @param clazz PO �� VO ��Ķ���
     * @return Ҫ��ѯ���������ֶ�����ӳ���ϵ
     */
    public Map<String, Field> getQueryColumnFieldMap(Class<?> clazz) {
        Map<String, Field> resultMap = new LinkedHashMap<String, Field>();
        Field[] declaredFields = clazz.getDeclaredFields(); // �����������ֶ�
        Set<String> voFieldNames = new LinkedHashSet<String>(); // VO ���������ֶ����Ƽ���
        int mod;
        com.cdphantom.suzaku.annotation.Field fieldAnnotation;
        for (Field field : declaredFields) {
            mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || Modifier.isVolatile(mod)
                    || Modifier.isTransient(mod)) {
                continue; // ����� static, final, volatile, transient ���ֶΣ���ֱ������
            }

            voFieldNames.add(field.getName());
            fieldAnnotation = field.getAnnotation(com.cdphantom.suzaku.annotation.Field.class);
            if (fieldAnnotation != null && fieldAnnotation.query()) {
                resultMap.put(getColumnName(field), field);
            }
        }

        // ���� PO �ж�����ֶ�
        Class<?> poClass = clazz.getSuperclass();
        if (poClass.isAnnotationPresent(Entity.class)) {
            declaredFields = poClass.getDeclaredFields();
            for (Field field : declaredFields) {
                if (voFieldNames.contains(field.getName())) {
                    continue; // ����� VO �ж����ˣ���ǰ���Ѿ���������� ��ֱ������
                }

                mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || Modifier.isVolatile(mod)
                        || Modifier.isTransient(mod)) {
                    continue; // ����� static, final, volatile, transient ���ֶΣ���ֱ������
                }

                try {
                    resultMap.put(getColumnName(field), field);
                } catch (RuntimeException e) {
                    LOGGER.debug(field.getName() + " is @Transient.");
                }
            }
        }

        return resultMap;
    }

    @Override
    public String generateQueryLanguage(Class<?> resultType, Map<String, ?> params, Dialect dialect)
            throws RuntimeException {
        // ����Ƿ���VO��
        if (!resultType.isAnnotationPresent(Relations.class)) {
            throw new RuntimeException(resultType.getName() + " isn't assigned with @" + Relations.class.getName());
        }

        return super.generateQueryLanguage(resultType, params, dialect);
    }

    @Override
    protected String getQueryColumns(Class<?> resultType) {
        StringBuffer columns = new StringBuffer(); // Ҫ��ѯ���ֶΣ��ֶ�֮��ʹ��Ӣ�Ķ��ŷָ���
        Map<String, Field> columnFieldMap = getQueryColumnFieldMap(resultType);
        for (Entry<String, Field> entry : columnFieldMap.entrySet()) {
            columns.append(entry.getKey()).append(" as ").append(entry.getValue().getName()).append(", ");
        }
        // ɾ��ĩβ�� ��, �� �󷵻�
        return columns.delete(columns.length() - 2, columns.length()).toString();
    }

    @Override
    protected String getQueryTableName(Class<?> resultType, Map<String, ?> params, Dialect dialect) {
        StringBuffer result = new StringBuffer();
        Class<?> basePoClass = resultType.getAnnotation(Relations.class).base();
        result.append(getTableName(basePoClass)).append(" ").append(getTableAlias(basePoClass))
                .append(getOnCondition(resultType, params, dialect));
        return result.toString();
    }

    @Override
    protected String getColumnName(Field field) throws RuntimeException {
        Class<?> poClass = null;
        Field fieldOfPO = null;
        Class<?> declaringClass = field.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(Entity.class)) { // PO
            poClass = declaringClass;
            fieldOfPO = field;
        } else if (declaringClass.isAnnotationPresent(Relations.class)
                || declaringClass.isAnnotationPresent(Relation.class)) { // VO
            com.cdphantom.suzaku.annotation.Field fieldAnnotation = field
                    .getAnnotation(com.cdphantom.suzaku.annotation.Field.class);
            if (fieldAnnotation == null) {
                // ������ֶ�û��ʹ�� @gboat2.base.core.annotation.Field ��ע
                throw new RuntimeException("VO �� [" + declaringClass.getName() + "] ���ֶ� [" + field.getName()
                        + "] û��ʹ��ע�� @" + com.cdphantom.suzaku.annotation.Field.class.getName() + " ���б�ע��������Ը��ֶν��в�ѯ");
            }

            poClass = fieldAnnotation.clazz();
            fieldOfPO = ClassHelper.getField(poClass,
                    StringUtils.defaultIfBlank(fieldAnnotation.column(), field.getName()));
            if (fieldOfPO == null) {
                throw new RuntimeException("VO �� [" + declaringClass + "] �е��ֶ� [" + field.getName()
                        + "] �����Ӧ�� PO �� [" + poClass.getName()
                        + "] ��û���ҵ�ƥ����ֶζ��壬�뽫 VO �� PO ���е��ֶ����ƵĴ�Сд����һ�£���ͨ�� @Field ע��� column ������ȷָ������ PO �ж�Ӧ���ֶΡ�");
            }
        } else {
            throw new RuntimeException("�ֶ� [" + field.getName() + "] �������� [" + declaringClass.getName()
                    + "] �Ȳ��� PO�� �ֲ��� VO��������Ը��ֶν��в�ѯ");
        }

        String fieldName = fieldOfPO.getName();
        if (isTransient(fieldOfPO)) {
            throw new RuntimeException("�� [" + poClass.getName() + "] �е��ֶ� [" + fieldName + "] ����עΪ @"
                    + Transient.class.getName());
        }

        // ����ֶζ����Ƿ�ʹ���� @Column ע��
        Column column = fieldOfPO.getAnnotation(Column.class);
        if (column == null) {
            // ��� get �����Ƿ������� @Column
            Method getMethod = ClassHelper.findGetMethod(poClass, fieldOfPO);
            if (getMethod != null) {
                column = getMethod.getAnnotation(Column.class);
            }
        }

        if (column == null) {
            // ��� set �����Ƿ������� @Column
            Method setMethod = ClassHelper.findSetMethod(poClass, fieldOfPO);
            if (setMethod != null) {
                column = setMethod.getAnnotation(Column.class);
            }
        }

        if (column == null) {
            throw new RuntimeException("can't parse database column name for " + poClass.getName() + "'s "
                    + fieldName);
        }

        String dbColumn = StringUtils.defaultIfBlank(column.name(), fieldName);
        return getTableAlias(poClass) + "." + dbColumn;
    }
    
    @Override
    protected String getQueryOrderby(Object orderbyValue, Class<?> resultType) {
        String orderby = super.getQueryOrderby(orderbyValue, resultType);
        StringBuffer result = new StringBuffer(orderby.length());
        Matcher matcher = Pattern.compile("\\w+\\.\\w+").matcher(orderby);
        while (matcher.find()) {
            matcher.appendReplacement(result, fieldName2ColumnName(matcher.group(), resultType));
            
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * ����ѯ������ _block ��Ӧ�� SQL ����е� [fieldName] �滻�� COLUMN_NAME��fieldName Ϊ VO ��
     * PO �е��ֶ����� COLUMN_NAME Ϊ���ݱ��е�������
     * 
     * @param block SQL ���飬 VO ���е��ֶ����� [] ��Χ���磺 [loginId] is null or [loginId]=''
     * @param voClass VO �ඨ��
     * @return ת�����ԭ�� SQL ���
     */
    @Override
    protected String processBlockValue(Object block, Class<?> recordType) {
        String strValue = super.processBlockValue(block, recordType);
        StringBuffer result = new StringBuffer();

        String fieldName;
        Field field;
        String columnName;
        // ���� ��[����.�ֶ���]�� �� ��[�ֶ���]��
        Matcher matcher = Pattern.compile("\\[\\s*(([a-zA-Z]\\w*\\.)?[a-zA-Z]\\w*)\\s*\\]").matcher(strValue);
        while (matcher.find()) {
            fieldName = matcher.group(1);
            if (fieldName.contains(".")) { // ͬʱָ�����������ֶ���
                columnName = fieldName2ColumnName(fieldName, recordType);
            } else {
                field = ClassHelper.getField(recordType, fieldName);
                if (field == null) {
                    throw new RuntimeException("���� [" + PARAM_BLOCK + "] ָ�����ֶ� [" + fieldName + "] �� VO �� ["
                            + recordType.getName() + "] �в���");
                }
                columnName = getColumnName(field);
            }
            matcher.appendReplacement(result, columnName);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * ���� PO ���ඨ���ȡ PO ��Ӧ�ı�����
     * 
     * @param poClass ʹ���� &#64;Table ע��� PO �ඨ��
     * @return �����ƣ��磺 gboat.G2_T_USER �� G2_T_USER
     */
    private String getTableName(Class<?> poClass) {
        Table table = poClass.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("�� [" + poClass.getName() + "] û��ʹ�� @" + Table.class.getName()
                    + " ע�⣬�޷���ȡ���Ӧ�����ݱ�����");
        }

        StringBuffer tablename = new StringBuffer();
        if (StringUtils.isNotBlank(table.catalog())) {
            tablename.append(table.catalog().trim()).append(".");
        }
        if (StringUtils.isNotBlank(table.schema())) {
            tablename.append(table.schema().trim()).append(".");
        }
        return tablename.append(table.name()).toString();
    }

    /**
     * ���� PO ���ȡ���ڲ�ѯ SQL �ж�Ӧ�ı���
     * 
     * @param poClass PO �ඨ��
     * @return SQL ����еı����
     */
    private String getTableAlias(Class<?> poClass) {
        String key = poClass.getName();
        String alias = TABLE_ALIASES.get(key);
        if (alias == null) {
            alias = poClass.getSimpleName() + "0_";
            TABLE_ALIASES.put(key, alias);
        }
        return alias;
    }

    private String getOnCondition(Class<?> voClass, Map<String, ?> params, Dialect dialect)
            throws RuntimeException {
        StringBuffer result = new StringBuffer();
        Relations relations = voClass.getAnnotation(Relations.class);
        Class<?> basePo = relations.base();
        Relation[] relationArray = relations.value();
        Relation relation = null;
        
        Class<?> baseClass = null;
        String[] baseColumns = null;
        Field baseField = null;
        
        Class<?> referClass = null;
        String[] referColumns = null;
        Field referField = null;
        
        for (int i = 0; i < relationArray.length; i++) {
            relation = relationArray[i];
            // ��� @Relation û�ж��� base ���ԣ���ʹ�� @Relations ����� base ��Ϊ������
            baseClass = (relation.base() == Relation.DefaultBase.class ? basePo : relation.base());
            baseColumns = relation.baseColumn();
            referClass = relation.refer();
            referColumns = relation.referColumn();

            result.append(" ").append(relation.type().name()).append(" join ").append(getTableName(referClass))
                    .append(" ").append(getTableAlias(referClass)).append(" on ");

            if (StringUtils.isNotBlank(relation.on())) {
                result.append(processOn(relation.on(), baseClass, voClass, params, dialect));
            } else {
                if (baseColumns.length != referColumns.length) {
                    throw new RuntimeException("VO �� [" + voClass.getName() + "] �е� [" + (i + 1)
                            + "] �� @Relation ע��� baseColumns ����ֵ�� referColumns ����ֵ�ĳ��ȱ������");
                }
                for (int j = 0; j < baseColumns.length; j++) {
                    if (StringUtils.isBlank(baseColumns[j])) {
                        throw new RuntimeException("VO �� [" + voClass.getName() + "] �е� [" + (i + 1)
                                + "] �� @Relation ע��� baseColumn ���Եĵ� [" + (j + 1) + "] ��ֵΪ���ַ���");
                    }
                    if (StringUtils.isBlank(referColumns[j])) {
                        throw new RuntimeException("VO �� [" + voClass.getName() + "] �е� [" + (i + 1)
                                + "] �� @Relation ע��� referColumn ���Եĵ� [" + (j + 1) + "] ��ֵΪ���ַ���");
                    }

                    baseField = ClassHelper.getField(baseClass, StringUtils.trim(baseColumns[j]));
                    if (baseField == null) {
                        throw new RuntimeException("VO �� [" + voClass.getName() + "] �е� [" + (i + 1)
                                + "] �� @Relation ע��� baseColumn ���Եĵ� [" + (j + 1) + "] ��ֵ [" + baseColumns[j]
                                + "] �� PO [" + baseClass.getName() + "] ��û���ҵ���Ӧ���ֶ�");
                    }

                    referField = ClassHelper.getField(referClass, StringUtils.trim(referColumns[j]));
                    if (referField == null) {
                        throw new RuntimeException("VO �� [" + voClass.getName() + "] �е� [" + (i + 1)
                                + "] �� @Relation ע��� referColumn ���Եĵ� [" + (j + 1) + "] ��ֵ [" + referColumns[j]
                                + "] �� PO [" + referClass.getName() + "] ��û���ҵ���Ӧ���ֶ�");
                    }

                    if (j > 0) {
                        result.append(" and ");
                    }
                    result.append(getColumnName(baseField)).append("=").append(getColumnName(referField));
                }
            }
        }
  
        return result.toString();
    }
    
    private String processOn(String on, Class<?> base, Class<?> voClass, Map<String, ?> params, Dialect dialect) {
        StringBuffer result = new StringBuffer();
        // ���� on �ж�����ֶΣ��ֶβ��� [] ����������ʽ����Ϊ����������������.������������ָ������Ĭ��Ϊ Relation �� base ��
        Matcher matcher = Pattern.compile("\\[\\s*(([a-zA-Z]\\w*\\.)?[a-zA-Z]\\w*)\\s*\\]").matcher(on);
        String fieldName; // �ֶ�����
        Field field; // �ֶζ���
        String columnName;
        while (matcher.find()) {
            fieldName = matcher.group(1);
            if (fieldName.contains(".")) { // ͬʱָ�����������ֶ���
                columnName = fieldName2ColumnName(fieldName, voClass);
            } else {
                field = ClassHelper.getField(base, fieldName);
                if (field == null) {
                    throw new RuntimeException("VO �� [" + voClass.getName() + "] �� @Relation ע��� on ����ֵ [" + on
                            + "] ��ָ�����ֶ��� [" + fieldName + "] �� PO �� [" + base.getName() + "] �в�����");
                }
                columnName = getColumnName(field);
            }
            matcher.appendReplacement(result, columnName);
        }
        matcher.appendTail(result);

        StringBuffer sql = new StringBuffer();
        // on ������ϵ�еı�����ʽΪ ��{��������}��
        Matcher matcher2 = Pattern.compile("\\{(\\s*[a-zA-Z]\\w*\\s*)\\}").matcher(result.toString());
        String variableName;
        Object value;
        String valueStr;
        while (matcher2.find()) {
            variableName = matcher2.group(1);
            value = params.get(variableName);
            if (value == null) {
                throw new RuntimeException("VO �� [" + voClass.getName() + "] �� @Relation ע��� on ����ֵ [" + on
                        + "] ��ָ���Ĳ��� [" + variableName + "] ��ֵΪ null");
            }

            if (value instanceof Number) { // ����
                valueStr = value.toString();
            } else if ((value instanceof Boolean) || (value instanceof Character) || (value instanceof CharSequence)
                    || (value instanceof Enum)) { // �������ַ�����ö�٣��õ����Ž�ֵ������
                valueStr = "'" + value + "'";
            } else if ((value instanceof Date) || (value instanceof Calendar)) {
                // ���ڡ�������ת�������ݿ��е����ڶ���
                Date date = ((value instanceof Calendar) ? ((Calendar) value).getTime() : (Date) value);
                valueStr = getString2DateSQL(DateUtil.format(date), dialect);
            } else {
                // XXX hemw �������Կ���ʵ�ֶ� java.lang.Iterable��java.util.Iterator ���������֧��
                throw new RuntimeException("@Relation ע��� on ����ֵ [" + on + "] ��ָ���Ĳ��� [" + variableName
                        + "] ��ֵΪ [" + value + "]��Ŀǰ����֧�����Ӧ���������� [" + value.getClass().getName() + "]");
            }
            matcher2.appendReplacement(sql, valueStr);
        }
        matcher2.appendTail(sql);
        return sql.toString();
    }

    /**
     * ���ֶ���ת���ɶ�Ӧ�����ݿ��ֶ���
     * 
     * @param name �ֶ�������ʽΪ��������д.�ֶ�����
     * @param voClass VO ��
     * @return ��Ӧ�����ݿ��ֶ���
     * @throws IllegalArgumentException ������� fieldName �ĸ�ʽ������Ҫ��ʱ�׳����쳣
     */
    private String fieldName2ColumnName(String name, Class<?> voClass) {
        String[] array = name.split("\\.");
        if (array.length != 2) {
            throw new IllegalArgumentException("�ֶ��� [" + name + "] �ĸ�ʽ����ȷ��Ӧ��Ϊ��������д.�ֶ�����");
        }

        String classSimpleName = array[0].trim();
        String fieldName = array[1].trim();
        if (classSimpleName.isEmpty() || fieldName.isEmpty()) {
            throw new IllegalArgumentException("�ֶ��� [" + name + "] �ĸ�ʽ����ȷ��Ӧ��Ϊ��������д.�ֶ�����");
        }

        Class<?> declaringClass = null;
        if (voClass.getSimpleName().equals(classSimpleName)) {
            declaringClass = voClass;
        } else {
            Relations relations = voClass.getAnnotation(Relations.class);
            if (relations.base().getSimpleName().equals(classSimpleName)) {
                declaringClass = relations.base();
            } else {
                Relation[] relationArray = relations.value();
                for (Relation relation : relationArray) {
                    if (relation.base() != DefaultBase.class && relation.base().getSimpleName().equals(classSimpleName)) {
                        declaringClass = relation.base();
                    } else if (relation.refer().getSimpleName().equals(classSimpleName)) {
                        declaringClass = relation.refer();
                    }

                    if (declaringClass != null) {
                        break;
                    }
                }
            }
        }

        if (declaringClass == null) {
            throw new RuntimeException("�ֶ��� [" + name + "] ��Ч�� ָ����ʵ���� [" + classSimpleName + "] û���� VO ["
                    + voClass.getName() + "] ����");
        }

        Field field = ClassHelper.getFieldIgnoreCase(declaringClass, fieldName);
        if (field == null) {
            throw new RuntimeException("�ֶ��� [" + name + "] ��Ч�� ʵ���� [" + declaringClass.getName()
                    + "] �в���������Ϊ [" + fieldName + "] ���ֶ�");
        }
        return getColumnName(field);
    }
}
