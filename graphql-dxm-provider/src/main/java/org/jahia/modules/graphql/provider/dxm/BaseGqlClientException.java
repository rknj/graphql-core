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

import graphql.ErrorType;
import org.jahia.exceptions.JahiaRuntimeException;

import java.util.Map;

/**
 * Base exception for the GraphQL errors.
 */
public class BaseGqlClientException extends JahiaRuntimeException {

    private static final long serialVersionUID = 2380023950503433037L;

    private ErrorType errorType;
    private Map<String,Object> extensions;

    public BaseGqlClientException(ErrorType errorType) {
        this.errorType = errorType;
    }

    public BaseGqlClientException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public BaseGqlClientException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public BaseGqlClientException(String message, Throwable cause, Map<String,Object> extensions) {
        super(message, cause);
        this.extensions = extensions;
    }

    public BaseGqlClientException(String message, Throwable cause, ErrorType errorType, Map<String,Object> extensions) {
        super(message, cause);
        this.errorType = errorType;
        this.extensions = extensions;
    }

    public BaseGqlClientException(Throwable cause, ErrorType errorType) {
        super(cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }
}