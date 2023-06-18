package com.kuang.utils;
//专门用来提取网页的工具类：
import com.kuang.pojo.Content;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
@Component  //之后要使用这个工具类，直接autowired注入即可
public class HtmlParseUtil {

//    封装工具类:
    public List<Content> parseJD(String keywords) throws Exception { //String keywords是为了让他更智能，可以去搜关键字

// jsoup不能抓取ajax的请求，除非自己模拟浏览器进行请求！
// 1、https://search.jd.com/Search?keyword=java  获取请求
        String url = "https://search.jd.com/Search?keyword="+keywords;
// 2、解析网页（需要联网）
        Document document = Jsoup.parse(new URL(url), 30000);//返回一个具体的地址，可以拿来用。30s内如果还返回不来，就报错
//        这个返回的Document，就是js页面对象（即浏览器的Document对象。）

// 3、抓取搜索到的数据！
// Document 就是我们JS的Document对象，你可以看到很多JS语法。就可以通过Document把数据扒下来了
        Element element = document.getElementById("J_goodsList");


// 4、找到所有的li元素
        Elements elements = element.getElementsByTag("li");

        ArrayList<Content> goodsList = new ArrayList<>();

// 获取元素中的内容：京东的商品信息
        for (Element el : elements) {
// 这种图片特别多的网站，一般为了保证效率，一般会延时加载图片（图片懒加载过程）
// String img = el.getElementsByTag("img").eq(0).attr("src");
            String img = el.getElementsByTag("img").eq(0).attr("data-lazy-img");
            //获取img标签;获取当前的第一个元素：eq(0);获取source-datalazy-img属性
            String price = el.getElementsByClass("p-price").eq(0).text();
            String title = el.getElementsByClass("p-name").eq(0).text();

//            System.out.println(img);
//            System.out.println(price);
//            System.out.println(title);
//            System.out.println("================================");

            // 封装获取的数据,封装成content对象；
            Content content = new Content();
            content.setImg(img);
            content.setPrice(price);
            content.setTitle(title);
            goodsList.add(content);
        }
        return goodsList;
    }
}