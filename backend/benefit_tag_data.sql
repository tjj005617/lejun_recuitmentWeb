-- 福利标签初始数据（公司福利）
INSERT IGNORE INTO benefit_tag (name, type, sort_order) VALUES
('五险一金', 'company', 100),
('六险二金', 'company', 99),
('年终奖', 'company', 98),
('股票期权', 'company', 97),
('带薪年假', 'company', 96),
('弹性工作', 'company', 95),
('远程办公', 'company', 94),
('免费三餐', 'company', 93),
('零食下午茶', 'company', 92),
('健身房', 'company', 91),
('定期体检', 'company', 90),
('补充医疗保险', 'company', 89),
('交通补贴', 'company', 88),
('住房补贴', 'company', 87),
('通讯补贴', 'company', 86),
('节日福利', 'company', 85),
('生日福利', 'company', 84),
('团建旅游', 'company', 83),
('培训发展', 'company', 82),
('晋升空间', 'company', 81);

-- 福利标签初始数据（岗位福利）
INSERT IGNORE INTO benefit_tag (name, type, sort_order) VALUES
('加班补贴', 'job', 80),
('周末双休', 'job', 79),
('带薪病假', 'job', 78),
('员工旅游', 'job', 77),
('子女教育', 'job', 76);
