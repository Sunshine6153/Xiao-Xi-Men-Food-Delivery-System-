import request from '../utils/request.js'

export function getTurnoverReportApi(params) {
  return request.get('/admin/report/turnover', { params })
}

export function getUserReportApi(params) {
  return request.get('/admin/report/userreport', { params })
}

export function getOrderReportApi(params) {
  return request.get('/admin/report/ordersStatistics', { params })
}

export function getSalesTop10Api(params) {
  return request.get('/admin/report/salesTop10', { params })
}

export function exportBusinessDataApi(params) {
  return request.get('/admin/report/export', { params, responseType: 'blob' })
}
