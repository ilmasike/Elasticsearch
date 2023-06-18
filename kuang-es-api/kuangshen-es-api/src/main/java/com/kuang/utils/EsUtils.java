package com.kuang.utils;


import com.alibaba.fastjson2.JSON;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class EsUtils<T> {
    @Autowired
    @Qualifier("restHighLevelClient")
    private RestHighLevelClient client;

    /**
     * 判断索引是否存在
     *
     * @param index
     * @return
     * @throws IOException
     */
    public boolean existsIndex(String index) throws IOException {
        GetIndexRequest request = new GetIndexRequest(index);
        boolean exists = client.indices().exists(request,
                RequestOptions.DEFAULT);
        return exists;
    }

    /**
     * 创建索引
     *
     * @param index
     * @throws IOException
     */
    public boolean createIndex(String index) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(index);
        CreateIndexResponse createIndexResponse
                = client.indices().create(request, RequestOptions.DEFAULT);
        return createIndexResponse.isAcknowledged();
    }

    /**
     * 删除索引
     *
     * @param index
     * @return
     * @throws IOException
     */
    public boolean deleteIndex(String index) throws IOException {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);
        AcknowledgedResponse response = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        return response.isAcknowledged();
    }

    /**
     * 判断某索引下文档id是否存在
     *
     * @param index
     * @param id
     * @return
     * @throws IOException
     */
    public boolean docExists(String index, String id) throws IOException {
        GetRequest getRequest = new GetRequest(index, id);
//只判断索引是否存在不需要获取_source
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        getRequest.storedFields("_none_");
        boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
        return exists;
    }

    /**
     * 添加文档记录
     *
     * @param index
     * @param id
     * @param t     要添加的数据实体类
     * @return
     * @throws IOException
     */
    public boolean addDoc(String index, String id, T t) throws IOException {
        IndexRequest request = new IndexRequest(index);
        request.id(id);
//timeout
        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("1s");
        request.source(JSON.toJSONString(t), XContentType.JSON);
        IndexResponse indexResponse = client.index(request,
                RequestOptions.DEFAULT);
        RestStatus Status = indexResponse.status();
        return Status == RestStatus.OK || Status == RestStatus.CREATED;
    }

    /**
     * 根据id来获取记录
     *
     * @param index
     * @param id
     * @return
     * @throws IOException
     */
    public GetResponse getDoc(String index, String id) throws IOException {
        GetRequest request = new GetRequest(index, id);
        GetResponse getResponse = client.get(request,
                RequestOptions.DEFAULT);
        return getResponse;
    }

    /**
     * 批量添加文档记录
     * 没有设置id ES会自动生成一个，如果要设置 IndexRequest的对象.id()即可
     *
     * @param index
     * @param list
     * @return
     * @throws IOException
     */
    public boolean bulkAdd(String index, List<T> list) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
//timeout
        bulkRequest.timeout(TimeValue.timeValueMinutes(2));
        bulkRequest.timeout("2m");
        for (int i = 0; i < list.size(); i++) {
            bulkRequest.add(new IndexRequest(index).source(JSON.toJSONString(list.get(i))));
        }
        BulkResponse bulkResponse = client.bulk(bulkRequest,
                RequestOptions.DEFAULT);
        return !bulkResponse.hasFailures();
    }
/**
 * 批量删除和更新就不写了可根据上面几个方法来写
 */
    /**
     * 更新文档记录
     *
     * @param index
     * @param id
     * @param t
     * @return
     * @throws IOException
     */

    public boolean updateDoc(String index, String id, T t) throws IOException {
        UpdateRequest request = new UpdateRequest(index, id);
        request.doc(JSON.toJSONString(t));
        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("1s");
        UpdateResponse updateResponse = client.update(
                request, RequestOptions.DEFAULT);
        return updateResponse.status() == RestStatus.OK;
    }

    /**
     * 删除文档记录
     *
     * @param index
     * @param id
     * @return
     * @throws IOException
     */
    public boolean deleteDoc(String index, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(index, id);
//timeout
        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("1s");
        DeleteResponse deleteResponse = client.delete(
                request, RequestOptions.DEFAULT);
        return deleteResponse.status() == RestStatus.OK;
    }

    /**
     * 根据某字段来搜索
     *
     * @param index
     * @param field
     * @param key   要收搜的关键字
     * @throws IOException
     */
    public void search(String index, String field, String key, Integer
            from, Integer size) throws IOException {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery(field, key));
//控制搜素
        sourceBuilder.from(from);
        sourceBuilder.size(size);
//最大搜索时间。
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest,
                RequestOptions.DEFAULT);
        System.out.println(JSON.toJSONString(searchResponse.getHits()));
    }
}