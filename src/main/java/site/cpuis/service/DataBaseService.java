package site.cpuis.service;

import java.util.List;
import java.util.Map;

public interface DataBaseService<T> {

    void createCollection(String name);

    void deleteCollection(String name);

    boolean collectionExists(String name);

    List<T> selectAll(String collectionName, Class<T> clazz);

    long countAll(String collectionName, Class<T> clazz);


    List<T> selectRange(String collectionName, Class<T> clazz, int currentPage,
                        int pageSize, int rangeMin, int rangeMax);

    List<T> selectByEquals(String collectionName, Class<T> clazz, String field, String data);

    List<T> selectByEqualsGeneric(String collectionName, Class<T> clazz, String field, Object data);


    String createIndex(String collectionName, String fieldName, boolean unique, boolean ascending);

    String createTextIndex(String collectionName, String fieldName, boolean unique);


    List<String> getAllIndexes(String collectionName);


    void insert(T info, String collectionName);


    void insertMulti(List<T> infos, String collectionName);


    void updateById(String id, String collectionName, T info);


    void deleteById(String id, Class<T> clazz, String collectionName);

    void deleteByEqual(String data, Class<T> clazz, String collectionName, String fieldName);

    <E> void deleteByEqualGeneric(E data, Class<T> clazz, String collectionName, String fieldName);


    T selectById(String id, Class<T> clazz, String collectionName);

    T selectById(Integer id, Class<T> clazz, String collectionName);

    List<T> searchRegex(String column, Class<T> clazz, String collectionName, String regex, Integer currentPage, Integer pageSize);

    long countRegex(String column, Class<T> clazz, String collectionName, String regex);

    List<T> searchNormal(String column, Class<T> clazz, String collectionName, String v, Integer currentPage, Integer pageSize);

    long countNormal(String column, Class<T> clazz, String collectionName, String v);

    List<T> searchFullText(Class<T> clazz, String collectionName, String text, Integer currentPage, Integer pageSize);

    long countFullText(Class<T> clazz, String collectionName, String text);


    List<T> selectList(String collectName, Class<T> clazz);


    List<T> selectList(String collectName, Class<T> clazz, Integer currentPage, Integer pageSize);


    List<T> selectByCondition(String collectName, Map<String, String> conditions, Class<T> clazz, Integer currentPage, Integer pageSize);
}
