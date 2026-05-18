package com.harry.aifrontier.service;

import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.vo.GitHubRepoCandidateVO;

public interface GitHubService {

    GitHubRepoCandidateVO findRepo(String fullName);

    PageResult<GitHubRepoCandidateVO> searchRepos(String keyword, String sort, Long pageNum, Long pageSize);
}
