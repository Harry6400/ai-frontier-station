package com.harry.aifrontier.service;

import com.harry.aifrontier.entity.FetchLog;
import java.util.List;

public interface FetchLogService {
    void saveLog(FetchLog fetchLog);
    void log(String source, String keyword, int found, int imported, String status, String message);
    List<FetchLog> recentLogs(int days);
    List<FetchLog> recentLogsBySource(String source, int days);
}
