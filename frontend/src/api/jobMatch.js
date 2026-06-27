import request from './request'

// 根据简历匹配岗位
export const matchJobsByResume = (resumeId, params = {}) => {
  return request.post(`/job-match/resume/${resumeId}`, null, { params })
}

// 同步单个岗位到 Milvus
export const syncJobToMilvus = (jobId) => {
  return request.post(`/job-match/sync/${jobId}`)
}

// 全量同步岗位到 Milvus
export const syncAllJobsToMilvus = () => {
  return request.post('/job-match/sync-all')
}
