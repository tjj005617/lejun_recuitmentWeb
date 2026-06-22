import request from './request'

export const uploadResume = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/resume/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getResume = (id) => {
  return request.get(`/resume/${id}`)
}

export const getUserResumes = (userId) => {
  return request.get(`/user/${userId}/resumes`)
}

export const deleteUserResume = (userId, resumeId) => {
  return request.delete(`/user/${userId}/resumes/${resumeId}`)
}
