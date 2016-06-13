package com.cdphantom.suzaku.util;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassHelper {
private static final Logger LOGGER = LoggerFactory.getLogger(ClassHelper.class.getName());
    
    /** Java 编译后的二进制类文件的后缀名 */
    public static final String CLASS_SUFFIX = ".class";
    
    /** 实现了 {@link java.io.Serializable} 接口的类定义序列化版本唯一标识的字段的名称 */
    public static final String SERIAL_VERSION_UID = "serialVersionUID";

    /**
     * 检查字段是否在类中被声明了（不会进行递归查找）
     * 
     * @param className 类名
     * @param fieldName 字段名，该名称大小写敏感
     * @return 字段在该类中被声明了，则返回 true， 反之则返回 false
     */
    public static boolean checkField(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            return checkField(clazz, fieldName);
        } catch(ClassNotFoundException e){
            LOGGER.debug("类 [{}] 不存在。{}", className, e);
        }
        return false;
    }

    /**
     * 检查字段是否在类中被声明了（不会进行递归查找）
     * 
     * @param clazz 类名
     * @param fieldName 字段名，该名称大小写敏感
     * @return 字段在该类中被声明了，则返回 true， 反之则返回 false
     */
    public static boolean checkField(Class<?> clazz, String fieldName) {
        try {
            return (clazz.getDeclaredField(fieldName) != null);
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    /**
     * 根据类名称字段名获取字段定义。如果本类中没有找到，会递归从所有祖先类中进行查找
     * 
     * @param className 类名
     * @param fieldName 字段名
     * @return 对应的字段定义。如果对应的类或字段不存在，则返回 null
     */
    public static Field getField(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            return getField(clazz, fieldName, true);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("类 [{}] 不存在。 {}", className, e);
        }
        return null;
    }

    /**
     * 根据类定义和字段名获取类中对应的字段定义。如果本类中没有找到，会递归从所有祖先类中进行查找
     * 
     * @param clazz 类定义
     * @param fieldName 字段名称
     * @return 对应的字段定义。如果传入的类中不存在对应的字段，则返回 null
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        return getField(clazz, fieldName, true);
    }

    /**
     * 根据类定义和字段名获取类中对应的字段定义。如果本类中没有找到，会递归从所有祖先类中进行查找
     * 
     * @param clazz 类定义
     * @param fieldName 字段名称
     * @param caseSensitive 是否区分字段名称的大小写，如果值为 <code>true</code>，表示大小写敏感
     * @return 对应的字段定义。如果传入的类中不存在对应的字段，则返回 null
     */
    public static Field getField(Class<?> clazz, String fieldName, boolean caseSensitive) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            List<Field> fields = getFields(clazz);
            for (Field field : fields) {
                if (field.getName().equals(fieldName)
                        || (!caseSensitive && field.getName().equalsIgnoreCase(fieldName))) {
                    return field;
                }
            }
        }
        return null;
    }
    
    /**
     * 根据类名称字段名（字段名不区分大小写）获取字段定义。如果本类中没有找到，会递归从所有祖先类中进行查找
     * 
     * @param className 类名
     * @param fieldName 字段名
     * @return 对应的字段定义。如果对应的类或字段不存在，则返回 null
     */
    public static Field getFieldIgnoreCase(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            return getField(clazz, fieldName, false);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("类 [{}] 不存在。 {}", className, e);
        }
        return null;
    }
    
    /**
     * 根据类定义和字段名（不区分大小写）获取类中对应的字段定义（如果本类中没有找到，会递归从所有祖先类中进行查找）
     * 
     * @param clazz 类定义
     * @param fieldName 字段名称
     * @return 对应的字段定义。如果传入的类中不存在对应的字段，则返回 null
     */
    public static Field getFieldIgnoreCase(Class<?> clazz, String fieldName) {
        return getField(clazz, fieldName, false);
    }

    /**
     * 获取类的所有变量名称
     * 
     * @param className 类名
     * @return 获取类中所有的字段名称
     */
    public static List<String> getFieldNames(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return getFieldNames(clazz);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("类 [{}] 不存在。 {}", className, e);
        }

        return new LinkedList<String>();
    }

    /**
     * 获取类的所有变量
     * 
     * @param clazz 类名
     * @return 获取类中所有的域
     */
    public static List<String> getFieldNames(Class<?> clazz) {
        List<String> fieldNames = new LinkedList<String>();
        List<Field> fields = getFields(clazz);
        for (Field field : fields) {
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    /**
     * 递归获取指定类及其所有祖先类中定义的字段（成员变量）
     * 
     * @param clazz 类名
     * @return 传入的类及其所有祖先类中定义的字段集合
     */
    public static List<Field> getFields(Class<?> clazz) {
        List<Field> returnFields = new ArrayList<Field>();
        while (clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            Collections.addAll(returnFields, fields);
            clazz = clazz.getSuperclass();
        }
        return returnFields;
    }

    /**
     * 递归获取指定类及其所有祖先类中定义的字段（成员变量）
     * 
     * @param clazz 类名
     * @return 传入的类及其所有祖先类中定义的字段的名称及字段定义的键值对
     */
    public static Map<String, Field> getFieldMap(Class<?> clazz) {
        List<Field> fields = getFields(clazz);
        Map<String, Field> fieldMap = new LinkedHashMap<String, Field>(fields.size());
        for (Field field : fields) {
            fieldMap.put(field.getName(), field);
        }
        return fieldMap;
    }

    /**
     * 读取类名（不包含包路径）
     * 
     * @param clazz 类名
     * @return 获取不包含包名的类名
     */
    public static String getNameWithoutPackage(Class<?> clazz) {
        Package pkg = clazz.getPackage();
        return (pkg == null ? clazz.getName() : StringUtils.removeStart(clazz.getName(), pkg.getName() + "."));
    }
    
    /**
     * Attempt to find a {@link Method} on the supplied class with the supplied name
     * and no parameters. Searches all superclasses up to {@code Object}.
     * <p>Returns {@code null} if no {@link Method} can be found.
     * @param clazz the class to introspect
     * @param name the name of the method
     * @return the Method object, or {@code null} if none found
     */
    public static Method findMethod(Class<?> clazz, String name) {
        return findMethod(clazz, name, new Class[0]);
    }
    
    /**
     * Attempt to find a {@link Method} on the supplied class with the supplied name
     * and parameter types. Searches all superclasses up to {@code Object}.
     * <p>Returns {@code null} if no {@link Method} can be found.
     * @param clazz the class to introspect
     * @param name the name of the method
     * @param paramTypes the parameter types of the method
     * (may be {@code null} to indicate any signature)
     * @return the Method object, or {@code null} if none found
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> searchType = clazz;
        while (searchType != null) {
            Method[] methods = (searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods());
            for (Method method : methods) {
                if (name.equals(method.getName())
                        && (paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * 根据字段的名称查找字段的 get 方法： 先检查是否有不带参数的 getXxx() 方法，如果没有再检查是否有不带参数的 isXxx() 方法
     * @param clazz 字段所在的类
     * @param fieldName 字段名称
     * @return 字段对应的 get 方法，如果没有对应的 get 方法，则返回 <code>null</code>
     */
    public static Method findGetMethod(Class<?> clazz, String fieldName) {
        Field field = getField(clazz, fieldName);
        if(field != null) {
            return findGetMethod(clazz, field);
        }
        
        String capitalizedName = WordUtils.capitalize(fieldName);
        Method getMethod = findMethod(clazz, "get" + capitalizedName);
        if (getMethod == null) { // boolean 类型字段的 get 方法为 isXxx() 的形式
            getMethod = findMethod(clazz, "is" + capitalizedName);
        }
        return getMethod;
    }

    /**
     * 查找字段的 get 方法： 先检查是否有不带参数的 getXxx() 方法，如果没有再检查是否有不带参数的 isXxx() 方法
     * @param clazz 字段所在的类
     * @param field 字段
     * @return 字段对应的 get 方法，如果没有对应的 get 方法，则返回 <code>null</code>
     */
    public static Method findGetMethod(Class<?> clazz, Field field) {
        String capitalizedName = WordUtils.capitalize(field.getName());
        Method getMethod = findMethod(clazz, "get" + capitalizedName);
        if (getMethod == null && (field.getType() == boolean.class || field.getType() == Boolean.class)) {
            // boolean 类型字段的 get 方法为 isXxx() 的形式
            getMethod = findMethod(clazz, "is" + capitalizedName);
        }
        return getMethod;
    }

    /**
     * 查找字段的 set 方法
     * @param clazz 字段所在的类
     * @param field 字段
     * @return 字段对应的 set 方法，如果没有对应的 set 方法，则返回 <code>null</code>
     */
    public static Method findSetMethod(Class<?> clazz, Field field) {
        return findMethod(clazz, "set" + WordUtils.capitalize(field.getName()), field.getType());
    }

    /**
     * 获取指定包名下的所有类
     * <p>
     * <b>注意：</b><i>此方法只能获取文件系统和普通 jar 包中指定包中的类， 如果需要获取 OSGI Bundle
     * 中指定包下的所有类请使用</i>
     * {@link gboat2.base.bridge.util.BundleUtil#getClasses(String, org.osgi.framework.Bundle)}
     * </p>
     * 
     * @param packageName 包名
     * @return 包下的所有类，如果包不存在或包下没有任何类，则返回一个 size=0 的 List
     */
    public static List<Class<?>> getClassList(String packageName) {
        return getClassList(packageName, false);
    }
    
    /**
     * 获取指定包名下的所有类
     * <p>
     * <b>注意：</b><i>此方法只能获取文件系统和普通 jar 包中指定包中的类， 如果需要获取 OSGI Bundle
     * 中指定包下的所有类请使用</i>
     * {@link gboat2.base.bridge.util.BundleUtil#getClasses(String, org.osgi.framework.Bundle)}
     * </p>
     * 
     * @param packageName 包名
     * @param isRecursive 是否递归
     * @return 包下的所有类，如果包不存在或包下没有任何类，则返回一个 size=0 的 List
     */
    public static List<Class<?>> getClassList(String packageName, boolean isRecursive) {
        return getClassList(packageName, isRecursive, null);
    }

    /**
     * 获取指定包名下的所有类
     * <p>
     * <b>注意：</b><i>此方法只能获取文件系统和普通 jar 包中指定包中的类， 如果需要获取 OSGI Bundle
     * 中指定包下的所有类请使用</i>
     * {@link gboat2.base.bridge.util.BundleUtil#getClasses(String, org.osgi.framework.Bundle)}
     * </p>
     * 
     * @param packageName 包名
     * @param isRecursive 是否递归
     * @param classLoader 包对应的类加载器
     * @return 包下的所有类，如果包不存在或包下没有任何类，则返回一个 size=0 的 List
     */
    public static List<Class<?>> getClassList(String packageName, boolean isRecursive, ClassLoader classLoader) {
        List<Class<?>> classList = new ArrayList<Class<?>>();
        packageName = StringUtils.trimToEmpty(packageName);
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        
        URL url = null;
        String protocol = null;
        try {
            Enumeration<URL> urls = classLoader.getResources(packageName.replace('.', '/'));
            while (urls != null && urls.hasMoreElements()) {
                url = urls.nextElement();
                if (url == null) {
                    continue;
                }
                
                protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    addClass(classList, url.getPath(), packageName, isRecursive);
                } else if ("jar".equals(protocol)) {
                    JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                    JarFile jarFile = jarURLConnection.getJarFile();
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry jarEntry = jarEntries.nextElement();
                        String jarEntryName = jarEntry.getName();
                        if(jarEntry.isDirectory() && isRecursive) {
                            classList.addAll(getClassList(jarEntryName.replace('/', '.'), isRecursive, classLoader)); // 递归
                        } else if (StringUtils.endsWith(jarEntryName, CLASS_SUFFIX)) {
                            String className = StringUtils.removeEnd(jarEntryName, CLASS_SUFFIX).replace('/', '.');
                            if (isRecursive || packageName.equals(StringUtils.substringBeforeLast(className, "."))) {
                                classList.add(Class.forName(className));
                            }
                        }
                    }
                } else {
                    throw new Exception("获取 java 包 [" + packageName + "] 下的所有类时，资源 [" + url + "] 的协议 [" + url + "] 不被支持。");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取 java 包 [" + packageName + "] 下的所有类发生错误。", e);
        }
        return classList;
    }
 
    private static void addClass(List<Class<?>> classList, String packagePath, String packageName, boolean isRecursive) {
        File[] files = new File(packagePath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (file.isFile() && file.getName().endsWith(CLASS_SUFFIX)) || file.isDirectory();
            }
        });
        if (files == null || files.length == 0) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();
            if (file.isFile()) {
                String className = StringUtils.removeEnd(fileName, CLASS_SUFFIX);
                if (StringUtils.isNotEmpty(packageName)) {
                    className = packageName + "." + className;
                }
                try {
                    classList.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("目录 [" + file.getParent() + "] 下不存在 java 类 [" + className + "]", e);
                }
            } else if (isRecursive) {
                addClass(classList, file.getAbsolutePath(), (StringUtils.isNotEmpty(packageName) ? fileName
                        : packageName + "." + fileName), isRecursive);
            }
        }
    }
}
