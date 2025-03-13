package org.dromara.cloudeon.utils;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BeanCopyUtils {
    public BeanCopyUtils() {
    }

    public static <T> T copy(Object object, Class<T> clazz) {
        T t = BeanUtils.instantiateClass(clazz);
        BeanUtils.copyProperties(object, t);
        return t;
    }

    public static <T> List copyList(List<?> list, final Class<T> clazz) {
        List<T> result = (List) list.stream().map((obj) -> {
            return copy(obj, clazz);
        }).collect(Collectors.toList());
        return result;
    }

    public static <T> T deepCopy(Object source, Class<T> tClass) {
        String jsonString = JSONObject.toJSONString(source);
        return JSONObject.parseObject(jsonString, tClass);
    }

    public static <T> List deepCopyList(List<?> list, final Class<T> clazz) {
        List<T> result = (List) list.stream().map((obj) -> {
            return deepCopy(obj, clazz);
        }).collect(Collectors.toList());
        return result;
    }

    public static String[] getNullPropertyNames(Object source) {
        BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();

        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }

        String[] result = new String[emptyNames.size()];
        return (String[]) emptyNames.toArray(result);
    }

    public static void copyPropertiesIgnoreNull(Object src, Object target) {
        BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
    }
}
