-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE COMMENT '用户名',
    password VARCHAR(100) COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    role_type INTEGER DEFAULT 1 COMMENT '角色类型: 1-求职者 2-HR 3-管理员',
    company_id INTEGER COMMENT '关联公司ID（HR角色）',
    status VARCHAR(20) DEFAULT 'active' COMMENT '账号状态：active-正常 disabled-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0 COMMENT '删除标记'
);

-- 角色类型表
CREATE TABLE IF NOT EXISTS role_type (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) COMMENT '角色名称',
    code VARCHAR(50) UNIQUE COMMENT '角色编码',
    description VARCHAR(200) COMMENT '角色描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- 初始化角色数据
INSERT INTO role_type (name, code, description) VALUES
('求职者', 'JOB_SEEKER', '普通用户，可上传简历、参加面试'),
('HR', 'HR', '招聘人员，可发布职位、查看候选人'),
('管理员', 'ADMIN', '系统管理员，拥有所有权限')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 简历表
CREATE TABLE IF NOT EXISTS resume (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) COMMENT '文件名',
    object_name VARCHAR(255) COMMENT 'MinIO对象名称',
    raw_content TEXT COMMENT '原始内容',
    name VARCHAR(50) COMMENT '姓名',
    education VARCHAR(50) COMMENT '学历',
    skills TEXT COMMENT '技能',
    work_experience TEXT COMMENT '工作经历',
    project_experience TEXT COMMENT '项目经历',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0 COMMENT '删除标记'
);

-- 用户-简历关系表（一个用户最多3份简历）
CREATE TABLE IF NOT EXISTS user_resume (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER COMMENT '用户ID',
    resume_id INTEGER COMMENT '简历ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0 COMMENT '删除标记'
);

-- 面试记录表
CREATE TABLE IF NOT EXISTS interview (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER COMMENT '用户ID',
    resume_id INTEGER COMMENT '简历ID',
    job_type VARCHAR(50) COMMENT '岗位类型',
    total_rounds INTEGER DEFAULT 10 COMMENT '总轮数',
    current_round INTEGER DEFAULT 0 COMMENT '当前轮数',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    total_score REAL COMMENT '总分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by INTEGER COMMENT '修改人ID',
    completed_at TIMESTAMP COMMENT '完成时间',
    deleted INTEGER DEFAULT 0 COMMENT '删除标记'
);

-- 面试问答表
CREATE TABLE IF NOT EXISTS interview_qa (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    interview_id INTEGER COMMENT '面试ID',
    round INTEGER COMMENT '轮次',
    question TEXT COMMENT '问题',
    answer TEXT COMMENT '回答',
    scores TEXT COMMENT '评分JSON',
    feedback TEXT COMMENT '反馈',
    reference_answer TEXT COMMENT '参考答案',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0 COMMENT '删除标记'
);

-- 评估报告表
CREATE TABLE IF NOT EXISTS report (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    interview_id INTEGER UNIQUE COMMENT '面试ID',
    summary TEXT COMMENT '总结',
    strengths TEXT COMMENT '优势',
    weaknesses TEXT COMMENT '不足',
    suggestions TEXT COMMENT '建议',
    recommendation TEXT COMMENT '录用建议',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0 COMMENT '删除标记'
);

-- ============================================
-- 招聘网站新增表
-- ============================================

-- 公司表
CREATE TABLE IF NOT EXISTS company (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER NOT NULL COMMENT 'HR用户ID',
    name VARCHAR(100) NOT NULL COMMENT '公司名称',
    logo VARCHAR(500) COMMENT '公司Logo',
    industry VARCHAR(50) COMMENT '所属行业',
    scale VARCHAR(50) COMMENT '公司规模',
    type VARCHAR(50) COMMENT '公司类型',
    city VARCHAR(50) COMMENT '所在城市（冗余，兼容旧数据）',
    region_id INTEGER COMMENT '省市区ID（区级level=3）',
    address VARCHAR(200) COMMENT '详细地址（街道门牌号）',
    website VARCHAR(200) COMMENT '公司官网',
    description TEXT COMMENT '公司介绍',
    view_count INTEGER DEFAULT 0 COMMENT '浏览量',
    follow_count INTEGER DEFAULT 0 COMMENT '关注数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_city (city),
    INDEX idx_region_id (region_id)
);

-- 用户-公司关注关系表
CREATE TABLE IF NOT EXISTS user_company_follow (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER NOT NULL COMMENT '用户ID',
    company_id INTEGER NOT NULL COMMENT '公司ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_company (user_id, company_id),
    INDEX idx_user_id (user_id),
    INDEX idx_company_id (company_id)
);

-- 职位分类表
CREATE TABLE IF NOT EXISTS job_category (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id INTEGER DEFAULT 0 COMMENT '父分类ID',
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0,
    UNIQUE INDEX uk_name_parent (name, parent_id)
);

-- 初始化职位分类
INSERT INTO job_category (name, parent_id, sort_order) VALUES
('技术类', 0, 1),
('产品类', 0, 2),
('设计类', 0, 3),
('运营类', 0, 4),
('职能类', 0, 5),
('Java开发', 1, 1),
('前端开发', 1, 2),
('Python开发', 1, 3),
('产品经理', 2, 1),
('UI设计', 3, 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 职位表
CREATE TABLE IF NOT EXISTS job (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    company_id INTEGER NOT NULL COMMENT '公司ID',
    user_id INTEGER NULL COMMENT '负责HR用户ID（NULL表示待认领）',
    category_id INTEGER COMMENT '职位分类ID',
    title VARCHAR(100) NOT NULL COMMENT '职位名称',
    city VARCHAR(50) NOT NULL COMMENT '工作城市',
    address VARCHAR(200) COMMENT '工作地点',
    salary_min INTEGER COMMENT '薪资下限',
    salary_max INTEGER COMMENT '薪资上限',
    job_type VARCHAR(20) NOT NULL COMMENT '工作类型：full_time/part_time/intern',
    experience VARCHAR(50) COMMENT '经验要求',
    education VARCHAR(50) COMMENT '学历要求',
    description TEXT NOT NULL COMMENT '职位描述',
    requirements TEXT COMMENT '任职要求',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active/paused/closed',
    view_count INTEGER DEFAULT 0 COMMENT '浏览次数',
    apply_count INTEGER DEFAULT 0 COMMENT '申请次数',
    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0,
    INDEX idx_company_id (company_id),
    INDEX idx_city (city),
    INDEX idx_status (status),
    FULLTEXT INDEX ft_title_desc (title, description)
);

-- 申请表
CREATE TABLE IF NOT EXISTS application (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    job_id INTEGER NOT NULL COMMENT '职位ID',
    user_id INTEGER NOT NULL COMMENT '求职者用户ID',
    resume_id INTEGER NOT NULL COMMENT '简历ID',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending/screening/interview/offer/rejected/withdrawn',
    hr_remark TEXT COMMENT 'HR备注',
    job_deleted TINYINT DEFAULT 0 COMMENT '职位是否已删除',
    reject_reason VARCHAR(500) COMMENT '拒绝原因',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0,
    UNIQUE INDEX uk_job_user (job_id, user_id),
    INDEX idx_user_id (user_id)
);

-- 消息表
CREATE TABLE IF NOT EXISTS message (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    sender_id INTEGER NOT NULL COMMENT '发送者ID',
    receiver_id INTEGER NOT NULL COMMENT '接收者ID',
    job_id INTEGER COMMENT '关联职位ID',
    resume_id INTEGER COMMENT '关联简历ID（投递消息时携带）',
    type VARCHAR(20) NOT NULL COMMENT '消息类型：chat/application/system/video_invite',
    content TEXT NOT NULL COMMENT '消息内容',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INTEGER COMMENT '修改人ID',
    deleted INTEGER DEFAULT 0,
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_receiver_read (receiver_id, is_read)
);

-- 收藏表
CREATE TABLE IF NOT EXISTS favorite (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER NOT NULL COMMENT '用户ID',
    job_id INTEGER NOT NULL COMMENT '职位ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER COMMENT '创建人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INTEGER COMMENT '修改人ID',
    job_deleted TINYINT DEFAULT 0 COMMENT '职位是否已删除',
    deleted INTEGER DEFAULT 0,
    UNIQUE INDEX uk_user_job (user_id, job_id),
    INDEX idx_job_id (job_id)
);

-- 省市区表
CREATE TABLE IF NOT EXISTS region (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '名称',
    parent_id INTEGER DEFAULT 0 COMMENT '父级ID（0=省级）',
    level TINYINT NOT NULL COMMENT '层级：1=省 2=市 3=区',
    area_code VARCHAR(20) COMMENT '行政区划代码',
    INDEX idx_parent_id (parent_id),
    INDEX idx_level (level)
);

-- 福利标签表
CREATE TABLE IF NOT EXISTS benefit_tag (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    type VARCHAR(20) NOT NULL DEFAULT 'company' COMMENT '类型：company-公司福利 job-岗位福利',
    sort_order INTEGER DEFAULT 0 COMMENT '排序权重（越大越靠前）',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：1=启用 0=禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name_type (name, type)
);

-- 公司-福利关联表
CREATE TABLE IF NOT EXISTS company_benefit (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    company_id INTEGER NOT NULL COMMENT '公司ID',
    benefit_tag_id INTEGER NOT NULL COMMENT '福利标签ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_company_tag (company_id, benefit_tag_id),
    INDEX idx_company_id (company_id),
    INDEX idx_benefit_tag_id (benefit_tag_id)
);

-- 职位-福利关联表
CREATE TABLE IF NOT EXISTS job_benefit (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    job_id INTEGER NOT NULL COMMENT '职位ID',
    benefit_tag_id INTEGER NOT NULL COMMENT '福利标签ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_job_tag (job_id, benefit_tag_id),
    INDEX idx_job_id (job_id),
    INDEX idx_benefit_tag_id (benefit_tag_id)
);

-- 用户档案表（扩展个人信息）
CREATE TABLE IF NOT EXISTS user_profile (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    user_id INTEGER UNIQUE NOT NULL COMMENT '关联用户ID',
    education VARCHAR(50) COMMENT '最高学历：高中/大专/本科/硕士/博士',
    personal_summary TEXT COMMENT '个人优势/自我介绍',
    job_status VARCHAR(30) DEFAULT 'looking' COMMENT '求职状态：looking-找工作 not_looking-暂不考虑 available-随时到岗',
    expect_city VARCHAR(100) COMMENT '期望城市',
    expect_salary_min INTEGER COMMENT '期望最低薪资',
    expect_salary_max INTEGER COMMENT '期望最高薪资',
    expect_job_type VARCHAR(50) COMMENT '期望职位类型',
    work_experience JSON COMMENT '工作/实习经历 [{company,position,startDate,endDate,description}]',
    project_experience JSON COMMENT '项目经历 [{name,role,startDate,endDate,description}]',
    education_experience JSON COMMENT '教育经历 [{school,major,degree,startDate,endDate}]',
    certificates JSON COMMENT '资格证书 [{name,issuer,obtainDate}]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
);

-- ============================================
-- 管理后台新增表
-- ============================================

-- 管理员操作日志表
CREATE TABLE IF NOT EXISTS admin_operation_log (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    admin_id INTEGER NOT NULL COMMENT '操作管理员ID',
    module VARCHAR(50) NOT NULL COMMENT '操作模块：user/company/job/application/interview/benefit_tag/job_category/region',
    action VARCHAR(50) NOT NULL COMMENT '操作类型：create/update/delete/status_change/role_change/import/export',
    target_id INTEGER COMMENT '操作对象ID',
    target_name VARCHAR(200) COMMENT '操作对象名称（冗余，方便查询）',
    detail TEXT COMMENT '操作详情JSON：{before: ..., after: ...}',
    ip VARCHAR(50) COMMENT '操作IP地址',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_admin_id (admin_id),
    INDEX idx_module (module),
    INDEX idx_action (action),
    INDEX idx_target (module, target_id),
    INDEX idx_created_at (created_at)
) COMMENT '管理员操作日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'string' COMMENT '值类型：string/number/boolean/json',
    description VARCHAR(200) COMMENT '配置说明',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INTEGER COMMENT '修改人ID'
) COMMENT '系统配置表';

-- 初始化系统配置
INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('site_name', 'AI面试官', 'string', '站点名称'),
('site_logo', '/logo.png', 'string', '站点Logo'),
('maintenance_mode', 'false', 'boolean', '是否维护模式'),
('registration_enabled', 'true', 'boolean', '是否开放注册'),
('max_resume_count', '3', 'number', '每用户最大简历数'),
('max_import_rows', '500', 'number', '单次导入最大行数'),
('admin_email', 'admin@ai-interview.com', 'string', '管理员邮箱')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);

-- 职位审核日志表
CREATE TABLE IF NOT EXISTS job_audit_log (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    job_id INTEGER NOT NULL COMMENT '职位ID',
    admin_id INTEGER NOT NULL COMMENT '审核管理员ID',
    action VARCHAR(20) NOT NULL COMMENT '操作：approve/reject',
    reason TEXT COMMENT '审核原因（拒绝时必填）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
    INDEX idx_job_id (job_id),
    INDEX idx_admin_id (admin_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) COMMENT '职位审核日志表';
