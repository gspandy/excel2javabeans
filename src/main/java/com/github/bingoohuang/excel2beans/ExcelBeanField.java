package com.github.bingoohuang.excel2beans;

import com.github.bingoohuang.excel2beans.annotations.ExcelColTitle;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

@Slf4j
public class ExcelBeanField {
    @Getter private final String fieldName;
    private final String setter;
    private final String getter;

    private final boolean titleRequired;
    @Getter private final Class elementType;
    private final Method valueOfMethod;
    private final Class<?> beanClass;
    private final ReflectAsmCache reflectAsmCache;

    @Setter @Getter private boolean titleColumnFound;
    @Setter @Getter private int columnIndex;
    @Setter @Getter private CellStyle cellStyle;

    @Getter private final String title;
    @Getter private final boolean cellDataType;
    @Getter private final boolean multipleColumns;
    @Getter
    private final List<Integer> multipleColumnIndexes = Lists.newArrayList();

    public ExcelBeanField(Class<?> beanClass, Field f, int columnIndex, ReflectAsmCache reflectAsmCache) {
        this.beanClass = beanClass;
        this.columnIndex = columnIndex;
        this.fieldName = f.getName();
        this.setter = "set" + StringUtils.capitalize(fieldName);
        this.getter = "get" + StringUtils.capitalize(fieldName);

        val colTitle = f.getAnnotation(ExcelColTitle.class);
        if (colTitle != null) {
            this.titleRequired = colTitle.required();
            this.title = colTitle.value().toUpperCase();
        } else {
            this.titleRequired = false;
            this.title = null;
        }

        val gt = f.getGenericType();
        this.multipleColumns = gt instanceof ParameterizedType && List.class.isAssignableFrom(f.getType());
        this.elementType = this.multipleColumns ? (Class) ((ParameterizedType) gt).getActualTypeArguments()[0] : f.getType();
        this.cellDataType = this.elementType == CellData.class;
        this.valueOfMethod = elementType == String.class ? null : ValueOfs.getValueOfMethodFrom(elementType);

        this.reflectAsmCache = reflectAsmCache;
    }

    public void setFieldValue(Object target, Object cellValue) {
        try {
            reflectAsmCache.getMethodAccess(beanClass).invoke(target, setter, cellValue);
            return;
        } catch (Exception e) {
            log.warn("call setter {} failed", setter, e);
        }

        try {
            reflectAsmCache.getFieldAccess(beanClass).set(target, fieldName, cellValue);
        } catch (Exception e) {
            log.warn("field set {} failed", fieldName, e);
        }
    }

    public Object getFieldValue(Object target) {
        try {
            return reflectAsmCache.getMethodAccess(beanClass).invoke(target, getter);
        } catch (Exception e) {
            log.warn("call getter {} failed", getter, e);
        }

        try {
            return reflectAsmCache.getFieldAccess(beanClass).get(target, fieldName);
        } catch (Exception e) {
            log.warn("field get {} failed", getter, e);
        }

        return "";
    }

    public boolean hasTitle() {
        return StringUtils.isNotEmpty(title);
    }

    public boolean containTitle(String cellValue) {
        return cellValue != null && cellValue.toUpperCase().contains(title);
    }

    public boolean isImageDataField() {
        return elementType == ImageData.class;
    }

    private boolean isStringField() {
        return elementType == String.class;
    }

    public void addMultipleColumnIndex(int columnIndex) {
        multipleColumnIndexes.add(columnIndex);
    }

    public Object convert(String cellValue) {
        return valueOfMethod == null ? cellValue
                : ValueOfs.invokeValueOf(elementType, cellValue);
    }

    public boolean isTitleNotMatched() {
        return hasTitle() && titleRequired && !titleColumnFound;
    }

    public boolean isElementTypeSupported() {
        return isImageDataField() || isStringField() || cellDataType || valueOfMethod != null;
    }

}
