import request from '../utils/request.js'

export function getBusinessDataApi() {
  return request.get('/admin/workspace/businessData')
}

export function getOrderOverviewApi() {
  return request.get('/admin/workspace/overviewOrders')
}

export function pagePendingOrdersApi(params) {
  return request.get('/admin/workspace/pendingOrders', { params })
}

export function getMerchantTurnoverReportApi(params) {
  return request.get('/admin/workspace/turnover', { params })
}

export function getMerchantOrderReportApi(params) {
  return request.get('/admin/workspace/ordersStatistics', { params })
}

export function getMerchantSalesTop10Api(params) {
  return request.get('/admin/workspace/salesTop10', { params })
}
