package com.kuang.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration  //这个注解表示相当于以前学过的xml来注入bean。
public class ElasticsearchClientConfig {
    @Bean   //知识点回顾：这里的注入就相当于spring中： <beans id="restHighLevelClient"  class="RestHighLevelClient" >
    // 其中class是返回值。而springboot配置bean，就太简单了直接@Bean  即可
    public RestHighLevelClient restHighLevelClient() {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("127.0.0.1", 9200, "http")));//是集群，就构建多个；这里不是集群，故只有一个
        return client;
    }
}

/*spring配置两步骤：
1.找对象
2.放到spring中待用
* */
