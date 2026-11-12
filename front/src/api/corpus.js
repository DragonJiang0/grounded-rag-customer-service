import http from './http'

export const corpusApi = {
  page: (data) => http.post('/corpus/page', data),

  create: (data) => http.post('/corpus/add', data),

  update: (id, data) => http.post('/corpus/edit', { id, ...data }),

  remove: (id) => http.post(`/corpus/delete/${id}`),

  toggleEnabled: (id) => http.post(`/corpus/toggle-enabled/${id}`),
}
