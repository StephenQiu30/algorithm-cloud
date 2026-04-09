package com.stephen.cloud.post.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.utils.DocumentUtils;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 帖子导出管理接口
 * <p>
 * 提供帖子导出为 PDF 或 Word 文档的功能。
 * 导出为管理员权限操作，用于内容备份或分享。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/post/export")
@SaCheckRole(UserConstant.ADMIN_ROLE)
@Slf4j
@Tag(name = "PostExportController", description = "帖子导出管理")
public class PostExportController {

    @Resource
    private PostService postService;

    /**
     * 导出帖子
     *
     * @param id   帖子 ID
     * @param type 导出类型 (pdf | word)
     */
    @GetMapping("/export")
    @Operation(summary = "导出帖子", description = "将帖子内容导出为 PDF 或 Word (仅管理员)")
    public void exportPost(@RequestParam("id") Long id, @RequestParam("type") String type,
                           HttpServletResponse response) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Post post = postService.getById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }

        String title = post.getTitle();
        String content = post.getContent();
        if (content == null) {
            content = "";
        }
        String fileName = URLEncoder.encode(title, StandardCharsets.UTF_8);

        try (OutputStream outputStream = response.getOutputStream()) {
            if ("pdf".equalsIgnoreCase(type)) {
                response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".pdf");
                response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
                DocumentUtils.exportToPdf(content, outputStream);
            } else if ("word".equalsIgnoreCase(type)) {
                response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".docx");
                response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
                DocumentUtils.exportToWord(content, outputStream);
            } else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的导出类型");
            }
            outputStream.flush();
        } catch (Exception e) {
            log.error("导出帖子失败, id: {}, type: {}", id, type, e);
            // 注意：由于已经写入了 header，这里抛出异常可能无法返回正常的 JSON 错误信息
        }
    }
}
