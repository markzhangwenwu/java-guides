---
layout: post
title:  "redis基础学习之字典"
date:   2017-09-04 01:06:05
categories: noSQL
tags: redis
excerpt: redis
---


* content
{:toc}

### 字典定义以及应用

字典,又称为符号表,关联数组或映射,是一种用于保存键值对的抽象数据结构.字典中,一个键可以和一个值进行关联,这些关联的键和值就称为键值对.

字典中的每个键都是独一无二的,程序可以在字典中根据键查找与之关联的值,或通过键里更新值,或根据键来删除整个键值对.

redis的数据库就是使用字典作为底层实现的,对数据库的增删查改操作也是构建在对字典的操作之上的.字典还是哈希键的底层实现之一,当一个哈希键包含的键值对比较多,又或者键值对中的元素都是比较长的字符串时,redis就会使用字典作为哈希键的底层实现.


### 字典实现

Redis的字典使用哈希表作为底层实现,一个哈希表里面可以有多个哈希表节点,而每个哈希表节点就保存了字典中的一个键值对.

```
typedef struct dictht{
    //哈希表数组
    dictEntry **table;
    //哈希表大小
    unsigned long size;
    //哈希表大小掩码,用于计算索引值
    //总是等于size-1
    unsigned long sizemask;
    //哈希表已有节点的数量
    unsigned long used;
}dictht;

```
table属性是一个数组,数组中的每个元素都是一个指向dict.h/dictEntry结构的指针,每个dictEntry结构保存着一个键值对.size属性记录了哈希表的大小,也即table数组的大小,而used属性则记录了哈希表目前已有节点的数量.sizemask属性的值总是等于size-1,这个属性和哈希值一起决定一个键应该被放到table数组的那个索引上面.

![image](https://ws1.sinaimg.cn/large/b1eb59d9ly1fwxnzmlehtj20bi06zq3i.jpg)


#### 哈希表节点

哈希表节点使用dictEntry结构表示,每个dictEntry结构都保存着一个键值对:

```
typedef  struct dictEnty{
    //键
    void * key;
    //值
    union{
        void *val;
        uint64_tu64;
        int64_ts64;
    }v;
    //指向下个哈希表节点,形成链表
    struct dictEntry *next;
}dictEntry;


```

key属性保存着简直对中的键,而v属性则保存着键值对中的值,其中键值对的值可以是一个指针,或者是一个uint64_t整数,又或者是一个int64_t整数.

next属性是指向另一个哈希表节点的指针,**这个指针可以将多个哈希值相同的键值对连接在一次,以此来解决键冲突的问题**

![image](https://ws1.sinaimg.cn/large/b1eb59d9ly1fwxo3kaxdcj20lg07f0ts.jpg)

#### 字典

```
typedef struct dict{
    //类型特定函数
    dictType * type;
    //私有数据
    void *privadata;
    //哈希表
    dictht ht[2];
    //rehash 索引
    //当rehash不在进行时,值为-1
    int trehashidx;
}

```

type属性和privadata属性是针对不同类型的键值对,为创建多态字典而设置的:

- type属性是一个指向dictType结构的指针,每个dictType结构保存了一簇用于操作特定类型键值对的函数,redis会为用途不同的字典设置不同的类型特定函数.

- privdata属性则保存了需要传给那些类型特定函数的可选参数

ht属性是一个包含两个项的数组,数组中的每个项都是一个dictht哈希表,一般情况下,字典只使用ht[0]哈希表,ht[1]哈希表只会在对ht[0]哈希表进行rehash时使用.

![image](https://ws1.sinaimg.cn/large/b1eb59d9ly1fwxo6wlbuhj20lr0cbabs.jpg)


### 哈希算法

当要将一个新的键值对添加到字典里面时,程序要先根据键值对的键计算出哈希值和索引值,然后再根据索引值,将包含新键值对的哈希表节点放到哈希表数组的指定索引上面.

#### 使用字典设置的哈希 函数,计算键key的哈希值索引值

hash = dict->type->hashFunction(key);
#### 使用哈希表的sizemask属性和哈希值,计算出索引值
###### 根据情况不同,ht[x]可以是ht[0]或者ht[1]
index = hash & dict->ht[x].sizemask;


### 解决键冲突

当有两个或以上数量的键被分配到了哈希表数组的同一个索引上面时,我们称这些键发生了冲突.

redis的哈希表使用链地址法来解决键冲突,每个哈希表节点都有一个next指针,多个哈希表节点可以用next指针构成一个单向链表,被分配到同一个索引上的多个节点可以用这个单向链表连接起来,这就解决了键冲突的问题.

例如:k2和v2添加到下图hash表里面,并且计算出k2的索引值为2,那么k1和k2将产生冲突,而解决冲突的方法就是使用next指针将k2和k1所在的节点连接起来.



### rehash

为了让hash表的负载因子维持在一个合理范围之内,当哈希表保存的键值对数量太多或太少时,程序需要对哈希表的大小进行相应的拓展或收缩.

拓展和收缩哈希表的工作交由rehash操作来完成,redis对字典的哈希表执行rehash的步骤如下:

- 为ht[1]哈希表分配空间,这个哈希表的空间大小取决于要执行的操作,以及ht[0]当前包含的键值对数量(也即ht[0].used)
  - 如果执行的是拓展操作,那么ht[1]的大小为第一个大于等于ht[0].used*2的2^n(2的n次方幂)
  - 如果执行的是收缩操作,那么ht[1]的大小为第一个大于等于ht[0].used的2^n

- 将保存在ht[0]中的所有键值对rehash到ht[1]上面:rehash指的是重新计算键的哈希值和索引值,然后将键值对放置到ht[1]哈希表的指定位置上.

- 当ht[0]包含的所有键值对都迁移到了ht[1]之后(ht[0]变为空表),释放ht[0],将ht[1]新创建一个空白哈希表,为下一次rehash做准备.

### 总结

字典在redis中广泛应用，包括数据库和hash数据结构

每个字典有两个哈希表，一个是正常使用，一个用于rehas期间使用。

当redis计算哈希时，采用的是MurmurHash2哈希算法

哈希表采用链地址法避免键的冲突，被分配到同一个地址的键会构成一个单向链表。

在rehash对哈希表进行扩展或者收缩过程中，会将所有键值对进行迁移，并且这个并且这个迁移是渐进式的迁移。