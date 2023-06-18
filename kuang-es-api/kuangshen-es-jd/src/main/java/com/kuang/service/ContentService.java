package com.kuang.service;
//业务编写

import com.alibaba.fastjson2.JSON;
import com.kuang.pojo.Content;
import com.kuang.utils.HtmlParseUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ContentService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;


    /*注意：这里如果直接进行测试，会报空指针错：不能直接使用，因为这里parseContent使用的是@Autowired，需要spring容器。
     * */
//    public static void main(String[] args) throws Exception {
//        new ContentService().parseContent("java");
//    }


    // 1、解析数据存入es
    public Boolean parseContent(String keywords) throws Exception {
// 解析查询出来的数据
        List<Content> contents = new HtmlParseUtil().parseJD(keywords);
// 封装数据到索引库中！
        BulkRequest bulkRequest = new BulkRequest();
//        bulkRequest.timeout(TimeValue.timeValueMinutes(2));
        bulkRequest.timeout("2m");
        for (int i = 0; i < contents.size(); i++) {
            bulkRequest.add(new IndexRequest("jd_goods").source(JSON.toJSONString(contents.get(i)), XContentType.JSON));
        }//插入jd_goods索引（库）中，
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);//客户端执行。
        return !bulkResponse.hasFailures();
    }


    // 2、实现搜索功能，带分页处理（去已经保存的es库中去搜索）
    public List<Map<String, Object>>  searchContentPage(String keyword, int pageNo, int pageSize) throws IOException {
// 基本的参数判断！
        if (pageNo <= 1) {
            pageNo = 1;
        }
// 基本的条件搜索
        SearchRequest searchRequest = new SearchRequest("jd_goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();//构建的话，需要用SearchSourceBuilder
// 分页
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);
// 精准匹配 QueryBuilders 根据自己要求配置查询条件即可！
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", keyword);//精准查询：只要tittle中包好了关键字的
        sourceBuilder.query(termQueryBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
// 客户端执行搜索
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
// 解析结果！
        List<Map<String, Object>> list = new ArrayList<>();
        for (SearchHit documentFields : response.getHits().getHits()) {
            list.add(documentFields.getSourceAsMap());
        }
        return list;
    }





    // 3、实现搜索功能，带高亮（去已经保存的es库中去搜索）
    public List<Map<String, Object>> searchContentHighlighter(String keyword, int pageNo, int pageSize) throws IOException {
// 基本的参数判断！
        if (pageNo <= 1) {
            pageNo = 1;
        }
// 基本的条件搜索
        SearchRequest searchRequest = new SearchRequest("jd_goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();//构建的话，需要用SearchSourceBuilder
// 分页
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);
// 精准匹配 QueryBuilders 根据自己要求配置查询条件即可！
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", keyword);//精准查询：只要tittle中包好了关键字的
        sourceBuilder.query(termQueryBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

//        构建高亮：
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");//设置高亮的字段
        highlightBuilder.requireFieldMatch(false);//是否需要同一个页面多个字段进行高亮：比如同一个商品标题上多个java
        //高亮前缀,设置为红色标签
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>"); //后缀是闭合标签
        sourceBuilder.highlighter(highlightBuilder);
// 客户端执行搜索
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
// 解析结果！个人认为此时的搜索结果response中，标题都是没高亮前后缀的（red前缀）。我们把这个不带前缀的遍历
//        过程中，替换为带<red>前缀的，进而实现高亮！

 /*进行实测：  System.out.println(response);结果为：response的title没有前缀，response的highlight有前缀。
 "title":"蓝精灵冒险故事（套装共4册）(中国环境标志产品 绿色印刷) 蓝精灵冒险故事（套装共4册）(中国环境标志产品 绿色印刷)"},
 "highlight":{"title":["蓝精灵冒险故事（套装共<span style='color:red'>4</span>册）(中国环境标志产品 绿色印刷) 蓝精灵冒险故事（套装共<span style='color:red'>4</span>册）(中国环境标志产品 绿色印刷)"]}},
* */

        List<Map<String, Object>> list = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
//           解析高亮的字段： 将高亮字段，替换原来没有高亮的字段
            //先获取高亮字段
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField titleField = highlightFields.get("title");//拿到带前缀的
            Map<String, Object> source = hit.getSourceAsMap();//这是原来的结果，个人理解不带前缀的。(参考上面方法2searchContentHighlighter)
 /* 输出 System.out.println(source);发现source确实不带前缀。输出结果为：
 {title=蓝精灵冒险故事系列 全4册 5-8岁孩子百看不厌的魔法冒险故事 【接力出版社童书旗舰店】接力出版社直营，正版保障！更多商品请进店选购点此}
            * */


//千万记得要记得判断是不是为空,不然你匹配的第一个结果没有高亮内容,那么就会报空指针异常,这个错误一开始真的搞了很久
//           用带高亮前缀的titleField，替换原来source没有高亮的字段
            if(titleField!=null){
                Text[] fragments = titleField.fragments();//取出高亮字段。
                String name = "";
                for (Text text : fragments) {
                    name += text;
                }
                source.put("title", name); //高亮字段替换掉原本的内容
            }
            list.add(source);



        }
        return list;
    }




}

