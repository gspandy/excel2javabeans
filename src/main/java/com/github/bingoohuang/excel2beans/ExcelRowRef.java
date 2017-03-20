package com.github.bingoohuang.excel2beans;

import com.github.bingoohuang.excel2beans.annotations.ExcelColIgnore;
import lombok.Data;

/**
 * bean reference to related excel row.
 * @author bingoohuang [bingoohuang@gmail.com] Created on 2016/11/10.
 */
@Data
public class ExcelRowRef {
    @ExcelColIgnore private int rowNum;
}