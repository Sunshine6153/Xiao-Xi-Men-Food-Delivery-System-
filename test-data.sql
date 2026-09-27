-- 小西门客户端联调测试数据；使用 INSERT ... SELECT 避免重复插入。
INSERT INTO merchant (username, password, merchant_name, phone, location, status)
SELECT 'seed_zhang', 'test123', '张记炒饭', '13800000001', '西门入口右侧', 1
WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE merchant_name = '张记炒饭');
INSERT INTO merchant (username, password, merchant_name, phone, location, status)
SELECT 'seed_chen', 'test123', '陈记面馆', '13800000002', '美食街中段', 1
WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE merchant_name = '陈记面馆');
INSERT INTO merchant (username, password, merchant_name, phone, location, status)
SELECT 'seed_wang', 'test123', '王记炸串', '13800000003', '美食街中段', 1
WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE merchant_name = '王记炸串');
INSERT INTO merchant (username, password, merchant_name, phone, location, status)
SELECT 'seed_guoguo', 'test123', '果果饮品', '13800000004', '西门出口旁', 1
WHERE NOT EXISTS (SELECT 1 FROM merchant WHERE merchant_name = '果果饮品');

INSERT INTO category (name, sort, status)
SELECT '炒饭', 10, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '炒饭');
INSERT INTO category (name, sort, status)
SELECT '面食', 20, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '面食');
INSERT INTO category (name, sort, status)
SELECT '炸串', 30, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '炸串');
INSERT INTO category (name, sort, status)
SELECT '饮品', 40, 1 WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = '饮品');

INSERT INTO dish (name, category_id, merchant_id, price, image, description, status)
SELECT '招牌蛋炒饭', c.id, m.id, 8.00, 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=600&q=80', '粒粒分明，火候足，配料丰富的经典蛋炒饭。', 1
FROM category c JOIN merchant m ON m.merchant_name = '张记炒饭'
WHERE c.name = '炒饭' AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '招牌蛋炒饭' AND d.merchant_id = m.id);
INSERT INTO dish (name, category_id, merchant_id, price, image, description, status)
SELECT '香辣鸡排炒饭', c.id, m.id, 13.00, 'https://images.unsplash.com/photo-1512058564366-18510be2db19?w=600&q=80', '现切鸡排配香辣酱汁，饱腹又过瘾。', 1
FROM category c JOIN merchant m ON m.merchant_name = '张记炒饭'
WHERE c.name = '炒饭' AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '香辣鸡排炒饭' AND d.merchant_id = m.id);
INSERT INTO dish (name, category_id, merchant_id, price, image, description, status)
SELECT '老式热干面', c.id, m.id, 7.00, 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=600&q=80', '芝麻酱香浓，面条劲道，校园里的熟悉味道。', 1
FROM category c JOIN merchant m ON m.merchant_name = '陈记面馆'
WHERE c.name = '面食' AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '老式热干面' AND d.merchant_id = m.id);
INSERT INTO dish (name, category_id, merchant_id, price, image, description, status)
SELECT '脆皮炸鸡柳', c.id, m.id, 10.00, 'https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=600&q=80', '外酥里嫩，现点现炸，撒上秘制椒盐。', 1
FROM category c JOIN merchant m ON m.merchant_name = '王记炸串'
WHERE c.name = '炸串' AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '脆皮炸鸡柳' AND d.merchant_id = m.id);
INSERT INTO dish (name, category_id, merchant_id, price, image, description, status)
SELECT '冰镇柠檬茶', c.id, m.id, 6.00, 'https://images.unsplash.com/photo-1523677011781-c91d1bbe2f9e?w=600&q=80', '酸甜清爽，搭配炸物和炒饭刚刚好。', 1
FROM category c JOIN merchant m ON m.merchant_name = '果果饮品'
WHERE c.name = '饮品' AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '冰镇柠檬茶' AND d.merchant_id = m.id);

INSERT INTO dish_flavor (dish_id, name, value)
SELECT d.id, '辣度', v.value FROM dish d JOIN (SELECT '不辣' value UNION ALL SELECT '微辣' UNION ALL SELECT '中辣') v
WHERE d.name = '招牌蛋炒饭' AND NOT EXISTS (SELECT 1 FROM dish_flavor f WHERE f.dish_id = d.id AND f.value = v.value);
INSERT INTO dish_flavor (dish_id, name, value)
SELECT d.id, '辣度', v.value FROM dish d JOIN (SELECT '微辣' value UNION ALL SELECT '中辣' UNION ALL SELECT '特辣') v
WHERE d.name = '香辣鸡排炒饭' AND NOT EXISTS (SELECT 1 FROM dish_flavor f WHERE f.dish_id = d.id AND f.value = v.value);
INSERT INTO dish_flavor (dish_id, name, value)
SELECT d.id, '口味', v.value FROM dish d JOIN (SELECT '原味' value UNION ALL SELECT '孜然' UNION ALL SELECT '香辣') v
WHERE d.name = '脆皮炸鸡柳' AND NOT EXISTS (SELECT 1 FROM dish_flavor f WHERE f.dish_id = d.id AND f.value = v.value);
