import request from './request'

export function uploadFile(file, bizType = 'common') {
  const form = new FormData()
  form.append('file', file)
  form.append('biz_type', bizType)
  return request.post('/file/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
