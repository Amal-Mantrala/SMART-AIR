package com.example.b07demosummer2024.services;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;

public class DataAccessServiceTest {
    private DataAccessService dataAccessService;

    @Before
    public void setUp() {
        dataAccessService = new DataAccessService();
    }

    @Test
    public void canWrite_alwaysReturnsFalse() {
        DataAccessService.AccessCallback callback = mock(DataAccessService.AccessCallback.class);
        dataAccessService.canWrite("parentId", "providerId", "childId", callback);
        verify(callback).onResult(false);
    }

    @Test
    public void getReadOnlyData_nullProviderId_returnsEmptyData() {
        DataAccessService.DataCallback callback = mock(DataAccessService.DataCallback.class);
        dataAccessService.getReadOnlyData("childId", null, callback);
        ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
        verify(callback).onResult(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    public void getReadOnlyData_nullChildId_returnsEmptyData() {
        DataAccessService.DataCallback callback = mock(DataAccessService.DataCallback.class);
        dataAccessService.getReadOnlyData(null, "providerId", callback);
        ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
        verify(callback).onResult(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    public void canRead_nullProviderId_returnsFalse() {
        DataAccessService.AccessCallback callback = mock(DataAccessService.AccessCallback.class);
        dataAccessService.canRead("parentId", null, "childId", callback);
        verify(callback).onResult(false);
    }

    @Test
    public void canRead_nullParentId_returnsFalse() {
        DataAccessService.AccessCallback callback = mock(DataAccessService.AccessCallback.class);
        dataAccessService.canRead(null, "providerId", "childId", callback);
        verify(callback).onResult(false);
    }

    @Test
    public void canRead_nullChildId_returnsFalse() {
        DataAccessService.AccessCallback callback = mock(DataAccessService.AccessCallback.class);
        dataAccessService.canRead("parentId", "providerId", null, callback);
        verify(callback).onResult(false);
    }

    @Test
    public void dataAccessService_hasVerifyProviderRoleMethod() {
        try {
            java.lang.reflect.Method method = DataAccessService.class.getDeclaredMethod("verifyProviderRole", String.class, DataAccessService.AccessCallback.class);
            assertNotNull(method);
            assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()));
        } catch (NoSuchMethodException e) {
            fail("verifyProviderRole method should exist");
        }
    }
}
