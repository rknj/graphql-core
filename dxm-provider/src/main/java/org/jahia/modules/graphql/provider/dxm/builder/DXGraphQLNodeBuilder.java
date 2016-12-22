package org.jahia.modules.graphql.provider.dxm.builder;

import graphql.schema.*;
import graphql.servlet.GraphQLServlet;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.model.DXGraphQLNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.servlet.Servlet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

@Component(service = DXGraphQLNodeBuilder.class)
public class DXGraphQLNodeBuilder extends DXGraphQLBuilder {
    public static final String PROPERTY_PREFIX = "";
    public static final String UNNAMED_PROPERTY_PREFIX = "_";
    public static final String CHILD_PREFIX = "";
    public static final String UNNAMED_CHILD_PREFIX = "_";
    private static Logger logger = LoggerFactory.getLogger(DXGraphQLNodeBuilder.class);
    private static Pattern VALID_NAME = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*$");

    private Map<String, GraphQLObjectType> knownTypes = new ConcurrentHashMap<>();

    private DXGraphQLPropertiesBuilder propertiesBuilder;

    private DXGraphQLNodeTypeBuilder nodeTypeBuilder;

    private NodeTypeRegistry nodeTypeRegistry;

    private GraphQLObjectType obj;
    private List<GraphQLFieldDefinition> fieldDefinitions;

    private DataFetcher identityDataFetcher = new IdentityDataFetcher();

    @Override
    public String getName() {
        return "node";
    }


    public Map<String, GraphQLObjectType> getKnownTypes() {
        if (knownTypes.isEmpty()) {
            final NodeTypeRegistry.JahiaNodeTypeIterator nodeTypes = nodeTypeRegistry.getAllNodeTypes();
            for (ExtendedNodeType type : nodeTypes) {
                final String typeName = escape(type.getName());
                if (!knownTypes.containsKey(typeName)) {
                    knownTypes.put(typeName, createGraphQLType(type, typeName, (GraphQLInterfaceType) getType()));
                } else {
                    logger.debug("Already generated {}", typeName);
                }
            }
        }
        return knownTypes;
    }

    @Reference(service = DXGraphQLExtender.class, target = "(graphQLType=node)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindExtender(DXGraphQLExtender extender) {
        this.extenders.add(extender);
    }

    public void unbindExtender(DXGraphQLExtender extender) {
        this.extenders.remove(extender);
    }

    @Reference
    public void setNodeTypeBuilder(DXGraphQLNodeTypeBuilder nodeTypeBuilder) {
        this.nodeTypeBuilder = nodeTypeBuilder;
    }

    @Reference
    public void setPropertiesBuilder(DXGraphQLPropertiesBuilder propertiesBuilder) {
        this.propertiesBuilder = propertiesBuilder;
    }

    @Reference(service = NodeTypeRegistry.class, cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    public void setNodeTypeRegistry(NodeTypeRegistry nodeTypeRegistry) {
        this.nodeTypeRegistry = nodeTypeRegistry;
    }

    @Override
    public GraphQLOutputType getType() {
        if (type == null) {
            GraphQLInterfaceType.Builder builder = newInterface()
                    .name(getName());
            builder.typeResolver(new TypeResolver() {
                @Override
                public GraphQLObjectType getType(Object object) {
                    String type = ((DXGraphQLNode) object).getType();
                    return getKnownTypes().get(escape(type));
                }
            });

            List<GraphQLFieldDefinition> fields = new ArrayList<>(getFields());
            for (DXGraphQLExtender extender : extenders) {
                fields.addAll(extender.getFields());
            }

            for (GraphQLFieldDefinition definition : fields) {
                builder.field(newFieldDefinition()
                        .name(definition.getName())
                        .type(definition.getType())
                        .argument(definition.getArguments())
                        .build()
                );
            }

            this.type = builder.build();
        }
        return type;
    }

    private GraphQLObjectType createGraphQLType(ExtendedNodeType type, String typeName, GraphQLInterfaceType interfaceType) {
        final String escapedTypeName = escape(typeName);
        logger.debug("Creating {}", escapedTypeName);

        final GraphQLObjectType.Builder builder = newObject().name(escapedTypeName)
                .withInterface(interfaceType)
                .fields(getFields());

        for (DXGraphQLExtender extender : extenders) {
            builder.fields(extender.getFields());
        }


        final PropertyDefinition[] properties = type.getPropertyDefinitions();
        if (properties.length > 0) {
            final Set<String> multiplePropertyTypes = new HashSet<>(properties.length);
            final GraphQLFieldDefinition.Builder propertiesField = newFieldDefinition().name("namedProperties").dataFetcher(identityDataFetcher);
            final GraphQLObjectType.Builder propertiesType = newObject().name(escapedTypeName + "Properties");
            for (PropertyDefinition property : properties) {
                final String propName = property.getName();
                final int propertyType = property.getRequiredType();
                final boolean multiple = property.isMultiple();
                if (!"*".equals(propName)) {
                    final String escapedPropName = PROPERTY_PREFIX + escape(propName);
                    propertiesType.field(newFieldDefinition()
                            .name(escapedPropName)
                            .dataFetcher(new NamedPropertiesDataFetcher())
                            .type(getGraphQLType(propertyType, multiple))
                            .build());
                } else {
                    final String propertyTypeName = PropertyType.nameFromValue(propertyType);
                    if (!multiplePropertyTypes.contains(propertyTypeName)) {
                        propertiesType.field(
                                newFieldDefinition()
                                        .name(UNNAMED_PROPERTY_PREFIX + propertyTypeName)
                                        .type(getGraphQLType(propertyType, false))
                                        .dataFetcher(new UnnamedPropertiesDataFetcher())
//                                        .argument(newArgument()
//                                                .name("name")
//                                                .type(GraphQLString)
//                                                .build())
                                        .build()
                        );
                        multiplePropertyTypes.add(propertyTypeName);
                    }
                }
            }
            propertiesField.type(propertiesType.build());
            builder.field(propertiesField);
        }

        final NodeDefinition[] children = type.getChildNodeDefinitions();
        if (children.length > 0) {
            final Set<String> multipleChildTypes = new HashSet<>(children.length);
            final GraphQLFieldDefinition.Builder childrenField = newFieldDefinition().name("namedChildren").dataFetcher(identityDataFetcher);
            final GraphQLObjectType.Builder childrenType = newObject().name(escapedTypeName + "Children");
            for (NodeDefinition child : children) {
                final String childName = child.getName();

                if (!"*".equals(childName)) {
                    final String escapedChildName = CHILD_PREFIX + escape(childName);
                    final String childTypeName = getChildTypeName(child);
                    GraphQLOutputType gqlChildType = new GraphQLTypeReference(escape(childTypeName));
                    childrenType.field(newFieldDefinition()
                            .name(escapedChildName)
                            .type(gqlChildType)
                            .dataFetcher(new NamedChildDataFetcher())
                            .build());
                } else {
                    final String childTypeName = getChildTypeName(child);
                    if (!multipleChildTypes.contains(childTypeName)) {
                        final String escapedChildTypeName = escape(childTypeName);
                        childrenType.field(
                                newFieldDefinition()
                                        .name(UNNAMED_CHILD_PREFIX + escapedChildTypeName)
                                        .type(new GraphQLList(new GraphQLTypeReference(escapedChildTypeName)))
                                        .dataFetcher(new UnnamedChildNodesDataFetcher())
//                                        .argument(newArgument()
//                                                .name("name")
//                                                .type(GraphQLString)
//                                                .build())
                                        .build()
                        );
                        multipleChildTypes.add(childTypeName);
                    }
                }
            }
            childrenField.type(childrenType.build());
            builder.field(childrenField);
        }

        builder.description(type.getDescription(Locale.ENGLISH));

        return builder.build();
    }


    private String getChildTypeName(NodeDefinition child) {
        String childTypeName = child.getDefaultPrimaryTypeName();
        if (childTypeName == null) {
            final String[] primaryTypeNames = child.getRequiredPrimaryTypeNames();
            if (primaryTypeNames.length > 1) {
                // todo: do something here
                logger.warn("Multiple primary types (" + primaryTypeNames +
                        ") for child " + child.getName() + " of type "
                        + child.getDeclaringNodeType().getName());
                childTypeName = Constants.NT_BASE;
            } else {
                childTypeName = primaryTypeNames[0];
            }
        }
        return childTypeName;
    }

    private GraphQLOutputType getGraphQLType(int jcrPropertyType, boolean multiValued) {
        GraphQLScalarType type;
        switch (jcrPropertyType) {
            case PropertyType.BOOLEAN:
                type = GraphQLBoolean;
                break;
            case PropertyType.DATE:
            case PropertyType.DECIMAL:
            case PropertyType.LONG:
                type = GraphQLLong;
                break;
            case PropertyType.DOUBLE:
                type = GraphQLFloat;
                break;
            case PropertyType.BINARY:
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.STRING:
            case PropertyType.UNDEFINED:
            case PropertyType.URI:
            case PropertyType.WEAKREFERENCE:
                type = GraphQLString;
                break;
            default:
                logger.warn("Couldn't find equivalent GraphQL type for "
                        + PropertyType.nameFromValue(jcrPropertyType)
                        + " property type will use string type instead!");
                type = GraphQLString;
        }

        return multiValued ? new GraphQLList(type) : type;
    }



    protected List<GraphQLFieldDefinition> getFields() {
        if (fieldDefinitions == null) {
            GraphQLInputObjectType propertyFilterType = GraphQLInputObjectType.newInputObject().name("propertyFilter")
                    .field(GraphQLInputObjectField.newInputObjectField()
                            .name("key").type(GraphQLString).build())
                    .field(GraphQLInputObjectField.newInputObjectField()
                            .name("value").type(GraphQLString).build())
                    .build();

            fieldDefinitions = Arrays.asList(
                    newFieldDefinition()
                            .name("identifier")
                            .type(GraphQLString)
                            .build(),
                    newFieldDefinition()
                            .name("name")
                            .type(GraphQLString)
                            .build(),
                    newFieldDefinition()
                            .name("path")
                            .type(GraphQLString)
                            .build(),
                    newFieldDefinition()
                            .name("parentPath")
                            .type(GraphQLString)
                            .build(),
                    newFieldDefinition()
                            .name("parentIdentifier")
                            .type(GraphQLString)
                            .build(),
                    newFieldDefinition()
                            .name("primaryNodeType")
                            .type(nodeTypeBuilder.getType())
                            .dataFetcher(new PrimaryNodeTypeDataFetcher())
                            .build(),
                    newFieldDefinition()
                            .name("mixinTypes")
                            .type(new GraphQLList(nodeTypeBuilder.getType()))
                            .dataFetcher(new MixinNodeTypeDataFetcher())
                            .build(),
                    newFieldDefinition()
                            .name("isNodeType")
                            .description("Boolean value indicating if the node matches the specified nodetype(s)")
                            .type(GraphQLBoolean)
                            .argument(newArgument().name("anyType")
                                    .type(new GraphQLList(GraphQLString))
                                    .build())
                            .dataFetcher(new IsNodeTypeDataFetcher())
                            .build(),
                    newFieldDefinition()
                            .name("properties")
                            .description("List of node properties")
                            .type(new GraphQLList(propertiesBuilder.getType()))
                            .argument(newArgument().name("names")
                                    .type(new GraphQLList(GraphQLString))
                                    .defaultValue(Collections.emptyList())
                                    .build())
                            .dataFetcher(new PropertiesDataFetcher())
                            .build(),
                    newFieldDefinition()
                            .name("children")
                            .description("List of child nodes")
                            .type(new GraphQLTypeReference("nodeList"))
                            .argument(newArgument().name("names")
                                    .description("Filter the list of children on a list of names. Only these nodes will be returned.")
                                    .type(new GraphQLList(GraphQLString))
                                    .defaultValue(Collections.emptyList())
                                    .build())
                            .argument(newArgument().name("anyType")
                                    .description("Filter the list of children on the specified nodetypes. Only nodes matching at least one of these types will be returned.")
                                    .type(new GraphQLList(GraphQLString))
                                    .defaultValue(Collections.emptyList())
                                    .build())
                            .argument(newArgument().name("properties")
                                    .description("Filter the list of children based on properties value.")
                                    .type(new GraphQLList(propertyFilterType))
                                    .defaultValue(Collections.emptyList())
                                    .build())
                            .argument(newArgument()
                                    .name("asMixin")
                                    .description("Specify a mixin that will be used for the node")
                                    .type(GraphQLString)
                                    .build())
                            .dataFetcher(new ChildrenDataFetcher())
                            .build(),
                    newFieldDefinition()
                            .name("ancestors")
                            .type(new GraphQLTypeReference("nodeList"))
                            .argument(newArgument().name("upToPath")
                                    .type(GraphQLString)
                                    .defaultValue("")
                                    .build())
                            .dataFetcher(new AncestorsDataFetcher())
                            .build());
        }
        return fieldDefinitions;
    }

    public static String escape(String name) {
        name = name.replace(":", "__").replace(".", "___");
        if (!VALID_NAME.matcher(name).matches()) {
            logger.error("Invalid name: " + name);
        }
        return name;
    }

    public static String unescape(String name) {
        return name.replace("___", ".").replace("__", ":");
    }


}
