/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.upload;

import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.GraphQLContext;
import org.apache.commons.fileupload.FileItem;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrWrongInputException;

import java.util.List;

/**
 * Get FileItem from multipart request
 */
public class UploadHelper {

    /**
     * Check if the specified value matches a part in the request
     * @param name Name of the part
     * @param environment The DataFetchingEnvironment
     * @return true if a FileItem is found
     */
    public static boolean isFileUpload(String name, DataFetchingEnvironment environment) {
        GraphQLContext context = environment.getContext();
        if (!context.getFiles().isPresent()) {
            return false;
        }
        List<FileItem> f = context.getFiles().get().get(name);
        if (f == null || f.size() != 1) {
            return false;
        }
        return true;
    }

    /**
     * Return the FileItem for the specified part nane
     * @param name Name of the part
     * @param environment The DataFetchingEnvironment
     * @return The FileItem matching the specifid name
     */
    public static FileItem getFileUpload(String name, DataFetchingEnvironment environment) {
        GraphQLContext context = environment.getContext();
        if (!context.getFiles().isPresent()) {
            throw new GqlJcrWrongInputException("Must use multipart request");
        }
        List<FileItem> f = context.getFiles().get().get(name);
        if (f == null || f.size() != 1) {
            throw new GqlJcrWrongInputException("Must send file as multipart request for "+name);
        }
        return f.get(0);
    }
}