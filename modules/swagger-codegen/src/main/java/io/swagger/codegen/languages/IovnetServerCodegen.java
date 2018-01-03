package io.swagger.codegen.languages;


import io.swagger.codegen.*;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.properties.*;
import io.swagger.codegen.utils.ModelUtils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;
import java.io.File;

public class IovnetServerCodegen extends DefaultCodegen implements CodegenConfig {
    protected String implFolder = "/src/api";
    

    public static final String IOVNET_SERVER_UPDATE = "update";
    protected Boolean iovnetServerUpdate = Boolean.FALSE;

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "iovnet-server";
    }

    @Override
    public String getHelp() {
        return "Generates a stub for an Iovnet service control plane";
    }

    public IovnetServerCodegen() {
        super();

        apiPackage = "io.swagger.server.api";
        modelPackage = "io.swagger.server.model";

        modelTemplateFiles.put("json-object-header.mustache", "JsonObject.h");
        modelTemplateFiles.put("json-object-source.mustache", "JsonObject.cpp");
        
        modelTemplateFiles.put("interface.mustache", "Interface.h");
        
        modelTemplateFiles.put("object-header.mustache", ".h");
        modelTemplateFiles.put("object-source.mustache", ".cpp");
        modelTemplateFiles.put("object-source-defaultimpl.mustache", "DefaultImpl.cpp");

        apiTemplateFiles.put("api-header.mustache", ".h");
        apiTemplateFiles.put("api-source.mustache", ".cpp");

        embeddedTemplateDir = templateDir = "iovnet-server";

        reservedWords = new HashSet<>();

        supportingFiles.add(new SupportingFile("json-object-base-header.mustache", "src/serializer", "JsonObjectBase.h"));
        supportingFiles.add(new SupportingFile("json-object-base-source.mustache", "src/serializer", "JsonObjectBase.cpp"));
        //supportingFiles.add(new SupportingFile("cmake.mustache", "control_api", "CMakeLists.txt"));
        supportingFiles.add(new SupportingFile("service-cmake.mustache", "", "CMakeLists.txt"));
        supportingFiles.add(new SupportingFile("swagger-codegen-ignore.mustache", "", ".swagger-codegen-ignore"));


        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList("int", "char", "bool", "long", "float", "double", "int8_t", "int16_t", "uint8_t", "uint16_t", "uint32_t", "int32_t", "int64_t", "std::string"));

        typeMapping = new HashMap<String, String>();
        typeMapping.put("date", "std::string");
        typeMapping.put("DateTime", "std::string");
        typeMapping.put("string", "std::string");
        typeMapping.put("integer", "int32_t");
        typeMapping.put("long", "int64_t");
        typeMapping.put("boolean", "bool");
        typeMapping.put("array", "std::vector");
        typeMapping.put("map", "std::map");
        typeMapping.put("file", "std::string");
        typeMapping.put("object", "Object");
        typeMapping.put("binary", "std::string");
        typeMapping.put("number", "double");
        typeMapping.put("UUID", "std::string");

        super.importMapping = new HashMap<String, String>();
        importMapping.put("std::vector", "#include <vector>");
        importMapping.put("std::map", "#include <map>");
        importMapping.put("std::string", "#include <string>");
        importMapping.put("Object", "#include \"Object.h\"");

        cliOptions.clear();
        cliOptions.add(new CliOption(IOVNET_SERVER_UPDATE, "If set to TRUE the generator will not " +
                "override the implementation files", "boolean").defaultValue("false"));
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(IOVNET_SERVER_UPDATE)) {
            if(additionalProperties.get(IOVNET_SERVER_UPDATE) instanceof String){
                this.iovnetServerUpdate = Boolean.parseBoolean((String)additionalProperties.get(IOVNET_SERVER_UPDATE));
            }
        }

        additionalProperties.put(IOVNET_SERVER_UPDATE, this.iovnetServerUpdate);

        if (!this.iovnetServerUpdate) {
            apiTemplateFiles.put("api-impl-header.mustache", ".h");
            apiTemplateFiles.put("api-impl-source.mustache", ".cpp");

            //apiTemplateFiles.put("service-header.mustache", ".h");
            //apiTemplateFiles.put("service-source-api.mustache", ".cpp");
        }

        additionalProperties.put("modelNamespaceDeclarations", modelPackage.split("\\."));
        additionalProperties.put("modelNamespace", modelPackage.replaceAll("\\.", "::"));
        additionalProperties.put("apiNamespaceDeclarations", apiPackage.split("\\."));
        additionalProperties.put("apiNamespace", apiPackage.replaceAll("\\.", "::"));
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle
     * escaping those terms here. This logic is only called if a variable
     * matches the reserved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        return "_" + name; // add an underscore to the name
    }

    @Override
    public String toModelImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        } else {
            return "#include \"" + name + "JsonObject.h\"";
        }
    }

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
        Set<String> oldImports = codegenModel.imports;
        codegenModel.imports = new HashSet<>();
        List<String> interfaceImports = new ArrayList<String>();
        for (String imp : oldImports) {
            String newImp = toModelImport(imp);
            if (!newImp.isEmpty()) {
                codegenModel.imports.add(newImp); 
                if (!importMapping.containsKey(imp)){
                    interfaceImports.add(imp);
                }       
            }
        }
        codegenModel.vendorExtensions.put("x-interface-imports", interfaceImports); 
        
        List<CodegenProperty> cpl = codegenModel.vars;
        for(CodegenProperty cp : cpl){
            List<Map<String, String>> l = (List<Map<String, String>>) cp.vendorExtensions.get("x-key-list");
            if(l != null){
                l.get(l.size() - 1).put("lastKey", "true");
                for(int i = 0; i < l.size(); i++){
                    l.get(i).put("varName", toVarName(l.get(i).get("name"))); //used in update method
                    l.get(i).put("getter", toLowerCamelCase("get_" + l.get(i).get("varName")));
                    l.get(i).put("setter", toLowerCamelCase("set_" + l.get(i).get("varName")));
                    if(l.get(i).get("type").equals("integer")){
                      String format = l.get(i).get("format");
                        l.get(i).put("type", format + "_t");
                        
                    }
                    if(l.get(i).get("type").equals("string")){
                        if(l.get(i).get("isEnum") != null){
                            if(l.get(i).get("x-typedef") != null){
                                 String enumType = initialCaps((String)l.get(i).get("x-typedef")) + "Enum";        
                                 l.get(i).put("type", enumType); 
                            }
                            else{
                                String enumType = cp.nameInCamelCase + toUpperCamelCase(l.get(i).get("name")) + "Enum";
                                l.get(i).put("type", enumType);
                            }
                        } 
                        else   
                          l.get(i).put("type", "std::string");
                    }
                }
            }
        }
        
        //at this point only ports has this vendorExtensions
        if(codegenModel.vendorExtensions.get("x-inherits-from") != null)
            codegenModel.vendorExtensions.put("x-classname-inherited", "Port"); 
        
        if(codegenModel.vendorExtensions.get("x-parent") != null){
            if(codegenModel.vendorExtensions.get("x-parent").equals(codegenModel.name)){
                codegenModel.vendorExtensions.remove("x-parent");
                codegenModel.vendorExtensions.put("x-inherits-from", "iovnet::service::IOModule");
            }
        }
        
        
        
        
        
        return codegenModel;
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation,
      Map<String, Model> definitions, Swagger swagger) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation, definitions, swagger);

        
        //check the kind of httpMethod and basing on it
        //initialize the method string and add response code
        String method = null;
        //get the bodyParam
        CodegenParameter bodyParam = op.bodyParam;
        
        if(op.httpMethod.equals("POST")){
            op.returnType = null;
            op.returnBaseType = null;
            op.vendorExtensions.put("x-response-code", "Created");
            method = "add";
        }
        else if(op.httpMethod.equals("DELETE")){
            op.vendorExtensions.put("x-response-code", "Ok");
            method = "del";
        }
        else if(op.httpMethod.equals("PUT")){
            op.vendorExtensions.put("x-response-code", "Ok");
            if(bodyParam != null && bodyParam.isPrimitiveType)
                method = "set";
            else if(bodyParam != null)
                method = "replace";
        }
        else if(op.httpMethod.equals("PATCH")){
            op.vendorExtensions.put("x-response-code", "Ok");
            if(bodyParam != null && bodyParam.isPrimitiveType)
                method = "set";
            else if(bodyParam != null)
                method = "update";
        }   
        else if(op.httpMethod.equals("GET")){
            op.vendorExtensions.put("x-response-code", "Ok");
            method = "get";
        }
        
        if (op.operationId.contains("List")) {
            op.vendorExtensions.put("x-is-list", true);
            if(op.httpMethod.equals("PATCH"))
                op.vendorExtensions.put("x-is-list-update", true);//now we not support update of list
            if(bodyParam != null && !bodyParam.isPrimitiveType){
                op.bodyParam.dataType = op.bodyParam.dataType.replace(">", "JsonObject>");  
                op.bodyParam.baseType += "JsonObject";
            }
            if(op.httpMethod.equals("PUT"))
                op.vendorExtensions.put("x-is-list-update", true);//now we not support update of list
            if(op.returnType != null && !op.returnTypeIsPrimitive)
                op.returnType = op.returnType.replace(">", "JsonObject>");
            //if(method.equals("del") || method.equals("get") || method.equals("add")) //in case of list we return the whole object and not only                one element
              //  method += "All";
        }
        else {
            if(op.returnType != null && !op.returnTypeIsPrimitive)
                op.returnType += "JsonObject";
            if(bodyParam != null && !bodyParam.isPrimitiveType)
                op.bodyParam.dataType += "JsonObject";
        }

        if(bodyParam != null)
            op.bodyParam.paramName = "value";
            
        op.vendorExtensions.put("x-call-sequence-method", getCallMethodSequence(method, path, op));

        String pathForRouter = path.replaceAll("\\{(.*?)}", ":$1");
        op.vendorExtensions.put("x-codegen-iovnet-router-path", pathForRouter);

        return op;
    }

    private String toUpperCamelCase(String stringToConvert) {
        StringBuffer result = new StringBuffer();
        Matcher m = Pattern.compile("(?:\\B_|\\b\\-|^)([a-zA-Z0-9])").matcher(stringToConvert);
        while (m.find()) {
            m.appendReplacement(result, m.group(1).toUpperCase());
        }
        m.appendTail(result);
        return result.toString();
    }

    private String toLowerCamelCase(String stringToConvert) {
        StringBuffer result = new StringBuffer();
        Matcher m = Pattern.compile("(?:\\B_|\\b\\-)([a-zA-Z0-9])").matcher(stringToConvert);
        while (m.find()) {
            m.appendReplacement(result, m.group(1).toUpperCase());
        }
        m.appendTail(result);

        String newString = Character.toLowerCase(result.charAt(0)) + result.substring(1);

        return newString;
    }
    
    private List<Map<String, String>> getCallMethodSequence(String method, String path, CodegenOperation op){
        //this list will contain the sequence of method call
        List<Map<String,String>> l = new ArrayList<Map<String,String>>();
        
        //this list will contain the path element without name 
        List<String> path_without_keys = new ArrayList<String>();
        
        //get the path element
        for(String retval : path.split("/")){
            if(retval.length() > 0 && retval.charAt(0) != '{')
                path_without_keys.add(retval);
        }
        int len = path_without_keys.size(); 
        String objectName = null;
        boolean lastCall = false;
        CodegenParameter bodyParam = op.bodyParam;
        
        if(len > 0){
            for(int i = 0; i < len; i++){
                if(i == (len - 1) && !method.equals("update"))
                    lastCall = true;
                String methodCall = null;
                Map<String, String> m = new HashMap<String, String>();
                List<String> method_parameters_name = new ArrayList<String>();
                //split the path in two substring, in particular we consider the second to get the params 
                //linked to the particular path element
                String[] st = path.split("/" + path_without_keys.get(i));   
                if(st.length <= 1)
                    break;
                for(String str : st[1].split("/")){
                    //get each key name in the path until a new path element is reached 
                    if(str.length() > 2 && str.charAt(0) == '{'){
                        str = str.replaceAll("\\{(.*?)}", "$1");
                        method_parameters_name.add(toParamName(str));
                    }
                    else if(str.length() > 0)
                        break;  
                }
                int index = method_parameters_name.size();
                //put the object name
                m.put("varName", toVarName(path_without_keys.get(i)));
                //if i == 0 the path element is the service
                if(i == 0)
                    methodCall = "get_iomodule";
                else if(lastCall && i != 1) //the last path element has a particular method basing on httpMethod
                    methodCall = path_without_keys.get(i-1) + "->" + method + toUpperCamelCase(path_without_keys.get(i));
                else if(lastCall && i == 1)
                    methodCall = path_without_keys.get(i-1) + "." + method + toUpperCamelCase(path_without_keys.get(i));
                else if(i == 1) //the second path element has a get method but called by .
                    methodCall = path_without_keys.get(i-1) + ".get" + toUpperCamelCase(path_without_keys.get(i));
                else //the remaining methods are all get
                    methodCall = path_without_keys.get(i-1) + "->get" + toUpperCamelCase(path_without_keys.get(i));
                if(lastCall && op.operationId.contains("List")){
                    methodCall += "List(";
                    //methodCall += "i";
                }
                //else if(lastCall && bodyParam != null && !method.equals("replace")) //the last method call take only the body param, but if the method is replace need also the keys
                    //methodCall += bodyParam.paramName;
                else{
                    methodCall += "(";
                    for(int j = 0; j < index; j++){ //take all the parameter for the method
                        methodCall = methodCall + method_parameters_name.get(j);
                        if(j < index - 1)
                            methodCall += ", ";
                    }
                }
                if(bodyParam != null && lastCall){
                  if(index != 0) //if there are keys index is != 0
                    methodCall += ", ";
                  methodCall += bodyParam.paramName;
                }  
                methodCall += ")";
                //check if is the lastCall method and if the returnType is not primitive in order to call the toJsonObject() properly
                if(op.returnType != null && lastCall && !op.returnTypeIsPrimitive && !op.operationId.contains("List")){
                    if(i == 0)
                        methodCall += ".";
                    else
                        methodCall += "->";
                }

                if(methodCall.indexOf("get_iomodule") != -1){
                    m.put("methodCall", methodCall);
                } else if(methodCall.toLowerCase().indexOf("get_ports") != -1){
                    m.put("methodCall", methodCall.replaceAll("(?i)(get_ports)", "get_ports"));
                } else {
                    m.put("methodCall", toLowerCamelCase(methodCall));
                }

                if(lastCall){
                //mark the last method call, useful to determine if have to apply the return in template
                  m.put("lastCall", "true");   
                }   
                l.add(m);
                //if the method is update and this is the last object call the update on the last element
                if(method.equals("update") && i == (len - 1)){
                    Map<String, String> m2 = new HashMap<String, String>();
                    m2.put("varName", toVarName(path_without_keys.get(i)));
                    if(i == 0)
                        methodCall = toUpperCamelCase(path_without_keys.get(i)) + "." + method + "(" + bodyParam.paramName + ")";
                    else
                        methodCall = toUpperCamelCase(path_without_keys.get(i)) + "->" + method + "(" + bodyParam.paramName + ")";

                    if(methodCall.indexOf("get_iomodule") != -1){
                        m2.put("methodCall", methodCall);
                    } else if(methodCall.toLowerCase().indexOf("get_ports") != -1){
                        m2.put("methodCall", methodCall.replaceAll("(?i)(get_ports)", "get_ports"));
                    } else {
                        m2.put("methodCall", toLowerCamelCase(methodCall));
                    }

                    m2.put("lastCall", "true");
                    l.add(m2);
                }
            }
        }
        return l;
    }
    
    @Override
    public CodegenProperty fromProperty(String name, Property p) {
        CodegenProperty property = super.fromProperty(name, p);
        property.getter = toLowerCamelCase("get"+ getterAndSetterCapitalize(name));
        property.setter = toLowerCamelCase("set"+ getterAndSetterCapitalize(name));
        property.nameInCamelCase = toUpperCamelCase(name);
        property.name = toLowerCamelCase(name);

        return property;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        List<Object> modelsList = (List<Object>) objs.get("models");
        for(int i = 0; i < modelsList.size(); i++){     
            Map<String, Object> models = (Map<String, Object>) modelsList.get(i);
            CodegenModel model = (CodegenModel) models.get("model");
            List<CodegenProperty> lp = model.vars;
            String portsClassName = "Ports";
            for(CodegenProperty p : lp){
                List<String> lenum = p._enum;//retrieve enum list
                if(lenum != null){//if it is not empty
                    List<Map<String, String>> l = new ArrayList<Map<String, String>>(); 
                    for(int j = 0; j < lenum.size(); j++){
                        Map<String, String> mv = new HashMap<String, String>();
                        if(model.vendorExtensions.get("x-parent") == null && p.baseName.contains("type")){
                            mv.put("stringValue", lenum.get(j).toLowerCase());//here because if it is TYPE_TC the string value must be type_tc and no type_cls 
                            if(lenum.get(j).equals("TYPE_TC")){
                              lenum.set(j, "TYPE_CLS");
                            }  
                            p.datatype = "IOModuleType";
                            p.vendorExtensions.put("x-is-iomodule-type", "true");
                        }
                        else{
                            if(p.vendorExtensions.get("x-typedef") != null){
                              p.datatype = initialCaps((String)p.vendorExtensions.get("x-typedef")) + "Enum";
                              p.datatypeWithEnum = p.datatype.toUpperCase();//used in ifndef clause
                            }
                            else
                              p.datatype = model.name + p.nameInCamelCase + "Enum";
                            mv.put("stringValue", lenum.get(j).toLowerCase());//save the string value 
                        }
                        mv.put("value", lenum.get(j).toUpperCase());//save the enum value
                        l.add(mv);
                        if(j < lenum.size() - 1)
                            lenum.set(j, lenum.get(j).toUpperCase() + ",");//add comma if the value there are more values 
                        else
                            lenum.set(j, lenum.get(j).toUpperCase()); 
                    }
                    if(p.allowableValues != null)
                        p.allowableValues.put("values", l); //add allowable values to enum
                }
            }

            //Add vendor extension to recognize the class Ports
            if(model.vendorExtensions.containsKey("x-inherits-from") && ((String)model.vendorExtensions.get("x-inherits-from")).equals("iovnet::service::Port")){
            	model.vendorExtensions.put("x-is-port-class", true);
            	portsClassName = model.classname;

            	for(CodegenProperty cp : lp) {
            		switch(cp.baseName.toLowerCase()){
            			case "status":
            				cp.vendorExtensions.put("x-is-port-status", true);
            				cp.vendorExtensions.put("x-has-default-impl", true);
            				break;
            			case "peer":
            				cp.vendorExtensions.put("x-is-port-peer", true);
            				cp.vendorExtensions.put("x-has-default-impl", true);
            				break;
            			case "name":
            				cp.vendorExtensions.put("x-is-port-name", true);
            				cp.vendorExtensions.put("x-has-default-impl", true);
            				break;
            			case "uuid":
            				cp.vendorExtensions.put("x-is-port-uuid", true);
            				cp.vendorExtensions.put("x-has-default-impl", true);
            				break;
            			default:
            				cp.vendorExtensions.put("x-has-default-impl", false);
            				break;
            		}
            	}
            }

            if(model.vendorExtensions.containsKey("x-inherits-from") && ((String)model.vendorExtensions.get("x-inherits-from")).equals("iovnet::service::IOModule")){
            	model.vendorExtensions.put("x-child-ports-classname", portsClassName);

            	for(CodegenProperty cp : lp) {
            		switch(cp.baseName.toLowerCase()){
            			case "name":
            				cp.vendorExtensions.put("x-is-iomodule-name", true);
            				cp.vendorExtensions.put("x-has-default-impl", true);
            				break;
            			case "uuid":
            				cp.vendorExtensions.put("x-is-iomodule-uuid", true);
            				cp.vendorExtensions.put("x-has-default-impl", true);
            				break;
            			case "type":
            				cp.vendorExtensions.put("x-is-iomodule-type", true);
            				cp.vendorExtensions.put("x-has-default-impl", true);
            				break;
            			case "ports":
            				cp.vendorExtensions.put("x-is-port-class", true);
            				break;
            			default:
            				cp.vendorExtensions.put("x-has-default-impl", false);
            				break;
            		}
            	}
            }
        }
        
        return objs;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        String classname = (String) operations.get("classname");
        operations.put("classnameSnakeUpperCase", DefaultCodegen.underscore(classname).toUpperCase());
        operations.put("classnameSnakeLowerCase", DefaultCodegen.underscore(classname).toLowerCase());
        
        String s = classname.replace("Api", "Type");
        
        List<CodegenOperation> operationList = (List<CodegenOperation>) operations.get("operation");
        for (CodegenOperation op : operationList) {
            op.httpMethod = op.httpMethod.substring(0, 1).toUpperCase() + op.httpMethod.substring(1).toLowerCase();
            List<String> names = new ArrayList<String>();
            String var = null;
            //get varname to build enum name
                    if(op.vendorExtensions.get("x-call-sequence-method") != null){
                        for(Map<String, String> m : (List<Map<String, String>>)op.vendorExtensions.get("x-call-sequence-method")){
                            for(CodegenParameter cp : op.allParams){
                                if(cp.isEnum && m.get("methodCall").contains(cp.paramName)){
                                    //cp.baseName = initialCaps(cp.baseName);
                                    if(cp.vendorExtensions.get("x-typedef") != null){
                                        cp.dataType = initialCaps((String)cp.vendorExtensions.get("x-typedef")) + "Enum";
                                        cp.enumName = cp.dataType;
                                    }
                                    else{
                                      if(cp.paramName.contains(m.get("varName")))
                                          cp.dataType = cp.enumName;
                                      else
                                          cp.dataType = initialCaps(m.get("varName")) + initialCaps(cp.baseName) + "Enum"; //enum dataType 
                                    }
                                }
                            }
                            for(CodegenParameter cp : op.pathParams){
                                if(cp.isEnum && m.get("methodCall").contains(cp.paramName)){
                                    //cp.baseName = initialCaps(cp.baseName);
                                    cp.datatypeWithEnum = initialCaps(m.get("varName")) + "JsonObject"; //enum class object
                                    if(cp.vendorExtensions.get("x-typedef") != null){
                                        cp.enumName = initialCaps((String)cp.vendorExtensions.get("x-typedef")) + "Enum";
                                    }
                                    else{
                                      if(!cp.enumName.contains(initialCaps(m.get("varName"))))
                                          cp.enumName = initialCaps(m.get("varName")) + cp.enumName; 
                                    }
                                }
                            }
                            
                            if(m.get("lastCall") == null)
                                names.add(m.get("varName"));
                            else
                                var = m.get("varName");
                        }
                    }
                    String name = null;//this string will store the enum name
                    if(names.size() > 1){
                            for(int i = 1; i < names.size(); i++){
                                    if(name != null)
                                            name += initialCaps(names.get(i));
                                    else
                                            name = initialCaps(names.get(i));
                            }
                    }
                    else if(names.size() == 1)
                            name = initialCaps(names.get(0));
            if(op.bodyParam != null && op.bodyParam.vendorExtensions.get("x-is-enum") != null){
                        op.bodyParam.isEnum = true;
                        op.bodyParam.vendorExtensions.remove("x-is-enum");
                        op.bodyParam.vendorExtensions.put("x-enum-class", name + "JsonObject"); //enum  class name
                        //op.bodyParam.baseName = initialCaps(op.bodyParam.baseName); 
                        if(op.bodyParam.vendorExtensions.get("x-typedef") != null)
                            op.bodyParam.dataType = initialCaps((String)op.bodyParam.vendorExtensions.get("x-typedef")) + "Enum";
                        else
                            op.bodyParam.dataType = name + initialCaps(op.bodyParam.baseName) + "Enum"; //enum dataType
                        if(op.bodyParam.dataType.equals(s)){
                            op.bodyParam.dataType = "IOModuleType";
                        }
                    }
                    
            if(op.responses != null){ //in case  the return type is enum
                        for(CodegenResponse r : op.responses){
                            if(r.vendorExtensions.get("x-is-enum") != null){
                                if(r.vendorExtensions.get("x-typedef") != null)
                                  op.returnType = initialCaps((String)r.vendorExtensions.get("x-typedef")) + "Enum";                                  
                                else
                                  op.returnType = name + initialCaps(var) + "Enum";
                                if(op.returnType.equals(s + "Enum"))
                                    op.returnType = "IOModuleType";
                                op.returnBaseType = initialCaps(var);
                                op.returnSimpleType = false; 
                                op.vendorExtensions.put("x-enum-class", name + "JsonObject");
                            }
                        }
                }
        }

        return objs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs){
        Map<String, Object> apiInfo = (Map<String, Object>) objs.get("apiInfo");
        List<Map<String, Object>> apis = (List<Map<String, Object>>)apiInfo.get("apis");

        String api_classname = (String) apis.get(0).get("classname");
        objs.put("apiClassnameCamelCase", api_classname);
        objs.put("firstClassnameSnakeLowerCase", DefaultCodegen.underscore(api_classname).toLowerCase());

        String service_name = (String) apis.get(0).get("classVarName");
        service_name = service_name.toLowerCase();
        objs.put("serviceNameLowerCase", service_name);
        String service_name_camel_case = service_name.substring(0, 1).toUpperCase() + service_name.substring(1);
        objs.put("serviceNameCamelCase", service_name_camel_case);

        // Files that are use to generate a server stub
        if(!this.iovnetServerUpdate) {
          //supportingFiles.add(new SupportingFile("service-source.mustache", "src", service_name_camel_case + ".cpp"));
          supportingFiles.add(new SupportingFile("service-dp.mustache", "src", service_name_camel_case + "_dp.h"));
          supportingFiles.add(new SupportingFile("service-lib.mustache", "src", service_name_camel_case + "-lib.cpp"));
          supportingFiles.add(new SupportingFile("service-src-cmake.mustache", "src", "CMakeLists.txt"));
        }
        
        return objs;
    }


    @Override
    public String apiFilename(String templateName, String tag) {
        String result = super.apiFilename(templateName, tag);

        if (templateName.endsWith("impl-header.mustache")) {
            int ix = result.lastIndexOf('/');
            result = result.substring(0, ix) + result.substring(ix, result.length() - 2) + "Impl.h";
            result = result.replace(apiFileFolder(), implFileFolder());
        } else if (templateName.endsWith("impl-source.mustache")) {
            int ix = result.lastIndexOf('/');
            result = result.substring(0, ix) + result.substring(ix, result.length() - 4) + "Impl.cpp";
            result = result.replace(apiFileFolder(), implFileFolder());
        } else if (templateName.endsWith("service-header.mustache")) {
            int ix = result.lastIndexOf('/');
            result = result.substring(0, ix) + result.substring(ix, result.length() - 5) + ".h";
            result = result.replace(apiFileFolder(), outputFolder + "/src");
        } else if (templateName.endsWith("service-source-api.mustache")) {
            int ix = result.lastIndexOf('/');
            result = result.substring(0, ix) + result.substring(ix, result.length() - 7) + "Api.cpp";
            result = result.replace(apiFileFolder(), outputFolder + "/src");
        }
        return result;
    }

    @Override
    public String toApiFilename(String name) {
        return initialCaps(name) + "Api";
    }

    /**
     * Optional - type declaration. This is a String which is used by the
     * templates to instantiate your types. There is typically special handling
     * for different property types
     *
     * @return a string value used as the `dataType` field for model templates,
     *         `returnType` for api templates
     */
    @Override
    public String getTypeDeclaration(Property p) {
        String swaggerType = getSwaggerType(p);

        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return getSwaggerType(p) + "<" + getTypeDeclaration(inner) + ">";
        }
        if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();
            return getSwaggerType(p) + "<std::string, " + getTypeDeclaration(inner) + ">";
        }
        if (p instanceof StringProperty || p instanceof DateProperty
                || p instanceof DateTimeProperty || p instanceof FileProperty
                || languageSpecificPrimitives.contains(swaggerType)) {
            return toModelName(swaggerType);
        }

        return swaggerType;
    }

    @Override
    public String toDefaultValue(Property p) {
        if (p instanceof StringProperty) {
            return "\"\"";
        } else if (p instanceof BooleanProperty) {
            return "false";
        } else if (p instanceof DateProperty) {
            return "\"\"";
        } else if (p instanceof DateTimeProperty) {
            return "\"\"";
        } else if (p instanceof DoubleProperty) {
            return "0.0";
        } else if (p instanceof FloatProperty) {
            return "0.0f";
        } else if (p instanceof IntegerProperty || p instanceof BaseIntegerProperty) {
            return "0";
        } else if (p instanceof LongProperty) {
            return "0L";
        } else if (p instanceof DecimalProperty) {
            return "0.0";
        } else if (p instanceof MapProperty) {
            MapProperty ap = (MapProperty) p;
            String inner = getSwaggerType(ap.getAdditionalProperties());
            return "std::map<std::string, " + inner + ">()";
        } else if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            String inner = getSwaggerType(ap.getItems());
            return "std::vector<" + inner + ">()";
        } else if (p instanceof RefProperty) {
            RefProperty rp = (RefProperty) p;
            return toModelName(rp.getSimpleRef());
        }
        return "nullptr";
    }

    /**
     * Location to write model files. You can use the modelPackage() as defined
     * when the class is instantiatedolder
     */
    public String modelFileFolder() {
        return outputFolder + "/src";
    }

    /**
     * Location to write api files. You can use the apiPackage() as defined when
     * the class is instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + "/src/api";
    }

    private String implFileFolder() {
        return outputFolder + "/" + implFolder;
    }

    /**
     * Optional - swagger type conversion. This is used to map swagger types in
     * a `Property` into either language specific types via `typeMapping` or
     * into complex models if there is not a mapping.
     *
     * @return a string value of the type or complex model for this property
     * @see io.swagger.models.properties.Property
     */
    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if(swaggerType.equals("integer")){
          String format = p.getFormat();
          type = format + "_t";
          return type; 
        }
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type))
                return toModelName(type);
        } else
            type = swaggerType;
        return toModelName(type);
    }

    @Override
    public String toModelName(String type) {
        if (typeMapping.keySet().contains(type) || typeMapping.values().contains(type)
                || importMapping.values().contains(type) || defaultIncludes.contains(type)
                || languageSpecificPrimitives.contains(type)) {
            return type;
        } else {
            return Character.toUpperCase(type.charAt(0)) + type.substring(1);
        }
    }
    
    @Override
    public String toModelFilename(String name) {
        //name = name.replace("Schema", "");
        return initialCaps(name);
    }

    @Override
    public String toVarName(String name) {
        String newName = Character.toLowerCase(name.charAt(0)) + name.substring(1);

        if (typeMapping.keySet().contains(name) || typeMapping.values().contains(name)
                || importMapping.values().contains(name) || defaultIncludes.contains(name)
                || languageSpecificPrimitives.contains(name)) {
            return toLowerCamelCase(newName);
        }

        if (name.length() > 1) {
            return toLowerCamelCase(newName);
            //Character.toUpperCase(name.charAt(0)) + name.substring(1)
        }

        return toLowerCamelCase(newName);
    }

    @Override
    public void processSwagger(Swagger swagger) {
        File folder = new File(outputFolder + "/src");
        File[] listOfFiles = folder.listFiles();
        File interfaceFolder = new File(outputFolder + "/src/interface");
        interfaceFolder.mkdir();
        File modelFolder = new File(outputFolder + "/src/serializer");
        modelFolder.mkdir();
        File sourceFolder = new File(outputFolder + "/src/src");
        sourceFolder.mkdir();
        for(File f : listOfFiles){
            if(f.getName().contains("Interface.h")){
                f.renameTo(new File(outputFolder + "/src/interface/" + f.getName()));
            }
            else if(f.getName().contains("JsonObject.h") || f.getName().contains("JsonObject.cpp")){
                f.renameTo(new File(outputFolder + "/src/serializer/" + f.getName()));
            }
            else if((f.getName().contains("DefaultImpl.cpp") || f.getName().contains(".h")) && !f.getName().contains("_dp.h"))
                f.renameTo(new File(outputFolder + "/src/src/" + f.getName()));
        }
    }

    @Override
    public String toApiName(String type) {
        return Character.toUpperCase(type.charAt(0)) + type.substring(1) + "Api";
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }
    
    

}
