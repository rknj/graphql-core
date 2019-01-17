package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.services.content.JCRSessionFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static graphql.Scalars.GraphQLString;

public class ByPathFinderDataFetcher extends FinderDataFetcher {
    public ByPathFinderDataFetcher(String type) {
        super(type);
    }

    public ByPathFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        List<GraphQLArgument> defaultArguments = getDefaultArguments();
        defaultArguments.add(GraphQLArgument.newArgument().name("path").type(GraphQLString).build());
        return defaultArguments;
    }

    @Override
    public GqlJcrNode get(DataFetchingEnvironment environment) {
        try {
            return new GqlJcrNodeImpl(getCurrentUserSession(environment).getNode(environment.getArgument("path")));
        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
