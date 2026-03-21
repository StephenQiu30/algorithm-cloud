package com.stephen.cloud.common.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Excel 工具类 (基于 EasyExcel)
 * 秉持“简单、通用、不重复造轮子”的原则
 *
 * @author StephenQiu30
 */
@Slf4j
public class ExcelUtils {

    /**
     * 默认每批次处理的数据量
     */
    private static final int BATCH_COUNT = 100;

    /**
     * 导出 Excel (直接输出到流)
     *
     * @param out       输出流
     * @param head      数据头类
     * @param data      数据列表
     * @param sheetName 分页名称
     * @param <T>       类型
     */
    public static <T> void write(OutputStream out, Class<T> head, List<T> data, String sheetName) {
        EasyExcel.write(out, head).sheet(sheetName).doWrite(data);
    }

    /**
     * 导出 Excel (默认分页名)
     */
    public static <T> void write(OutputStream out, Class<T> head, List<T> data) {
        write(out, head, data, "Sheet1");
    }

    /**
     * 通用导入 (简单分批处理，无需手动写 Listener)
     *
     * @param is           输入流
     * @param head         数据头类
     * @param batchHandler 每一批次数据的处理逻辑 (Consumer)
     * @param <T>          类型
     */
    public static <T> void read(InputStream is, Class<T> head, Consumer<List<T>> batchHandler) {
        EasyExcel.read(is, head, new ReadListener<T>() {
            /**
             * 临时存储读取到的数据
             */
            private List<T> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

            @Override
            public void invoke(T data, AnalysisContext context) {
                cachedDataList.add(data);
                // 达到批量处理阈值，触发回调
                if (cachedDataList.size() >= BATCH_COUNT) {
                    batchHandler.accept(cachedDataList);
                    cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 处理收尾数据
                if (!cachedDataList.isEmpty()) {
                    batchHandler.accept(cachedDataList);
                }
                log.info("Excel import completed.");
            }
        }).sheet().doRead();
    }

    /**
     * 将 Excel 转换为 CSV 格式
     * 用于轻量化存储或后续处理
     *
     * @param is Excel 输入流
     * @param os CSV 输出流
     */
    public static void excelToCsv(InputStream is, OutputStream os) {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            EasyExcel.read(is, new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    String line = data.values().stream()
                            .map(v -> v == null ? "" : "\"" + v.replace("\"", "\"\"") + "\"")
                            .collect(Collectors.joining(","));
                    writer.println(line);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    writer.flush();
                }
            }).sheet().doRead();
        } catch (Exception e) {
            log.error("Excel to CSV conversion failed", e);
            throw new RuntimeException("Excel 转 CSV 失败", e);
        }
    }
}
