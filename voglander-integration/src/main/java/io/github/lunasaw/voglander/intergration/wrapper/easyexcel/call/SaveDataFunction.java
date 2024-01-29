package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call;

import java.util.List;
import java.util.Map;

/**
 * @author weidian
 */
public interface SaveDataFunction<T> {
    int save(List<T> list);
}