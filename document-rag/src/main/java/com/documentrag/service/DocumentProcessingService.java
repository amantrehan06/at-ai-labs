package com.documentrag.service;

import com.documentrag.model.DocumentUploadResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// JavaParser imports
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.Comment;

@Slf4j
@Service
public class DocumentProcessingService {

  @Autowired private DocumentChatService documentChatService;

  // In-memory document store
  private final ConcurrentHashMap<String, DocumentInfo> documentStore = new ConcurrentHashMap<>();

  public DocumentUploadResponse processJavaDocument(MultipartFile file, String sessionId) {
    DocumentUploadResponse response = new DocumentUploadResponse();

    try {
      String documentId = UUID.randomUUID().toString();
      String fileName = file.getOriginalFilename();

      // Parse Java file and extract structured information
      List<TextSegment> segments = parseJavaFile(file, documentId, sessionId);
      if (segments.isEmpty()) {
        response.setSuccess(false);
        response.setMessage(
            "Could not parse Java file. The file may be empty or contain invalid Java code.");
        return response;
      }

      // Add document to vector store for chat functionality
      documentChatService.addDocumentToVectorStore(documentId, segments, "java");

      // Store document info
      DocumentInfo docInfo =
          new DocumentInfo(
              documentId,
              fileName,
              "java",
              "Java source code file",
              file.getSize(),
                  segments.size(),
                  segments.size(), // All segments processed
              "Java code with " + segments.size() + " semantic segments");
      documentStore.put(documentId, docInfo);

      // Build response
      response.setSuccess(true);
      response.setMessage(
          "Java file uploaded successfully! You can now ask questions about your code.");
      response.setDocumentId(documentId);
      response.setFileName(fileName);
      response.setDocumentType("java");
      response.setFileSize(file.getSize());
      response.setSegmentsProcessed(segments.size());

      // Add metadata
      Map<String, Object> metadataMap = new HashMap<>();
      metadataMap.put("totalSegments", segments.size());
      metadataMap.put("processedSegments", segments.size());
      metadataMap.put("documentType", "java");
      metadataMap.put("description", "Java source code file");
      metadataMap.put("vectorStore", "Pinecone");
      metadataMap.put("codeLines", countCodeLines(segments));
      metadataMap.put("classes", countClasses(segments));
      metadataMap.put("methods", countMethods(segments));
      metadataMap.put("sessionId", sessionId);
      response.setMetadata(metadataMap);

      // Log successful Java processing
      log.info(
          "Java file processed successfully - ID: {}, Session: {}, Name: {}, Segments: {}",
          documentId,
          sessionId,
          fileName,
          segments.size());

    } catch (Exception e) {
      log.error("Error processing Java file: {}", e.getMessage(), e);
      response.setSuccess(false);
      response.setMessage("Error processing Java file: " + e.getMessage());
    }

    return response;
  }

  private boolean isValidJavaFile(MultipartFile file) {
    return file != null
        && !file.isEmpty()
        && file.getOriginalFilename() != null
        && file.getOriginalFilename().toLowerCase().endsWith(".java")
        && file.getSize() > 0
        && file.getSize() <= 100 * 1024; // 100KB limit for Java files
  }

  /** Parse Java file using JavaParser and create structured segments */
  private List<TextSegment> parseJavaFile(MultipartFile file, String documentId, String sessionId)
      throws IOException {
    List<TextSegment> segments = new ArrayList<>();

    try {
      // Read file content
      String fileContent = new String(file.getBytes());
      String fileName = file.getOriginalFilename();

      // Parse with JavaParser
      List<CodeElement> codeElements = parseJavaCodeWithParser(fileContent, fileName);

      // Convert to TextSegments with rich metadata
      for (int i = 0; i < codeElements.size(); i++) {
        CodeElement element = codeElements.get(i);
        TextSegment segment = createCodeSegment(element, documentId, sessionId, i + 1);
        segments.add(segment);
      }

      log.info(
          "Java file parsing completed - File: {}, Elements: {}, Segments: {}",
          fileName,
          codeElements.size(),
          segments.size());

    } catch (Exception e) {
      log.error("Error parsing Java file: {}", e.getMessage(), e);
      throw new IOException("Failed to parse Java file: " + e.getMessage());
    }

    return segments;
  }

  /** Parse Java code using JavaParser for accurate line numbers and structure */
  private List<CodeElement> parseJavaCodeWithParser(String fileContent, String fileName) {
    List<CodeElement> elements = new ArrayList<>();

    try {
      // Parse the Java code with JavaParser
      CompilationUnit cu = StaticJavaParser.parse(fileContent);

      // Extract package information
      String packageName =
          cu.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("default");

      // Extract imports
      List<String> imports =
          cu.getImports().stream()
              .map(ImportDeclaration::toString)
              .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

      // Parse classes and interfaces - this is the clean approach GPT recommended
      cu.findAll(ClassOrInterfaceDeclaration.class)
          .forEach(
              cls -> {
                // Add the class itself
                elements.add(createClassElement(cls, packageName));

                // Get methods from this class
                cls.getMethods()
                    .forEach(
                        method -> {
                          elements.add(
                              createMethodElement(method, cls.getNameAsString(), packageName));
                        });

                // Get constructors from this class
                cls.getConstructors()
                    .forEach(
                        constructor -> {
                          elements.add(
                              createConstructorElement(
                                  constructor, cls.getNameAsString(), packageName));
                        });

                // Get fields from this class
                cls.getFields()
                    .forEach(
                        field -> {
                          elements.add(
                              createFieldElement(field, cls.getNameAsString(), packageName));
                        });
              });

      // Parse enums
      cu.findAll(EnumDeclaration.class)
          .forEach(
              enumDecl -> {
                elements.add(createEnumElement(enumDecl, packageName));
              });

      // Parse annotation declarations
      cu.findAll(AnnotationDeclaration.class)
          .forEach(
              annDecl -> {
                elements.add(createAnnotationElement(annDecl, packageName));
              });

      // Add package and imports as separate elements
      if (!packageName.equals("default")) {
        elements.add(0, createPackageElement(packageName, cu.getPackageDeclaration().get()));
      }

      if (!imports.isEmpty()) {
        elements.add(1, createImportsElement(imports, cu.getImports()));
      }

      log.info(
          "JavaParser analysis completed - Package: {}, Classes: {}, Methods: {}, Fields: {}",
          packageName,
          cu.findAll(ClassOrInterfaceDeclaration.class).size(),
          cu.findAll(ClassOrInterfaceDeclaration.class).stream()
              .mapToInt(cls -> cls.getMethods().size())
              .sum(),
          cu.findAll(ClassOrInterfaceDeclaration.class).stream()
              .mapToInt(cls -> cls.getFields().size())
              .sum());

    } catch (Exception e) {
      log.error("Error parsing Java code with JavaParser: {}", e.getMessage(), e);
      // Fallback to simple parsing if JavaParser fails
      List<CodeElement> fallbackElements = parseJavaCodeFallback(fileContent, fileName);
      elements.addAll(fallbackElements);
    }

    return elements;
  }

  /** Create a CodeElement for a class declaration */
  private CodeElement createClassElement(ClassOrInterfaceDeclaration cls, String packageName) {
    String className = cls.getNameAsString();
    String type = cls.isInterface() ? "interface" : "class";
    String modifiers =
        cls.getModifiers().stream()
            .map(mod -> mod.toString())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
            .toString();

    // Get the full class content including annotations, modifiers, etc.
    String source = cls.toString();

    // Get accurate line numbers
    int startLine = cls.getBegin().map(pos -> pos.line).orElse(1);
    int endLine = cls.getEnd().map(pos -> pos.line).orElse(1);

    // Extract Javadoc
    String javadoc = cls.getJavadocComment().map(JavadocComment::getContent).orElse("");

    return new CodeElement(
        type, className, className, source, startLine, endLine, javadoc, packageName, modifiers);
  }

  /** Create a CodeElement for a method declaration */
  private CodeElement createMethodElement(
      MethodDeclaration method, String className, String packageName) {
    String methodName = method.getNameAsString();
    String returnType = method.getType().toString();
    String parameters = method.getParameters().toString();
    String modifiers =
        method.getModifiers().stream()
            .map(mod -> mod.toString())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
            .toString();

    // Get the full method content
    String source = method.toString();

    // Get accurate line numbers
    int startLine = method.getBegin().map(pos -> pos.line).orElse(1);
    int endLine = method.getEnd().map(pos -> pos.line).orElse(1);

    // Extract Javadoc
    String javadoc = method.getJavadocComment().map(JavadocComment::getContent).orElse("");

    return new CodeElement(
        "method",
        methodName,
        className,
        source,
        startLine,
        endLine,
        javadoc,
        packageName,
        modifiers);
  }

  /** Create a CodeElement for a constructor */
  private CodeElement createConstructorElement(
      ConstructorDeclaration constructor, String className, String packageName) {
    String constructorName = constructor.getNameAsString();
    String parameters = constructor.getParameters().toString();
    String modifiers =
        constructor.getModifiers().stream()
            .map(mod -> mod.toString())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
            .toString();

    String source = constructor.toString();
    int startLine = constructor.getBegin().map(pos -> pos.line).orElse(1);
    int endLine = constructor.getEnd().map(pos -> pos.line).orElse(1);

    String javadoc = constructor.getJavadocComment().map(JavadocComment::getContent).orElse("");

    return new CodeElement(
        "constructor",
        constructorName,
        className,
        source,
        startLine,
        endLine,
        javadoc,
        packageName,
        modifiers);
  }

  /** Create a CodeElement for a field declaration */
  private CodeElement createFieldElement(
      FieldDeclaration field, String className, String packageName) {
    String fieldNames =
        field.getVariables().stream()
            .map(var -> var.getNameAsString())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
            .toString();

    String type = field.getElementType().toString();
    String modifiers =
        field.getModifiers().stream()
            .map(mod -> mod.toString())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
            .toString();

    String source = field.toString();
    int startLine = field.getBegin().map(pos -> pos.line).orElse(1);
    int endLine = field.getEnd().map(pos -> pos.line).orElse(1);

    String javadoc = field.getJavadocComment().map(JavadocComment::getContent).orElse("");

    return new CodeElement(
        "field",
        fieldNames,
        className,
        source,
        startLine,
        endLine,
        javadoc,
        packageName,
        modifiers);
  }

  /** Create a CodeElement for an enum declaration */
  private CodeElement createEnumElement(EnumDeclaration enumDecl, String packageName) {
    String enumName = enumDecl.getNameAsString();
    String source = enumDecl.toString();
    int startLine = enumDecl.getBegin().map(pos -> pos.line).orElse(1);
    int endLine = enumDecl.getEnd().map(pos -> pos.line).orElse(1);

    String javadoc = enumDecl.getJavadocComment().map(JavadocComment::getContent).orElse("");

    return new CodeElement(
        "enum", enumName, enumName, source, startLine, endLine, javadoc, packageName, "");
  }

  /** Create a CodeElement for an annotation declaration */
  private CodeElement createAnnotationElement(AnnotationDeclaration annDecl, String packageName) {
    String annName = annDecl.getNameAsString();
    String source = annDecl.toString();
    int startLine = annDecl.getBegin().map(pos -> pos.line).orElse(1);
    int endLine = annDecl.getEnd().map(pos -> pos.line).orElse(1);

    String javadoc = annDecl.getJavadocComment().map(JavadocComment::getContent).orElse("");

    return new CodeElement(
        "annotation", annName, annName, source, startLine, endLine, javadoc, packageName, "");
  }

  /** Create a CodeElement for package declaration */
  private CodeElement createPackageElement(String packageName, PackageDeclaration packageDecl) {
    String source = packageDecl.toString();
    int startLine = packageDecl.getBegin().map(pos -> pos.line).orElse(1);
    int endLine = packageDecl.getEnd().map(pos -> pos.line).orElse(1);

    return new CodeElement(
        "package", packageName, "N/A", source, startLine, endLine, "", packageName, "");
  }

  /** Create a CodeElement for imports */
  private CodeElement createImportsElement(
      List<String> imports, List<ImportDeclaration> importDecls) {
    String source =
        importDecls.stream()
            .map(ImportDeclaration::toString)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
            .toString();

    int startLine = importDecls.get(0).getBegin().map(pos -> pos.line).orElse(1);
    int endLine = importDecls.get(importDecls.size() - 1).getEnd().map(pos -> pos.line).orElse(1);

    return new CodeElement("imports", "imports", "N/A", source, startLine, endLine, "", "N/A", "");
  }

  /** Fallback parsing if JavaParser fails */
  private List<CodeElement> parseJavaCodeFallback(String fileContent, String fileName) {
    List<CodeElement> elements = new ArrayList<>();

    // Simple parsing for demonstration
    String[] lines = fileContent.split("\\n");
    String currentClass = "UnknownClass";

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();

      if (line.startsWith("public class ") || line.startsWith("class ")) {
        currentClass = line.replaceAll(".*class\\s+([A-Za-z0-9_]+).*", "$1");
        elements.add(
            new CodeElement(
                "class", currentClass, currentClass, line, i + 1, i + 1, "", "default", ""));
      } else if (line.matches(".*public\\s+.*\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(.*\\).*\\{?")) {
        String methodName =
            line.replaceAll(".*public\\s+.*\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(.*", "$1");
        elements.add(
            new CodeElement(
                "method", methodName, currentClass, line, i + 1, i + 1, "", "default", ""));
      }
    }

    return elements;
  }

  /** Create a TextSegment from a CodeElement with rich metadata */
  private TextSegment createCodeSegment(
      CodeElement element, String documentId, String sessionId, int segmentIndex) {
    // Create comprehensive metadata for code elements
    dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();
    metadata.add("documentId", documentId);
    metadata.add("sessionId", sessionId);
    metadata.add("chunkIndex", String.valueOf(segmentIndex));
    metadata.add("type", element.type);
    metadata.add("name", element.name);
    metadata.add("class", element.className);
    metadata.add("package", element.packageName);
    metadata.add("modifiers", element.modifiers);
    metadata.add("source", element.source);
    metadata.add("startLine", String.valueOf(element.startLine));
    metadata.add("endLine", String.valueOf(element.endLine));
    metadata.add("javadoc", element.javadoc);
    metadata.add("contentType", "java_code");
    metadata.add("processingTimestamp", String.valueOf(System.currentTimeMillis()));

    // Create the segment text with context
    String segmentText =
        String.format(
            "[%s] %s.%s (Lines %d-%d)\n%s",
            element.type.toUpperCase(),
            element.className,
            element.name,
            element.startLine,
            element.endLine,
            element.source);

    return TextSegment.from(segmentText, metadata);
  }

  private int countCodeLines(List<TextSegment> segments) {
    return segments.stream()
        .mapToInt(
            seg -> {
              try {
                int startLine = Integer.parseInt(seg.metadata().get("startLine"));
                int endLine = Integer.parseInt(seg.metadata().get("endLine"));
                return endLine - startLine + 1;
              } catch (NumberFormatException e) {
                return 0;
              }
            })
        .sum();
  }

  private int countClasses(List<TextSegment> segments) {
    return (int)
        segments.stream().filter(seg -> "class".equals(seg.metadata().get("type"))).count();
  }

  private int countMethods(List<TextSegment> segments) {
    return (int)
        segments.stream().filter(seg -> "method".equals(seg.metadata().get("type"))).count();
  }

  /** Inner class to represent parsed Java code elements */
  private static class CodeElement {
    final String type; // class, method, field, constructor, enum, annotation, package, imports
    final String name; // element name
    final String className; // containing class (or N/A for package/imports)
    final String source; // source code
    final int startLine; // start line number
    final int endLine; // end line number
    final String javadoc; // javadoc comment
    final String packageName; // package name
    final String modifiers; // modifiers (public, private, static, etc.)

    CodeElement(
        String type,
        String name,
        String className,
        String source,
        int startLine,
        int endLine,
        String javadoc,
        String packageName,
        String modifiers) {
      this.type = type;
      this.name = name;
      this.className = className;
      this.source = source;
      this.startLine = startLine;
      this.endLine = endLine;
      this.javadoc = javadoc;
      this.packageName = packageName;
      this.modifiers = modifiers;
    }
  }

  // Inner class to store document information
  public static class DocumentInfo {
    private final String documentId;
    private final String fileName;
    private final String documentType;
    private final String description;
    private final long fileSize;
    private final int totalSegments;
    private final int processedSegments;
    private final String content;
    private final long uploadedAt;

    public DocumentInfo(
        String documentId,
        String fileName,
        String documentType,
        String description,
        long fileSize,
        int totalSegments,
        int processedSegments,
        String content) {
      this.documentId = documentId;
      this.fileName = fileName;
      this.documentType = documentType;
      this.description = description;
      this.fileSize = fileSize;
      this.totalSegments = totalSegments;
      this.processedSegments = processedSegments;
      this.content = content;
      this.uploadedAt = System.currentTimeMillis();
    }

    // Getters
    public String getDocumentId() {
      return documentId;
    }

    public String getFileName() {
      return fileName;
    }

    public String getDocumentType() {
      return documentType;
    }

    public String getDescription() {
      return description;
    }

    public long getFileSize() {
      return fileSize;
    }

    public int getTotalSegments() {
      return totalSegments;
    }

    public int getProcessedSegments() {
      return processedSegments;
    }

    public String getContent() {
      return content;
    }

    public long getUploadedAt() {
      return uploadedAt;
    }
  }
}
