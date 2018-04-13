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
package org.jahia.modules.graphql.provider.dxm.service.vanity;

import graphql.annotations.annotationTypes.*;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.GqlConstraintViolationException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.NonUniqueUrlMappingException;
import org.jahia.services.seo.jcr.VanityUrlService;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

@GraphQLTypeExtension(GqlJcrNodeMutation.class)
@GraphQLName("VanityUrlJCRNodeMutationExtensions")
public class VanityUrlJCRNodeMutationExtensions  extends GqlJcrMutationSupport {

    private VanityUrlMutationService vanityUrlMutationService;

    /**
     * Initializes an instance of this class.
     *
     * @param node the corresponding GraphQL node
     */
    public VanityUrlJCRNodeMutationExtensions(GqlJcrNodeMutation node) {
        this.vanityUrlMutationService = new VanityUrlMutationService(node.getNode().getNode(), BundleUtils.getOsgiService(VanityUrlService.class, null));
    }

    /**
     * Add the vanity URL.
     *
     * @param active Desired value of the active flag or null to keep existing value
     * @param defaultMapping Desired value of the default flag or null to keep existing value
     * @param language Desired vanity URL language or null to keep existing value
     * @param url Desired URL value or null to keep existing value
     * @return Always true
     * @throws GqlConstraintViolationException In case the desired values violate a vanity URL uniqueness constraint
     */
    @GraphQLField
    @GraphQLDescription("Add vanity URL")
    @GraphQLName("addVanityUrl")
    public boolean addVanityUrl(@GraphQLName("active") @GraphQLDescription("Desired value of the active flag or null to keep existing value") Boolean active,
                          @GraphQLName("defaultMapping") @GraphQLNonNull @GraphQLDescription("Desired value of the default flag or null to keep existing value") Boolean defaultMapping,
                          @GraphQLName("language") @GraphQLNonNull @GraphQLDescription("Desired vanity URL language or null to keep existing value") String language,
                          @GraphQLName("url") @GraphQLNonNull @GraphQLDescription("Desired URL value or null to keep existing value") String url
    ) throws GqlConstraintViolationException {
        return vanityUrlMutationService.updateAndSaveVanity(new VanityUrl(), active, defaultMapping, language, url);
    }
}
