/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.SpecializedType;
import org.jahia.modules.graphql.provider.dxm.sdl.extension.FinderMixinInterface;
import org.jahia.modules.graphql.provider.dxm.sdl.extension.PropertyFetcherExtensionInterface;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface DXGraphQLExtensionsProvider {

    default Collection<Class<?>> getExtensions() {
        return BundleScanner.getClasses(this, GraphQLTypeExtension.class);
    }

    default Collection<Class<? extends GqlJcrNode>> getSpecializedTypes() {
        return BundleScanner.getClasses(this, SpecializedType.class);
    }

    // Makes it possible to add functionality to finders where GqlJCRNode from original finder is passed to the one defined as mixin
    default List<FinderMixinInterface> getFinderMixins() {
        return Collections.emptyList();
    }

    // Makes it possible to add custom property fetchers, works together with @mapping(fetcher: "yourFetcherName")
    default Map<String, PropertyFetcherExtensionInterface> getPropertyFetchers() {
        return Collections.emptyMap();
    }
}
