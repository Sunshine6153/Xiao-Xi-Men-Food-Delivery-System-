const STATUS_TEXT = {
  1: '待支付',
  2: '待接单',
  3: '制作中',
  4: '待取餐',
  5: '配送中',
  6: '待确认收餐',
  7: '已完成',
  8: '已取消'
}

const request = require('./request')

function normalizeOrder(order = {}) {
  const dishes = (order.dishes || []).map(dish => ({
    ...dish,
    price: Number(dish.amount || 0).toFixed(2),
    subtotal: (Number(dish.amount || 0) * Number(dish.number || 0)).toFixed(2)
  }))
  const merchantMap = {}
  dishes.forEach(dish => {
    const key = String(dish.merchantId || 0)
    if (!merchantMap[key]) merchantMap[key] = { id: dish.merchantId, name: dish.merchantName || '商户', location: dish.merchantLocation || '商户暂未填写取餐位置', preparationStatus: dish.status || 1, dishes: [] }
    merchantMap[key].preparationStatus = Math.min(merchantMap[key].preparationStatus, dish.status || 1)
    merchantMap[key].dishes.push(dish)
  })
  return {
    ...order,
    statusValue: order.status,
    status: STATUS_TEXT[order.status] || '未知状态',
    orderNumber: order.number,
    fee: Number(order.deliveryFee || 0).toFixed(2),
    total: Number(order.amount || 0).toFixed(2),
    createdAt: order.orderTime ? String(order.orderTime).replace('T', ' ').slice(0, 16) : '',
    summary: dishes.map(dish => `${dish.name} ×${dish.number}`).join(' · '),
    dishes,
    merchantCount: Object.keys(merchantMap).length,
    itemCount: dishes.reduce((count, dish) => count + Number(dish.number || 0), 0),
    merchantGroups: Object.values(merchantMap).map(group => ({ ...group, preparationText: ({ 1: '待商户确认', 2: '制作中', 3: '已出餐' })[group.preparationStatus] }))
  }
}

module.exports = {
  submit: data => request('/user/dish/submit', data),
  payment: orderNumber => request('/user/order/payment', { orderNumber }),
  reminder: id => request(`/user/dish/reminder/${id}`, null, 'GET'),
  page: data => request('/user/order/page', data, 'GET').then(result => ({
    total: Number((result && result.total) || 0),
    records: ((result && result.records) || []).map(normalizeOrder)
  })),
  detail: id => request(`/user/order/${id}`, null, 'GET').then(normalizeOrder),
  cancel: id => request(`/user/order/cancel/${id}`, null, 'PUT'),
  confirmReceipt: id => request(`/user/order/confirm/${id}`, null, 'PUT'),
  request,
  normalizeOrder
}
