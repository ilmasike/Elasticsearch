package com.kuang;

import com.kuang.service.ContentService;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KuangshenEsJdApplicationTests {

    @Autowired
    private ContentService contentService;

    @Test
    void contextLoads() throws Exception {
        contentService.parseContent("java");
    }

}
