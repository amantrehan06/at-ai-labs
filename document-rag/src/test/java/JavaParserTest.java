import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

public class JavaParserTest {
    
    public static void main(String[] args) {
        // Test with a simple Java class
        String javaCode = """
            package com.codeassistant.service.ai;
            
            import org.springframework.stereotype.Service;
            
            @Service
            public class GroqAIChatService implements AIChatService {
                
                private final Map<AnalysisType, AnalysisStrategy> analysisStrategies;
                
                public GroqAIChatService(AIServiceManager aiServiceManager,
                                       List<AnalysisStrategy> strategies) {
                    this.analysisStrategies = strategies.stream()
                        .collect(Collectors.toMap(AnalysisStrategy::getAnalysisType, strategy -> strategy));
                }
                
                @Override
                public AnalysisResponse analyzeCode(AnalysisRequest request) throws AIServiceException {
                    try {
                        AnalysisStrategy strategy = analysisStrategies.get(request.getAnalysisType());
                        if (strategy == null) {
                            throw new AIServiceException("No analysis strategy found for type: " + request.getAnalysisType());
                        }
                        
                        ChatLanguageModel chatModel = aiServiceManager.getModel(AIServiceConstants.GROQ_SERVICE, null);
                        List<ChatMessage> messages = strategy.buildMessages(request).toLangChain4jMessages();
                        
                        Response<AiMessage> response = chatModel.generate(messages);
                        String analysis = response.content().text();
                        
                        return AnalysisResponse.builder()
                            .analysis(analysis)
                            .analysisType(request.getAnalysisType())
                            .language(request.getLanguage())
                            .success(true)
                            .build();
                            
                    } catch (Exception e) {
                        throw new AIServiceException("Failed to analyze code: " + e.getMessage(), e);
                    }
                }
            }
            """;
        
        try {
            // Parse the Java code
            CompilationUnit cu = StaticJavaParser.parse(javaCode);
            
            System.out.println("=== JavaParser Test Results ===");
            
            // Get package
            String packageName = cu.getPackageDeclaration()
                .map(pkg -> pkg.getNameAsString())
                .orElse("default");
            System.out.println("Package: " + packageName);
            
            // Get all classes
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                System.out.println("\nClass: " + cls.getNameAsString());
                System.out.println("Type: " + (cls.isInterface() ? "interface" : "class"));
                System.out.println("Start Line: " + cls.getBegin().map(pos -> pos.line).orElse(-1));
                System.out.println("End Line: " + cls.getEnd().map(pos -> pos.line).orElse(-1));
                
                // Get methods from this class
                cls.getMethods().forEach(method -> {
                    System.out.println("  Method: " + method.getNameAsString());
                    System.out.println("    Start Line: " + method.getBegin().map(pos -> pos.line).orElse(-1));
                    System.out.println("    End Line: " + method.getEnd().map(pos -> pos.line).orElse(-1));
                    System.out.println("    Return Type: " + method.getType());
                    System.out.println("    Parameters: " + method.getParameters());
                });
                
                // Get constructors
                cls.getConstructors().forEach(constructor -> {
                    System.out.println("  Constructor: " + constructor.getNameAsString());
                    System.out.println("    Start Line: " + constructor.getBegin().map(pos -> pos.line).orElse(-1));
                    System.out.println("    End Line: " + constructor.getEnd().map(pos -> pos.line).orElse(-1));
                    System.out.println("    Parameters: " + constructor.getParameters());
                });
                
                // Get fields
                cls.getFields().forEach(field -> {
                    System.out.println("  Field: " + field.getVariables());
                    System.out.println("    Type: " + field.getElementType());
                    System.out.println("    Start Line: " + field.getBegin().map(pos -> pos.line).orElse(-1));
                    System.out.println("    End Line: " + field.getEnd().map(pos -> pos.line).orElse(-1));
                });
            });
            
            System.out.println("\n=== Test Completed Successfully ===");
            
        } catch (Exception e) {
            System.err.println("Error parsing Java code: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 