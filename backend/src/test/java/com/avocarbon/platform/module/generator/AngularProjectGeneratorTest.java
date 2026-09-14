package com.avocarbon.platform.module.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AngularProjectGeneratorTest {

    private AngularProjectGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new AngularProjectGenerator();
    }

    @Test
    void testDeterministicScaffolding_GeneratesProductFiles() {
        // Arrange: create a mock parsed OpenAPI spec representing a 'product' resource
        ParsedOpenApiSpec.ParsedSchema productSchema = ParsedOpenApiSpec.ParsedSchema.builder()
                .name("Product")
                .properties(Map.of("name", "string", "price", "number"))
                .build();

        ParsedOpenApiSpec.ParsedEndpoint getEndpoint = ParsedOpenApiSpec.ParsedEndpoint.builder()
                .method("GET")
                .path("/products")
                .build();

        ParsedOpenApiSpec.ParsedResource productResource = ParsedOpenApiSpec.ParsedResource.builder()
                .name("products")
                .endpoints(List.of(getEndpoint))
                .build();

        ParsedOpenApiSpec spec = ParsedOpenApiSpec.builder()
                .title("Test API")
                .version("1.0.0")
                .schemas(List.of(productSchema))
                .resources(List.of(productResource))
                .build();

        // Act
        Map<String, String> result = generator.assemble(Collections.emptyMap(), spec, GenerationType.ANGULAR_FULL);

        // Assert: 1. Product model is generated
        assertTrue(result.containsKey("src/app/core/models/products.model.ts"));
        String modelContent = result.get("src/app/core/models/products.model.ts");
        assertTrue(modelContent.contains("export interface Products"), "Model class name mismatch");
        assertTrue(modelContent.contains("name?: string;"), "Missing model property");

        // Assert: 2. Product service is generated with CRUD methods
        assertTrue(result.containsKey("src/app/core/services/products.service.ts"));
        String serviceContent = result.get("src/app/core/services/products.service.ts");
        assertTrue(serviceContent.contains("export class ProductsService"), "Service class name mismatch");
        assertTrue(serviceContent.contains("getAll(): Observable<Products[]>"), "Missing CRUD method getAll");
        assertTrue(serviceContent.contains("create(item: Products)"), "Missing CRUD method create");

        // Assert: 3. ProductListComponent is generated
        assertTrue(result.containsKey("src/app/features/products/products-list/products-list.component.ts"));
        String listTsContent = result.get("src/app/features/products/products-list/products-list.component.ts");
        assertTrue(listTsContent.contains("export class ProductsListComponent"), "List class name mismatch");
        assertTrue(listTsContent.contains("templateUrl: './products-list.component.html'"), "Template URL mismatch");

        // Assert: 4. ProductFormComponent is generated
        assertTrue(result.containsKey("src/app/features/products/products-form/products-form.component.ts"));
        String formTsContent = result.get("src/app/features/products/products-form/products-form.component.ts");
        assertTrue(formTsContent.contains("export class ProductsFormComponent"), "Form class name mismatch");
        assertTrue(formTsContent.contains("templateUrl: './products-form.component.html'"), "Form template URL mismatch");

        // Assert: 14. List component does NOT contain Reactive Forms code
        assertFalse(listTsContent.contains("FormBuilder"), "List component should not contain Reactive Forms");
        assertFalse(listTsContent.contains("ReactiveFormsModule"), "List component should not import Reactive Forms");

        // Assert: 15. Form component contains Reactive Forms code
        assertTrue(formTsContent.contains("FormBuilder"), "Form component must use FormBuilder");
        assertTrue(formTsContent.contains("ReactiveFormsModule"), "Form component must import ReactiveFormsModule");

        // Assert: 9 & 10. Routes are generated correctly
        assertTrue(result.containsKey("src/app/app.routes.ts"));
        String routesContent = result.get("src/app/app.routes.ts");
        assertTrue(routesContent.contains("path: 'products'"), "Missing product list route");
        assertTrue(routesContent.contains("path: 'products/new'"), "Missing product new route");
        assertTrue(routesContent.contains("redirectTo: 'products'"), "Missing root redirect");

        // Assert: 11-13. No Default* classes are generated
        assertFalse(result.containsKey("src/app/core/models/default.model.ts"));
        assertFalse(result.containsKey("src/app/core/services/default.service.ts"));
        assertFalse(result.containsKey("src/app/features/default/default-list/default-list.component.ts"));
    }

    @Test
    void testOpenApiParser_UntaggedEndpointsInferResourceFromPath() {
        // Arrange
        String openApiYaml = """
                openapi: 3.0.1
                info:
                  title: Test API
                  version: 1.0.0
                paths:
                  /products:
                    get:
                      summary: Get products
                    post:
                      summary: Create product
                  /products/{id}:
                    get:
                      summary: Get product by id
                """;
        OpenApiParser parser = new OpenApiParser();

        // Act
        ParsedOpenApiSpec spec = parser.parse(openApiYaml, null);

        // Assert
        assertEquals(1, spec.getResources().size(), "Should infer 1 resource");
        ParsedOpenApiSpec.ParsedResource resource = spec.getResources().get(0);
        assertEquals("products", resource.getName(), "Resource name should be inferred from path prefix");
        assertEquals(3, resource.getEndpoints().size(), "Resource should have 3 endpoints");

        // Verify that tags are empty list in the endpoints
        assertTrue(resource.getEndpoints().get(0).getTags().isEmpty(), "Tags should be empty, not 'default'");
    }
}
