
### 跳跃表

跳跃表是一种有序数据结构,它通过在每个节点中维持多个指向其他节点的指针,从而达到快速访问节点的目的.

跳跃表支持平均o(logN),最坏O(N)复杂度的节点查找,还可以通过顺序性操作来批量处理节点.

redis使用跳跃表作为有序集合键的底层实现之一,如果一个有序集合包含的元素数量比较多,或者有序集合中元素的成员是比较长的字符串时,redis就会使用跳跃表来作为有序集合键的底层实现.

#### 数据结构redis.h.zskiplistNode

```
typedef struct zskiplistNode{
  //层
  struct zskiplistLevel{
    //前进指针
    struct zskiplistNode * forward;
    //跨度
    unsigned int span;
  }level[];

  //后退指针
  struct zskiplistNode *backward;
  //分值
  double score;
  //成员对象
  robj *obj;
}zskiplistNode;


```

![image](https://ws1.sinaimg.cn/large/b1eb59d9ly1fwxpaj3bvtj20lr08xgnc.jpg)

- header:指向跳跃表的表头节点
- tail:指向跳跃表的表尾节点
- level:记录目前跳跃表内,层数最大的那个节点的层数
- length:记录跳跃表的长度,也即是,跳跃表目前包含节点的数量

#### 位于zskiplist结构右面的是四个zskiplistNode结构,该结构包含以下属性:

- 层(level):节点中用l1,l2,l3等标记节点的各个层,l1代表第一层,l2代表第二层.每个层都带有两个属性:前进指针和跨度.前进指针用于访问位于表尾方向的其他节点,而跨度则记录了前进指针所指向节点和当前节点的距离.当程序从表头向表位进行遍历时,访问会沿着前进的指针进行.一般来说,层的数量越多,访问其他节点的速度就越快.每次创建新节点的时候,程序根据幂次定律随机生成一个介于1和32之间的值作为level素组的大小,这个大小就是层的高度.

- 后退指针(backward):节点中用bw字样标记节点的后退指针,它指向位于当前节点的前一个节点.后退指针在程序从表尾向表头遍历时使用.

- 分值:各个节点中的1.0,2.0,3.0是节点所保存的分值.在跳跃表中,节点按各自所保存的分值从小到大排列

- 成员对象(obj):各个节点中的o1,o2和o3是节点所保存的成员对象.
