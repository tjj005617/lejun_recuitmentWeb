package com.interview.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 公司搜索 Repository
 */
public interface CompanySearchRepository extends ElasticsearchRepository<CompanyDocument, Long> {
}
