package com.stephen.cloud.ai.knowledge.retrieval;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.filter.Filter;

/**
 * Spring AI Filter Expression → Elasticsearch Query 转换器
 * <p>
 * 支持 EQ 比较 + AND/OR 复合表达式 + Group 分组的递归转换。
 * 异常不再静默吞掉，改为 warn 日志记录以保持降级行为但增加可观测性。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
public class ElasticsearchFilterExpressionConverter {

    public Query convert(Filter.Expression expression) {
        if (expression == null) {
            return null;
        }
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        process(expression, boolBuilder);
        return boolBuilder.build()._toQuery();
    }

    private void process(Object obj, BoolQuery.Builder boolBuilder) {
        if (obj == null) {
            return;
        }
        String typeName = obj.getClass().getSimpleName();

        switch (typeName) {
            case "Group" -> processGroup(obj, boolBuilder);
            case "Comparison" -> processComparison(obj, boolBuilder);
            case "And" -> processLogical(obj, boolBuilder, "And");
            case "Or" -> processLogical(obj, boolBuilder, "Or");
            default -> log.warn("[FilterConverter] 不支持的表达式类型: {}", typeName);
        }
    }

    /**
     * 处理 Group(Type type, Expression content) — 递归解包
     */
    private void processGroup(Object obj, BoolQuery.Builder boolBuilder) {
        try {
            Object content = obj.getClass().getMethod("content").invoke(obj);
            process(content, boolBuilder);
        } catch (Exception e) {
            log.warn("[FilterConverter] 解析 Group 表达式失败: {}", e.getMessage());
        }
    }

    /**
     * 处理 Comparison(Type type, Key left, Value right) — 目前仅支持 EQ
     */
    private void processComparison(Object obj, BoolQuery.Builder boolBuilder) {
        try {
            Object type = obj.getClass().getMethod("type").invoke(obj);
            Object left = obj.getClass().getMethod("left").invoke(obj);
            Object right = obj.getClass().getMethod("right").invoke(obj);

            if ("EQ".equals(String.valueOf(type))) {
                String key = left.toString();
                Object valNode = right.getClass().getMethod("value").invoke(right);
                String val = String.valueOf(valNode);

                boolBuilder.filter(f -> f.bool(b -> b
                        .should(s -> s.term(t -> t.field("metadata." + key).value(val)))
                        .should(s -> s.term(t -> t.field(key).value(val)))
                        .minimumShouldMatch("1")
                ));
            } else {
                log.warn("[FilterConverter] 不支持的比较类型: {}", type);
            }
        } catch (Exception e) {
            log.warn("[FilterConverter] 解析 Comparison 表达式失败: {}", e.getMessage());
        }
    }

    /**
     * 处理 And(Expression left, Expression right) 和 Or(Expression left, Expression right)
     * 递归处理两侧子表达式
     */
    private void processLogical(Object obj, BoolQuery.Builder parentBuilder, String logicType) {
        try {
            Object left = obj.getClass().getMethod("left").invoke(obj);
            Object right = obj.getClass().getMethod("right").invoke(obj);

            if ("And".equals(logicType)) {
                // AND: 两侧都必须命中，直接在同一 boolBuilder 上叠加 filter
                process(left, parentBuilder);
                process(right, parentBuilder);
            } else {
                // OR: 两侧满足任一即可，各自构建子 bool 查询放入 should
                BoolQuery.Builder leftBuilder = new BoolQuery.Builder();
                process(left, leftBuilder);
                BoolQuery.Builder rightBuilder = new BoolQuery.Builder();
                process(right, rightBuilder);

                parentBuilder.filter(f -> f.bool(b -> b
                        .should(s -> s.bool(leftBuilder.build()))
                        .should(s -> s.bool(rightBuilder.build()))
                        .minimumShouldMatch("1")
                ));
            }
        } catch (Exception e) {
            log.warn("[FilterConverter] 解析 {} 表达式失败: {}", logicType, e.getMessage());
        }
    }
}
