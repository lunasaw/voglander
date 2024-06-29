package io.github.lunasaw.voglander.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author luna
 */
public interface AbstractMapper<T> extends BaseMapper<T> {

    default LambdaQueryWrapper<T> lambdaWrapper() {
        return Wrappers.lambdaQuery();
    }

    default LambdaUpdateWrapper<T> updateWrapper() {
        return Wrappers.lambdaUpdate();
    }

    default T selectOneEq(SFunction<T, ?> sFunction, Object object) {
        return selectOne(lambdaWrapper().eq(sFunction, object));
    }

    default T selectOneWith(Function<LambdaQueryWrapper<T>, LambdaQueryWrapper<T>> func) {
        return selectOne(func.apply(lambdaWrapper()));
    }

    default List<T> selectListWith(Function<LambdaQueryWrapper<T>, LambdaQueryWrapper<T>> func) {
        return selectList(func.apply(lambdaWrapper()));
    }

    default List<T> selectListEq(SFunction<T, ?> sFunction, Object object) {
        return selectList(lambdaWrapper().eq(sFunction, object));
    }

    default List<T> selectListIn(SFunction<T, ?> sFunction, Collection<?> values) {
        return selectList(lambdaWrapper().in(sFunction, values));
    }

    default int deleteWith(Function<LambdaQueryWrapper<T>, LambdaQueryWrapper<T>> func) {
        return delete(func.apply(lambdaWrapper()));
    }

    default int deleteEq(SFunction<T, ?> sFunction, Object object) {
        return delete(lambdaWrapper().eq(sFunction, object));
    }

    default int deleteIn(SFunction<T, ?> sFunction, Object object) {
        return delete(lambdaWrapper().in(sFunction, object));
    }

    default int updateEq(T entity, SFunction<T, ?> sFunction, Object object) {
        return update(entity, lambdaWrapper().eq(sFunction, object));
    }

    default int insertBatch(Collection<T> entityList) {
        int row = 0;
        for (T entity : entityList) {
            row += insert(entity);
        }
        return row;
    }
}