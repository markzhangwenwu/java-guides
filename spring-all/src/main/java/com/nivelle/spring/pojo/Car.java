package com.nivelle.spring.pojo;

import java.math.BigDecimal;

/**
 * @author nivelle
 * @date 2019/09/24
 */
public class Car {

    private String name;

    private String level;

    private BigDecimal price;

    public Car(String name, String level, BigDecimal price) {
        this.name = name;
        this.level = level;
        this.price = price;
    }

    public Car() {

    }
}
