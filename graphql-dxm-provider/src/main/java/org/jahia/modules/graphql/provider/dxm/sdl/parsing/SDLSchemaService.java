package org.jahia.modules.graphql.provider.dxm.sdl.parsing;

import graphql.GraphQLException;
import graphql.language.*;
import graphql.schema.*;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.Finder;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.FinderDataFetcher;
import org.jahia.modules.graphql.provider.dxm.sdl.fetchers.FinderFetchersFactory;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatus;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLDefinitionStatusType;
import org.jahia.modules.graphql.provider.dxm.sdl.parsing.status.SDLSchemaInfo;
import org.jahia.modules.graphql.provider.dxm.sdl.registration.SDLRegistrationService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

@Component(service = SDLSchemaService.class, immediate = true)
public class SDLSchemaService {
    private static Logger logger = LoggerFactory.getLogger(SDLSchemaService.class);

    private GraphQLSchema graphQLSchema;
    private SDLRegistrationService sdlRegistrationService;
    private Map<String, List<SDLSchemaInfo>> bundlesSDLSchemaStatus = new TreeMap<>();
    private Map<String, SDLDefinitionStatus> sdlDefinitionStatusMap = new TreeMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    public void setSdlRegistrationService(SDLRegistrationService sdlRegistrationService) {
        this.sdlRegistrationService = sdlRegistrationService;
    }

    public void generateSchema() {
        if (sdlRegistrationService != null && sdlRegistrationService.getSDLResources().size() > 0) {
            graphQLSchema = null;
            bundlesSDLSchemaStatus.clear();
            sdlDefinitionStatusMap.clear();
            SchemaParser schemaParser = new SchemaParser();
            TypeDefinitionRegistry typeDefinitionRegistry = prepareTypeRegistryDefinition();
            Map<TypeDefinition, String> sources = new HashMap<>();

            for (Map.Entry<String, URL> entry : sdlRegistrationService.getSDLResources().entrySet()) {
                if (entry.getKey().equals("graphql-core")) continue;

                String bundle = entry.getKey();
                try {
                    TypeDefinitionRegistry parsedRegistry = schemaParser.parse(new InputStreamReader(entry.getValue().openStream()));
                    parsedRegistry.types().forEach((key,type) -> sources.put(type, bundle));
                    parsedRegistry.objectTypeExtensions().forEach((type,list) -> list.forEach(ext -> sources.put(ext, bundle)));
                    typeDefinitionRegistry.merge(parsedRegistry);
                    getOrCreateBundleSDLSchemaList(bundle);
                } catch (IOException ex) {
                    logger.error("Failed to read sdl resource.", ex);
                } catch (GraphQLException ex) {
                    logger.warn("Failed to merge schema from bundle [{}]: {}", bundle, ex.getMessage());
                    getOrCreateBundleSDLSchemaList(bundle).add(new SDLSchemaInfo(bundle, SDLSchemaInfo.SDLSchemaStatus.SYNTAX_ERROR, ex.getMessage()));
                }
            }

            Set<TypeDefinition> invalidTypes = new HashSet<>();
            do {
                invalidTypes.clear();
                sources.forEach((type, bundle) -> {
                    SDLDefinitionStatus status = SDLTypeChecker.checkType(type, typeDefinitionRegistry);
                    sdlDefinitionStatusMap.put(type.getName(), status);
                    if (status.getStatus() != SDLDefinitionStatusType.OK) {
                        invalidTypes.add(type);
                        getOrCreateBundleSDLSchemaList(bundle).add(new SDLSchemaInfo(bundle, SDLSchemaInfo.SDLSchemaStatus.DEFINITION_ERROR, MessageFormat.format("Definition {0} : {1}", type.getName(), status.getStatusString())));
                    }
                });
                sources.keySet().removeAll(invalidTypes);
                invalidTypes.forEach(typeDefinitionRegistry::remove);
            } while (!invalidTypes.isEmpty());
            SDLTypeChecker.printStatuses(sdlDefinitionStatusMap);
            SchemaGenerator schemaGenerator = new SchemaGenerator();

            TypeDefinitionRegistry cleanedTypeRegistry = new TypeDefinitionRegistry();
            typeDefinitionRegistry.types().forEach((k,t)-> cleanedTypeRegistry.add(t));
            typeDefinitionRegistry.objectTypeExtensions().forEach((k,t)-> t.forEach(cleanedTypeRegistry::add));
            typeDefinitionRegistry.scalars().forEach((k,t) -> cleanedTypeRegistry.add(t));
            typeDefinitionRegistry.getDirectiveDefinitions().forEach((k,t) -> cleanedTypeRegistry.add(t));
            try {
                graphQLSchema = schemaGenerator.makeExecutableSchema(
                        SchemaGenerator.Options.defaultOptions().enforceSchemaDirectives(false),
                        cleanedTypeRegistry,
                        SDLRuntimeWiring.runtimeWiring(new SDLDirectiveWiring())
                );
            } catch (Exception e) {
                logger.warn("Invalid type definition(s) detected during schema generation: " + e.getMessage());
            }
        }
    }

    public List<GraphQLFieldDefinition> getSDLQueries() {
        List<GraphQLFieldDefinition> defs = new ArrayList<>();
        if (graphQLSchema != null) {
            List<GraphQLFieldDefinition> fieldDefinitions = graphQLSchema.getQueryType().getFieldDefinitions();
            for (GraphQLFieldDefinition fieldDefinition : fieldDefinitions) {
                GraphQLObjectType objectType = fieldDefinition.getType() instanceof GraphQLList ?
                        (GraphQLObjectType) ((GraphQLList)fieldDefinition.getType()).getWrappedType() : (GraphQLObjectType) fieldDefinition.getType();

                GraphQLDirective directive = objectType.getDirective("mapping");

                if (directive != null) {
                    String nodeType = directive.getArgument("node").getValue().toString();

                    /** implicit data fetcher for all customer types  **/
                    this.applyDefaultFetcher(defs, directive, fieldDefinition.getType(), FinderFetchersFactory.DefaultFetcherNames.ById.name());
                    this.applyDefaultFetcher(defs, directive, fieldDefinition.getType(), FinderFetchersFactory.DefaultFetcherNames.ByPath.name());

                    FinderDataFetcher fetcher = FinderFetchersFactory.getFetcher(fieldDefinition, nodeType);
                    GraphQLFieldDefinition sdlDef = GraphQLFieldDefinition.newFieldDefinition()
                            .name(fieldDefinition.getName())
                            .description(fieldDefinition.getDescription())
                            .dataFetcher(fetcher)
                            .argument(fetcher.getArguments())
                            .type(fieldDefinition.getType()) // todo return a connection to type if finder is multiple
                            .build();
                    defs.add(sdlDef);
                }
            }

            /** implicit data fetcher for all customer types  **/
            for (GraphQLType type : graphQLSchema.getAdditionalTypes()) {
               GraphQLDirective directive = ((GraphQLObjectType)type).getDirective("mapping");
               this.applyDefaultFetcher(defs, directive, (GraphQLOutputType)type, FinderFetchersFactory.DefaultFetcherNames.ById.name());
               this.applyDefaultFetcher(defs, directive, (GraphQLOutputType)type, FinderFetchersFactory.DefaultFetcherNames.ByPath.name());
            }

        }
        return defs;
    }

    public List<GraphQLType> getSDLTypes() {
        List<GraphQLType> types = new ArrayList<>();
        if (graphQLSchema != null) {
            List<String> reservedType = Arrays.asList("Query", "Mutation", "Subscription");
            for (Map.Entry<String, GraphQLType> gqlTypeEntry : graphQLSchema.getTypeMap().entrySet()) {
                if (!gqlTypeEntry.getKey().startsWith("__") && !reservedType.contains(gqlTypeEntry.getKey()) && !(gqlTypeEntry.getValue() instanceof GraphQLScalarType)) {
                    types.add(gqlTypeEntry.getValue());
                }
            }
        }
        return types;
    }

    public GraphQLSchema getGraphQLSchema() {
        return graphQLSchema;
    }

    public Map<String, SDLDefinitionStatus> getSdlDefinitionStatusMap() {
        return sdlDefinitionStatusMap;
    }

    public Map<String, List<SDLSchemaInfo>> getBundlesSDLSchemaStatus() {
        return bundlesSDLSchemaStatus;
    }

    private List<SDLSchemaInfo> getOrCreateBundleSDLSchemaList(String bundle) {
        if (!bundlesSDLSchemaStatus.containsKey(bundle)) {
            bundlesSDLSchemaStatus.put(bundle, new LinkedList<>());
        }

        return bundlesSDLSchemaStatus.get(bundle);
    }

    /**
     *
     * @param defs
     * @param defaultFinder
     */
    private void applyDefaultFetcher(final List<GraphQLFieldDefinition> defs, final GraphQLDirective directive,
                                     GraphQLOutputType type, final String defaultFinder){
        boolean shouldIgnoreDefaultQueries = false;
        if (directive.getArgument("ignoreDefaultQueries").getValue() != null) {
            shouldIgnoreDefaultQueries = (Boolean) directive.getArgument("ignoreDefaultQueries").getValue();
        }

        if(!shouldIgnoreDefaultQueries){
            //when the type is defined in extending Query, it is type of GraphQLList like [MyType]
            if (type instanceof GraphQLList) type = (GraphQLOutputType)((GraphQLList)type).getWrappedType();

            GraphQLArgument argument = ((GraphQLObjectType)type).getDirective("mapping").getArgument("node");

            FinderFetchersFactory.FetcherTypes fetcherTypes = FinderFetchersFactory.FetcherTypes.STRING;
            if (defaultFinder.equals(FinderFetchersFactory.DefaultFetcherNames.ById.name())) fetcherTypes = FinderFetchersFactory.FetcherTypes.ID;
            else if (defaultFinder.equals(FinderFetchersFactory.DefaultFetcherNames.ByPath.name())) fetcherTypes = FinderFetchersFactory.FetcherTypes.PATH;

            if(argument!=null){
                final String finderName = type.getName() + defaultFinder;

                Finder finder = new Finder();
                finder.setType(argument.getValue().toString());
                FinderDataFetcher dataFetcher = FinderFetchersFactory.getFetcherType(finder, fetcherTypes);
                final String defaultFinderName = type.getName() + defaultFinder;
                defs.add(GraphQLFieldDefinition.newFieldDefinition()
                        .name(finderName)
                        .description("default finder for " + finderName)
                        .dataFetcher(dataFetcher)
                        .argument(dataFetcher.getArguments())
                        .type(type)
                        .build());
            }

        }
    }

    private TypeDefinitionRegistry prepareTypeRegistryDefinition() {
        TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        typeDefinitionRegistry.add(new ObjectTypeDefinition("Query"));
        typeDefinitionRegistry.add(new ScalarTypeDefinition("Date"));
        typeDefinitionRegistry.add(new ScalarTypeDefinition("Metadata"));
        typeDefinitionRegistry.add(DirectiveDefinition.newDirectiveDefinition()
                .name("mapping")
                .directiveLocations(Arrays.asList(
                        DirectiveLocation.newDirectiveLocation().name("OBJECT").build(),
                        DirectiveLocation.newDirectiveLocation().name("FIELD_DEFINITION").build()))
                .inputValueDefinitions(Arrays.asList(
                        InputValueDefinition.newInputValueDefinition().name("node").type(TypeName.newTypeName("String").build()).build(),
                        InputValueDefinition.newInputValueDefinition().name("property").type(TypeName.newTypeName("String").build()).build(),
                        InputValueDefinition.newInputValueDefinition().name("ignoreDefaultQueries").type(TypeName.newTypeName("Boolean").build()).build()))
                .build());
        typeDefinitionRegistry.add(DirectiveDefinition.newDirectiveDefinition()
                .name("description")
                .directiveLocations(Arrays.asList(
                        DirectiveLocation.newDirectiveLocation().name("OBJECT").build(),
                        DirectiveLocation.newDirectiveLocation().name("FIELD_DEFINITION").build()))
                .inputValueDefinitions(Arrays.asList(
                        InputValueDefinition.newInputValueDefinition().name("value").type(TypeName.newTypeName("String").build()).build()))
                .build());

        return typeDefinitionRegistry;
    }

}
