-- ==================== 用户（4个HR + 6个求职者）====================
INSERT IGNORE INTO user (id, username, password, nickname, phone, email, role_type, created_by) VALUES
(3, 'hr_zhangsan', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '张三HR', '13800001001', 'zhangsan@hr.com', 2, 3),
(4, 'hr_lisi', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '李四HR', '13800001002', 'lisi@hr.com', 2, 4),
(5, 'hr_wangwu', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '王五HR', '13800001003', 'wangwu@hr.com', 2, 5),
(6, 'hr_zhaoliu', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '赵六HR', '13800001004', 'zhaoliu@hr.com', 2, 6),
(7, 'seeker_sunqi', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '孙七', '13800002001', 'sunqi@qq.com', 1, 7),
(8, 'seeker_zhouba', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '周八', '13800002002', 'zhouba@qq.com', 1, 8),
(9, 'seeker_wujiu', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '吴九', '13800002003', 'wujiu@qq.com', 1, 9),
(10, 'seeker_zhengshi', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '郑十', '13800002004', 'zhengshi@qq.com', 1, 10),
(11, 'seeker_qianyi', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '钱一', '13800002005', 'qianyi@qq.com', 1, 11),
(12, 'seeker_liuer', 'jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', '刘二', '13800002006', 'liuer@qq.com', 1, 12);

-- ==================== 简历（求职者各1份）====================
INSERT IGNORE INTO resume (id, name, education, skills, work_experience, project_experience, raw_content, created_by) VALUES
(2, '孙七', '本科', 'Java, Spring Boot, MySQL', '3年Java开发经验', '电商后台管理系统', '孙七的简历内容', 7),
(3, '周八', '硕士', 'Python, Django, TensorFlow', '2年算法工程师经验', '推荐系统项目', '周八的简历内容', 8),
(4, '吴九', '本科', 'Vue, React, TypeScript', '4年前端开发经验', '移动端H5项目', '吴九的简历内容', 9),
(5, '郑十', '本科', 'Go, Docker, K8s', '3年运维开发经验', '容器化部署平台', '郑十的简历内容', 10),
(6, '钱一', '硕士', 'Java, Spring Cloud, Redis', '5年后端开发经验', '微服务架构改造', '钱一的简历内容', 11),
(7, '刘二', '本科', 'C++, 数据结构, 算法', '2年游戏开发经验', '3D渲染引擎优化', '刘二的简历内容', 12);

-- ==================== 用户-简历关联 =====================
INSERT IGNORE INTO user_resume (user_id, resume_id) VALUES
(7, 2), (8, 3), (9, 4), (10, 5), (11, 6), (12, 7);

-- ==================== 公司（4家）====================
INSERT IGNORE INTO company (id, user_id, name, industry, scale, type, city, address, website, description, benefits, created_by) VALUES
(1, 3, '星辰科技', '互联网', '100-499人', '民营', '北京', '北京市海淀区中关村', 'https://www.xingchen.com', '专注于人工智能和大数据领域的创新型企业', '["五险一金","带薪年假","免费午餐","团建活动"]', 3),
(2, 4, '金融宝', '金融', '500-999人', '国企', '上海', '上海市浦东新区陆家嘴', 'https://www.jinrongbao.com', '国内领先的金融科技服务平台', '["六险二金","年终奖金","股票期权","弹性工作"]', 4),
(3, 5, '未来教育', '教育', '20-99人', '创业公司', '深圳', '深圳市南山区科技园', 'https://www.weilai.edu', '用AI技术革新教育行业', '["五险一金","扁平管理","零食下午茶","学习补贴"]', 5),
(4, 6, '云端网络', '互联网', '1000-9999人', '民营企业', '杭州', '杭州市余杭区未来科技城', 'https://www.yunduan.com', '云计算和分布式系统解决方案提供商', '["六险一金","带薪年假","住房补贴","年度体检"]', 6);

-- ==================== 职位（10个）====================
INSERT IGNORE INTO job (id, company_id, user_id, category_id, title, city, address, salary_min, salary_max, job_type, experience, education, description, requirements, benefits, status, view_count, apply_count, published_at, created_by) VALUES
(1, 1, 3, 6, 'Java高级工程师', '北京', '北京市海淀区中关村', 25000, 40000, 'full_time', '3-5年', '本科', '负责公司核心业务系统的设计和开发', '1. 精通Java/Spring Boot\n2. 熟悉MySQL/Redis\n3. 有微服务经验优先', '["五险一金","带薪年假","股票期权"]', 'active', 156, 12, '2026-06-01 10:00:00', 3),
(2, 1, 3, 7, '前端开发工程师', '北京', '北京市海淀区中关村', 20000, 35000, 'full_time', '2-4年', '本科', '负责公司产品的前端开发和优化', '1. 精通Vue/React\n2. 熟悉TypeScript\n3. 有性能优化经验', '["五险一金","带薪年假","弹性工作"]', 'active', 98, 8, '2026-06-02 10:00:00', 3),
(3, 2, 4, 6, 'Java开发工程师', '上海', '上海市浦东新区陆家嘴', 20000, 35000, 'full_time', '2-5年', '本科', '负责金融交易系统的后端开发', '1. 精通Java\n2. 了解金融业务优先\n3. 熟悉分布式系统', '["六险二金","年终奖金","股票期权"]', 'active', 210, 18, '2026-06-01 14:00:00', 4),
(4, 2, 4, 8, '产品经理', '上海', '上海市浦东新区陆家嘴', 25000, 40000, 'full_time', '3-5年', '本科', '负责公司金融产品的规划和设计', '1. 3年以上产品经理经验\n2. 有金融行业经验优先\n3. 优秀的沟通能力', '["六险二金","年终奖金","弹性工作"]', 'active', 87, 5, '2026-06-03 10:00:00', 4),
(5, 3, 5, 6, 'Python开发工程师', '深圳', '深圳市南山区科技园', 18000, 30000, 'full_time', '1-3年', '本科', '负责AI教育产品的后端开发', '1. 精通Python/Django\n2. 了解机器学习基础\n3. 有REST API开发经验', '["五险一金","扁平管理","零食下午茶"]', 'active', 65, 6, '2026-06-04 10:00:00', 5),
(6, 3, 5, 9, 'UI设计师', '深圳', '深圳市南山区科技园', 15000, 25000, 'full_time', '1-3年', '本科', '负责公司产品的UI/UX设计', '1. 精通Figma/Sketch\n2. 有B端产品设计经验\n3. 良好的审美能力', '["五险一金","扁平管理","学习补贴"]', 'active', 43, 3, '2026-06-05 10:00:00', 5),
(7, 4, 6, 6, 'Go开发工程师', '杭州', '杭州市余杭区未来科技城', 25000, 45000, 'full_time', '3-5年', '本科', '负责云平台核心服务的开发', '1. 精通Go语言\n2. 熟悉Docker/K8s\n3. 有高并发系统经验', '["六险一金","住房补贴","年度体检"]', 'active', 178, 15, '2026-06-01 09:00:00', 6),
(8, 4, 6, 7, '前端开发工程师', '杭州', '杭州市余杭区未来科技城', 20000, 35000, 'full_time', '2-4年', '本科', '负责云平台管理控制台的前端开发', '1. 精通Vue/React\n2. 熟悉Node.js\n3. 有可视化开发经验优先', '["六险一金","住房补贴","弹性工作"]', 'active', 112, 9, '2026-06-02 14:00:00', 6),
(9, 1, 3, 6, 'Java中级工程师', '北京', '北京市海淀区中关村', 15000, 25000, 'full_time', '1-3年', '本科', '参与公司业务系统的开发和维护', '1. 熟悉Java/Spring\n2. 了解MySQL\n3. 有团队协作精神', '["五险一金","带薪年假","免费午餐"]', 'paused', 54, 4, '2026-06-06 10:00:00', 3),
(10, 2, 4, 8, '运营专员', '上海', '上海市浦东新区陆家嘴', 10000, 18000, 'full_time', '1-3年', '本科', '负责公司产品的用户运营和增长', '1. 有互联网运营经验\n2. 熟悉数据分析工具\n3. 良好的文案能力', '["六险二金","年终奖金"]', 'closed', 32, 2, '2026-06-07 10:00:00', 4);

-- ==================== 应聘申请（10条）====================
INSERT IGNORE INTO application (job_id, user_id, resume_id, status, applied_at, created_by) VALUES
(1, 7, 2, 'pending', '2026-06-05 09:00:00', 7),
(1, 11, 6, 'screening', '2026-06-05 10:00:00', 11),
(2, 9, 4, 'interview', '2026-06-05 11:00:00', 9),
(3, 7, 2, 'pending', '2026-06-06 09:00:00', 7),
(3, 8, 3, 'offer', '2026-06-06 10:00:00', 8),
(5, 8, 3, 'pending', '2026-06-07 09:00:00', 8),
(7, 10, 5, 'screening', '2026-06-07 10:00:00', 10),
(7, 11, 6, 'interview', '2026-06-07 11:00:00', 11),
(8, 9, 4, 'rejected', '2026-06-08 09:00:00', 9),
(9, 12, 7, 'pending', '2026-06-08 10:00:00', 12);

-- ==================== 消息（10条）====================
INSERT IGNORE INTO message (sender_id, receiver_id, job_id, type, content, is_read, created_at, created_by) VALUES
(3, 7, 1, 'chat', '您好，看到了您的简历，想了解一下您的项目经验', 1, '2026-06-05 09:30:00', 3),
(7, 3, 1, 'chat', '好的，我之前做的是电商后台系统，主要负责订单模块', 1, '2026-06-05 09:35:00', 7),
(3, 7, 1, 'chat', '不错，我们这边正好需要有电商经验的Java工程师，方便这周面试吗？', 0, '2026-06-05 09:40:00', 3),
(4, 8, 3, 'chat', '您好，您的简历很符合我们的岗位要求', 1, '2026-06-06 10:30:00', 4),
(8, 4, 3, 'chat', '谢谢，请问具体面试时间是？', 1, '2026-06-06 10:35:00', 8),
(4, 8, 3, 'chat', '明天下午2点可以吗？线上面试', 0, '2026-06-06 10:40:00', 4),
(5, 8, 5, 'chat', '看到您投递了我们的Python开发岗位', 0, '2026-06-07 09:30:00', 5),
(6, 10, 7, 'chat', '您好，收到了您的简历，想聊聊Go开发经验', 0, '2026-06-07 10:30:00', 6),
(6, 11, 7, 'chat', '您好，简历已收到，请问方便这周面试吗？', 0, '2026-06-07 11:30:00', 6),
(9, 3, 9, 'chat', '请问这个Java中级岗位还招人吗？', 0, '2026-06-08 10:00:00', 9);

-- ==================== 收藏（10条）====================
INSERT IGNORE INTO favorite (user_id, job_id, created_by) VALUES
(7, 1, 7),
(7, 3, 7),
(8, 5, 8),
(8, 7, 8),
(9, 2, 9),
(9, 8, 9),
(10, 7, 10),
(11, 1, 11),
(11, 7, 11),
(12, 9, 12);
