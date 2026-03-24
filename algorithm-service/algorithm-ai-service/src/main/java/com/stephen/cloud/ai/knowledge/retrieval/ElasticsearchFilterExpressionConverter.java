package com.stephen.cloud.ai.knowledge.retrieval;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.ai.vectorstore.filter.Filter;

/**
 * Simplified converter from Spring AI Filter Expression to Elasticsearch Query.
 */
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
        if (obj == null) return;
        String typeName = obj.getClass().getSimpleName();

        if (typeName.equals("Group")) {
            try {
                // record Group(Type type, Expression content)
                Object content = obj.getClass().getMethod("content").invoke(obj);
                process(content, boolBuilder);
            } catch (Exception ignored) {}
        } else if (typeName.equals("Comparison")) {
            try {
                // record Comparison(Type type, Key left, Value right)
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
                }
            } catch (Exception ignored) {}
        }
    }
}
