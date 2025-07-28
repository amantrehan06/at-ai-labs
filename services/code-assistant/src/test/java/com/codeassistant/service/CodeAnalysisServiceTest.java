package com.codeassistant.service;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.model.AnalysisType;
import com.codeassistant.service.ai.AIChatService;
import com.codeassistant.service.ai.AIServiceException;
import com.codeassistant.service.factory.AIServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeAnalysisServiceTest {

    @Mock
    private AIServiceFactory mockAIServiceFactory;

    @Mock
    private AIChatService mockAIChatService;

    private CodeAnalysisService codeAnalysisService;

    @BeforeEach
    void setUp() {
        // Create service with mock AI service factory
        codeAnalysisService = new CodeAnalysisService(mockAIServiceFactory);
    }

    @Test
    void testAnalyzeCode_Success() throws AIServiceException {
        // Arrange
        AnalysisRequest request = new AnalysisRequest();
        request.setCode("public class Test { }");
        request.setAnalysisType(AnalysisType.EXPLAIN);
        request.setLanguage("java");

        AnalysisResponse expectedResponse = AnalysisResponse.builder()
            .analysis("Mock analysis result")
            .analysisType(AnalysisType.EXPLAIN)
            .language("java")
            .success(true)
            .build();

        when(mockAIServiceFactory.getBestAvailableService()).thenReturn(mockAIChatService);
        when(mockAIChatService.analyzeCode(request)).thenReturn(expectedResponse);

        // Act
        AnalysisResponse result = codeAnalysisService.analyzeCode(request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.getAnalysis(), result.getAnalysis());
        assertEquals(expectedResponse.getAnalysisType(), result.getAnalysisType());
        assertTrue(result.isSuccess());
        
        verify(mockAIServiceFactory).getBestAvailableService();
        verify(mockAIChatService).analyzeCode(request);
    }

    @Test
    void testAnalyzeCode_NoServiceAvailable() {
        // Arrange
        AnalysisRequest request = new AnalysisRequest();
        request.setCode("public class Test { }");
        request.setAnalysisType(AnalysisType.EXPLAIN);

        when(mockAIServiceFactory.getBestAvailableService()).thenThrow(new AIServiceException("No services available"));

        // Act & Assert
        AIServiceException exception = assertThrows(AIServiceException.class, 
            () -> codeAnalysisService.analyzeCode(request));
        
        assertEquals("No services available", exception.getMessage());
        verify(mockAIServiceFactory).getBestAvailableService();
        verify(mockAIChatService, never()).analyzeCode(any());
    }

    @Test
    void testAnalyzeCode_ServiceThrowsException() throws AIServiceException {
        // Arrange
        AnalysisRequest request = new AnalysisRequest();
        request.setCode("public class Test { }");
        request.setAnalysisType(AnalysisType.EXPLAIN);

        when(mockAIServiceFactory.getBestAvailableService()).thenReturn(mockAIChatService);
        when(mockAIChatService.analyzeCode(request)).thenThrow(new AIServiceException("Service error"));

        // Act & Assert
        AIServiceException exception = assertThrows(AIServiceException.class, 
            () -> codeAnalysisService.analyzeCode(request));
        
        assertEquals("Service error", exception.getMessage());
        verify(mockAIServiceFactory).getBestAvailableService();
        verify(mockAIChatService).analyzeCode(request);
    }

    @Test
    void testAnalyzeCode_WithSpecificService() throws AIServiceException {
        // Arrange
        AnalysisRequest request = new AnalysisRequest();
        request.setCode("public class Test { }");
        request.setAnalysisType(AnalysisType.EXPLAIN);

        AnalysisResponse expectedResponse = AnalysisResponse.builder()
            .analysis("Mock analysis result")
            .analysisType(AnalysisType.EXPLAIN)
            .language("java")
            .success(true)
            .build();

        when(mockAIServiceFactory.getService("OpenAIChatService")).thenReturn(mockAIChatService);
        when(mockAIChatService.analyzeCode(request)).thenReturn(expectedResponse);

        // Act
        AnalysisResponse result = codeAnalysisService.analyzeCode(request, "OpenAIChatService");

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.getAnalysis(), result.getAnalysis());
        assertTrue(result.isSuccess());
        
        verify(mockAIServiceFactory).getService("OpenAIChatService");
        verify(mockAIChatService).analyzeCode(request);
    }

    @Test
    void testHasAvailableService_True() {
        // Arrange
        when(mockAIServiceFactory.hasAvailableServices()).thenReturn(true);

        // Act
        boolean result = codeAnalysisService.hasAvailableService();

        // Assert
        assertTrue(result);
        verify(mockAIServiceFactory).hasAvailableServices();
    }

    @Test
    void testHasAvailableService_False() {
        // Arrange
        when(mockAIServiceFactory.hasAvailableServices()).thenReturn(false);

        // Act
        boolean result = codeAnalysisService.hasAvailableService();

        // Assert
        assertFalse(result);
        verify(mockAIServiceFactory).hasAvailableServices();
    }

    @Test
    void testGetAvailableServices() {
        // Arrange
        Map<String, AIChatService> services = new HashMap<>();
        services.put("OpenAIChatService", mockAIChatService);
        
        when(mockAIServiceFactory.getAllAvailableServices()).thenReturn(services);

        // Act
        var result = codeAnalysisService.getAvailableServices();

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("OpenAIChatService"));
        verify(mockAIServiceFactory).getAllAvailableServices();
    }

    @Test
    void testGetAvailableServiceCount() {
        // Arrange
        when(mockAIServiceFactory.getAvailableServiceCount()).thenReturn(2);

        // Act
        int result = codeAnalysisService.getAvailableServiceCount();

        // Assert
        assertEquals(2, result);
        verify(mockAIServiceFactory).getAvailableServiceCount();
    }
} 