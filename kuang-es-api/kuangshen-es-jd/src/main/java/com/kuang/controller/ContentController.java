package com.kuang.controller;

import com.kuang.service.ContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

//请求编写
@RestController
public class ContentController {
    @Autowired
    private ContentService contentService;


    @GetMapping("/parse/{keyword}")
    public Boolean parse(@PathVariable("keyword") String keyword) throws Exception {
        return contentService.parseContent(keyword);
    }



    //http://localhost:9090/search/java/1/10
    @GetMapping("/search/{keyword}/{pageNo}/{pageSize}")
    public List<Map<String, Object>> search(@PathVariable("keyword") String keyword,
                                            @PathVariable("pageNo") int pageNo,
                                            @PathVariable("pageSize") int pageSize) throws Exception {
        return contentService.searchContentPage(keyword,pageNo,pageSize);
    }



    //http://localhost:9090/searchHigh/java/1/10
    @GetMapping("/searchHigh/{keyword}/{pageNo}/{pageSize}")
    public List<Map<String, Object>> searchContentHighlighter(@PathVariable("keyword") String keyword,
                                            @PathVariable("pageNo") int pageNo,
                                            @PathVariable("pageSize") int pageSize) throws Exception {
        return contentService.searchContentHighlighter(keyword,pageNo,pageSize);

    }


}
