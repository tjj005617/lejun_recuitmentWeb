-- ============================================
-- 批量生成20000条职位数据
-- 执行：mysql -h host -u user -p dbname < generate_jobs.sql
-- ============================================

-- 先清空旧职位数据
TRUNCATE TABLE job;

-- 创建临时数字表（0-19999）
DROP TEMPORARY TABLE IF EXISTS nums;
CREATE TEMPORARY TABLE nums (n INT);
INSERT INTO nums (n)
SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000
FROM
    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d,
    (SELECT 0 AS N UNION SELECT 1) e;

-- 批量插入20000条职位
INSERT INTO job (company_id, category_id, title, city, address, salary_min, salary_max, job_type, experience, education, description, requirements, benefits, status, view_count, apply_count, published_at, deleted)
SELECT
    -- 随机公司ID (1-20)
    FLOOR(1 + RAND() * 20) AS company_id,
    -- 随机分类ID
    ELT(FLOOR(1 + RAND() * 11), 6, 6, 6, 7, 7, 8, 8, 9, 10, NULL, NULL) AS category_id,
    -- 随机职位名称
    ELT(FLOOR(1 + RAND() * 30),
        'Java开发工程师', '高级Java工程师', 'Java架构师', '后端开发工程师',
        '前端开发工程师', '高级前端工程师', 'Vue开发工程师', 'React开发工程师',
        'Python开发工程师', 'AI算法工程师', '数据工程师', '大数据开发工程师',
        '产品经理', '高级产品经理', '数据产品经理', '策略产品经理',
        'UI设计师', 'UX设计师', '平面设计师', '视觉设计师',
        '测试工程师', '自动化测试工程师', '运维工程师', 'DevOps工程师',
        'Android开发工程师', 'iOS开发工程师', 'C++开发工程师', 'Go开发工程师',
        '项目经理', '技术总监'
    ) AS title,
    -- 随机城市
    ELT(FLOOR(1 + RAND() * 12),
        '北京', '北京', '北京', '上海', '上海', '上海',
        '杭州', '杭州', '深圳', '深圳', '广州', '成都'
    ) AS city,
    -- 随机地址
    CONCAT(
        ELT(FLOOR(1 + RAND() * 12),
            '北京', '北京', '北京', '上海', '上海', '上海',
            '杭州', '杭州', '深圳', '深圳', '广州', '成都'
        ),
        '市',
        ELT(FLOOR(1 + RAND() * 6), '海淀区', '朝阳区', '浦东新区', '南山区', '余杭区', '天府新区'),
        ELT(FLOOR(1 + RAND() * 6), '科技园', '软件园', '创新中心', '产业基地', '总部大楼', '创业园')
    ) AS address,
    -- 随机薪资下限
    ELT(FLOOR(1 + RAND() * 8), 4000, 6000, 8000, 10000, 12000, 15000, 20000, 25000) AS salary_min,
    -- 随机薪资上限（下限的1.3-2.5倍，取整到千）
    FLOOR(
        ELT(FLOOR(1 + RAND() * 8), 4000, 6000, 8000, 10000, 12000, 15000, 20000, 25000)
        * ELT(FLOOR(1 + RAND() * 5), 1.3, 1.5, 1.8, 2.0, 2.5)
        / 1000
    ) * 1000 AS salary_max,
    -- 随机工作类型
    ELT(FLOOR(1 + RAND() * 10),
        'full_time', 'full_time', 'full_time', 'full_time', 'full_time',
        'full_time', 'full_time', 'part_time', 'intern', 'full_time'
    ) AS job_type,
    -- 随机经验要求
    ELT(FLOOR(1 + RAND() * 6), '不限', '应届生', '1-3年', '3-5年', '5-10年', '10年以上') AS experience,
    -- 随机学历要求
    ELT(FLOOR(1 + RAND() * 5), '不限', '大专', '本科', '本科', '硕士') AS education,
    -- 职位描述
    CONCAT(
        '负责相关岗位的工作。',
        ELT(FLOOR(1 + RAND() * 4),
            '参与公司核心产品的设计与开发，持续优化系统性能和用户体验。',
            '与产品、设计团队紧密协作，高质量完成功能开发和代码评审。',
            '负责技术方案的设计与落地，解决线上技术问题。',
            '参与系统架构设计，推动技术创新和最佳实践的落地。'
        )
    ) AS description,
    -- 任职要求
    CONCAT(
        '1. 具备相关工作经验',
        IF(ELT(FLOOR(1 + RAND() * 5), '不限', '大专', '本科', '本科', '硕士') != '不限',
           CONCAT('，', ELT(FLOOR(1 + RAND() * 5), '不限', '大专', '本科', '本科', '硕士'), '及以上学历'), ''),
        '\n2. ',
        ELT(FLOOR(1 + RAND() * 6),
            '熟悉主流开发框架和工具',
            '具备良好的编码规范和文档能力',
            '有大型项目开发经验优先',
            '具备良好的沟通和团队协作能力',
            '对技术有热情，持续学习新技术',
            '有互联网行业经验优先'
        ),
        '\n3. ',
        ELT(FLOOR(1 + RAND() * 5),
            '熟悉敏捷开发流程',
            '具备问题分析和解决能力',
            '有性能优化经验优先',
            '了解微服务架构',
            '有开源项目贡献经验优先'
        ),
        '\n4. 良好的团队协作精神和沟通能力'
    ) AS requirements,
    -- 福利JSON
    ELT(FLOOR(1 + RAND() * 6),
        '["五险一金","带薪年假","弹性工作","免费三餐"]',
        '["五险一金","带薪年假","股票期权","免费班车","健身房"]',
        '["五险一金","带薪年假","年终奖","节日福利","培训基金"]',
        '["五险一金","带薪年假","补充医疗保险","员工旅游","租房补贴"]',
        '["五险一金","带薪年假","弹性工作","免费三餐","团建活动"]',
        '["五险一金","带薪年假","股票期权","免费三餐","弹性工作","健身房"]'
    ) AS benefits,
    -- 随机状态
    ELT(FLOOR(1 + RAND() * 10),
        'active', 'active', 'active', 'active', 'active',
        'active', 'active', 'active', 'paused', 'closed'
    ) AS status,
    -- 随机浏览数
    FLOOR(RAND() * 2000) AS view_count,
    -- 随机申请数
    FLOOR(RAND() * 200) AS apply_count,
    -- 随机发布日期（最近90天内）
    DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 90) DAY) AS published_at,
    0 AS deleted
FROM nums
WHERE n < 20000;

-- 清理临时表
DROP TEMPORARY TABLE IF EXISTS nums;

-- 验证
SELECT COUNT(*) AS total_jobs FROM job;
