package org.jahia.modules.graphql.provider.dxm.nodetype;

import graphql.annotations.GraphQLField;
import graphql.annotations.GraphQLName;

/**
 * TODO Comment me
 *
 * @author toto
 */
@GraphQLName("NodeDefinition")
public class GqlJcrNodeDefinition {
    private String name;

    @GraphQLField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}