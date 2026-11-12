import http from './http'

export const groupApi = {
  all: () => http.get('/groups/all'),
  create: (data) => http.post('/groups/add', data),
  update: (id, data) => http.post('/groups/edit', { id, ...data }),
  remove: (id) => http.post(`/groups/delete/${id}`),
}
