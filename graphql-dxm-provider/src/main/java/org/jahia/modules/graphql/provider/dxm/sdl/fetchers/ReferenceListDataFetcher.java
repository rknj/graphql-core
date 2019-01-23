/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.graphql.provider.dxm.sdl.fetchers;

import graphql.schema.*;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.sdl.SDLConstants;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created at 23 Jan$
 *
 * @author chooliyip
 **/
public class ReferenceListDataFetcher implements DataFetcher<List> {

    Field field;

    public ReferenceListDataFetcher(Field field){
        this.field = field;
    }

    @Override
    public List get(DataFetchingEnvironment environment) throws Exception {
        GqlJcrNode node = environment.getSource();
        JCRNodeWrapper jcrNode = node.getNode();
        GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) environment.getFieldDefinition().getType()).getWrappedType();
        GraphQLDirective mappingDirective = type.getDirective(SDLConstants.MAPPING_DIRECTIVE);
        if (mappingDirective != null) {
            GraphQLArgument nodeProperty = mappingDirective.getArgument(SDLConstants.MAPPING_DIRECTIVE_NODE);
            if (nodeProperty != null) {
                try {
                    JCRNodeWrapper subNode = jcrNode.getNode(field.getProperty());
                    String nodeType = nodeProperty.getValue().toString();
                    return JCRContentUtils.getChildrenOfType(subNode, nodeType).stream()
                            .map(GqlJcrNodeImpl::new)
                            .collect(Collectors.toList());
                }
                catch (RepositoryException e) {
                    //Do nothing, return empty list below
                }
            }
        }
        return Collections.emptyList();
    }

}
