/*
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
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.node.FieldSorterInput;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedTypesHandler;
import org.jahia.modules.graphql.provider.dxm.predicate.FieldEvaluator;
import org.jahia.modules.graphql.provider.dxm.predicate.SorterHelper;
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

import static graphql.Scalars.GraphQLBoolean;

/**
 * Created at 10 Jan$
 *
 * @author chooliyip
 **/
public class BooleanFinderDataFetcher extends FinderDataFetcher {

    private static final String VALUE = "value";

    BooleanFinderDataFetcher(Finder finder) {
        super(finder.getType(), finder);
    }

    @Override
    public List<GraphQLArgument> getArguments() {
        return Collections.singletonList(GraphQLArgument
                .newArgument()
                .name(VALUE)
                .description("select content if boolean value true or false")
                .type(GraphQLBoolean)
                .defaultValue(true)
                .build());
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        FieldSorterInput sorterInput = getFieldSorterInput(environment);
        if (hasValidArguments(environment)) {
            try {
                String statement = buildSQL2Statement(environment);
                JCRSessionWrapper currentUserSession = getCurrentUserSession(environment);
                JCRNodeIteratorWrapper it = currentUserSession
                        .getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2).execute().getNodes();
                Stream<GqlJcrNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize((Iterator<JCRNodeWrapper>) it, Spliterator.ORDERED), false)
                        .filter(node -> PermissionHelper.hasPermission(node, environment))
                        .map(ThrowingFunction.unchecked(SpecializedTypesHandler::getNode));

                return sorterInput!=null ?
                        stream.sorted(SorterHelper.getFieldComparator(sorterInput, FieldEvaluator.forList(environment))).collect(Collectors.toList())
                        :
                        stream.collect(Collectors.toList());
            } catch (RepositoryException e) {
                throw new DataFetchingException(e);
            }
        } else {
            throw new DataFetchingException("By boolean data fetcher needs 'value' argument");
        }
    }

    /**
     * Construct SQL2 statement with arguments
     *
     * @param environment
     * @return
     */
    private String buildSQL2Statement(DataFetchingEnvironment environment) {
        Boolean value = environment.getArgument(VALUE);

        return "SELECT * FROM [" + type + "] WHERE [" + finder.getProperty() + "] = " + value;

    }

    /**
     * Argument of value is needed
     *
     * @param environment
     * @return
     */
    private boolean hasValidArguments(DataFetchingEnvironment environment) {
        if (environment.getArgument(VALUE) == null) {
            return false;
        } else {
            return true;
        }
    }
}
