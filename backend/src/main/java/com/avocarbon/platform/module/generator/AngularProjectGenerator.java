package com.avocarbon.platform.module.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assembles the final Angular project file tree by combining LLM-generated
 * application files with deterministic Angular project scaffolding.
 *
 * Deterministic structural files (models, services, routes, list/form components)
 * always take priority and overwrite LLM output to guarantee valid routing and architecture.
 */
@Component
@Slf4j
public class AngularProjectGenerator {

    /**
     * Merge LLM-generated files with deterministic Angular scaffolding and boilerplate.
     *
     * @param llmFiles  application-specific files produced by McpClient/OpenAiClient
     * @param spec      parsed OpenAPI spec (used to fill README / index.html)
     * @param type      generation mode (informational only at this stage)
     * @return complete file tree ready for ZIP packaging
     */
    public Map<String, String> assemble(Map<String, String> llmFiles,
                                        ParsedOpenApiSpec spec,
                                        GenerationType type) {
        Map<String, String> files = new LinkedHashMap<>(llmFiles);
        String appName = kebab(spec.getTitle());

        log.info("Generating deterministic Angular project for '{}' ({} resources)", spec.getTitle(), spec.getResources().size());

        for (ParsedOpenApiSpec.ParsedResource resource : spec.getResources()) {
            String name      = sanitize(resource.getName());
            String className = pascal(name);
            String apiBase   = deriveBasePath(resource);

            // TypeScript model interface
            files.put("src/app/core/models/" + name + ".model.ts",
                      modelTs(className, spec, name));

            // HttpClient service
            files.put("src/app/core/services/" + name + ".service.ts",
                      serviceTs(name, className, apiBase));

            if (type == GenerationType.ANGULAR_FULL || type == GenerationType.ANGULAR_CRUD) {
                files.put("src/app/features/" + name + "/" + name + "-list/" + name + "-list.component.ts",
                          listComponentTs(name, className));
                files.put("src/app/features/" + name + "/" + name + "-list/" + name + "-list.component.html",
                          listComponentHtml(name, className));
            }

            if (type == GenerationType.ANGULAR_FULL || type == GenerationType.ANGULAR_FORMS) {
                files.put("src/app/features/" + name + "/" + name + "-form/" + name + "-form.component.ts",
                          formComponentTs(name, className, spec));
                files.put("src/app/features/" + name + "/" + name + "-form/" + name + "-form.component.html",
                          formComponentHtml(name, className, spec));
            }
        }

        if (type == GenerationType.ANGULAR_FULL) {
            files.put("src/app/app.routes.ts",    routesTs(spec));
            files.put("src/app/app.component.ts", appComponentTs(spec.getTitle()));
            files.put("src/app/app.component.html", appComponentHtml(spec));
            files.put("src/app/app.config.ts",    appConfigTs());
        }

        files.putIfAbsent("package.json",            packageJson(appName));
        files.putIfAbsent("angular.json",            angularJson(appName));
        files.putIfAbsent("tsconfig.json",           tsconfigJson());
        files.putIfAbsent("tsconfig.app.json",       tsconfigAppJson());
        files.putIfAbsent("src/main.ts",             mainTs());
        files.putIfAbsent("src/index.html",          indexHtml(spec.getTitle()));
        files.putIfAbsent("src/styles.css",          stylesCss());
        files.putIfAbsent(".gitignore",              gitIgnore());
        files.putIfAbsent("README.md",               readme(spec, type, files.size() + 9));

        log.info("Angular project assembled: {} files total", files.size());
        return files;
    }

    // -------------------------------------------------------------------------
    // Deterministic Templates
    // -------------------------------------------------------------------------

    private String modelTs(String className, ParsedOpenApiSpec spec, String resourceName) {
        ParsedOpenApiSpec.ParsedSchema schema = spec.getSchemas().stream()
                .filter(s -> s.getName().equalsIgnoreCase(className)
                          || s.getName().equalsIgnoreCase(resourceName))
                .findFirst().orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("export interface ").append(className).append(" {\n");
        if (schema != null && !schema.getProperties().isEmpty()) {
            schema.getProperties().forEach((prop, t) ->
                    sb.append("  ").append(prop).append("?: ").append(mapType(t)).append(";\n"));
        } else {
            sb.append("  id?: number;\n");
            sb.append("  name?: string;\n");
            sb.append("  createdAt?: string;\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String serviceTs(String name, String className, String apiBase) {
        return "import { Injectable, inject } from '@angular/core';\n"
             + "import { HttpClient } from '@angular/common/http';\n"
             + "import { Observable } from 'rxjs';\n"
             + "import { " + className + " } from '../models/" + name + ".model';\n\n"
             + "@Injectable({ providedIn: 'root' })\n"
             + "export class " + className + "Service {\n"
             + "  private readonly http = inject(HttpClient);\n"
             + "  private readonly baseUrl = '" + apiBase + "';\n\n"
             + "  getAll(): Observable<" + className + "[]> {\n"
             + "    return this.http.get<" + className + "[]>(this.baseUrl);\n"
             + "  }\n\n"
             + "  getById(id: number): Observable<" + className + "> {\n"
             + "    return this.http.get<" + className + ">(`${this.baseUrl}/${id}`);\n"
             + "  }\n\n"
             + "  create(item: " + className + "): Observable<" + className + "> {\n"
             + "    return this.http.post<" + className + ">(this.baseUrl, item);\n"
             + "  }\n\n"
             + "  update(id: number, item: " + className + "): Observable<" + className + "> {\n"
             + "    return this.http.put<" + className + ">(`${this.baseUrl}/${id}`, item);\n"
             + "  }\n\n"
             + "  delete(id: number): Observable<void> {\n"
             + "    return this.http.delete<void>(`${this.baseUrl}/${id}`);\n"
             + "  }\n"
             + "}\n";
    }

    private String listComponentTs(String name, String className) {
        return "import { Component, OnInit, inject } from '@angular/core';\n"
             + "import { CommonModule } from '@angular/common';\n"
             + "import { RouterLink } from '@angular/router';\n"
             + "import { " + className + "Service } from '../../../core/services/" + name + ".service';\n"
             + "import { " + className + " } from '../../../core/models/" + name + ".model';\n\n"
             + "@Component({\n"
             + "  selector: 'app-" + name + "-list',\n"
             + "  standalone: true,\n"
             + "  imports: [CommonModule, RouterLink],\n"
             + "  templateUrl: './" + name + "-list.component.html'\n"
             + "})\n"
             + "export class " + className + "ListComponent implements OnInit {\n"
             + "  private readonly service = inject(" + className + "Service);\n"
             + "  items: " + className + "[] = [];\n"
             + "  loading = false;\n\n"
             + "  ngOnInit(): void {\n"
             + "    this.loading = true;\n"
             + "    this.service.getAll().subscribe({\n"
             + "      next: data => { this.items = data; this.loading = false; },\n"
             + "      error: ()  => this.loading = false\n"
             + "    });\n"
             + "  }\n\n"
             + "  deleteItem(id: number): void {\n"
             + "    this.service.delete(id).subscribe(() => {\n"
             + "      this.items = this.items.filter(i => i.id !== id);\n"
             + "    });\n"
             + "  }\n"
             + "}\n";
    }

    private String listComponentHtml(String name, String className) {
        return "<div class=\"list-container\">\n"
             + "  <h2>" + className + "s</h2>\n"
             + "  <a routerLink=\"/" + name + "/new\">Add " + className + "</a>\n"
             + "  <p *ngIf=\"loading\">Loading…</p>\n"
             + "  <p *ngIf=\"!loading && items.length === 0\">No " + name + "s found.</p>\n"
             + "  <table *ngIf=\"!loading && items.length > 0\">\n"
             + "    <thead><tr><th>ID</th><th>Name</th><th>Actions</th></tr></thead>\n"
             + "    <tbody>\n"
             + "      <tr *ngFor=\"let item of items\">\n"
             + "        <td>{{ item.id }}</td>\n"
             + "        <td>{{ item.name }}</td>\n"
             + "        <td><button (click)=\"deleteItem(item.id!)\">Delete</button></td>\n"
             + "      </tr>\n"
             + "    </tbody>\n"
             + "  </table>\n"
             + "</div>\n";
    }

    private String formComponentTs(String name, String className, ParsedOpenApiSpec spec) {
        ParsedOpenApiSpec.ParsedSchema schema = spec.getSchemas().stream()
                .filter(s -> s.getName().equalsIgnoreCase(className)).findFirst().orElse(null);
        Map<String, String> props = schema != null ? schema.getProperties() : Map.of("name", "string");
        String controls = props.entrySet().stream()
                .filter(e -> !Set.of("id", "createdAt", "updatedAt").contains(e.getKey()))
                .map(e -> "      " + e.getKey() + ": ['', Validators.required]")
                .collect(Collectors.joining(",\n"));

        return "import { Component, inject } from '@angular/core';\n"
             + "import { CommonModule } from '@angular/common';\n"
             + "import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';\n"
             + "import { " + className + "Service } from '../../../core/services/" + name + ".service';\n\n"
             + "@Component({\n"
             + "  selector: 'app-" + name + "-form',\n"
             + "  standalone: true,\n"
             + "  imports: [CommonModule, ReactiveFormsModule],\n"
             + "  templateUrl: './" + name + "-form.component.html'\n"
             + "})\n"
             + "export class " + className + "FormComponent {\n"
             + "  private readonly fb      = inject(FormBuilder);\n"
             + "  private readonly service = inject(" + className + "Service);\n\n"
             + "  form = this.fb.group({\n"
             + controls + "\n"
             + "  });\n\n"
             + "  onSubmit(): void {\n"
             + "    if (this.form.valid) {\n"
             + "      this.service.create(this.form.value as any).subscribe({\n"
             + "        next: () => { alert('" + className + " created!'); this.form.reset(); },\n"
             + "        error: err => console.error(err)\n"
             + "      });\n"
             + "    }\n"
             + "  }\n"
             + "}\n";
    }

    private String formComponentHtml(String name, String className, ParsedOpenApiSpec spec) {
        ParsedOpenApiSpec.ParsedSchema schema = spec.getSchemas().stream()
                .filter(s -> s.getName().equalsIgnoreCase(className)).findFirst().orElse(null);
        Map<String, String> props = schema != null ? schema.getProperties() : Map.of("name", "string");

        StringBuilder sb = new StringBuilder();
        sb.append("<form [formGroup]=\"form\" (ngSubmit)=\"onSubmit()\">\n");
        sb.append("  <h2>").append(className).append(" Form</h2>\n");
        props.entrySet().stream()
                .filter(e -> !Set.of("id", "createdAt", "updatedAt").contains(e.getKey()))
                .forEach(e -> sb.append("  <div>\n")
                                .append("    <label>").append(e.getKey()).append("</label>\n")
                                .append("    <input formControlName=\"").append(e.getKey()).append("\" />\n")
                                .append("  </div>\n"));
        sb.append("  <button type=\"submit\" [disabled]=\"form.invalid\">Save</button>\n");
        sb.append("</form>\n");
        return sb.toString();
    }

    private String routesTs(ParsedOpenApiSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("import { Routes } from '@angular/router';\n\n");
        sb.append("export const routes: Routes = [\n");
        for (ParsedOpenApiSpec.ParsedResource r : spec.getResources()) {
            String n = sanitize(r.getName());
            String c = pascal(n);
            sb.append("  { path: '").append(n).append("', loadComponent: () =>\n")
              .append("      import('./features/").append(n).append("/").append(n).append("-list/").append(n).append("-list.component')\n")
              .append("        .then(m => m.").append(c).append("ListComponent) },\n");
            sb.append("  { path: '").append(n).append("/new', loadComponent: () =>\n")
              .append("      import('./features/").append(n).append("/").append(n).append("-form/").append(n).append("-form.component')\n")
              .append("        .then(m => m.").append(c).append("FormComponent) },\n");
            sb.append("  { path: '").append(n).append("/:id/edit', loadComponent: () =>\n")
              .append("      import('./features/").append(n).append("/").append(n).append("-form/").append(n).append("-form.component')\n")
              .append("        .then(m => m.").append(c).append("FormComponent) },\n");
        }
        String first = spec.getResources().isEmpty() ? "" : sanitize(spec.getResources().get(0).getName());
        sb.append("  { path: '', redirectTo: '").append(first).append("', pathMatch: 'full' }\n");
        sb.append("];\n");
        return sb.toString();
    }

    private String appComponentTs(String title) {
        return "import { Component } from '@angular/core';\n"
             + "import { RouterOutlet, RouterLink } from '@angular/router';\n\n"
             + "@Component({\n"
             + "  selector: 'app-root',\n"
             + "  standalone: true,\n"
             + "  imports: [RouterOutlet, RouterLink],\n"
             + "  templateUrl: './app.component.html'\n"
             + "})\n"
             + "export class AppComponent {\n"
             + "  title = '" + title + "';\n"
             + "}\n";
    }

    private String appComponentHtml(ParsedOpenApiSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("<nav>\n  <span>").append(spec.getTitle()).append("</span>\n  <span>\n");
        for (ParsedOpenApiSpec.ParsedResource r : spec.getResources()) {
            String n = sanitize(r.getName());
            sb.append("    <a routerLink=\"/").append(n).append("\">").append(pascal(n)).append("</a>\n");
        }
        sb.append("  </span>\n</nav>\n<main><router-outlet></router-outlet></main>\n");
        return sb.toString();
    }

    private String appConfigTs() {
        return "import { ApplicationConfig } from '@angular/core';\n"
             + "import { provideRouter } from '@angular/router';\n"
             + "import { provideHttpClient } from '@angular/common/http';\n"
             + "import { routes } from './app.routes';\n\n"
             + "export const appConfig: ApplicationConfig = {\n"
             + "  providers: [provideRouter(routes), provideHttpClient()]\n"
             + "};\n";
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private String pascal(String kebab) {
        return Arrays.stream(kebab.split("-"))
                .filter(s -> !s.isBlank())
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining());
    }

    private String mapType(String openApiType) {
        return switch (openApiType) {
            case "integer", "number" -> "number";
            case "boolean"           -> "boolean";
            case "array"             -> "any[]";
            default                  -> "string";
        };
    }

    private String deriveBasePath(ParsedOpenApiSpec.ParsedResource resource) {
        if (resource.getEndpoints().isEmpty()) {
            return "/api/" + sanitize(resource.getName()) + "s";
        }
        String raw = resource.getEndpoints().get(0).getPath();
        // Strip path parameters to get the collection path
        return raw.replaceAll("/\\{[^}]+\\}.*$", "");
    }

    // -------------------------------------------------------------------------
    // Boilerplate templates
    // -------------------------------------------------------------------------

    private String packageJson(String appName) {
        return "{\n" +
               "  \"name\": \"" + appName + "\",\n" +
               "  \"version\": \"0.0.0\",\n" +
               "  \"scripts\": {\n" +
               "    \"ng\": \"ng\",\n" +
               "    \"start\": \"ng serve\",\n" +
               "    \"build\": \"ng build\",\n" +
               "    \"test\": \"ng test\"\n" +
               "  },\n" +
               "  \"dependencies\": {\n" +
               "    \"@angular/animations\": \"^17.0.0\",\n" +
               "    \"@angular/common\": \"^17.0.0\",\n" +
               "    \"@angular/compiler\": \"^17.0.0\",\n" +
               "    \"@angular/core\": \"^17.0.0\",\n" +
               "    \"@angular/forms\": \"^17.0.0\",\n" +
               "    \"@angular/platform-browser\": \"^17.0.0\",\n" +
               "    \"@angular/router\": \"^17.0.0\",\n" +
               "    \"rxjs\": \"~7.8.0\",\n" +
               "    \"tslib\": \"^2.6.0\",\n" +
               "    \"zone.js\": \"~0.14.0\"\n" +
               "  },\n" +
               "  \"devDependencies\": {\n" +
               "    \"@angular-devkit/build-angular\": \"^17.0.0\",\n" +
               "    \"@angular/cli\": \"^17.0.0\",\n" +
               "    \"@angular/compiler-cli\": \"^17.0.0\",\n" +
               "    \"typescript\": \"~5.2.0\"\n" +
               "  }\n" +
               "}\n";
    }

    private String angularJson(String appName) {
        return "{\n" +
               "  \"$schema\": \"./node_modules/@angular/cli/lib/config/schema.json\",\n" +
               "  \"version\": 1,\n" +
               "  \"newProjectRoot\": \"projects\",\n" +
               "  \"projects\": {\n" +
               "    \"" + appName + "\": {\n" +
               "      \"projectType\": \"application\",\n" +
               "      \"root\": \"\",\n" +
               "      \"sourceRoot\": \"src\",\n" +
               "      \"prefix\": \"app\",\n" +
               "      \"architect\": {\n" +
               "        \"build\": {\n" +
               "          \"builder\": \"@angular-devkit/build-angular:browser-esbuild\",\n" +
               "          \"options\": {\n" +
               "            \"outputPath\": \"dist/" + appName + "\",\n" +
               "            \"index\": \"src/index.html\",\n" +
               "            \"main\": \"src/main.ts\",\n" +
               "            \"tsConfig\": \"tsconfig.app.json\",\n" +
               "            \"styles\": [\"src/styles.css\"]\n" +
               "          }\n" +
               "        },\n" +
               "        \"serve\": {\n" +
               "          \"builder\": \"@angular-devkit/build-angular:dev-server\",\n" +
               "          \"options\": { \"buildTarget\": \"" + appName + ":build\" }\n" +
               "        }\n" +
               "      }\n" +
               "    }\n" +
               "  }\n" +
               "}\n";
    }

    private String tsconfigJson() {
        return "{\n" +
               "  \"compileOnSave\": false,\n" +
               "  \"compilerOptions\": {\n" +
               "    \"strict\": true,\n" +
               "    \"noImplicitOverride\": true,\n" +
               "    \"noPropertyAccessFromIndexSignature\": true,\n" +
               "    \"noImplicitReturns\": true,\n" +
               "    \"noFallthroughCasesInSwitch\": true,\n" +
               "    \"skipLibCheck\": true,\n" +
               "    \"esModuleInterop\": true,\n" +
               "    \"sourceMap\": true,\n" +
               "    \"declaration\": false,\n" +
               "    \"experimentalDecorators\": true,\n" +
               "    \"moduleResolution\": \"bundler\",\n" +
               "    \"target\": \"ES2022\",\n" +
               "    \"useDefineForClassFields\": false\n" +
               "  },\n" +
               "  \"angularCompilerOptions\": {\n" +
               "    \"enableI18nLegacyMessageIdFormat\": false,\n" +
               "    \"strictInjectionParameters\": true,\n" +
               "    \"strictInputAccessModifiers\": true,\n" +
               "    \"strictTemplates\": true\n" +
               "  }\n" +
               "}\n";
    }

    private String tsconfigAppJson() {
        return "{\n" +
               "  \"extends\": \"./tsconfig.json\",\n" +
               "  \"compilerOptions\": {\n" +
               "    \"outDir\": \"./out-tsc/app\",\n" +
               "    \"types\": []\n" +
               "  },\n" +
               "  \"files\": [\"src/main.ts\"],\n" +
               "  \"include\": [\"src/**/*.d.ts\"]\n" +
               "}\n";
    }

    private String mainTs() {
        return "import { bootstrapApplication } from '@angular/platform-browser';\n" +
               "import { appConfig } from './app/app.config';\n" +
               "import { AppComponent } from './app/app.component';\n\n" +
               "bootstrapApplication(AppComponent, appConfig)\n" +
               "  .catch(err => console.error(err));\n";
    }

    private String indexHtml(String title) {
        return "<!doctype html>\n" +
               "<html lang=\"en\">\n" +
               "<head>\n" +
               "  <meta charset=\"utf-8\">\n" +
               "  <title>" + title + "</title>\n" +
               "  <base href=\"/\">\n" +
               "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
               "  <link rel=\"icon\" type=\"image/x-icon\" href=\"favicon.ico\">\n" +
               "</head>\n" +
               "<body>\n" +
               "  <app-root></app-root>\n" +
               "</body>\n" +
               "</html>\n";
    }

    private String stylesCss() {
        return "/* Global styles */\n" +
               "* { box-sizing: border-box; margin: 0; padding: 0; }\n" +
               "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }\n" +
               "nav { background: #1976d2; color: white; padding: 1rem; }\n" +
               "nav a { color: white; margin-right: 1rem; text-decoration: none; }\n" +
               "main { padding: 2rem; }\n" +
               "table { width: 100%; border-collapse: collapse; }\n" +
               "th, td { padding: .5rem 1rem; border: 1px solid #ddd; text-align: left; }\n" +
               "th { background: #f5f5f5; }\n" +
               "button { padding: .4rem .8rem; cursor: pointer; }\n" +
               "input, select, textarea { padding: .4rem; width: 100%; border: 1px solid #ccc; border-radius: 4px; }\n" +
               "form div { margin-bottom: 1rem; }\n" +
               "form label { display: block; font-weight: bold; margin-bottom: .25rem; }\n";
    }

    private String gitIgnore() {
        return "# Node\n/node_modules\n\n" +
               "# Build\n/dist\n\n" +
               "# Angular cache\n.angular\n\n" +
               "# IDEs\n.vscode\n.idea\n*.suo\n*.ntvs*\n*.njsproj\n*.sln\n*.sw?\n";
    }

    private String readme(ParsedOpenApiSpec spec, GenerationType type, int fileCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(spec.getTitle()).append("\n\n");
        sb.append("Generated by AVOCarbon AI Platform Frontend Generator.\n\n");
        sb.append("**API Version:** ").append(spec.getVersion()).append("  \n");
        sb.append("**Generation Type:** ").append(type).append("  \n");
        sb.append("**Resources:** ").append(spec.getResources().stream()
                .map(ParsedOpenApiSpec.ParsedResource::getName)
                .collect(Collectors.joining(", "))).append("  \n");
        sb.append("**Total files:** ").append(fileCount).append("\n\n");
        sb.append("## Getting Started\n\n");
        sb.append("```bash\n");
        sb.append("npm install\n");
        sb.append("ng serve\n");
        sb.append("```\n\n");
        sb.append("The app will be available at `http://localhost:4200`.\n\n");
        sb.append("## Available Resources\n\n");
        for (ParsedOpenApiSpec.ParsedResource r : spec.getResources()) {
            sb.append("- **").append(r.getName()).append("** (").append(r.getEndpoints().size()).append(" endpoints)\n");
        }
        return sb.toString();
    }

    private String kebab(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
