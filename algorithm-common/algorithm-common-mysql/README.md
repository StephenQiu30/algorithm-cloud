# algorithm-common-mysql | MyBatis-Plus 数据访问组件

`algorithm-common-mysql` 为 `algorithm-cloud` 的 MySQL 持久化层提供 MyBatis-Plus 自动配置、分页、乐观锁、逻辑删除和公共字段填充能力。

## 核心能力

- 自动配置 MyBatis-Plus 分页和乐观锁插件。
- 统一处理 `is_delete` 逻辑删除字段。
- `MetaHandler` 自动填充 `create_time`、`update_time` 等公共字段。
- `SqlUtils` 对动态排序字段执行过滤，降低 SQL 注入风险。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-common-mysql</artifactId>
</dependency>
```

## 适用场景

适用于所有使用 MySQL 和 MyBatis-Plus 的用户、帖子、通知、日志及 AI 知识库业务模块。数据库初始化脚本见 [`sql/algorithm.sql`](../../sql/README.md)。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [SQL 初始化说明](../../sql/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
