# 慢查询

## 什么是慢查询

　　MySQL的慢查询日志是MySQL提供的一种日志记录, 它用来记录在MySQL中响应时间超过阈值的语句, 阈值指的是long_query_time 的值, 
若SQL运行时间超过该值则会被记录到慢查询日志中。long_query_time的值默认为 10, 意思是运行 10 秒以上的SQL语句。默认情况下, 
MySQL数据库中没有启动慢查询日志, 所以需要我们手动设置这个参数。
    
## 慢查询需要知道的点
- 企业级开发中, 慢查询日志是会打开的。这样做的同时也会对性能造成一定的影响。
- 慢查询日志支持将日志记录写入文件, 也支持将日志记录写入数据表。
- 默认的阈值(long_query_time)是10, 对于用户级体验而言, 该值过大, 通常将其设置为0.2。

## 慢查询相关的变量

### 查看变量的SQL语句

|SQL                                                  |含义                |备注                              |     
|:----                                               |:----             |:----                            |
|SHOW VARIABLES LIKE 'slow_query_log';                |是否开启慢查询日志    |**ON表示开启; OFF表示关闭**       | 
|SHOW VARIABLES LIKE 'slow_query_log_file';           |查看慢查询日志存储路径 |**和MySQL版本相关**                  |
|SHOW VARIABLES LIKE 'long_query_time';               |查看当前慢查询阈值    |**MySQL默认是10(秒), 需要手动进行修改**|     
|SHOW VARIABLES LIKE 'long_queries_not_using_indexes';|未使用索引的查询也被记录到查询日志中|**OFF: 关闭, ON: 开启**|  
|SHOW VARIABLES LIKE 'log_output';                    |查看慢查询日志的存储方式|**FILE: 文件(默认), TABLE: mysql.slow_log数据表**|
|SHOW GLOBAL STATUS LIKE 'slow_queries';              |查看慢查询记录条数     ||  

### 关于变量的说明

- 若要对变量其进行修改, 可以会直接修改my.cnf配置文件(常见做法), 也可以使用语句命令的方式:    
`SET GLOBAL long_query_time = 0.2;`
- 日志记录到系统的专用日志记录表中, 要比记录到文件耗费更多的系统资源, 所以不要将慢查询日志记录到表中。

## 慢查询的日志分析工具 (<font color=red>优化慢查询</font>)

### mysqldumpslow

### MySQL 内置了工具 mysqldumpslow 用于解析MySQL慢查询日志, 并打印其内容摘要
- 使用语法  
`mysqldumpslow [options] [log_file ...]`
- 常用选项(options)及解释  
-g pattern: 只显示与模式(pattern)匹配的语句, 不区分大小写。  
-r: 反转排序顺序  
-s sort_type: 指定排序输出的规则, 可选的sort_type如下  
　　t: 按消耗的总时间排序。  
　　l: 按锁定时间排序。  
　　r: 按总发送行排序。  
　　c: 按计数排序。  
　　at: 按查询时间或者平均查询时间排序。(默认)  
　　al: 按平均锁定时间排序。  
　　ar: 按平均行发送排序。   
-t N: 是top n 的意思, 即返回前面多少条的数据  
-v: 详细模式
- 使用示例  
\#&nbsp;显示两条结果, 且按照查询总时间排序, 且过滤group by 语句。  
`mysqldumpslow -t 2 -s t -g "group by" slow_query_log_file`  
\#&nbsp;按照时间排序的前10条里面含有左连接的查询语句。  
`mysqldumpslow -s t -t 10 -g "left join" slow_query_log_file`  
\#&nbsp;返回记录集最多的 10 个SQL  
`msyqldumpslow -s r -t 10 slow_query_log_file`  
\#&nbsp;可以结合more一起使用, 避免一次显示过多的SQL语句  
`mysqldumpslow -s r -t 20 slow_query_log_file | more`  
\#&nbsp;访问次数最多的10个SQL
`mysqldumpslow -s c -t 10 slow_query_log_file`
- mysqldumpslow 返回结果信息
    + Count: 代表这种类型的语句执行了几次
    + Time: 这种类型的语句执行的最大时间
    + Lock: 这种类型的语句执行时等待锁的时间
    + Rows: 单次返回的结果数  
    `Count: 2　　Time=3.21s (7s)　　Lock=0.00s (0s)　　Rows=1.0 (2), root[root]@localhost`  
    代表的含义是: 执行了两次, 最大时间是3.21s, 总共花费时间是7s, 等待锁的时间是0s, 单次返回的结果数是一条记录, 两次总共返回了两条记录。   
### EXPLAIN
### MySQL提供了EXPLAIN命令, 可以对慢查询(SELECT)进行分析, 并输出SELECT执行的详细信息。我们可以针对输出的信息对慢查询语句进行合理的优化。
- 使用方法  
`EXPLAIN SELECT * FROM user WHERE name LIKE '%'\G`
- EXPLAIN输出信息及解释  
```
mysql>EXPLAIN SELECT * FROM user WHERE name LIKE '%'\G
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: user
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 2
     filtered: 50.00
        Extra: Using where
1 row in set, 1 warning (0.00 sec) 
```
```
id: 查询的唯一标识符 
select_type: 查询的类型 
table: 查询的表 
partitions: 匹配的分区
type: JOIN类型
possible_keys: 查询中可能用到的索引
key: 查询中使用到的索引
key_len: 查询优化器是用了的索引的字节数
ref: 哪个字段或常亮与 key 一起被使用
rows: 当前查询一共扫描了多少行 (估值)
fiftered: 查询条件过滤的数据百分比
Extra: 额外信息
```    
- **select_type: 最常见的查询类型是SIMPLE, 这表示查询中没有子查询, 也没有UNION查询**  
- **type: 这个字段是判断查询是否高效的重要提示。可以判断查询是全表扫描还是索引扫描。例如: all表示全表扫描, 性能最差; range表示
          使用索引范围扫描, 通常是where条件中带有数字对比的; index表示全索引扫描, 扫描索引而不扫描数据。**  
- **possible_keys: 表示查询可能会使用到的索引, 但是并不表示一定会使用。真正的使用了哪些索引由key决定。**
- **rows: MySQL优化器会估算此次查询需要扫描的数据记录数(行数), 这个值越小, 查询效率越高。**
- **Extra: 这个是查询语句所对应的"额外信息", 常见的有:**  
    - **Using filesort: 表示MySQL需额外的排序操作, 不能通过索引顺序达到排序效果。这样的查询是需要避免的, CPU消耗很高。**
    - **Using where:　在查找使用索引的情况下, 需要回表去查询所需的数据。**
    - **Using index: 表示查询在索引树中就可查找所需数据, 不用扫描表数据文件。**
    - **Using temporary: 查询过程中会使用到临时表, 常见于ORDER BY、GROUP BY、JOIN等场景, 性能较低。**
### 为什么会产生慢查询
- **两张比较大的表进行JOIN, 但是没有给表的相应字段加索引。**
- **表存在索引, 但是查询条件过多, 且字段顺序与索引顺序不一致。**
- **对很多查询结果进行GROUP BY**
- **......**

 

