package com.kuang.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    private String title; // 商格
    private String img; // 商品封面品名称
    private String price; // 商品价
// 如果还需要其他属性，大家可以自行扩展使用

}
