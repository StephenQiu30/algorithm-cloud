package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.ai.mapper.KnowledgeBaseMapper;
import com.stephen.cloud.ai.mapper.KnowledgeDocumentMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.model.enums.KnowledgeParseStatusEnum;
import com.stephen.cloud.ai.service.KnowledgeIngestService;
import com.stephen.cloud.ai.service.KnowledgeSeedService;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeDocIngestMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 知识库轮种子服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class KnowledgeSeedServiceImpl implements KnowledgeSeedService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeIngestService knowledgeIngestService;

    private static final String KB_NAME = "排序算法教学知识库";
    private static final String SEED_DIR = "/tmp/algorithm-seeds/";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long seedSortingAlgorithms(Long userId) {
        // 1. 获取或创建知识库
        LambdaQueryWrapper<KnowledgeBase> kbQw = new LambdaQueryWrapper<>();
        kbQw.eq(KnowledgeBase::getName, KB_NAME).eq(KnowledgeBase::getUserId, userId);
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(kbQw);
        if (kb == null) {
            kb = new KnowledgeBase();
            kb.setUserId(userId);
            kb.setName(KB_NAME);
            kb.setDescription("包含常见排序算法的原理、执行流程及复杂度分析，适用于算法教学。");
            kb.setStatus(1);
            knowledgeBaseMapper.insert(kb);
        }
        Long kbId = kb.getId();

        // 2. 准备种子数据
        Map<String, String> seeds = getAlgorithmSeeds();

        // 3. 准备临时目录
        try {
            Files.createDirectories(Paths.get(SEED_DIR));
        } catch (IOException e) {
            log.error("创建种子目录失败", e);
            return kbId;
        }

        // 4. 处理每个算法文档
        for (Map.Entry<String, String> entry : seeds.entrySet()) {
            String algoName = entry.getKey();
            String content = entry.getValue();
            String fileName = algoName + ".txt";
            Path filePath = Paths.get(SEED_DIR, fileName);

            try {
                // 写入文件
                Files.writeString(filePath, content);

                // 检查文档是否已存在（避免重复导入相同名称文档）
                LambdaQueryWrapper<KnowledgeDocument> docQw = new LambdaQueryWrapper<>();
                docQw.eq(KnowledgeDocument::getKnowledgeBaseId, kbId)
                        .eq(KnowledgeDocument::getOriginalName, algoName);
                KnowledgeDocument doc = knowledgeDocumentMapper.selectOne(docQw);

                if (doc == null) {
                    doc = new KnowledgeDocument();
                    doc.setKnowledgeBaseId(kbId);
                    doc.setUserId(userId);
                    doc.setOriginalName(algoName);
                    doc.setStoragePath(filePath.toString());
                    doc.setMimeType("text/plain");
                    doc.setSizeBytes((long) content.getBytes().length);
                    doc.setParseStatus(KnowledgeParseStatusEnum.PENDING.getValue());
                    knowledgeDocumentMapper.insert(doc);
                } else {
                    // 已存在则更新存储路径和状态，准备重新解析
                    doc.setStoragePath(filePath.toString());
                    doc.setSizeBytes((long) content.getBytes().length);
                    doc.setParseStatus(KnowledgeParseStatusEnum.PENDING.getValue());
                    knowledgeDocumentMapper.updateById(doc);
                }

                // 5. 触发异步/同步解析
                KnowledgeDocIngestMessage message = KnowledgeDocIngestMessage.builder()
                        .documentId(doc.getId())
                        .knowledgeBaseId(kbId)
                        .userId(userId)
                        .storagePath(filePath.toString())
                        .build();
                
                knowledgeIngestService.ingestDocument(message);
                log.info("成功处理算法种子: {}", algoName);

            } catch (IOException e) {
                log.error("处理算法种子失败: {}", algoName, e);
            }
        }

        return kbId;
    }

    private Map<String, String> getAlgorithmSeeds() {
        Map<String, String> seeds = new HashMap<>();

        seeds.put("冒泡排序 (Bubble Sort)", """
                冒泡排序（Bubble Sort）是一种简单的排序算法。它重复地遍历要排序的数列，一次比较两个元素，如果它们的顺序错误就把它们交换过来。
                
                【原理】
                比较相邻的元素。如果第一个比第二个大，就交换它们。对每一对相邻元素作同样的工作，从开始第一对到结尾的最后一对。这步做完后，最后的元素会是最大的数。
                
                【复杂度】
                - 时间复杂度：最好 O(n)，最坏 O(n²)，平均 O(n²)
                - 空间复杂度：O(1)
                - 稳定性：稳定
                """);

        seeds.put("快速排序 (Quick Sort)", """
                快速排序（Quick Sort）由 C. A. R. Hoare 在 1960 年提出。它是处理大数据集最快的排序算法之一。
                
                【原理】
                通过一趟排序将待排记录分隔成独立的两部分，其中一部分记录的关键字均比另一部分的关键字小，则可分别对这两部分记录继续进行排序，以达到整个序列有序。关键在于分治法（Divide and Conquer）的应用。
                
                【复杂度】
                - 时间复杂度：最好 O(n log n)，最坏 O(n²)，平均 O(n log n)
                - 空间复杂度：O(log n)
                - 稳定性：不稳定
                """);

        seeds.put("归并排序 (Merge Sort)", """
                归并排序（Merge Sort）是建立在归并操作上的一种有效的排序算法。该算法是采用分治法（Divide and Conquer）的一个非常典型的应用。
                
                【原理】
                将已有序的子序列合并，得到完全有序的序列；即先使每个子序列有序，再使子序列段间有序。若将两个有序表合并成一个有序表，称为二路归并。
                
                【复杂度】
                - 时间复杂度：最好 O(n log n)，最坏 O(n log n)，平均 O(n log n)
                - 空间复杂度：O(n)
                - 稳定性：稳定
                """);
        
        seeds.put("插入排序 (Insertion Sort)", """
                插入排序（Insertion Sort）的工作原理是通过构建有序序列，对于未排序数据，在已排序序列中从后向前扫描，找到相应位置并插入。
                
                【原理】
                从第一个元素开始，该元素可以认为已经被排序；取出下一个元素，在已经排序的元素序列中从后向前扫描；如果该元素（已排序）大于新元素，将该元素移到下一位置；重复步骤直到找到已排序的元素小于或者等于新元素的位置；将新元素插入到该位置。
                
                【复杂度】
                - 时间复杂度：最好 O(n)，最坏 O(n²)，平均 O(n²)
                - 空间复杂度：O(1)
                - 稳定性：稳定
                """);

        return seeds;
    }
}
