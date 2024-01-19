package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call;

import java.util.List;
import java.util.Map;

public interface SaveDataFunction<K, T> {
    int save(List<Map<K, T>> list);
}