package edu.nwpu.cpuis.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//抄的
@Service
@Slf4j
public class MongoService<T> {

    private final MongoTemplate mongoTemplate;

    public MongoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }


    /**
     * 功能描述: 创建一个集合
     * 同一个集合中可以存入多个不同类型的对象，我们为了方便维护和提升性能，
     * 后续将限制一个集合中存入的对象类型，即一个集合只能存放一个类型的数据
     *
     * @param name 集合名称，相当于传统数据库的表名
     * @return:void
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/20 17:27
     */
    public void createCollection(String name) {
        mongoTemplate.createCollection (name);
    }

    public void deleteCollection(String name) {
        mongoTemplate.getCollection (name).drop ();
    }

    public boolean collectionExists(String name) {
        return mongoTemplate.collectionExists (name);
    }

    public List<T> selectAll(String collectionName, Class<T> clazz) {
        return mongoTemplate.findAll (clazz, collectionName);
    }

    public long countAll(String collectionName, Class<T> clazz) {
        Query query = new Query ();
        return mongoTemplate.count (query, clazz, collectionName);
    }

    /**
     * @param currentPage 从1开始
     * @param rangeMin    都是include
     * @param rangeMax    都是include
     */
    public List<T> selectRange(String collectionName, Class<T> clazz, int currentPage,
                               int pageSize, int rangeMin, int rangeMax) {
        //设置分页参数
        Query query = new Query ();
        //设置分页信息
        query.limit (pageSize);
        query.skip ((long) pageSize * (currentPage - 1));
        query.addCriteria (Criteria.where ("id").lte (rangeMax).gte (rangeMin));
//        query.addCriteria (Criteria.where ("id").gte (1).lte (22));
        return mongoTemplate.find (query, clazz, collectionName);
    }

    public List<T> selectByEquals(String collectionName, Class<T> clazz, String field, String data) {
        //设置分页参数
        Query query = new Query ();
        //设置分页信息
        query.addCriteria (Criteria.where (field).is (data));
//        query.addCriteria (Criteria.where ("id").gte (1).lte (22));
        return mongoTemplate.find (query, clazz, collectionName);
    }

    /**
     * 功能描述: 创建索引
     * 索引是顺序排列，且唯一的索引
     *
     * @param collectionName 集合名称，相当于关系型数据库中的表名
     * @param fieldName      对象中的某个属性名
     * @return:java.lang.String
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/20 16:13
     */
    public String createIndex(String collectionName, String fieldName, boolean unique, boolean ascending) {
        //配置索引选项
        IndexOptions options = new IndexOptions ();
        // 设置为唯一
        options.unique (unique);
        //创建按filedName升序排的索引
        if (ascending)
            return mongoTemplate.getCollection (collectionName).createIndex (Indexes.ascending (fieldName), options);
        else
            return mongoTemplate.getCollection (collectionName).createIndex (Indexes.descending (fieldName), options);
    }

    public String createTextIndex(String collectionName, String fieldName, boolean unique) {
        //配置索引选项
        IndexOptions options = new IndexOptions ();
        // 设置为唯一
        options.unique (unique);
        //创建按filedName升序排的索引
        return mongoTemplate.getCollection (collectionName).createIndex (Indexes.text (), options);
    }


    /**
     * 功能描述: 获取当前集合对应的所有索引的名称
     *
     * @param collectionName
     * @return:java.util.List<java.lang.String>
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/20 16:46
     */
    public List<String> getAllIndexes(String collectionName) {
        ListIndexesIterable<Document> list = mongoTemplate.getCollection (collectionName).listIndexes ();
        //上面的list不能直接获取size，因此初始化arrayList就不设置初始化大小了
        List<String> indexes = new ArrayList<> ();
        for (Document document : list) {
            document.forEach ((key1, value) -> {
                //提取出索引的名称
                if (key1.equals ("name")) {
                    indexes.add (value.toString ());
                }
            });
        }
        return indexes;
    }

    /**
     * 功能描述: 往对应的集合中插入一条数据
     *
     * @param info           存储对象
     * @param collectionName 集合名称
     * @return:void
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/20 16:46
     */
    public void insert(T info, String collectionName) {
//        log.debug ("mongo insert {}", info);
        mongoTemplate.insert (info, collectionName);
    }

    /**
     * 功能描述: 往对应的集合中批量插入数据，注意批量的数据中不要包含重复的id
     *
     * @param infos 对象列表
     * @return:void
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/20 16:47
     */
    public void insertMulti(List<T> infos, String collectionName) {
        mongoTemplate.insert (infos, collectionName);
    }

    /**
     * 功能描述: 使用索引信息精确更改某条数据
     *
     * @param id             唯一键
     * @param collectionName 集合名称
     * @param info           待更新的内容
     * @return:void
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/20 18:42
     */
    public void updateById(String id, String collectionName, T info) {
        Query query = new Query (Criteria.where ("id").is (id));
        Update update = new Update ();
        String str = JSON.toJSONString (info);
        JSONObject jQuery = JSON.parseObject (str);
        jQuery.forEach ((key, value) -> {
            //因为id相当于传统数据库中的主键，这里使用时就不支持更新，所以需要剔除掉
            if (!key.equals ("id")) {
                update.set (key, value);
            }
        });
        mongoTemplate.updateMulti (query, update, info.getClass (), collectionName);
    }

    /**
     * 功能描述: 根据id删除集合中的内容
     *
     * @param id             序列id
     * @param collectionName 集合名称
     * @param clazz          集合中对象的类型
     * @return:void
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/20 16:47
     */
    public void deleteById(String id, Class<T> clazz, String collectionName) {
        // 设置查询条件，当id=#{id}
        Query query = new Query (Criteria.where ("id").is (id));
        // mongodb在删除对象的时候会判断对象类型，如果你不传入对象类型，只传入了集合名称，它是找不到的
        // 上面我们为了方便管理和提升后续处理的性能，将一个集合限制了一个对象类型，所以需要自行管理一下对象类型
        // 在接口传入时需要同时传入对象类型
        mongoTemplate.remove (query, clazz, collectionName);
    }

    public void deleteByEqual(String data, Class<T> clazz, String collectionName, String fieldName) {
        // 设置查询条件，当id=#{id}
        Query query = new Query ();
        query.addCriteria (Criteria.where (fieldName).is (data));
        // mongodb在删除对象的时候会判断对象类型，如果你不传入对象类型，只传入了集合名称，它是找不到的
        // 上面我们为了方便管理和提升后续处理的性能，将一个集合限制了一个对象类型，所以需要自行管理一下对象类型
        // 在接口传入时需要同时传入对象类型
        mongoTemplate.remove (query, clazz, collectionName);
    }

    /**
     * 功能描述: 根据id查询信息
     *
     * @param id             注解
     * @param clazz          类型
     * @param collectionName 集合名称
     * @return:java.util.List<T>
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/20 16:47
     */
    public T selectById(String id, Class<T> clazz, String collectionName) {
        // 查询对象的时候，不仅需要传入id这个唯一键，还需要传入对象的类型，以及集合的名称
        T byId = mongoTemplate.findById (id, clazz, collectionName);
//        log.debug ("mongo select {}", byId);
        return byId;
    }

    public T selectById(Integer id, Class<T> clazz, String collectionName) {
        // 查询对象的时候，不仅需要传入id这个唯一键，还需要传入对象的类型，以及集合的名称
        T byId = mongoTemplate.findById (id, clazz, collectionName);
//        log.debug ("mongo select {}", byId);
        return byId;
    }

    public List<T> searchRegex(Class<T> clazz, String collectionName, String regex, Integer currentPage, Integer pageSize) {
        //设置分页参数
        Query query = new Query ();
        //设置分页信息
        query.limit (pageSize);
        query.skip (pageSize * (currentPage - 1));
        query.addCriteria (Criteria.where ("userName").regex (regex));
        return mongoTemplate.find (query, clazz, collectionName);
    }

    public long countRegex(Class<T> clazz, String collectionName, String regex) {
        Query query = new Query ();
        query.addCriteria (Criteria.where ("userName").regex (regex));
        return mongoTemplate.count (query, clazz, collectionName);
    }

    public List<T> searchNormal(Class<T> clazz, String collectionName, String v, Integer currentPage, Integer pageSize) {
        //设置分页参数
        Query query = new Query ();
        //设置分页信息
        query.limit (pageSize);
        query.skip (pageSize * (currentPage - 1));
        query.addCriteria (Criteria.where ("userName").is (v));
        return mongoTemplate.find (query, clazz, collectionName);
    }

    public long countNormal(Class<T> clazz, String collectionName, String v) {
        Query query = new Query ();
        query.addCriteria (Criteria.where ("userName").is (v));
        return mongoTemplate.count (query, clazz, collectionName);
    }

    public List<T> searchFullText(Class<T> clazz, String collectionName, String text, Integer currentPage, Integer pageSize) {
        //设置分页参数
        Query query = new Query ();
        //设置分页信息
        query.limit (pageSize);
        query.skip (pageSize * (currentPage - 1));
        query.addCriteria (TextCriteria.forLanguage ("en").matchingPhrase (text));
        return mongoTemplate.find (query, clazz, collectionName);
    }

    public long countFullText(Class<T> clazz, String collectionName, String text) {
        Query query = new Query ();
        query.addCriteria (TextCriteria.forLanguage ("en").matchingPhrase (text));
        return mongoTemplate.count (query, clazz, collectionName);
    }

    /**
     * 功能描述: 查询列表信息
     * 将集合中符合对象类型的数据全部查询出来
     *
     * @param collectName 集合名称
     * @param clazz       类型
     * @return:java.util.List<T>
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/21 10:38
     */
    public List<T> selectList(String collectName, Class<T> clazz) {
        return selectList (collectName, clazz, null, null);
    }

    /**
     * 功能描述: 分页查询列表信息
     *
     * @param collectName 集合名称
     * @param clazz       对象类型
     * @param currentPage 当前页码
     * @param pageSize    分页大小
     * @return:java.util.List<T>
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/21 10:38
     */
    public List<T> selectList(String collectName, Class<T> clazz, Integer currentPage, Integer pageSize) {
        //设置分页参数
        Query query = new Query ();
        //设置分页信息
        if (!ObjectUtils.isEmpty (currentPage) && ObjectUtils.isEmpty (pageSize)) {
            query.limit (pageSize);
            query.skip (pageSize * (currentPage - 1));
        }
        return mongoTemplate.find (query, clazz, collectName);
    }


    /**
     * 功能描述: 根据条件查询集合
     *
     * @param collectName 集合名称
     * @param conditions  查询条件，目前查询条件处理的比较简单，仅仅做了相等匹配，没有做模糊查询等复杂匹配
     * @param clazz       对象类型
     * @param currentPage 当前页码
     * @param pageSize    分页大小
     * @return:java.util.List<T>
     * @since: v1.0
     * @Author:wangcanfeng
     * @Date: 2019/3/21 10:48
     */
    public List<T> selectByCondition(String collectName, Map<String, String> conditions, Class<T> clazz, Integer currentPage, Integer pageSize) {
        if (ObjectUtils.isEmpty (conditions)) {
            return selectList (collectName, clazz, currentPage, pageSize);
        } else {
            //设置分页参数
            Query query = new Query ();
            query.limit (pageSize);
            query.skip (currentPage);
            // 往query中注入查询条件
            conditions.forEach ((key, value) -> query.addCriteria (Criteria.where (key).is (value)));
            return mongoTemplate.find (query, clazz, collectName);
        }
    }
}
