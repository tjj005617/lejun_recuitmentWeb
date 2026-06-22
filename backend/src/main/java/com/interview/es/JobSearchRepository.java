package com.interview.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 职位搜索 Repository
 */
public interface JobSearchRepository extends ElasticsearchRepository<JobDocument, Long> {
}
