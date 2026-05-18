package com.harry.aifrontier.service;

public interface PaperSummaryService {

    /**
     * 调用 AI 为论文生成中文摘要
     *
     * @param title        论文标题
     * @param abstractText 论文英文摘要
     * @return 中文摘要纯文本
     */
    String generateChineseSummary(String title, String abstractText);

    /**
     * 批量为所有已发布内容生成中文摘要（跳过已有中文摘要的）
     *
     * @return 处理结果统计信息
     */
    String generateAllMissing();
}
