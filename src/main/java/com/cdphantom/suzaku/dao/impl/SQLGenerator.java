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
     * 获取 PO 或 VO 类中要查询的列名与字段名的映射关系
     * @param clazz PO 或 VO 类的定义
     * @return 要查询的列名与字段名的映射关系
     */
    public Map<String, Field> getQueryColumnFieldMap(Class<?> clazz) {
        Map<String, Field> resultMap = new LinkedHashMap<String, Field>();
        Field[] declaredFields = clazz.getDeclaredFields(); // 声明的所有字段
        Set<String> voFieldNames = new LinkedHashSet<String>(); // VO 类声明的字段名称集合
        int mod;
        com.cdphantom.suzaku.annotation.Field fieldAnnotation;
        for (Field field : declaredFields) {
            mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || Modifier.isVolatile(mod)
                    || Modifier.isTransient(mod)) {
                continue; // 如果是 static, final, volatile, transient 的字段，则直接跳过
            }

            voFieldNames.add(field.getName());
            fieldAnnotation = field.getAnnotation(com.cdphantom.suzaku.annotation.Field.class);
            if (fieldAnnotation != null && fieldAnnotation.query()) {
                resultMap.put(getColumnName(field), field);
            }
        }

        // 处理 PO 中定义的字段
        Class<?> poClass = clazz.getSuperclass();
        if (poClass.isAnnotationPresent(Entity.class)) {
            declaredFields = poClass.getDeclaredFields();
            for (Field field : declaredFields) {
                if (voFieldNames.contains(field.getName())) {
                    continue; // 如果在 VO 中定义了（在前面已经处理过）， 则直接跳过
                }

                mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || Modifier.isVolatile(mod)
                        || Modifier.isTransient(mod)) {
                    continue; // 如果是 static, final, volatile, transient 的字段，则直接跳过
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
        // 检查是否是VO类
        if (!resultType.isAnnotationPresent(Relations.class)) {
            throw new RuntimeException(resultType.getName() + " isn't assigned with @" + Relations.class.getName());
        }

        return super.generateQueryLanguage(resultType, params, dialect);
    }

    @Override
    protected String getQueryColumns(Class<?> resultType) {
        StringBuffer columns = new StringBuffer(); // 要查询的字段（字段之间使用英文逗号分隔）
        Map<String, Field> columnFieldMap = getQueryColumnFieldMap(resultType);
        for (Entry<String, Field> entry : columnFieldMap.entrySet()) {
            columns.append(entry.getKey()).append(" as ").append(entry.getValue().getName()).append(", ");
        }
        // 删除末尾的 “, ” 后返回
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
                // 传入的字段没有使用 @gboat2.base.core.annotation.Field 标注
                throw new RuntimeException("VO 类 [" + declaringClass.getName() + "] 的字段 [" + field.getName()
                        + "] 没有使用注解 @" + com.cdphantom.suzaku.annotation.Field.class.getName() + " 进行标注，不允许对该字段进行查询");
            }

            poClass = fieldAnnotation.clazz();
            fieldOfPO = ClassHelper.getField(poClass,
                    StringUtils.defaultIfBlank(fieldAnnotation.column(), field.getName()));
            if (fieldOfPO == null) {
                throw new RuntimeException("VO 类 [" + declaringClass + "] 中的字段 [" + field.getName()
                        + "] 在其对应的 PO 类 [" + poClass.getName()
                        + "] 中没有找到匹配的字段定义，请将 VO 和 PO 类中的字段名称的大小写保持一致，或通过 @Field 注解的 column 属性明确指定其在 PO 中对应的字段。");
            }
        } else {
            throw new RuntimeException("字段 [" + field.getName() + "] 的声明类 [" + declaringClass.getName()
                    + "] 既不是 PO， 又不是 VO，不允许对该字段进行查询");
        }

        String fieldName = fieldOfPO.getName();
        if (isTransient(fieldOfPO)) {
            throw new RuntimeException("类 [" + poClass.getName() + "] 中的字段 [" + fieldName + "] 被标注为 @"
                    + Transient.class.getName());
        }

        // 检查字段定义是否使用了 @Column 注解
        Column column = fieldOfPO.getAnnotation(Column.class);
        if (column == null) {
            // 检查 get 方法是否配置了 @Column
            Method getMethod = ClassHelper.findGetMethod(poClass, fieldOfPO);
            if (getMethod != null) {
                column = getMethod.getAnnotation(Column.class);
            }
        }

        if (column == null) {
            // 检查 set 方法是否配置了 @Column
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
     * 将查询条件中 _block 对应的 SQL 语句中的 [fieldName] 替换成 COLUMN_NAME（fieldName 为 VO 或
     * PO 中的字段名， COLUMN_NAME 为数据表中的列名）
     * 
     * @param block SQL 语句块， VO 类中的字段名用 [] 包围，如： [loginId] is null or [loginId]=''
     * @param voClass VO 类定义
     * @return 转换后的原生 SQL 语句
     */
    @Override
    protected String processBlockValue(Object block, Class<?> recordType) {
        String strValue = super.processBlockValue(block, recordType);
        StringBuffer result = new StringBuffer();

        String fieldName;
        Field field;
        String columnName;
        // 解析 “[类名.字段名]” 或 “[字段名]”
        Matcher matcher = Pattern.compile("\\[\\s*(([a-zA-Z]\\w*\\.)?[a-zA-Z]\\w*)\\s*\\]").matcher(strValue);
        while (matcher.find()) {
            fieldName = matcher.group(1);
            if (fieldName.contains(".")) { // 同时指定了类名和字段名
                columnName = fieldName2ColumnName(fieldName, recordType);
            } else {
                field = ClassHelper.getField(recordType, fieldName);
                if (field == null) {
                    throw new RuntimeException("参数 [" + PARAM_BLOCK + "] 指定的字段 [" + fieldName + "] 在 VO 类 ["
                            + recordType.getName() + "] 中不存");
                }
                columnName = getColumnName(field);
            }
            matcher.appendReplacement(result, columnName);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 根据 PO 的类定义获取 PO 对应的表名称
     * 
     * @param poClass 使用了 &#64;Table 注解的 PO 类定义
     * @return 表名称，如： gboat.G2_T_USER 或 G2_T_USER
     */
    private String getTableName(Class<?> poClass) {
        Table table = poClass.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("类 [" + poClass.getName() + "] 没有使用 @" + Table.class.getName()
                    + " 注解，无法获取其对应的数据表名称");
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
     * 根据 PO 类获取其在查询 SQL 中对应的别名
     * 
     * @param poClass PO 类定义
     * @return SQL 语句中的表别名
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
            // 如果 @Relation 没有定义 base 属性，则使用 @Relations 定义的 base 作为基础表
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
                    throw new RuntimeException("VO 类 [" + voClass.getName() + "] 中第 [" + (i + 1)
                            + "] 个 @Relation 注解的 baseColumns 属性值和 referColumns 属性值的长度必须相等");
                }
                for (int j = 0; j < baseColumns.length; j++) {
                    if (StringUtils.isBlank(baseColumns[j])) {
                        throw new RuntimeException("VO 类 [" + voClass.getName() + "] 中第 [" + (i + 1)
                                + "] 个 @Relation 注解的 baseColumn 属性的第 [" + (j + 1) + "] 个值为空字符串");
                    }
                    if (StringUtils.isBlank(referColumns[j])) {
                        throw new RuntimeException("VO 类 [" + voClass.getName() + "] 中第 [" + (i + 1)
                                + "] 个 @Relation 注解的 referColumn 属性的第 [" + (j + 1) + "] 个值为空字符串");
                    }

                    baseField = ClassHelper.getField(baseClass, StringUtils.trim(baseColumns[j]));
                    if (baseField == null) {
                        throw new RuntimeException("VO 类 [" + voClass.getName() + "] 中第 [" + (i + 1)
                                + "] 个 @Relation 注解的 baseColumn 属性的第 [" + (j + 1) + "] 个值 [" + baseColumns[j]
                                + "] 在 PO [" + baseClass.getName() + "] 中没有找到对应的字段");
                    }

                    referField = ClassHelper.getField(referClass, StringUtils.trim(referColumns[j]));
                    if (referField == null) {
                        throw new RuntimeException("VO 类 [" + voClass.getName() + "] 中第 [" + (i + 1)
                                + "] 个 @Relation 注解的 referColumn 属性的第 [" + (j + 1) + "] 个值 [" + referColumns[j]
                                + "] 在 PO [" + referClass.getName() + "] 中没有找到对应的字段");
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
        // 处理 on 中定义的字段，字段采用 [] 括起来，格式可以为“属性名”或“类名.属性名”，不指定类名默认为 Relation 中 base 类
        Matcher matcher = Pattern.compile("\\[\\s*(([a-zA-Z]\\w*\\.)?[a-zA-Z]\\w*)\\s*\\]").matcher(on);
        String fieldName; // 字段名称
        Field field; // 字段定义
        String columnName;
        while (matcher.find()) {
            fieldName = matcher.group(1);
            if (fieldName.contains(".")) { // 同时指定了类名和字段名
                columnName = fieldName2ColumnName(fieldName, voClass);
            } else {
                field = ClassHelper.getField(base, fieldName);
                if (field == null) {
                    throw new RuntimeException("VO 类 [" + voClass.getName() + "] 中 @Relation 注解的 on 属性值 [" + on
                            + "] 中指定的字段名 [" + fieldName + "] 在 PO 类 [" + base.getName() + "] 中不存在");
                }
                columnName = getColumnName(field);
            }
            matcher.appendReplacement(result, columnName);
        }
        matcher.appendTail(result);

        StringBuffer sql = new StringBuffer();
        // on 关联关系中的变量格式为 “{变量名称}”
        Matcher matcher2 = Pattern.compile("\\{(\\s*[a-zA-Z]\\w*\\s*)\\}").matcher(result.toString());
        String variableName;
        Object value;
        String valueStr;
        while (matcher2.find()) {
            variableName = matcher2.group(1);
            value = params.get(variableName);
            if (value == null) {
                throw new RuntimeException("VO 类 [" + voClass.getName() + "] 中 @Relation 注解的 on 属性值 [" + on
                        + "] 中指定的参数 [" + variableName + "] 的值为 null");
            }

            if (value instanceof Number) { // 数字
                valueStr = value.toString();
            } else if ((value instanceof Boolean) || (value instanceof Character) || (value instanceof CharSequence)
                    || (value instanceof Enum)) { // 布尔、字符串、枚举，用单引号将值包起来
                valueStr = "'" + value + "'";
            } else if ((value instanceof Date) || (value instanceof Calendar)) {
                // 日期、日历，转换成数据库中的日期对象
                Date date = ((value instanceof Calendar) ? ((Calendar) value).getTime() : (Date) value);
                valueStr = getString2DateSQL(DateUtil.format(date), dialect);
            } else {
                // 后续可以考虑实现对 java.lang.Iterable、java.util.Iterator 和数组进行支持
                throw new RuntimeException("@Relation 注解的 on 属性值 [" + on + "] 中指定的参数 [" + variableName
                        + "] 的值为 [" + value + "]，目前还不支持其对应的数据类型 [" + value.getClass().getName() + "]");
            }
            matcher2.appendReplacement(sql, valueStr);
        }
        matcher2.appendTail(sql);
        return sql.toString();
    }

    /**
     * 将字段名转换成对应的数据库字段名
     * 
     * @param name 字段名，格式为“类名简写.字段名”
     * @param voClass VO 类
     * @return 对应的数据库字段名
     * @throws IllegalArgumentException 当传入的 fieldName 的格式不符合要求时抛出该异常
     */
    private String fieldName2ColumnName(String name, Class<?> voClass) {
        String[] array = name.split("\\.");
        if (array.length != 2) {
            throw new IllegalArgumentException("字段名 [" + name + "] 的格式不正确，应该为“类名简写.字段名”");
        }

        String classSimpleName = array[0].trim();
        String fieldName = array[1].trim();
        if (classSimpleName.isEmpty() || fieldName.isEmpty()) {
            throw new IllegalArgumentException("字段名 [" + name + "] 的格式不正确，应该为“类名简写.字段名”");
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
            throw new RuntimeException("字段名 [" + name + "] 无效： 指定的实体类 [" + classSimpleName + "] 没有与 VO ["
                    + voClass.getName() + "] 关联");
        }

        Field field = ClassHelper.getFieldIgnoreCase(declaringClass, fieldName);
        if (field == null) {
            throw new RuntimeException("字段名 [" + name + "] 无效： 实体类 [" + declaringClass.getName()
                    + "] 中不存在名称为 [" + fieldName + "] 的字段");
        }
        return getColumnName(field);
    }
}