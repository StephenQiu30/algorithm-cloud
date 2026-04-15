package com.stephen.cloud.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseAddRequest;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeBaseControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void addKnowledgeBaseShouldReturnFriendlyMessageWhenUniqueConstraintIsHit() throws Exception {
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        when(knowledgeBaseService.isNameUnique("排序算法知识库", null)).thenReturn(true);
        when(knowledgeBaseService.save(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        KnowledgeBaseController controller = new KnowledgeBaseController();
        ReflectionTestUtils.setField(controller, "knowledgeBaseService", knowledgeBaseService);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        KnowledgeBaseAddRequest request = new KnowledgeBaseAddRequest();
        request.setName("排序算法知识库");
        request.setDescription("排序算法知识库");

        try (MockedStatic<SecurityUtils> securityUtils = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginUserId).thenReturn(1L);

            mockMvc.perform(post("/ai/kb/add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.OPERATION_ERROR.getCode()))
                    .andExpect(jsonPath("$.message").value("知识库名称已存在"));
        }
    }
}
