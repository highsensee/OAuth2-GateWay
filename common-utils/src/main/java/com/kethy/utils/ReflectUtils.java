package com.kethy.utils;

import com.kethy.lambda.SFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * es反射工具类
 *
 * @author andylau
 * 2023-02-20
 */
@SuppressWarnings("all")
@Slf4j
public class ReflectUtils {

    /**
     * 设置对象指定字段值
     */
    public static Object setFieldValue(Object entity, String fieldName, Object value) {
        Map<String, Field> fieldMap = getFieldMap(entity.getClass());
        try {
            Field field = fieldMap.get(fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Error: NoSuchField in %s for " + fieldName + ".");
            }
            field.setAccessible(true);
            field.set(entity, value);
            return entity;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error: Cannot read field in " + entity.getClass().getSimpleName() + ".  Cause:", e);
        }
    }

    /**
     * 获取对象中指定字段值
     */
    public static Object getFieldValue(Object entity, String fieldName) {
        Class<?> cls = entity.getClass();
        Map<String, Field> fieldMaps = getFieldMap(cls);
        try {
            Field field = fieldMaps.get(fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Error: NoSuchField in %s for " + fieldName + ".");
            }
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error: Cannot read field in " + cls.getSimpleName() + ".  Cause:", e);
        }
    }

    /**
     * 获取类中的字段  PropertyName - Field
     */
    private static Map<String, Field> getFieldMap(Class<?> clazz) {
        List<Field> fieldList = getFieldList(clazz);
        return CollectionUtils.isEmpty(fieldList) ?
                Collections.emptyMap() :
                fieldList.stream().collect(Collectors.toMap(Field::getName, (field) -> field));
    }

    /**
     * 不存在则向Map中设置key，存在则更新key
     */
    public static <K, V> V computeIfAbsent(Map<K, V> Map, K key, Function<K, V> function) {
        V v = Map.get(key);
        return v != null ? v : Map.computeIfAbsent(key, function);
    }

    /**
     * 获取类中所有的字段
     */
    public static List<Field> getFieldList(Class<?> clazz) {
        return Objects.isNull(clazz) ? Collections.emptyList() : computeIfAbsent(new HashMap<>(), clazz, (clz) -> {
            // 获取本类所有字段
            Field[] fields = clz.getDeclaredFields();
            List<Field> superFields = new ArrayList();
            // 循环获取子类父类中的属性
            for (Class currentClass = clz.getSuperclass(); currentClass != null; currentClass = currentClass.getSuperclass()) {
                Field[] declaredFields = currentClass.getDeclaredFields();
                Collections.addAll(superFields, declaredFields);
            }
            // 过滤重写的字段
            Map<String, Field> fieldMap = excludeOverrideSuperField(fields, superFields);
            return fieldMap.values().stream()
                    .filter((f) -> !Modifier.isStatic(f.getModifiers()))
                    .filter((f) -> !Modifier.isTransient(f.getModifiers()))
                    .collect(Collectors.toList());
        });
    }

    /**
     * 获取类中所有的字段名
     */
    public static List<String> getFieldNameList(Class<?> clazz) {
        List<Field> fieldList = getFieldList(clazz);
        return fieldList.stream().map(Field::getName).collect(Collectors.toList());
    }

    /**
     * 过滤字段数组中的重写字段
     */
    public static Map<String, Field> excludeOverrideSuperField(Field[] fields, List<Field> superFieldList) {
        // 抽取元素名、对应字段到Map中
        Map<String, Field> fieldMap = Stream.of(fields).collect(
                Collectors.toMap(Field::getName, Function.identity(),
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                        }, LinkedHashMap::new)
        );
        // 将不在当前类中的字段放入结果集
        superFieldList.stream()
                .filter((field) -> !fieldMap.containsKey(field.getName()))
                .forEach((f) -> fieldMap.put(f.getName(), f));
        return fieldMap;
    }

    /**
     * 获取当前泛型类Clazz
     */
    public static Class<?> getSuperClassGenericType(Class<?> clazz) {
        // 获取当前类中的泛型类型
        Type genType = clazz.getGenericSuperclass();
        // 若获取的泛型不是当前类父类中泛型的实例
        if (!(genType instanceof ParameterizedType)) {
            log.warn(String.format("Warn: %s's superclass not ParameterizedType", clazz.getSimpleName()));
            return Object.class;
        } else {
            // 获取泛型的类型
            Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
            if (params.length > 0) {
                // 如果不是Class的实例
                if (!(params[0] instanceof Class)) {
                    log.warn(String.format("Warn: %s not set the actual class on superclass generic parameter", clazz.getSimpleName()));
                    return Object.class;
                } else {
                    return (Class) params[0];
                }
            } else {
                log.warn(String.format("Warn: Index: %s, Size of %s's Parameterized Type: %s .", 0, clazz.getSimpleName(), params.length));
                return Object.class;
            }
        }
    }


    /**
     * 获取字段get方法名
     */
    public static String getMethodName(String propertyName) {
        String benignAlpha = propertyName.substring(0, 1).toUpperCase();
        return "get" + benignAlpha + propertyName.substring(1);
    }

    /**
     * 获取类名
     */
    public static String getClassSimpleName(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("Class can't be null.");
        }
        String classFullName = clazz.toString();
        int begin = classFullName.lastIndexOf('.') + 1;
        return classFullName.substring(begin);
    }

    /**
     * 获取object中的属性Map
     */
    public static Map<String, Object> getParamterMap(Object arg) {
        Assert.notNull(arg, "Arg can't be null.");
        Map<String, Object> args = new HashMap<>();
        Class<?> clazz = arg.getClass();
        do {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                Object value = getFieldValue(arg, field.getName());
                if (value == null || "".equals(value.toString())) {
                    continue;
                }
                args.put(field.getName(), getFieldValue(arg, field.getName()));
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return args;
    }

    /**
     * 解析类全路径并获取类
     */
    public static Class<?> phraseStringToClass(String classFullName) {
        try {
            return Class.forName(classFullName);
        } catch (ClassNotFoundException e) {
            log.error("Class load error.");
        }
        return Object.class;
    }

    /**
     * 获取启动类路径
     */
    public static Class<?> traceMainApplicationClass() {
        try {
            StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                if ("main".equals(stackTraceElement.getMethodName())) {
                    return Class.forName(stackTraceElement.getClassName());
                }
            }
        } catch (ClassNotFoundException ex) {
            // Swallow and continue
        }
        return null;
    }

    /**
     * 传入Funtion接口，返回
     */
    public static <T, R> SerializedLambda SFunction(SFunction<T, R> func) throws Exception {
        // 直接调用writeReplace
        Method writeReplace = func.getClass().getDeclaredMethod("writeReplace");
        writeReplace.setAccessible(true);
        // 反射调用
        Object sl = writeReplace.invoke(func);
        return (SerializedLambda) sl;
    }

    /**
     * 强转对象
     */
    public static <T> T castObject(Object object, Class<T> clazz) {
        return (T) object;
    }

    private static Class<?> getDeclareClass(SerializedLambda serializedLambda) throws ClassNotFoundException {
        String className = serializedLambda.getImplClass()
                .replace('/', '.');
        return Class.forName(className);
    }

    private static Class<?> getDeclareReturnClass(SerializedLambda serializedLambda) throws ClassNotFoundException {
        String returnType = serializedLambda.getInstantiatedMethodType();
        int i = returnType.lastIndexOf(')') + 2;
        int j = returnType.lastIndexOf(';');
        return Class.forName(returnType.substring(i, j)
                .replace('/', '.'));
    }
}
