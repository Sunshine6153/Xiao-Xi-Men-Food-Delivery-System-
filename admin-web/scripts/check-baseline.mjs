import net from 'node:net'

const backendUrl = process.env.BASELINE_BACKEND_URL || 'http://127.0.0.1:8080'
const checks = []

function addCheck(name, passed, detail) {
  checks.push({ name, passed, detail })
}

function checkPort(host, port) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host, port })
    const finish = (passed, detail) => {
      socket.destroy()
      resolve({ passed, detail })
    }
    socket.setTimeout(1500)
    socket.once('connect', () => finish(true, `${host}:${port} 可连接`))
    socket.once('timeout', () => finish(false, `${host}:${port} 连接超时`))
    socket.once('error', (error) => finish(false, `${host}:${port} 不可连接：${error.code || error.message}`))
  })
}

async function checkBackend() {
  try {
    const response = await fetch(`${backendUrl}/v3/api-docs`, { signal: AbortSignal.timeout(3000) })
    addCheck('后端 HTTP', response.ok, `${backendUrl}/v3/api-docs 返回 HTTP ${response.status}`)
  } catch (error) {
    addCheck('后端 HTTP', false, `${backendUrl} 不可访问：${error.message}`)
  }
}

async function checkAccount(username, password, label) {
  try {
    const loginResponse = await fetch(`${backendUrl}/admin/merchant/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
      signal: AbortSignal.timeout(5000)
    })
    const loginResult = await loginResponse.json()
    const loginPassed = loginResponse.ok && loginResult.code === 200 && Boolean(loginResult.data?.token)
    addCheck(`${label}登录`, loginPassed, `HTTP ${loginResponse.status}, code ${loginResult.code}`)
    if (!loginPassed) return

    const token = loginResult.data.token
    const accountId = loginResult.data.id
    const headers = { token }
    const [identityResponse, categoryResponse] = await Promise.all([
      fetch(`${backendUrl}/admin/auth/me`, { headers, signal: AbortSignal.timeout(5000) }),
      fetch(`${backendUrl}/admin/category/list`, { headers, signal: AbortSignal.timeout(5000) })
    ])
    const identityResult = await identityResponse.json()
    const categoryResult = await categoryResponse.json()
    const role = identityResult.data?.role
    addCheck(`${label}身份接口`, identityResponse.ok && identityResult.code === 200 && identityResult.data?.id === accountId && ['ADMIN', 'MERCHANT'].includes(role), `HTTP ${identityResponse.status}, code ${identityResult.code}, role ${role || '-'}`)
    addCheck(`${label}分类接口`, categoryResponse.ok && categoryResult.code === 200, `HTTP ${categoryResponse.status}, code ${categoryResult.code}`)

    if (role === 'MERCHANT') {
      const dishResponse = await fetch(`${backendUrl}/admin/dish/page?page=1&pageSize=10`, { headers, signal: AbortSignal.timeout(5000) })
      const dishResult = await dishResponse.json()
      addCheck(`${label}本店菜品分页`, dishResponse.ok && dishResult.code === 200, `HTTP ${dishResponse.status}, code ${dishResult.code}`)
      const records = dishResult.data?.records || []
      const isolated = records.every((dish) => String(dish.merchantId) === String(accountId))
      addCheck(`${label}菜品归属隔离`, isolated, `返回 ${records.length} 条，登录商户 ID ${accountId}`)
    } else if (role === 'ADMIN') {
      const [merchantResponse, dishResponse] = await Promise.all([
        fetch(`${backendUrl}/admin/merchant/page?page=1&pageSize=10`, { method: 'POST', headers, signal: AbortSignal.timeout(5000) }),
        fetch(`${backendUrl}/admin/dish/page?page=1&pageSize=10`, { headers, signal: AbortSignal.timeout(5000) })
      ])
      const merchantResult = await merchantResponse.json()
      const dishResult = await dishResponse.json()
      addCheck(`${label}商户分页`, merchantResponse.ok && merchantResult.code === 200, `HTTP ${merchantResponse.status}, code ${merchantResult.code}`)
      addCheck(`${label}平台菜品只读查询`, dishResponse.ok && dishResult.code === 200, `HTTP ${dishResponse.status}, code ${dishResult.code}`)
      const writeResponse = await fetch(`${backendUrl}/admin/dish`, {
        method: 'POST', headers: { ...headers, 'Content-Type': 'application/json' },
        body: '{}', signal: AbortSignal.timeout(5000)
      })
      const writeResult = await writeResponse.json()
      addCheck(`${label}禁止新增商户菜品`, writeResponse.ok && writeResult.code === 403, `HTTP ${writeResponse.status}, code ${writeResult.code}`)
    }
  } catch (error) {
    addCheck(`${label}接口联调`, false, error.message)
  }
}

const mysql = await checkPort('127.0.0.1', 3306)
const redis = await checkPort('127.0.0.1', 6379)
addCheck('MySQL', mysql.passed, mysql.detail)
addCheck('Redis', redis.passed, redis.detail)
await checkBackend()

const credentials = [
  [process.env.BASELINE_ACCOUNT_1_USERNAME || process.env.BASELINE_MERCHANT_1_USERNAME, process.env.BASELINE_ACCOUNT_1_PASSWORD || process.env.BASELINE_MERCHANT_1_PASSWORD, '测试账号 1'],
  [process.env.BASELINE_ACCOUNT_2_USERNAME || process.env.BASELINE_MERCHANT_2_USERNAME, process.env.BASELINE_ACCOUNT_2_PASSWORD || process.env.BASELINE_MERCHANT_2_PASSWORD, '测试账号 2']
]

if (credentials.every(([username, password]) => username && password)) {
  for (const [username, password, label] of credentials) {
    await checkAccount(username, password, label)
  }
} else {
  addCheck('测试账号联调', false, '未提供 BASELINE_ACCOUNT_1/2_USERNAME 和 BASELINE_ACCOUNT_1/2_PASSWORD，已跳过账号请求')
}

console.log('\n小西门管理端联调基线检查')
for (const check of checks) {
  console.log(`${check.passed ? 'PASS' : 'FAIL'}  ${check.name}：${check.detail}`)
}

const failed = checks.filter((check) => !check.passed)
if (failed.length) {
  console.error(`\n基线未通过：${failed.length} 项。请先按 docs/local-baseline.md 准备环境。`)
  process.exitCode = 1
} else {
  console.log('\n基线检查通过，当前联调环境可用。')
}
