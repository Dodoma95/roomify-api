package com.roomify.configuration;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

@Configuration
public class GraphQlConfiguration {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(buildDateScalar());
    }

    private static GraphQLScalarType buildDateScalar() {
        return GraphQLScalarType.newScalar()
                .name("Date")
                .description("Date ISO-8601 (YYYY-MM-DD), mappée sur java.time.LocalDate")
                .coercing(new Coercing<LocalDate, String>() {

                    @Override
                    public String serialize(@NonNull Object result, @NonNull GraphQLContext ctx, @NonNull Locale locale)
                            throws CoercingSerializeException {
                        if (result instanceof LocalDate ld)
                            return ld.toString();
                        throw new CoercingSerializeException("Attendu LocalDate, reçu : " + result.getClass().getSimpleName());
                    }

                    @Override
                    public LocalDate parseValue(@NonNull Object input, @NonNull GraphQLContext ctx, @NonNull Locale locale)
                            throws CoercingParseValueException {
                        if (input instanceof String s) {
                            try {
                                return LocalDate.parse(s);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseValueException("Date invalide (attendu YYYY-MM-DD) : " + input);
                            }
                        }
                        throw new CoercingParseValueException("Attendu String, reçu : " + input.getClass().getSimpleName());
                    }

                    @Override
                    public LocalDate parseLiteral(@NonNull Value<?> input, @NonNull CoercedVariables vars, @NonNull GraphQLContext ctx,
                            @NonNull Locale locale) throws CoercingParseLiteralException {
                        if (input instanceof StringValue sv) {
                            try {
                                return LocalDate.parse(sv.getValue());
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException("Date invalide (attendu YYYY-MM-DD) : " + sv.getValue());
                            }
                        }
                        throw new CoercingParseLiteralException("Attendu StringValue");
                    }

                    @Override
                    public Value<?> valueToLiteral(@NonNull Object input, @NonNull GraphQLContext ctx, @NonNull Locale locale) {
                        if (input instanceof LocalDate ld)
                            return StringValue.of(ld.toString());
                        throw new CoercingSerializeException("Attendu LocalDate");
                    }
                })
                .build();
    }
}
