package org.jahia.modules.graphql.provider.dxm.sdl.parsing.status;

import static org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatusTypes.*;

public class SDLDefinitionStatusDescription {
    private String field;
    private String message;

    public SDLDefinitionStatusDescription(String field) {
        this.field = field;
    }

    public SDLDefinitionStatusDescription(SDLDefinitionStatusTypes statusType) {
        setMessage(statusType);
    }

    public SDLDefinitionStatusDescription(String field, SDLDefinitionStatusTypes statusType) {
        this.field = field;
        setMessage(statusType);
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMessage(SDLDefinitionStatusTypes statusType) {
        switch (statusType) {
            case MISSING_JCRNODETYPE: this.message = "node type was not found";
                break;
            case MISSING_JCRPROPERTY: this.message = "property is missing from node type";
                break;
            default: this.message = "OK";
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s", this.field == null ? "" : this.field, this.message);
    }
}
