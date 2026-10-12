package com.sporthub.aspect;

import com.sporthub.service.SportService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceLoggingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private ServiceLoggingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new ServiceLoggingAspect();

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(SportService.class);
        when(signature.getName()).thenReturn("getAllSports");
    }

    @Test
    void shouldProceedAndReturnOriginalResult() throws Throwable {
        String expectedResult = "success";

        when(joinPoint.proceed()).thenReturn(expectedResult);

        Object result = aspect.measureExecutionTime(joinPoint);

        assertEquals(expectedResult, result);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void shouldRethrowExceptionWhenServiceMethodFails() throws Throwable {
        RuntimeException expectedException =
                new RuntimeException("Service failed");

        when(joinPoint.proceed()).thenThrow(expectedException);

        RuntimeException actualException = assertThrows(
                RuntimeException.class,
                () -> aspect.measureExecutionTime(joinPoint)
        );

        assertSame(expectedException, actualException);
        verify(joinPoint, times(1)).proceed();
    }
}