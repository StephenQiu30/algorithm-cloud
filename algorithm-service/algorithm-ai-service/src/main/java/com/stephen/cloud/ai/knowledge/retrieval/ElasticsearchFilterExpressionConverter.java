package com.stephen.cloud.ai.knowledge.retrieval;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.ai.vectorstore.filter.Filter;

/** Typed conversion for Spring AI 1.1's Expression/Group records. Unsupported filters fail closed. */
public class ElasticsearchFilterExpressionConverter {
    public Query convert(Filter.Expression expression) {return expression == null ? null : operand(expression);}
    private Query operand(Filter.Operand operand) {
        if (operand instanceof Filter.Group group) return operand(group.content());
        if (!(operand instanceof Filter.Expression e)) throw new IllegalArgumentException("Invalid filter operand");
        return switch (e.type()) {
            case AND -> Query.of(q -> q.bool(b -> b.filter(operand(e.left()), operand(e.right()))));
            case OR -> Query.of(q -> q.bool(b -> b.should(operand(e.left()), operand(e.right())).minimumShouldMatch("1")));
            case EQ -> equality(e);
            default -> throw new IllegalArgumentException("Unsupported keyword filter: " + e.type());
        };
    }
    private Query equality(Filter.Expression expression) {
        if (!(expression.left() instanceof Filter.Key key) || !(expression.right() instanceof Filter.Value value)) throw new IllegalArgumentException("Invalid comparison");
        if (!key.key().matches("[A-Za-z][A-Za-z0-9_]*") || value.value() == null) throw new IllegalArgumentException("Invalid metadata key/value");
        String field = key.key(), term = String.valueOf(value.value());
        return Query.of(q -> q.bool(b -> b
                .should(s -> s.term(t -> t.field("metadata." + field).value(term)))
                .should(s -> s.term(t -> t.field("metadata." + field + ".keyword").value(term)))
                .should(s -> s.term(t -> t.field(field).value(term)))
                .minimumShouldMatch("1")));
    }
}
