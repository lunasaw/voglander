package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.impl;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import java.util.Map;

public class ExcelDataListener2<Integer, String, T> implements ReadListener<Map<Integer, String>> {

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {

    }
}