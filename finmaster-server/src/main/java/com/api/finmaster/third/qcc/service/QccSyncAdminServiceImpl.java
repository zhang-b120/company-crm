package com.qykj.finmaster.third.qcc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.common.utils.third.QccUtil;
import com.qykj.finmaster.common.utils.third.req.QccSearchReq;
import com.qykj.finmaster.common.utils.third.resp.QccSearchResp;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.mapper.CustomerCompanyMapper;
import com.qykj.finmaster.third.qcc.domain.QccSearchResult;
import com.qykj.finmaster.third.qcc.mapper.QccSearchResultMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一次性同步：把 customer_company 的公司名（按前缀去重）批量到企查查模糊查询，
 * 返回结果落库到 qcc_search_result。
 */
@Slf4j
@Service
public class QccSyncAdminServiceImpl {

    /** 公司名取前缀的字符数 */
    private static final int SEARCH_KEY_PREFIX_LEN = 7;

    /** 两次调用企查查之间的节流间隔（毫秒，单线程内） */
    private static final long API_THROTTLE_MILLIS = 300L;

    /** 每个线程处理的 searchKey 条数 */
    private static final int CHUNK_SIZE = 500;

    /** 并发线程数上限（控制企查查 QPS，避免被限流） */
    private static final int MAX_CONCURRENT_THREADS = 5;

    @Autowired
    private CustomerCompanyMapper customerCompanyMapper;

    @Autowired
    private QccSearchResultMapper qccSearchResultMapper;

    public Map<String, Object> syncAll() {
        long start = System.currentTimeMillis();

        List<CustomerCompany> companies = customerCompanyMapper.selectList(
                new LambdaQueryWrapper<CustomerCompany>()
                        .select(CustomerCompany::getId, CustomerCompany::getCompanyName)
                        .isNotNull(CustomerCompany::getCompanyName)
                        .ne(CustomerCompany::getCompanyName, "")
                        .between(CustomerCompany::getId, 4001, 10000)
        );

        // 按 searchKey 去重，记录首个公司作为来源标识
        Map<String, CustomerCompany> searchKeyToSource = new LinkedHashMap<>();
        for (CustomerCompany c : companies) {
            String key = buildSearchKey(c.getCompanyName());
            if (key == null) {
                continue;
            }
            searchKeyToSource.putIfAbsent(key, c);
        }

        int totalCompanies = companies.size();
        int uniqueKeys = searchKeyToSource.size();

        // 拆分成 CHUNK_SIZE 一组
        List<List<Map.Entry<String, CustomerCompany>>> chunks = new ArrayList<>();
        List<Map.Entry<String, CustomerCompany>> buf = new ArrayList<>(CHUNK_SIZE);
        for (Map.Entry<String, CustomerCompany> e : searchKeyToSource.entrySet()) {
            buf.add(e);
            if (buf.size() == CHUNK_SIZE) {
                chunks.add(buf);
                buf = new ArrayList<>(CHUNK_SIZE);
            }
        }
        if (!buf.isEmpty()) {
            chunks.add(buf);
        }

        int chunkCount = chunks.size();
        int threadCount = Math.max(1, Math.min(MAX_CONCURRENT_THREADS, chunkCount));

        AtomicInteger apiSuccess = new AtomicInteger();
        AtomicInteger apiFail = new AtomicInteger();
        AtomicInteger inserted = new AtomicInteger();

        log.info("[QccSync] begin. totalCompanies={}, uniqueSearchKeys={}, chunks={}, threads={}",
                totalCompanies, uniqueKeys, chunkCount, threadCount);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r);
            t.setName("qcc-sync-" + t.getId());
            t.setDaemon(true);
            return t;
        });
        try {
            List<Future<?>> futures = new ArrayList<>(chunkCount);
            for (int i = 0; i < chunkCount; i++) {
                final int chunkIndex = i;
                final List<Map.Entry<String, CustomerCompany>> chunk = chunks.get(i);
                futures.add(pool.submit(() ->
                        processChunk(chunkIndex, chunk, apiSuccess, apiFail, inserted)));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("[QccSync] main thread interrupted while waiting chunks");
                    break;
                } catch (ExecutionException ee) {
                    log.error("[QccSync] chunk task threw: {}", ee.getMessage(), ee);
                }
            }
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        long cost = System.currentTimeMillis() - start;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCompanies", totalCompanies);
        summary.put("uniqueSearchKeys", uniqueKeys);
        summary.put("chunks", chunkCount);
        summary.put("threads", threadCount);
        summary.put("apiSuccess", apiSuccess.get());
        summary.put("apiFail", apiFail.get());
        summary.put("inserted", inserted.get());
        summary.put("costMillis", cost);
        log.info("[QccSync] done. {}", summary);
        return summary;
    }

    /**
     * 单个分片任务：顺序处理 chunk 内的 searchKey，调用企查查、落库，统计计数。
     */
    private void processChunk(int chunkIndex,
                              List<Map.Entry<String, CustomerCompany>> chunk,
                              AtomicInteger apiSuccess,
                              AtomicInteger apiFail,
                              AtomicInteger inserted) {
        int size = chunk.size();
        int localIndex = 0;
        for (Map.Entry<String, CustomerCompany> entry : chunk) {
            localIndex++;
            String searchKey = entry.getKey();
            CustomerCompany source = entry.getValue();

            List<QccSearchResp> respList = null;
            try {
                QccSearchReq req = new QccSearchReq();
                req.setSearchKey(searchKey);
                respList = QccUtil.getCompanyInfo(req);
                apiSuccess.incrementAndGet();
            } catch (Exception e) {
                apiFail.incrementAndGet();
                log.error("[QccSync][chunk-{}] call qcc fail. searchKey={}, err={}",
                        chunkIndex, searchKey, e.getMessage(), e);
            }

            if (respList != null && !respList.isEmpty()) {
                for (QccSearchResp r : respList) {
                    QccSearchResult row = new QccSearchResult();
                    row.setSourceCompanyId(source.getId());
                    row.setSourceCompanyName(source.getCompanyName());
                    row.setSearchKey(searchKey);
                    row.setKeyNo(r.getKeyNo());
                    row.setName(r.getName());
                    row.setCreditCode(r.getCreditCode());
                    row.setStartDate(r.getStartDate());
                    row.setOperName(r.getOperName());
                    row.setStatus(r.getStatus());
                    row.setNo(r.getNo());
                    row.setAddress(r.getAddress());
                    row.setCreatedTime(new Date());
                    try {
                        qccSearchResultMapper.insert(row);
                        inserted.incrementAndGet();
                    } catch (Exception e) {
                        log.error("[QccSync][chunk-{}] insert fail. keyNo={}, err={}",
                                chunkIndex, r.getKeyNo(), e.getMessage(), e);
                    }
                }
            }

            if (localIndex % 50 == 0) {
                log.info("[QccSync][chunk-{}] progress {}/{}", chunkIndex, localIndex, size);
            }

            try {
                Thread.sleep(API_THROTTLE_MILLIS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("[QccSync][chunk-{}] interrupted at localIndex={}", chunkIndex, localIndex);
                return;
            }
        }
        log.info("[QccSync][chunk-{}] finished. size={}", chunkIndex, size);
    }

    /**
     * 取公司名前 7 个字符作为搜索关键字。
     * 长度不足 7 则取原文。空白公司名返回 null。
     */
    private String buildSearchKey(String companyName) {
        if (StringUtils.isBlank(companyName)) {
            return null;
        }
        String trimmed = companyName.trim();
        if ( trimmed.length() >= 18) {
            return trimmed;
        }
        if ( trimmed.length() >= 15 && trimmed.length() < 18) {
            return trimmed.substring(0, 12);
        }
        if ( trimmed.length() >= 12 && trimmed.length() < 15) {
            return trimmed.substring(0, 9);
        }
        if (trimmed.length() <= SEARCH_KEY_PREFIX_LEN) {
            return trimmed;
        }
        return trimmed.substring(0, SEARCH_KEY_PREFIX_LEN);
    }
}
