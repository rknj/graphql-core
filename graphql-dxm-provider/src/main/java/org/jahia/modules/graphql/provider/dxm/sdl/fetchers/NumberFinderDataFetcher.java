package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLScalarType;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLUtil;
import org.jahia.modules.graphql.provider.dxm.security.PermissionHelper;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import pl.touk.throwing.ThrowingFunction;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static graphql.Scalars.*;

public class NumberFinderDataFetcher extends FinderListDataFetcher {

    private static final String GT = "gt";
    private static final String GTE = "gte";
    private static final String LT = "lt";
    private static final String LTE = "lte";
    private static final String EQ = "eq";
    private static final String NOTEQ = "noteq";


    public NumberFinderDataFetcher(NumberFinder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLArgument> list = getDefaultArguments();
        list.add(GraphQLArgument.newArgument()
                .name(GT)
                .type(getGraphQLScalarType(((NumberFinder) finder).getNumberType()))
                .description("Property greater than passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(GTE)
                .type(getGraphQLScalarType(((NumberFinder) finder).getNumberType()))
                .description("Property greater than or equal to passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(LT)
                .type(getGraphQLScalarType(((NumberFinder) finder).getNumberType()))
                .description("Property less than passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(LTE)
                .type(getGraphQLScalarType(((NumberFinder) finder).getNumberType()))
                .description("Property less than or equal to passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(EQ)
                .type(getGraphQLScalarType(((NumberFinder) finder).getNumberType()))
                .description("Property equal to passed parameter")
                .build());
        list.add(GraphQLArgument.newArgument()
                .name(NOTEQ)
                .type(getGraphQLScalarType(((NumberFinder) finder).getNumberType()))
                .description("Property not equal to passed parameter")
                .build());
        return list;
    }

    @Override
    public List<GqlJcrNode> get(DataFetchingEnvironment environment) {
        return getStream(environment).collect(Collectors.toList());
    }

    @Override
    public Stream<GqlJcrNode> getStream(DataFetchingEnvironment environment) {
        try {
            String statement = "SELECT * FROM [%s] as n where n.[%s]%s%s";
            Map<String, Object> arguments = SDLUtil.getArguments(environment);

            String comparisonParameterName = firstValidParameter(arguments);
            if (comparisonParameterName == null)
                throw new DataFetchingException(String.format("Entry point %s must have of on comparison parameter, look at the documentation for available parameter names.", environment.getFieldDefinition().getName()));

            switch (comparisonParameterName) {
                case LT:
                    statement = String.format(statement, type, finder.getProperty(), "<", arguments.get(comparisonParameterName));
                    break;
                case LTE:
                    statement = String.format(statement, type, finder.getProperty(), "<=", arguments.get(comparisonParameterName));
                    break;
                case GT:
                    statement = String.format(statement, type, finder.getProperty(), ">", arguments.get(comparisonParameterName));
                    break;
                case GTE:
                    statement = String.format(statement, type, finder.getProperty(), ">=", arguments.get(comparisonParameterName));
                    break;
                case EQ:
                    statement = String.format(statement, type, finder.getProperty(), "=", arguments.get(comparisonParameterName));
                    break;
                case NOTEQ:
                    statement = String.format(statement, type, finder.getProperty(), "<>", arguments.get(comparisonParameterName));
                    break;
                default:
            }
            JCRSessionWrapper currentUserSession = getCurrentUserSession(environment);
            JCRNodeIteratorWrapper it = currentUserSession.getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
            Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                    .filter(node -> PermissionHelper.hasPermission(node, environment))
                    .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));
            return resolveCollection(stream, environment);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e);
        }
    }

    private String firstValidParameter(Map<String, Object> arguments) {
        Set<Map.Entry<String, Object>> args = arguments.entrySet();
        Optional<Map.Entry<String, Object>> argEntry = args.stream()
                .filter(arg -> (arg.getKey().equals(GT) || arg.getKey().equals(GTE) || arg.getKey().equals(LT)
                        || arg.getKey().equals(LTE) || arg.getKey().equals(EQ) || arg.getKey().equals(NOTEQ)))
                .findFirst();
        return argEntry.isPresent() ? argEntry.get().getKey() : null;
    }

    private GraphQLScalarType getGraphQLScalarType(String name) {
        switch (name) {
            case "Int":
                return GraphQLInt;
            case "Long":
                return GraphQLLong;
            case "BigInteger":
                return GraphQLBigInteger;
            case "BigDecimal":
                return GraphQLBigDecimal;
            case "Float":
                return GraphQLFloat;
            case "Short":
                return GraphQLShort;
            default:
                return GraphQLInt;
        }
    }
}
