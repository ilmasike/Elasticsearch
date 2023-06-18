package com.kuang;
//es7.6.x  高级客户端的基本api
/*
 * 方法10查询：如果你有大写的英文字母,或者分词器中没有将你的名字当作一个词语的时候需要在name的后面加上.keyword进行查询才能查询到。*/
import com.alibaba.fastjson2.JSON;
import com.kuang.pojo.User;
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
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class KuangshenEsApiApplicationTests {
/*
同时注意，@Autowired这里的命名应与类型保持—致，用小写即可。不要写成client:会爆红。
springboot中关于RestHi....肯定有默认配置，集成完es对象后，后面的使用，肯定是基于默认的配置。此时尽量保持名字一致。这个时候才能锁定到RestHi..类型。
因为@autowire(按照类型匹配)默认选择的是其类型。但是如果非要写成client，那么client不是一个配置类，
此时点击右边的箭头→，会发现不能立即跳转到E..s..Config类(因为spring默认的就有别的类啥的，总之不会立即跳过去还得选择。)
这时候就需要加上@Qualifier去指定其名称。
* */

//    @Autowired
//    private RestHighLevelClient restHighLevelClient;

    @Autowired
    @Qualifier("restHighLevelClient")
    private RestHighLevelClient client;//client就好比es客户端，我们用其去执行的话，就类似用kibana去执行。
//    kibana是通过命令行来操作，现在java是通过面向对象来操作


    //  一、测试索引（库）的创建：Request
    @Test
    void contextLoads() throws IOException {
//        1.创建索引请求：
        CreateIndexRequest request = new CreateIndexRequest("kuang_index");//对应我们的put命令
//        2.客户端执行请求，请求后获得响应 ：    client.indices()会返回对象IndicesClient,     .create()获得响应
        CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);//源码return this.indicesClient;
        System.out.println(createIndexResponse);
    }


    //    二、测试获取索引（库）
    @Test
    void testExistIndex() throws IOException {
//        1.获取索引请求：
        GetIndexRequest request = new GetIndexRequest("kuang_index2");//获得kuang_index2这个库的索引请求
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        if (exists) {
            System.out.println("kuang_index2库的索引是存在的。接下来我们就可以获取里面的数据了");
        } else {
            System.out.println("kuang_index2库的索引是==不==存在的。您的搜索有误。");
        }
    }


    //    三、测试删除索引（库）：
    @Test
    void testDeleteIndex() throws IOException {
//        1.删除索引请求：
        DeleteIndexRequest request = new DeleteIndexRequest("kuang_index2");
//        2. 客户端执行请求，请求后获得响应
        AcknowledgedResponse delete = client.indices().delete(request, RequestOptions.DEFAULT);
        System.out.println(delete.isAcknowledged());//判断是否删除成功。
    }


    //    四、 添加文档（对象）(比如把用户信息直接添加到库里)
    @Test
    void testAddDocument() throws IOException {
//        1.创建对象：
        User user = new User("mako", 23);

//        2.1 创建请求：
        IndexRequest request = new IndexRequest("kuang_index");//关联kuang_index索引（库）的请求。以便在kuang_index库中插入数据
//        2.1 设置规则：以前是：put /kuang_index/_doc/1   .所以我们要设置一些id，version，type等相对应的一些规则。
        request.id("1");//设置id=1
//        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("1s");//设置过期规则。与上面代码功能一样。默认的请求里，其实这个不用写也没事。
//        2.3  将数据（json数据）放入请求
        request.source(JSON.toJSONString(user), XContentType.JSON);//pom引入fastjson，把user对象转化为json字符串

//       3. 客户端执行发送请求,获取响应结果
        IndexResponse index = client.index(request, RequestOptions.DEFAULT);
        System.out.println("打印输出index：" + index.toString());//打印输出index：IndexResponse[index=kuang_index,type=_doc,id=1,version=1,result=created,seqNo=0,primaryTerm=1,shards={"total":2,"successful":1,"failed":0}]
        System.out.println("打印输出index的状态：" + index.status());//created
    }


    //五、测试获取文档：判断是否存在。类似以前的：get /index/doc/1
    @Test
    void testIsExists() throws IOException {
//        1 创建请求：
        GetRequest getRequest = new GetRequest("kuang_index", "1");
//        2.不获取其内容，把内容给过滤掉:不获取返回的_source 的上下文(弹幕说是kibana那里的信息那一块)了。（下面两行代码仅学习，不写也没事）
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        getRequest.storedFields("_none_");//不要排序的一些字段

//        3.客户端执行请求：判断是否存在
        boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
        System.out.println("打印输出exists：" + exists);
    }


    //六、获取文档信息：
    @Test
    void testGetDocument() throws IOException {
//        1. 创建请求：
        GetRequest getRequest = new GetRequest("kuang_index", "1");
//        2.客户端执行请求:获取文档信息
        GetResponse documentFields = client.get(getRequest, RequestOptions.DEFAULT);

        System.out.println("输出打印文档的内容（json格式）：" + documentFields.getSourceAsString());//打印文档的内容
        System.out.println("输出打印文档的内容（map格式）：" + documentFields.getSource());
        System.out.println(documentFields.getVersion());//输出文档的版本
//      。。。。。。。。。。
        System.out.println("输出打印完整文档的内容（json格式）：" + documentFields);//打印完整的文档内容。这里返回的全部内容和命令是一样的。
    }


    //七、更新文档信息：
    @Test
    void testGetDocument1() throws IOException {
//        1. 创建请求：
        UpdateRequest updateRequest = new UpdateRequest("kuang_index", "1");
        updateRequest.timeout("1s");//设置更新的内容；
//        2.新建对象
        User user = new User("kakaruote", 24);
        updateRequest.doc(JSON.toJSONString(user), XContentType.JSON);//doc回忆；之前在kabana中的命令行，_update{"doc":{"name":"kakaruote"}}
//        3.客户端执行请求:更新文档信息
        UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
        System.out.println("更新结果：" + updateResponse.status());
    }


    //八、删除文档记录：
    @Test
    void testDeleteRequest() throws IOException {
//        1. 创建请求：
        DeleteRequest request = new DeleteRequest("kuang_index", "1");
        request.timeout("1s");//设置请求的时间，超过1s,就不执行了。
//        2.客户端执行请求:删除文档
        DeleteResponse deleteResponse = client.delete(request, RequestOptions.DEFAULT);
        System.out.println("删除结果：" + deleteResponse.status());
    }


    //九、特殊的，真实的项目一般都会    批量插入数据：(比如一开始从数据库中拿数据，也不太可能一条一条的放)
    @Test
    void testBulkRequest() throws IOException {
//        1. 创建请求：
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("10s");//设置请求的时间，超过10s,就不执行了。数据量越大，时间就最好大点。
//        2.设置一些数据：
        ArrayList<User> userList = new ArrayList<>();
        userList.add(new User("mako", 25));
        userList.add(new User("mako", 24));
        userList.add(new User("mako", 23));
        userList.add(new User("jj", 22));
        userList.add(new User("自来也", 21));
        userList.add(new User("水月", 20));
        userList.add(new User("梁美明", 19));
        userList.add(new User("雷通牙", 18));
        userList.add(new User("木", 17));
        userList.add(new User("佐仓千代", 16));
//        批处理请求：批量更新、删除等操作，就在这里修改对应的请求即可
        for (int i = 0; i < userList.size(); i++) {
            bulkRequest.add(
                    new IndexRequest("kuang_index")
//                    .id(""+(i+1))  //这里若是不设置id,会默认生成随机id
                    .source(JSON.toJSONString(userList.get(i)), XContentType.JSON)
            );
        }
//        2.客户端执行请求:批量添加文档

        BulkResponse bulkItemResponses = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        System.out.println("批处理的结果：" + bulkItemResponses.hasFailures());//是否执行失败。
    }


    //十、查询   1.SearchRequest搜索请求      2.SearchSourceBuilder条件构造：里面有非常多的方法，如高亮highlishtbuilder等
//   构建精确查询termQueryBuilder，匹配全部MatchAllQueryBuilder。。。。，可发现xxxQueryBuilder对应我们刚才看到的索引命令
    @Test
    void testSearch() throws IOException {
//        1. 创建请求：
        SearchRequest searchRequest = new SearchRequest("kuang_index");
//        2.构建查询条件：可使用QueryBuilders工具类快速匹配：如QueryBuilders.termQuery是精确匹配
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();//这里所有东西都是通过构建器来构建的

//        2.1精确匹配
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("name", "mako");
        sourceBuilder.query(termQueryBuilder);//把查询条件丢进构造器

////        2.2 匹配所有文件：
//        MatchAllQueryBuilder matchAllQueryBuilder = QueryBuilders.matchAllQuery();
//        sourceBuilder.query(matchAllQueryBuilder);

//        2.3.....等等

        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));//设置请求的时间，基本的延时，超过60s,就不执行了。

////        3  sourceBuilder已经构建完成，之后要构建分页也非常简单：
//        sourceBuilder.from();//分页从哪里开始
//        sourceBuilder.size();//分页大小。这里分页不写也行，有默认值


//        4.把searchRequest请求构建出来：构建搜索
        searchRequest.source(sourceBuilder);


//        5.客户端执行请求:搜索文档
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("搜索的响应结果：" + JSON.toJSONString(searchResponse.getHits()));//所有的结果都封装在getHits中
//        之后要去拿到数据，也是从getHits中去拿
        System.out.println("====================把结果遍历出来：==============================");
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            System.out.println(hit.getSourceAsMap());//获取所有资源，打印成map集合
        }

    }


}
