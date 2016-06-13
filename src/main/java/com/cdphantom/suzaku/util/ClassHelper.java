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
    
    /** Java �����Ķ��������ļ��ĺ�׺�� */
    public static final String CLASS_SUFFIX = ".class";
    
    /** ʵ���� {@link java.io.Serializable} �ӿڵ��ඨ�����л��汾Ψһ��ʶ���ֶε����� */
    public static final String SERIAL_VERSION_UID = "serialVersionUID";

    /**
     * ����ֶ��Ƿ������б������ˣ�������еݹ���ң�
     * 
     * @param className ����
     * @param fieldName �ֶ����������ƴ�Сд����
     * @return �ֶ��ڸ����б������ˣ��򷵻� true�� ��֮�򷵻� false
     */
    public static boolean checkField(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            return checkField(clazz, fieldName);
        } catch(ClassNotFoundException e){
            LOGGER.debug("�� [{}] �����ڡ�{}", className, e);
        }
        return false;
    }

    /**
     * ����ֶ��Ƿ������б������ˣ�������еݹ���ң�
     * 
     * @param clazz ����
     * @param fieldName �ֶ����������ƴ�Сд����
     * @return �ֶ��ڸ����б������ˣ��򷵻� true�� ��֮�򷵻� false
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
     * �����������ֶ�����ȡ�ֶζ��塣���������û���ҵ�����ݹ�������������н��в���
     * 
     * @param className ����
     * @param fieldName �ֶ���
     * @return ��Ӧ���ֶζ��塣�����Ӧ������ֶβ����ڣ��򷵻� null
     */
    public static Field getField(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            return getField(clazz, fieldName, true);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("�� [{}] �����ڡ� {}", className, e);
        }
        return null;
    }

    /**
     * �����ඨ����ֶ�����ȡ���ж�Ӧ���ֶζ��塣���������û���ҵ�����ݹ�������������н��в���
     * 
     * @param clazz �ඨ��
     * @param fieldName �ֶ�����
     * @return ��Ӧ���ֶζ��塣�����������в����ڶ�Ӧ���ֶΣ��򷵻� null
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        return getField(clazz, fieldName, true);
    }

    /**
     * �����ඨ����ֶ�����ȡ���ж�Ӧ���ֶζ��塣���������û���ҵ�����ݹ�������������н��в���
     * 
     * @param clazz �ඨ��
     * @param fieldName �ֶ�����
     * @param caseSensitive �Ƿ������ֶ����ƵĴ�Сд�����ֵΪ <code>true</code>����ʾ��Сд����
     * @return ��Ӧ���ֶζ��塣�����������в����ڶ�Ӧ���ֶΣ��򷵻� null
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
     * �����������ֶ������ֶ��������ִ�Сд����ȡ�ֶζ��塣���������û���ҵ�����ݹ�������������н��в���
     * 
     * @param className ����
     * @param fieldName �ֶ���
     * @return ��Ӧ���ֶζ��塣�����Ӧ������ֶβ����ڣ��򷵻� null
     */
    public static Field getFieldIgnoreCase(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            return getField(clazz, fieldName, false);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("�� [{}] �����ڡ� {}", className, e);
        }
        return null;
    }
    
    /**
     * �����ඨ����ֶ����������ִ�Сд����ȡ���ж�Ӧ���ֶζ��壨���������û���ҵ�����ݹ�������������н��в��ң�
     * 
     * @param clazz �ඨ��
     * @param fieldName �ֶ�����
     * @return ��Ӧ���ֶζ��塣�����������в����ڶ�Ӧ���ֶΣ��򷵻� null
     */
    public static Field getFieldIgnoreCase(Class<?> clazz, String fieldName) {
        return getField(clazz, fieldName, false);
    }

    /**
     * ��ȡ������б�������
     * 
     * @param className ����
     * @return ��ȡ�������е��ֶ�����
     */
    public static List<String> getFieldNames(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return getFieldNames(clazz);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("�� [{}] �����ڡ� {}", className, e);
        }

        return new LinkedList<String>();
    }

    /**
     * ��ȡ������б���
     * 
     * @param clazz ����
     * @return ��ȡ�������е���
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
     * �ݹ��ȡָ���༰�������������ж�����ֶΣ���Ա������
     * 
     * @param clazz ����
     * @return ������༰�������������ж�����ֶμ���
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
     * �ݹ��ȡָ���༰�������������ж�����ֶΣ���Ա������
     * 
     * @param clazz ����
     * @return ������༰�������������ж�����ֶε����Ƽ��ֶζ���ļ�ֵ��
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
     * ��ȡ��������������·����
     * 
     * @param clazz ����
     * @return ��ȡ����������������
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
     * �����ֶε����Ʋ����ֶε� get ������ �ȼ���Ƿ��в��������� getXxx() ���������û���ټ���Ƿ��в��������� isXxx() ����
     * @param clazz �ֶ����ڵ���
     * @param fieldName �ֶ�����
     * @return �ֶζ�Ӧ�� get ���������û�ж�Ӧ�� get �������򷵻� <code>null</code>
     */
    public static Method findGetMethod(Class<?> clazz, String fieldName) {
        Field field = getField(clazz, fieldName);
        if(field != null) {
            return findGetMethod(clazz, field);
        }
        
        String capitalizedName = WordUtils.capitalize(fieldName);
        Method getMethod = findMethod(clazz, "get" + capitalizedName);
        if (getMethod == null) { // boolean �����ֶε� get ����Ϊ isXxx() ����ʽ
            getMethod = findMethod(clazz, "is" + capitalizedName);
        }
        return getMethod;
    }

    /**
     * �����ֶε� get ������ �ȼ���Ƿ��в��������� getXxx() ���������û���ټ���Ƿ��в��������� isXxx() ����
     * @param clazz �ֶ����ڵ���
     * @param field �ֶ�
     * @return �ֶζ�Ӧ�� get ���������û�ж�Ӧ�� get �������򷵻� <code>null</code>
     */
    public static Method findGetMethod(Class<?> clazz, Field field) {
        String capitalizedName = WordUtils.capitalize(field.getName());
        Method getMethod = findMethod(clazz, "get" + capitalizedName);
        if (getMethod == null && (field.getType() == boolean.class || field.getType() == Boolean.class)) {
            // boolean �����ֶε� get ����Ϊ isXxx() ����ʽ
            getMethod = findMethod(clazz, "is" + capitalizedName);
        }
        return getMethod;
    }

    /**
     * �����ֶε� set ����
     * @param clazz �ֶ����ڵ���
     * @param field �ֶ�
     * @return �ֶζ�Ӧ�� set ���������û�ж�Ӧ�� set �������򷵻� <code>null</code>
     */
    public static Method findSetMethod(Class<?> clazz, Field field) {
        return findMethod(clazz, "set" + WordUtils.capitalize(field.getName()), field.getType());
    }

    /**
     * ��ȡָ�������µ�������
     * <p>
     * <b>ע�⣺</b><i>�˷���ֻ�ܻ�ȡ�ļ�ϵͳ����ͨ jar ����ָ�����е��࣬ �����Ҫ��ȡ OSGI Bundle
     * ��ָ�����µ���������ʹ��</i>
     * {@link gboat2.base.bridge.util.BundleUtil#getClasses(String, org.osgi.framework.Bundle)}
     * </p>
     * 
     * @param packageName ����
     * @return ���µ������࣬����������ڻ����û���κ��࣬�򷵻�һ�� size=0 �� List
     */
    public static List<Class<?>> getClassList(String packageName) {
        return getClassList(packageName, false);
    }
    
    /**
     * ��ȡָ�������µ�������
     * <p>
     * <b>ע�⣺</b><i>�˷���ֻ�ܻ�ȡ�ļ�ϵͳ����ͨ jar ����ָ�����е��࣬ �����Ҫ��ȡ OSGI Bundle
     * ��ָ�����µ���������ʹ��</i>
     * {@link gboat2.base.bridge.util.BundleUtil#getClasses(String, org.osgi.framework.Bundle)}
     * </p>
     * 
     * @param packageName ����
     * @param isRecursive �Ƿ�ݹ�
     * @return ���µ������࣬����������ڻ����û���κ��࣬�򷵻�һ�� size=0 �� List
     */
    public static List<Class<?>> getClassList(String packageName, boolean isRecursive) {
        return getClassList(packageName, isRecursive, null);
    }

    /**
     * ��ȡָ�������µ�������
     * <p>
     * <b>ע�⣺</b><i>�˷���ֻ�ܻ�ȡ�ļ�ϵͳ����ͨ jar ����ָ�����е��࣬ �����Ҫ��ȡ OSGI Bundle
     * ��ָ�����µ���������ʹ��</i>
     * {@link gboat2.base.bridge.util.BundleUtil#getClasses(String, org.osgi.framework.Bundle)}
     * </p>
     * 
     * @param packageName ����
     * @param isRecursive �Ƿ�ݹ�
     * @param classLoader ����Ӧ���������
     * @return ���µ������࣬����������ڻ����û���κ��࣬�򷵻�һ�� size=0 �� List
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
                            classList.addAll(getClassList(jarEntryName.replace('/', '.'), isRecursive, classLoader)); // �ݹ�
                        } else if (StringUtils.endsWith(jarEntryName, CLASS_SUFFIX)) {
                            String className = StringUtils.removeEnd(jarEntryName, CLASS_SUFFIX).replace('/', '.');
                            if (isRecursive || packageName.equals(StringUtils.substringBeforeLast(className, "."))) {
                                classList.add(Class.forName(className));
                            }
                        }
                    }
                } else {
                    throw new Exception("��ȡ java �� [" + packageName + "] �µ�������ʱ����Դ [" + url + "] ��Э�� [" + url + "] ����֧�֡�");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("��ȡ java �� [" + packageName + "] �µ������෢������", e);
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
                    throw new RuntimeException("Ŀ¼ [" + file.getParent() + "] �²����� java �� [" + className + "]", e);
                }
            } else if (isRecursive) {
                addClass(classList, file.getAbsolutePath(), (StringUtils.isNotEmpty(packageName) ? fileName
                        : packageName + "." + fileName), isRecursive);
            }
        }
    }
}
