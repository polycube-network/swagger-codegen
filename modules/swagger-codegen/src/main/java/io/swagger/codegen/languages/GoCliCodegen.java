package io.swagger.codegen.languages;


import com.sun.org.apache.xpath.internal.operations.Bool;
import io.swagger.codegen.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class GoCliCodegen extends GoClientCodegen {
    static Logger LOGGER = LoggerFactory.getLogger(GoCliCodegen.class);

    public static final String GO_CLI_IMPORT_PATH = "goCliImportPath";
    public static final String GO_CLI_GEN_AUTOCOMPLETE = "goCliAutocomplete";

    protected String packageName = "swagger";
    protected String packageVersion = "1.0.0";
    protected String goCliImportPath = "go-cli";
    protected Boolean goCliAutocomplete = Boolean.FALSE;

    @Override
    public CodegenType getTag() {
        return CodegenType.OTHER;
    }

    @Override
    public String getName() {
        return "go-cli";
    }

    @Override
    public String getHelp() {
        return "Generates a Go CLI library (beta).";
    }

    public GoCliCodegen() {
        super();

        outputFolder = "go-cli/cmd";
        modelTemplateFiles.clear();
        apiTemplateFiles.clear();

        //modelTemplateFiles.put("model.mustache", ".go");
        apiTemplateFiles.put("create.mustache", "_create.go");
        apiTemplateFiles.put("delete.mustache", "_delete.go");
        apiTemplateFiles.put("retrieve.mustache", "_retrieve.go");
        apiTemplateFiles.put("update.mustache", "_update.go");

        modelDocTemplateFiles.clear();
        apiDocTemplateFiles.clear();
        //modelDocTemplateFiles.put("model_doc.mustache", ".md");
        //apiDocTemplateFiles.put("api_doc.mustache", ".md");

        embeddedTemplateDir = templateDir = "go-cli";

        cliOptions.clear();
        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_NAME, "Go-cli package name (convention: lowercase).")
                .defaultValue("swagger"));
        cliOptions.add(new CliOption(GO_CLI_IMPORT_PATH, "The path of the go-cli package " +
                "under $GOPATH, without the final /"));
        cliOptions.add(new CliOption(GO_CLI_GEN_AUTOCOMPLETE, "If set generates a file called " +
                "autocomplete.go for the autocomplete bash script", "boolean").defaultValue("false"));
        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_VERSION, "Go-cli package version.")
                .defaultValue("1.0.0"));
        cliOptions.add(new CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP, "hides the timestamp when files were generated")
                .defaultValue(Boolean.TRUE.toString()));
    }

    @Override
    public void setOutputDir(String dir) {
        this.outputFolder = dir + '/' + outputFolder;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(GO_CLI_IMPORT_PATH)) {
            this.goCliImportPath = additionalProperties.get(GO_CLI_IMPORT_PATH).toString();
        }

        if (additionalProperties.containsKey(GO_CLI_GEN_AUTOCOMPLETE)) {
            this.goCliAutocomplete = (Boolean)additionalProperties.get(GO_CLI_GEN_AUTOCOMPLETE);
        }

        additionalProperties.put(GO_CLI_IMPORT_PATH, this.goCliImportPath);
        additionalProperties.put(GO_CLI_GEN_AUTOCOMPLETE, this.goCliAutocomplete);

        supportingFiles.clear();
        supportingFiles.add(new SupportingFile("main.mustache", "..", "main.go"));
        supportingFiles.add(new SupportingFile("root.mustache", "", "root.go"));

        if(this.goCliAutocomplete){
            supportingFiles.add(new SupportingFile("autocomplete.mustache", "..", "autocomplete.go"));
        }
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator;
    }

    @Override
    public String apiDocFileFolder() {
        return (outputFolder + "/" + apiDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String modelDocFileFolder() {
        return (outputFolder + "/" + modelDocPath).replace('/', File.separatorChar);
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        @SuppressWarnings("unchecked")
        Map<String, Object> objectMap = (Map<String, Object>) objs.get("operations");
        @SuppressWarnings("unchecked")
        List<CodegenOperation> operations = (List<CodegenOperation>) objectMap.get("operation");
        for (CodegenOperation operation : operations) {
            // http method verb conversion (e.g. PUT => Put)
            operation.httpMethod = camelize(operation.httpMethod.toLowerCase());
        }

        // remove model imports to avoid error
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        if (imports == null)
            return objs;

        Iterator<Map<String, String>> iterator = imports.iterator();
        while (iterator.hasNext()) {
            String _import = iterator.next().get("import");
            if (_import.startsWith(apiPackage()))
                iterator.remove();
        }
        // if the return type is not primitive, import encoding/json
        //for (CodegenOperation operation : operations) {
        //    if(operation.returnBaseType != null && needToImport(operation.returnBaseType)) {
        //        imports.add(createMapping("import", "encoding/json"));
        //        break; //just need to import once
        //    }
        //}

        // this will only import "fmt" if there are items in pathParams
        for (CodegenOperation operation : operations) {
            if(operation.pathParams != null && operation.pathParams.size() > 0) {
                imports.add(createMapping("import", "fmt"));
                break; //just need to import once
            }
        }


        // recursively add import for mapping one type to multiple imports
        List<Map<String, String>> recursiveImports = (List<Map<String, String>>) objs.get("imports");
        if (recursiveImports == null)
            return objs;

        ListIterator<Map<String, String>> listIterator = imports.listIterator();
        while (listIterator.hasNext()) {
            String _import = listIterator.next().get("import");
            // if the import package happens to be found in the importMapping (key)
            // add the corresponding import package to the list
            if (importMapping.containsKey(_import)) {
                listIterator.add(createMapping("import", importMapping.get(_import)));
            }
        }

        return objs;
    }
}
