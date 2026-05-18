package com.harry.aifrontier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harry.aifrontier.entity.FetchLog;
import com.harry.aifrontier.mapper.FetchLogMapper;
import com.harry.aifrontier.service.FetchLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FetchLogServiceImpl implements FetchLogService {

    private final FetchLogMapper fetchLogMapper;

    @Override
    public void saveLog(FetchLog fetchLog) {
        if (fetchLog.getCreatedAt() == null) {
            fetchLog.setCreatedAt(LocalDateTime.now());
        }
        fetchLogMapper.insert(fetchLog);
        log.info("[采集日志] source={}, fetched={}, imported={}, status={}",
                fetchLog.getSource(), fetchLog.getPapersFound(), fetchLog.getPapersImported(), fetchLog.getStatus());
    }

    @Override
    public void log(String source, String keyword, int found, int imported, String status, String message) {
        FetchLog fetchLog = new FetchLog();
        fetchLog.setSource(source);
        fetchLog.setKeyword(keyword);
        fetchLog.setPapersFound(found);
        fetchLog.setPapersImported(imported);
        fetchLog.setStatus(status);
        fetchLog.setErrorMessage(message);
        fetchLog.setCreatedAt(LocalDateTime.now());
        fetchLogMapper.insert(fetchLog);
        log.info("[采集日志] source={}, keyword={}, found={}, imported={}, status={}", source, keyword, found, imported, status);
    }

    @Override
    public List<FetchLog> recentLogs(int days) {
        return fetchLogMapper.selectList(
            new LambdaQueryWrapper<FetchLog>()
                .ge(FetchLog::getCreatedAt, LocalDateTime.now().minusDays(days))
                .orderByDesc(FetchLog::getCreatedAt)
        );
    }

    @Override
    public List<FetchLog> recentLogsBySource(String source, int days) {
        return fetchLogMapper.selectList(
            new LambdaQueryWrapper<FetchLog>()
                .eq(FetchLog::getSource, source)
                .ge(FetchLog::getCreatedAt, LocalDateTime.now().minusDays(days))
                .orderByDesc(FetchLog::getCreatedAt)
        );
    }
}
