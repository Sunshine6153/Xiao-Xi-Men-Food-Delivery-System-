import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { execFileSync } from 'node:child_process'

// 仅用于本机开发数据库；不修改接口、表结构或已有微信用户。
const config = fs.readFileSync('/Users/a123/Work/Spring/xiaoximen/sky-server/src/main/resources/application-dev.yml', 'utf8')
const password = config.match(/password:\s*([^\r\n]+)/)?.[1].trim().replace(/^['"]|['"]$/g, '')
if (!password || !config.includes('localhost:3306/xiaoximen')) throw new Error('请核对本地开发数据库配置')
const env = { ...process.env, MYSQL_PWD: password }
const mysql = '/opt/homebrew/bin/mysql'
const args = ['-h127.0.0.1', '-uroot', '--default-character-set=utf8mb4', '--batch', '--skip-column-names', 'xiaoximen']
function query(sql) { return execFileSync(mysql, [...args, '-e', sql], { env, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 }).trim() }
const quote = (value) => value == null ? 'NULL' : `'${String(value).replaceAll('\\', '\\\\').replaceAll("'", "''")}'`
const hash = crypto.createHash('md5').update('123456').digest('hex')
const prefix = 'demo_campus_'
if (Number(query("SELECT COUNT(*) FROM user WHERE username LIKE 'demo_campus_%'"))) throw new Error('拟真数据已存在，停止重复插入')
const targets = query("SELECT id FROM merchant WHERE id IN (6,7,8,9,10,11) AND username='testuser' AND merchant_name='炒饭' AND role='MERCHANT' ORDER BY id").split('\n').filter(Boolean).map(Number)
if (targets.length !== 6) throw new Error('测试商户与预期不符，停止删除')
const ids = targets.join(',')
const references = Number(query(`SELECT (SELECT COUNT(*) FROM dish WHERE merchant_id IN (${ids})) + (SELECT COUNT(*) FROM order_detail WHERE merchant_id IN (${ids})) + (SELECT COUNT(*) FROM shopping_cart WHERE merchant_id IN (${ids}))`))
if (references) throw new Error('测试商户存在关联数据，需要先人工确认')

const now = new Date()
function at(daysAgo, hour, minute = 0) {
  const date = new Date(now)
  date.setDate(date.getDate() - daysAgo)
  date.setHours(hour, minute, 0, 0)
  return date
}
function datetime(date) {
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
const time = date => quote(datetime(date))
const later = (date, minutes) => new Date(date.getTime() + minutes * 60000)
let randomState = 9272026
function random(max) { randomState = (randomState * 1664525 + 1013904223) >>> 0; return randomState % max }
const sql = ['START TRANSACTION;']
function insert(table, values, variable) {
  sql.push(`INSERT INTO ${table} (${Object.keys(values).map(k => '`'+k+'`').join(',')}) VALUES (${Object.values(values).join(',')});`)
  if (variable) sql.push(`SET ${variable}=LAST_INSERT_ID();`)
}
sql.push(`DELETE FROM merchant WHERE id IN (${ids}) AND username='testuser' AND merchant_name='炒饭' AND role='MERCHANT';`)
const merchants = [
  { id: 12, name: '张记炒饭', user: 'seed_zhang', days: 95 },
  { id: 13, name: '陈记面馆', user: 'seed_chen', days: 84 },
  { id: 14, name: '王记炸串', user: 'seed_wang', days: 73 },
  { id: 15, name: '果果饮品', user: 'seed_guoguo', days: 62 },
  { id: 1, name: '西门烧烤', user: 'testmerchant', days: 105 },
  { id: '@merchant5', name: '禾香盖饭', user: 'demo_campus_hexiang', days: 48 },
  { id: '@merchant6', name: '小巷麻辣烫', user: 'demo_campus_xiaoxiang', days: 36 },
]
merchants.forEach((m, index) => {
  if (typeof m.id === 'number') {
    sql.push(`UPDATE merchant SET merchant_name=${quote(m.name)},create_time=${time(at(m.days,9,index*4))},update_time=${time(at(m.days-2,14,12))} WHERE id=${m.id} AND username=${quote(m.user)} AND role='MERCHANT';`)
  } else {
    insert('merchant', {username:quote(m.user),password:quote(hash),merchant_name:quote(m.name),phone:quote(`13800009${String(index).padStart(3,'0')}`),location:quote(index===5?'西门美食街 B06 档口':'西门美食街 B08 档口'),status:1,business_status:1,role:quote('MERCHANT'),create_time:time(at(m.days,9,20)),update_time:time(at(m.days-1,10,15))}, m.id)
  }
})
// 使用现有图片，新增菜品无图时由客户端显示默认灰图。
const menus = [
  [['扬州炒饭',11,2],['牛肉炒饭',16,2],['老干妈炒饭',10,2]],
  [['红烧牛肉面',16,3],['番茄鸡蛋面',11,3],['酸辣拌面',10,3]],
  [['炸土豆片',5,4],['香酥鸡排',12,4],['炸年糕',6,4]],
  [['茉莉奶茶',9,5],['杨枝甘露',12,5],['乌梅冰茶',7,5]],
  [['孜然鸡翅',18,1],['烤五花肉',15,1],['烤玉米',6,1]],
  [['鱼香肉丝盖饭',14,2],['宫保鸡丁盖饭',15,2],['土豆牛肉盖饭',18,2]],
  [['经典麻辣烫',18,3],['番茄汤麻辣烫',19,3],['香辣干拌麻辣烫',20,3]],
]
const dishes = []
menus.forEach((menu, merchantIndex) => menu.forEach(([name, price, category], j) => {
  const variable = `@dish${dishes.length}`
  insert('dish', {name:quote(name),price,category_id:category,merchant_id:merchants[merchantIndex].id,image:'NULL',description:quote('现点现做，校园日常餐食。'),status:1,create_time:time(at(merchants[merchantIndex].days-1,10,j*8)),update_time:time(at(5+random(20),15,j*6))},variable)
  const flavor = merchantIndex===3 ? '正常糖，少冰' : '微辣'
  if (merchantIndex===3) {
    insert('dish_flavor',{dish_id:variable,name:quote('糖度'),value:quote('["正常糖","少糖","无糖"]')})
    insert('dish_flavor',{dish_id:variable,name:quote('冰量'),value:quote('["正常冰","少冰","去冰"]')})
  } else insert('dish_flavor',{dish_id:variable,name:quote('辣度'),value:quote('["不辣","微辣","中辣"]')})
  dishes.push({id:variable,merchant:merchants[merchantIndex].id,name,price,flavor})
}))
const names = ['林沐','陈晨','周宁','许知遥','李思源','王悦','赵子安','刘雨桐','沈溪','宋然','顾一凡','苏禾']
const addresses = ['东区一号宿舍楼一楼大厅','东区二号宿舍楼门口','西区三号宿舍楼门口','图书馆南门','教学楼 A 区一楼大厅','研究生公寓值班室旁']
const users = []
for (let i=0;i<60;i++) {
  const variable=`@user${i}`
  const daysAgo = i<12 ? 35+random(25) : Math.floor((i-12)*30/48)
  const name=names[i%names.length]+(i>=12?String(Math.floor(i/12)+1):'')
  const phone=`1390000${String(1000+i).padStart(4,'0')}`
  insert('user',{openid:quote(`demo_campus_openid_${i}`),username:quote(`${prefix}${String(i+1).padStart(3,'0')}`),password:quote(hash),name:quote(name),phone:quote(phone),status:1,create_time:time(at(daysAgo,8+random(2),random(60))),update_time:time(at(daysAgo,10,random(60)))},variable)
  insert('address_book',{user_id:variable,consignee:quote(name),phone:quote(phone),address:quote(addresses[i%addresses.length]),is_default:1,create_time:time(at(daysAgo,14,random(60))),update_time:time(at(daysAgo,14,random(60)))})
  users.push({id:variable,name,phone,address:addresses[i%addresses.length],daysAgo})
}
let orderCount=0, detailCount=0
for(let day=29;day>=0;day--) {
  const count = 9 + random(9) + (day<7?5:0)
  for(let n=0;n<count;n++) {
    const eligible=users.filter(u=>u.daysAgo>=day)
    const user=eligible[random(eligible.length)]
    const roll=random(100)
    let status=roll<84?7:8
    let ordered=at(day,n%3===0?18:n%3===1?11:12,random(50))
    if(day===0 && n<8) {status=n+1;ordered=later(now,-(status===1?2:status===2?4:status===3?8:status===4?12:status===7?55:20))}
    else if(day===0 && later(ordered,60)>now) ordered=later(now,-90-n*3)
    const first=dishes[random(dishes.length)]
    const items=[{...first,number:1+random(2)}]
    if(n%3===0) items.push({...dishes[(dishes.indexOf(first)+4+random(8))%dishes.length],number:1})
    const amount=items.reduce((sum,item)=>sum+item.price*item.number,0)+2
    const paid=status!==1 && !(status===8&&n%2===0)
    const delivered=status===6||status===7
    const accepted=status>=3&&status<=7
    const orderVariable=`@order${orderCount}`
    insert('orders',{number:quote(`DEMO${datetime(ordered).replace(/[- :]/g,'')}${String(orderCount).padStart(4,'0')}`),user_id:user.id,delivery_user_id:accepted?users[10+random(2)].id:'NULL',status,amount,delivery_fee:2,order_time:time(ordered),checkout_time:paid?time(later(ordered,1)):'NULL',order_delivery_time:time(later(ordered,45)),delivered_time:delivered?time(later(ordered,status===6?15:35)):'NULL',tableware_amount:items.reduce((s,x)=>s+x.number,0),consignee:quote(user.name),phone:quote(user.phone),address:quote(user.address),remark:quote(['少放辣，谢谢','到了请发消息','不要香菜','正常制作','餐具按份数放'][random(5)]),create_time:time(ordered),update_time:time(later(ordered,status===7?40:status===6?15:status===8?5:1))},orderVariable)
    items.forEach((item,index)=>{
      const detailStatus=status>=4&&status<=7?3:status===3?(items.length>1&&index===1?1:2):1
      insert('order_detail',{order_id:orderVariable,dish_id:item.id,merchant_id:item.merchant,name:quote(item.name),dish_flavor:quote(item.flavor),number:item.number,amount:item.price,image:'NULL',status:detailStatus,create_time:time(ordered),update_time:time(later(ordered,detailStatus===3?10:1))})
      detailCount++
    })
    orderCount++
  }
}
sql.push('COMMIT;')
const directory=path.resolve('local-data-backups',datetime(now).replace(/[- :]/g,''))
fs.mkdirSync(directory,{recursive:true,mode:0o700})
const dump=execFileSync('/opt/homebrew/bin/mysqldump',['-h127.0.0.1','-uroot','--single-transaction','--skip-lock-tables','--no-tablespaces','--set-gtid-purged=OFF','xiaoximen'],{env,maxBuffer:32*1024*1024})
fs.writeFileSync(path.join(directory,'before-seed.sql'),dump,{mode:0o600})
fs.writeFileSync(path.join(directory,'insert-demo.sql'),sql.join('\n'),{mode:0o600})
execFileSync(mysql,args,{env,input:sql.join('\n'),maxBuffer:16*1024*1024})
console.log(JSON.stringify({backup:directory,deletedMerchants:targets,addedMerchants:2,addedUsers:60,addedDishes:dishes.length,addedOrders:orderCount,addedDetails:detailCount},null,2))
console.log(query('SELECT status,COUNT(*) AS order_count FROM orders GROUP BY status; SELECT DATE(order_time),COUNT(*),SUM(CASE WHEN status=7 THEN amount ELSE 0 END) AS turnover FROM orders GROUP BY DATE(order_time) ORDER BY DATE(order_time);'))
