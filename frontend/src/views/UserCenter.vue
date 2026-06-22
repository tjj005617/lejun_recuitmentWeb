<template>
  <AppShell :bgImage="false">
    <div class="user-center">
      <div class="user-center__container">
        <!-- 左侧边栏 -->
        <aside class="sidebar">
          <!-- 用户信息卡片 -->
          <div class="user-card">
            <div class="user-card__avatar">
              <img v-if="user?.avatar" :src="user.avatar" alt="头像" />
              <span v-else>{{ user?.nickname?.charAt(0) || user?.username?.charAt(0) || 'U' }}</span>
            </div>
            <h3 class="user-card__name">{{ user?.nickname || user?.username }}</h3>
            <div class="user-card__id">ID: {{ user?.id }}</div>
            <div class="user-card__info">
              <div class="user-card__item">
                <span class="user-card__label">手机</span>
                <span class="user-card__value">{{ user?.phone || '未绑定' }}</span>
              </div>
              <div class="user-card__item">
                <span class="user-card__label">邮箱</span>
                <span class="user-card__value">{{ user?.email || '未绑定' }}</span>
              </div>
              <div class="user-card__item" v-if="profile.education">
                <span class="user-card__label">学历</span>
                <span class="user-card__value">{{ profile.education }}</span>
              </div>
              <div class="user-card__item" v-if="profile.jobStatus">
                <span class="user-card__label">求职状态</span>
                <span class="user-card__value">{{ jobStatusText(profile.jobStatus) }}</span>
              </div>
            </div>
            <el-button text type="primary" size="small" class="user-card__edit" @click="openEditDialog">
              编辑个人信息
            </el-button>
          </div>

          <!-- 菜单导航 -->
          <nav class="sidebar-menu">
            <div
              v-for="item in menuItems"
              :key="item.key"
              class="sidebar-menu__item"
              :class="{ active: activeTab === item.key }"
              @click="activeTab = item.key"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </div>
          </nav>
        </aside>

        <!-- 右侧内容区 -->
        <main class="content">
          <!-- 我的档案 -->
          <div v-show="activeTab === 'profile'" class="content-panel">
            <div class="content-header">
              <h2>我的档案</h2>
              <el-button type="primary" size="small" @click="openProfileEditDialog">编辑档案</el-button>
            </div>

            <!-- 求职意向 -->
            <div class="profile-section">
              <h3 class="profile-section__title">求职意向</h3>
              <div class="profile-fields">
                <div class="profile-field">
                  <span class="profile-field__label">最高学历</span>
                  <span class="profile-field__value">{{ profile.education || '未填写' }}</span>
                </div>
                <div class="profile-field">
                  <span class="profile-field__label">求职状态</span>
                  <span class="profile-field__value">{{ jobStatusText(profile.jobStatus) }}</span>
                </div>
                <div class="profile-field">
                  <span class="profile-field__label">期望城市</span>
                  <span class="profile-field__value">{{ profile.expectCity || '未填写' }}</span>
                </div>
                <div class="profile-field">
                  <span class="profile-field__label">期望薪资</span>
                  <span class="profile-field__value">{{ formatExpectSalary(profile.expectSalaryMin, profile.expectSalaryMax) }}</span>
                </div>
                <div class="profile-field">
                  <span class="profile-field__label">期望职位</span>
                  <span class="profile-field__value">{{ profile.expectJobType || '未填写' }}</span>
                </div>
              </div>
            </div>

            <!-- 个人优势 -->
            <div class="profile-section">
              <h3 class="profile-section__title">个人优势</h3>
              <div class="profile-summary">{{ profile.personalSummary || '未填写' }}</div>
            </div>

            <!-- 工作/实习经历 -->
            <div class="profile-section">
              <div class="profile-section__header">
                <h3 class="profile-section__title">工作/实习经历</h3>
                <el-button size="small" type="primary" plain @click="openExpDialog('work')">添加</el-button>
              </div>
              <div v-if="parsedWorkExp.length === 0" class="profile-empty">暂无经历</div>
              <div v-else class="profile-exp-list">
                <div v-for="(item, idx) in parsedWorkExp" :key="idx" class="profile-exp-card">
                  <div class="profile-exp-card__header">
                    <span class="profile-exp-card__company">{{ item.company }}</span>
                    <span class="profile-exp-card__time">{{ item.startDate }} ~ {{ item.endDate || '至今' }}</span>
                  </div>
                  <div class="profile-exp-card__position">{{ item.position }}</div>
                  <div class="profile-exp-card__desc" v-if="item.description">{{ item.description }}</div>
                  <div class="profile-exp-card__actions">
                    <el-button size="small" text type="primary" @click="editExp('work', idx)">编辑</el-button>
                    <el-button size="small" text type="danger" @click="removeExp('work', idx)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 项目经历 -->
            <div class="profile-section">
              <div class="profile-section__header">
                <h3 class="profile-section__title">项目经历</h3>
                <el-button size="small" type="primary" plain @click="openExpDialog('project')">添加</el-button>
              </div>
              <div v-if="parsedProjectExp.length === 0" class="profile-empty">暂无经历</div>
              <div v-else class="profile-exp-list">
                <div v-for="(item, idx) in parsedProjectExp" :key="idx" class="profile-exp-card">
                  <div class="profile-exp-card__header">
                    <span class="profile-exp-card__company">{{ item.name }}</span>
                    <span class="profile-exp-card__time">{{ item.startDate }} ~ {{ item.endDate || '至今' }}</span>
                  </div>
                  <div class="profile-exp-card__position" v-if="item.role">{{ item.role }}</div>
                  <div class="profile-exp-card__desc" v-if="item.description">{{ item.description }}</div>
                  <div class="profile-exp-card__actions">
                    <el-button size="small" text type="primary" @click="editExp('project', idx)">编辑</el-button>
                    <el-button size="small" text type="danger" @click="removeExp('project', idx)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 教育经历 -->
            <div class="profile-section">
              <div class="profile-section__header">
                <h3 class="profile-section__title">教育经历</h3>
                <el-button size="small" type="primary" plain @click="openExpDialog('education')">添加</el-button>
              </div>
              <div v-if="parsedEduExp.length === 0" class="profile-empty">暂无经历</div>
              <div v-else class="profile-exp-list">
                <div v-for="(item, idx) in parsedEduExp" :key="idx" class="profile-exp-card">
                  <div class="profile-exp-card__header">
                    <span class="profile-exp-card__company">{{ item.school }}</span>
                    <span class="profile-exp-card__time">{{ item.startDate }} ~ {{ item.endDate || '至今' }}</span>
                  </div>
                  <div class="profile-exp-card__position">{{ item.major }} · {{ item.degree }}</div>
                  <div class="profile-exp-card__actions">
                    <el-button size="small" text type="primary" @click="editExp('education', idx)">编辑</el-button>
                    <el-button size="small" text type="danger" @click="removeExp('education', idx)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 资格证书 -->
            <div class="profile-section">
              <div class="profile-section__header">
                <h3 class="profile-section__title">资格证书</h3>
                <el-button size="small" type="primary" plain @click="openExpDialog('certificate')">添加</el-button>
              </div>
              <div v-if="parsedCertificates.length === 0" class="profile-empty">暂无证书</div>
              <div v-else class="profile-exp-list">
                <div v-for="(item, idx) in parsedCertificates" :key="idx" class="profile-exp-card">
                  <div class="profile-exp-card__header">
                    <span class="profile-exp-card__company">{{ item.name }}</span>
                    <span class="profile-exp-card__time">{{ item.obtainDate }}</span>
                  </div>
                  <div class="profile-exp-card__position" v-if="item.issuer">{{ item.issuer }}</div>
                  <div class="profile-exp-card__actions">
                    <el-button size="small" text type="primary" @click="editExp('certificate', idx)">编辑</el-button>
                    <el-button size="small" text type="danger" @click="removeExp('certificate', idx)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 我的简历 -->
          <div v-show="activeTab === 'resumes'" class="content-panel">
            <div class="content-header">
              <h2>我的简历</h2>
              <el-button type="primary" size="small" @click="$router.push('/upload')" :disabled="resumeList.length >= 3">
                上传简历
              </el-button>
            </div>
            <div v-if="resumeList.length === 0" class="empty-state">
              <el-empty description="暂无简历">
                <el-button type="primary" @click="$router.push('/upload')">上传第一份简历</el-button>
              </el-empty>
            </div>
            <div v-else class="resume-list">
              <div v-for="resume in resumeList" :key="resume.id" class="resume-item">
                <div class="resume-item__icon"> </div>
                <div class="resume-item__info">
                  <div class="resume-item__name" @click="showResumeDetail(resume)" style="cursor:pointer;color:#10b981;text-decoration:underline">{{ resume.fileName }}</div>
                  <div class="resume-item__meta">
                    <span v-if="resume.name">{{ resume.name }}</span>
                    <span v-if="resume.education">· {{ resume.education }}</span>
                    <span class="resume-item__time">· 上传于 {{ formatDate(resume.createdAt) }}</span>
                  </div>
                </div>
                <div class="resume-item__actions">
                  <el-button size="small" type="primary" plain @click="startInterview(resume.id)">开始面试</el-button>
                  <el-popconfirm title="确定删除该简历吗？" @confirm="deleteResume(resume.id)">
                    <template #reference>
                      <el-button size="small" type="danger" plain>删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </div>

          <!-- 我的投递 -->
          <div v-show="activeTab === 'applications'" class="content-panel">
            <div class="content-header">
              <h2>我的投递</h2>
            </div>
            <div class="filter-bar">
              <el-radio-group v-model="applicationFilter" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="pending">待处理</el-radio-button>
                <el-radio-button value="screening">筛选中</el-radio-button>
                <el-radio-button value="interview">面试中</el-radio-button>
                <el-radio-button value="offer">已录用</el-radio-button>
                <el-radio-button value="rejected">已拒绝</el-radio-button>
              </el-radio-group>
            </div>
            <div v-if="filteredApplications.length === 0" class="empty-state">
              <el-empty description="暂无投递记录">
                <el-button type="primary" @click="$router.push('/jobs')">去找职位</el-button>
              </el-empty>
            </div>
            <div v-else class="job-list">
              <div v-for="app in filteredApplications" :key="app.id" class="application-card" :class="{ 'is-deleted': app.jobDeleted }" @click="!app.jobDeleted && $router.push(`/job/${app.jobId}`)">
                <div class="application-card__main">
                  <h4 class="application-card__title">
                    {{ app.jobTitle }}
                    <el-tag v-if="app.jobDeleted" type="danger" size="small" effect="plain" style="margin-left: 8px;">已删除</el-tag>
                  </h4>
                  <div class="application-card__company">{{ app.companyName || '-' }}</div>
                  <div class="application-card__meta">
                    <span v-if="app.city">{{ app.city }}</span>
                    <span v-if="app.salary">{{ app.salary }}</span>
                  </div>
                </div>
                <div class="application-card__right">
                  <div class="application-card__status">
                    <el-tag :type="getApplicationStatusType(app.status)" size="small">
                      {{ getApplicationStatusText(app.status) }}
                    </el-tag>
                    <span class="application-card__time">{{ formatDate(app.appliedAt) }}</span>
                  </div>
                  <el-button
                    v-if="app.status === 'pending' && !app.jobDeleted"
                    size="small"
                    type="danger"
                    plain
                    @click.stop="handleWithdraw(app)"
                  >
                    撤回
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 我的收藏 -->
          <div v-show="activeTab === 'favorites'" class="content-panel">
            <div class="content-header">
              <h2>我的收藏</h2>
            </div>
            <div v-if="favoriteList.length === 0" class="empty-state">
              <el-empty description="暂无收藏职位">
                <el-button type="primary" @click="$router.push('/jobs')">去找职位</el-button>
              </el-empty>
            </div>
            <div v-else class="job-list">
              <div v-for="job in favoriteList" :key="job.id" class="application-card" :class="{ 'is-deleted': job.jobDeleted }" @click="!job.jobDeleted && $router.push(`/job/${job.id}`)">
                <div class="application-card__main">
                  <h4 class="application-card__title">
                    {{ job.title }}
                    <el-tag v-if="job.jobDeleted" type="danger" size="small" effect="plain" style="margin-left: 8px;">已删除</el-tag>
                  </h4>
                  <div class="application-card__company">{{ job.companyName || '-' }}</div>
                  <div class="application-card__meta">
                    <span v-if="job.city">{{ job.city }}</span>
                    <span v-if="job.salaryMin || job.salaryMax">{{ formatSalary(job.salaryMin, job.salaryMax) }}</span>
                  </div>
                </div>
                <div class="application-card__status">
                  <span class="application-card__time">{{ formatTime(job.publishedAt) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 我的关注 -->
          <div v-show="activeTab === 'followed'" class="content-panel">
            <div class="content-header">
              <h2>我的关注</h2>
            </div>
            <div v-if="followedList.length === 0" class="empty-state">
              <el-empty description="暂无关注公司">
                <el-button type="primary" @click="$router.push('/companies')">去找公司</el-button>
              </el-empty>
            </div>
            <div v-else class="followed-list">
              <div v-for="company in followedList" :key="company.id" class="followed-card" @click="$router.push(`/company/${company.id}`)">
                <div class="followed-card__logo">
                  <img v-if="company.logo" :src="company.logo" :alt="company.name" />
                  <span v-else>{{ company.name?.charAt(0) }}</span>
                </div>
                <div class="followed-card__info">
                  <h4 class="followed-card__name">{{ company.name }}</h4>
                  <div class="followed-card__meta">
                    <span v-if="company.industry">{{ company.industry }}</span>
                    <span v-if="company.scale">· {{ company.scale }}</span>
                    <span v-if="company.city">· {{ company.city }}</span>
                  </div>
                  <div class="followed-card__tags">
                    <el-tag v-if="company.type" size="small" type="info" effect="plain">{{ company.type }}</el-tag>
                  </div>
                </div>
                <el-button size="small" type="primary" plain @click.stop="$router.push(`/company/${company.id}`)">查看详情</el-button>
              </div>
            </div>
          </div>

          <!-- 面试记录 -->
          <div v-show="activeTab === 'interviews'" class="content-panel">
            <div class="content-header">
              <h2>面试记录</h2>
            </div>
            <div v-if="interviewList.length === 0" class="empty-state">
              <el-empty description="暂无面试记录">
                <el-button v-if="!isHR" type="primary" @click="$router.push('/upload')">开始面试</el-button>
              </el-empty>
            </div>
            <div v-else class="interview-list">
              <div v-for="item in interviewList" :key="item.id" class="interview-card" @click="$router.push(`/report/${item.id}`)">
                <div class="interview-card__info">
                  <h4 class="interview-card__title">{{ item.jobType }}</h4>
                  <div class="interview-card__meta">
                    <el-tag :type="getInterviewStatusType(item.status)" size="small">
                      {{ getInterviewStatusText(item.status) }}
                    </el-tag>
                    <span class="interview-card__score" v-if="item.totalScore">
                      得分: {{ item.totalScore.toFixed(1) }}
                    </span>
                  </div>
                </div>
                <div class="interview-card__right">
                  <span class="interview-card__time">{{ formatDate(item.createdAt) }}</span>
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
            </div>
          </div>

          <!-- AI模拟面试 -->
          <div v-show="activeTab === 'ai-interview'" class="content-panel">
            <div class="content-header">
              <h2>AI模拟面试</h2>
            </div>
            <div class="ai-interview-card">
              <div class="ai-interview-card__icon">🤖</div>
              <div class="ai-interview-card__content">
                <h3>开始AI模拟面试</h3>
                <p>选择简历，AI智能出题，助你面试无忧</p>
              </div>
            </div>
            <div v-if="resumeList.length === 0" class="empty-state">
              <el-empty description="请先上传简历">
                <el-button type="primary" @click="$router.push('/upload')">上传简历</el-button>
              </el-empty>
            </div>
            <div v-else class="ai-resume-select">
              <h4>选择用于面试的简历：</h4>
              <div class="ai-resume-list">
                <div
                  v-for="resume in resumeList"
                  :key="resume.id"
                  class="ai-resume-item"
                  @click="startInterview(resume.id)"
                >
                  <div class="ai-resume-item__icon"> </div>
                  <div class="ai-resume-item__info">
                    <div class="ai-resume-item__name">{{ resume.fileName }}</div>
                    <div class="ai-resume-item__meta">
                      <span v-if="resume.name">{{ resume.name }}</span>
                      <span v-if="resume.education">· {{ resume.education }}</span>
                    </div>
                  </div>
                  <el-button type="primary" size="small">选择</el-button>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>

      <!-- 简历详情弹窗 -->
      <el-dialog v-model="showDetailDialog" title="简历详情" width="70%" :close-on-click-modal="false" style="max-width: 960px;">
        <div v-if="resumeDetail" class="resume-detail">
          <div class="resume-detail__section">
            <h4>基本信息</h4>
            <div class="resume-detail__grid">
              <div class="resume-detail__item"><span class="label">姓名</span><span>{{ resumeDetail.name || '-' }}</span></div>
              <div class="resume-detail__item"><span class="label">文件名</span><span>{{ resumeDetail.fileName }}</span></div>
              <div class="resume-detail__item"><span class="label">教育经历</span><span>{{ resumeDetail.education || '-' }}</span></div>
              <div class="resume-detail__item"><span class="label">上传时间</span><span>{{ formatDate(resumeDetail.createdAt) }}</span></div>
            </div>
          </div>
          <div class="resume-detail__section" v-if="resumeDetail.skills && resumeDetail.skills !== '[]'">
            <h4>技能标签</h4>
            <div class="resume-detail__tags">
              <el-tag v-for="skill in parseSkills(resumeDetail.skills)" :key="skill" size="small" type="success">{{ skill }}</el-tag>
            </div>
          </div>
          <div class="resume-detail__section" v-if="resumeDetail.workExperience">
            <h4>工作经历</h4>
            <div class="resume-detail__text">{{ resumeDetail.workExperience }}</div>
          </div>
          <div class="resume-detail__section" v-if="resumeDetail.projectExperience">
            <h4>项目经历</h4>
            <div class="resume-detail__text">{{ resumeDetail.projectExperience }}</div>
          </div>
          <div class="resume-detail__section">
            <h4>简历内容</h4>
            <div v-if="resumeDetail.fileUrl" class="resume-detail__pdf">
              <iframe :src="resumeDetail.fileUrl + '#zoom=page-width'" class="resume-detail__iframe"></iframe>
            </div>
            <div v-else class="resume-detail__raw">{{ resumeDetail.rawContent || '暂无内容' }}</div>
          </div>
        </div>
      </el-dialog>

      <!-- 编辑个人信息弹窗 -->
      <el-dialog v-model="showEditDialog" title="编辑个人信息" width="480px" :close-on-click-modal="false">
        <el-form :model="editForm" label-width="80px" label-position="left">
          <el-form-item label="昵称">
            <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="editForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="editForm.email" placeholder="请输入邮箱" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="submitEdit" :loading="editLoading">保存</el-button>
        </template>
      </el-dialog>

      <!-- 编辑档案弹窗 -->
      <el-dialog v-model="showProfileEditDialog" title="编辑档案" width="600px" :close-on-click-modal="false">
        <el-tabs v-model="profileEditTab">
          <el-tab-pane label="求职意向" name="intent">
            <el-form :model="profileForm" label-width="90px" label-position="left">
              <el-form-item label="最高学历">
                <el-select v-model="profileForm.education" placeholder="请选择" clearable>
                  <el-option label="高中" value="高中" />
                  <el-option label="大专" value="大专" />
                  <el-option label="本科" value="本科" />
                  <el-option label="硕士" value="硕士" />
                  <el-option label="博士" value="博士" />
                </el-select>
              </el-form-item>
              <el-form-item label="求职状态">
                <el-select v-model="profileForm.jobStatus" placeholder="请选择" clearable>
                  <el-option label="正在找工作" value="looking" />
                  <el-option label="暂不考虑" value="not_looking" />
                  <el-option label="随时到岗" value="available" />
                </el-select>
              </el-form-item>
              <el-form-item label="期望城市">
                <el-input v-model="profileForm.expectCity" placeholder="如：成都、北京" />
              </el-form-item>
              <el-form-item label="期望薪资">
                <div style="display:flex;gap:8px;align-items:center">
                  <el-input-number v-model="profileForm.expectSalaryMin" :min="0" :step="1000" placeholder="最低" controls-position="right" style="width:140px" />
                  <span>~</span>
                  <el-input-number v-model="profileForm.expectSalaryMax" :min="0" :step="1000" placeholder="最高" controls-position="right" style="width:140px" />
                  <span style="color:#94a3b8;font-size:12px">元/月</span>
                </div>
              </el-form-item>
              <el-form-item label="期望职位">
                <el-input v-model="profileForm.expectJobType" placeholder="如：前端开发、Java工程师" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="个人优势" name="summary">
            <el-input v-model="profileForm.personalSummary" type="textarea" :rows="8" placeholder="描述你的个人优势、专业技能、职业目标等..." />
          </el-tab-pane>
        </el-tabs>
        <template #footer>
          <el-button @click="showProfileEditDialog = false">取消</el-button>
          <el-button type="primary" @click="submitProfileEdit" :loading="profileEditLoading">保存</el-button>
        </template>
      </el-dialog>

      <!-- 经历编辑弹窗 -->
      <el-dialog v-model="showExpDialog" :title="expDialogTitle" width="560px" :close-on-click-modal="false">
        <!-- 工作经历 -->
        <el-form v-if="expType === 'work'" :model="expForm" label-width="80px" label-position="left">
          <el-form-item label="公司名称">
            <el-input v-model="expForm.company" placeholder="请输入公司名称" />
          </el-form-item>
          <el-form-item label="职位">
            <el-input v-model="expForm.position" placeholder="请输入职位" />
          </el-form-item>
          <el-form-item label="起止时间">
            <div style="display:flex;gap:8px;align-items:center">
              <el-date-picker v-model="expForm.startDate" type="month" placeholder="开始时间" value-format="YYYY-MM" style="width:150px" />
              <span>~</span>
              <el-date-picker v-model="expForm.endDate" type="month" placeholder="至今" value-format="YYYY-MM" style="width:150px" />
            </div>
          </el-form-item>
          <el-form-item label="工作描述">
            <el-input v-model="expForm.description" type="textarea" :rows="4" placeholder="描述工作内容和成果..." />
          </el-form-item>
        </el-form>

        <!-- 项目经历 -->
        <el-form v-else-if="expType === 'project'" :model="expForm" label-width="80px" label-position="left">
          <el-form-item label="项目名称">
            <el-input v-model="expForm.name" placeholder="请输入项目名称" />
          </el-form-item>
          <el-form-item label="担任角色">
            <el-input v-model="expForm.role" placeholder="如：前端开发、项目负责人" />
          </el-form-item>
          <el-form-item label="起止时间">
            <div style="display:flex;gap:8px;align-items:center">
              <el-date-picker v-model="expForm.startDate" type="month" placeholder="开始时间" value-format="YYYY-MM" style="width:150px" />
              <span>~</span>
              <el-date-picker v-model="expForm.endDate" type="month" placeholder="至今" value-format="YYYY-MM" style="width:150px" />
            </div>
          </el-form-item>
          <el-form-item label="项目描述">
            <el-input v-model="expForm.description" type="textarea" :rows="4" placeholder="描述项目内容和成果..." />
          </el-form-item>
        </el-form>

        <!-- 教育经历 -->
        <el-form v-else-if="expType === 'education'" :model="expForm" label-width="80px" label-position="left">
          <el-form-item label="学校名称">
            <el-input v-model="expForm.school" placeholder="请输入学校名称" />
          </el-form-item>
          <el-form-item label="专业">
            <el-input v-model="expForm.major" placeholder="请输入专业" />
          </el-form-item>
          <el-form-item label="学历">
            <el-select v-model="expForm.degree" placeholder="请选择">
              <el-option label="高中" value="高中" />
              <el-option label="大专" value="大专" />
              <el-option label="本科" value="本科" />
              <el-option label="硕士" value="硕士" />
              <el-option label="博士" value="博士" />
            </el-select>
          </el-form-item>
          <el-form-item label="起止时间">
            <div style="display:flex;gap:8px;align-items:center">
              <el-date-picker v-model="expForm.startDate" type="month" placeholder="开始时间" value-format="YYYY-MM" style="width:150px" />
              <span>~</span>
              <el-date-picker v-model="expForm.endDate" type="month" placeholder="至今" value-format="YYYY-MM" style="width:150px" />
            </div>
          </el-form-item>
        </el-form>

        <!-- 资格证书 -->
        <el-form v-else-if="expType === 'certificate'" :model="expForm" label-width="80px" label-position="left">
          <el-form-item label="证书名称">
            <el-input v-model="expForm.name" placeholder="如：CPA、PMP、软考" />
          </el-form-item>
          <el-form-item label="颁发机构">
            <el-input v-model="expForm.issuer" placeholder="如：中国注册会计师协会" />
          </el-form-item>
          <el-form-item label="获得时间">
            <el-date-picker v-model="expForm.obtainDate" type="month" placeholder="请选择" value-format="YYYY-MM" style="width:200px" />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="showExpDialog = false">取消</el-button>
          <el-button type="primary" @click="submitExp">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Document, Star, Timer, MagicStick, Upload, User } from '@element-plus/icons-vue'
import axios from 'axios'
import AppShell from '@/components/AppShell.vue'
import { getMyFavorites } from '@/api/job'
import { getMyApplications, withdrawApplication } from '@/api/application'
import { getUserFollowedCompanies } from '@/api/company'
import { useUserStore } from '@/stores/user'
import { getUserProfile, saveUserProfile } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const { user, isLoggedIn } = userStore

const isHR = computed(() => user.value?.roleType === 2)
const activeTab = ref('profile')
const applicationFilter = ref('all')
const showEditDialog = ref(false)

const menuItems = computed(() => {
  const items = [
    { key: 'profile', label: '我的档案', icon: User },
    { key: 'resumes', label: '我的简历', icon: Document },
    { key: 'applications', label: '我的投递', icon: Document },
    { key: 'favorites', label: '我的收藏', icon: Star },
    { key: 'followed', label: '我的关注', icon: Star },
    { key: 'interviews', label: '面试记录', icon: Timer },
    { key: 'ai-interview', label: 'AI模拟面试', icon: MagicStick }
  ]
  return items
})

const resumeList = ref([])
const interviewList = ref([])
const applicationList = ref([])
const favoriteList = ref([])
const followedList = ref([])
const showDetailDialog = ref(false)
const resumeDetail = ref(null)
const editForm = ref({ nickname: '', phone: '', email: '' })
const editLoading = ref(false)

// ========== 用户档案 ==========
const profile = ref({})
const showProfileEditDialog = ref(false)
const profileEditTab = ref('intent')
const profileEditLoading = ref(false)
const profileForm = ref({
  education: '',
  personalSummary: '',
  jobStatus: 'looking',
  expectCity: '',
  expectSalaryMin: null,
  expectSalaryMax: null,
  expectJobType: ''
})

// 经历编辑相关
const showExpDialog = ref(false)
const expType = ref('work') // work/project/education/certificate
const expEditIndex = ref(-1) // -1 表示新增
const expForm = ref({})

const expDialogTitle = computed(() => {
  const map = { work: '工作/实习经历', project: '项目经历', education: '教育经历', certificate: '资格证书' }
  const action = expEditIndex.value >= 0 ? '编辑' : '添加'
  return action + map[expType.value]
})

// 经历数据（独立 ref，避免 JSON.parse 重复创建）
const workExpList = ref([])
const projectExpList = ref([])
const eduExpList = ref([])
const certificateList = ref([])

const parsedWorkExp = computed(() => workExpList.value)
const parsedProjectExp = computed(() => projectExpList.value)
const parsedEduExp = computed(() => eduExpList.value)
const parsedCertificates = computed(() => certificateList.value)

function parseJsonArray(str) {
  if (!str) return []
  try {
    const arr = JSON.parse(str)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

// 将经历列表同步回 profile 并保存
async function syncExpToProfile() {
  const data = { ...profile.value }
  data.workExperience = JSON.stringify(workExpList.value)
  data.projectExperience = JSON.stringify(projectExpList.value)
  data.educationExperience = JSON.stringify(eduExpList.value)
  data.certificates = JSON.stringify(certificateList.value)
  await saveUserProfile(user.value.id, data)
  profile.value = data
}

const jobStatusText = (status) => {
  const map = { looking: '正在找工作', not_looking: '暂不考虑', available: '随时到岗' }
  return map[status] || '未填写'
}

const formatExpectSalary = (min, max) => {
  if (!min && !max) return '未填写'
  if (min && max) return `${(min / 1000).toFixed(0)}K-${(max / 1000).toFixed(0)}K`
  if (min) return `${(min / 1000).toFixed(0)}K起`
  return `最高${(max / 1000).toFixed(0)}K`
}

// 加载档案
const loadProfile = async () => {
  if (!user.value?.id) return
  try {
    const res = await getUserProfile(user.value.id)
    if (res?.data) {
      profile.value = res.data
      // 解析 JSON 经历字段到独立 ref
      workExpList.value = parseJsonArray(res.data.workExperience)
      projectExpList.value = parseJsonArray(res.data.projectExperience)
      eduExpList.value = parseJsonArray(res.data.educationExperience)
      certificateList.value = parseJsonArray(res.data.certificates)
    }
  } catch { /* 首次可能无档案 */ }
}

// 打开档案编辑弹窗
const openProfileEditDialog = () => {
  profileForm.value = {
    education: profile.value.education || '',
    personalSummary: profile.value.personalSummary || '',
    jobStatus: profile.value.jobStatus || 'looking',
    expectCity: profile.value.expectCity || '',
    expectSalaryMin: profile.value.expectSalaryMin || null,
    expectSalaryMax: profile.value.expectSalaryMax || null,
    expectJobType: profile.value.expectJobType || ''
  }
  profileEditTab.value = 'intent'
  showProfileEditDialog.value = true
}

// 提交档案编辑
const submitProfileEdit = async () => {
  profileEditLoading.value = true
  try {
    await saveUserProfile(user.value.id, profileForm.value)
    Object.assign(profile.value, profileForm.value)
    showProfileEditDialog.value = false
    ElMessage.success('档案已保存')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    profileEditLoading.value = false
  }
}

// 打开经历编辑弹窗
const openExpDialog = (type) => {
  expType.value = type
  expEditIndex.value = -1
  expForm.value = getEmptyExp(type)
  showExpDialog.value = true
}

// 编辑经历
const editExp = (type, idx) => {
  expType.value = type
  expEditIndex.value = idx
  const list = getExpListRef(type)
  expForm.value = { ...list.value[idx] }
  showExpDialog.value = true
}

// 删除经历
const removeExp = async (type, idx) => {
  try {
    await ElMessageBox.confirm('确定删除该条记录吗？', '删除确认', { type: 'warning' })
    const list = getExpListRef(type)
    list.value.splice(idx, 1)
    await syncExpToProfile()
    ElMessage.success('已删除')
  } catch { /* cancel */ }
}

// 提交经历编辑
const submitExp = async () => {
  const list = getExpListRef(expType.value)
  if (expEditIndex.value >= 0) {
    list.value[expEditIndex.value] = { ...expForm.value }
  } else {
    list.value.push({ ...expForm.value })
  }
  await syncExpToProfile()
  showExpDialog.value = false
  ElMessage.success('已保存')
}

// 根据类型获取对应 ref
function getExpListRef(type) {
  const map = { work: workExpList, project: projectExpList, education: eduExpList, certificate: certificateList }
  return map[type]
}

// 空经历模板
function getEmptyExp(type) {
  if (type === 'work') return { company: '', position: '', startDate: '', endDate: '', description: '' }
  if (type === 'project') return { name: '', role: '', startDate: '', endDate: '', description: '' }
  if (type === 'education') return { school: '', major: '', degree: '', startDate: '', endDate: '' }
  if (type === 'certificate') return { name: '', issuer: '', obtainDate: '' }
  return {}
}

const showResumeDetail = (resume) => {
  resumeDetail.value = resume
  showDetailDialog.value = true
}

const openEditDialog = () => {
  editForm.value = {
    nickname: user.value?.nickname || '',
    phone: user.value?.phone || '',
    email: user.value?.email || ''
  }
  showEditDialog.value = true
}

const submitEdit = async () => {
  editLoading.value = true
  try {
    await axios.put(`/api/user/${user.value.id}`, editForm.value)
    // 更新本地用户数据
    user.value.nickname = editForm.value.nickname
    user.value.phone = editForm.value.phone
    user.value.email = editForm.value.email
    showEditDialog.value = false
    ElMessage.success('个人信息已更新')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '更新失败')
  } finally {
    editLoading.value = false
  }
}

const parseSkills = (skills) => {
  try {
    const arr = JSON.parse(skills)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

const filteredApplications = computed(() => {
  if (applicationFilter.value === 'all') return applicationList.value
  return applicationList.value.filter(app => app.status === applicationFilter.value)
})

onMounted(async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await loadData()
})

const loadData = async () => {
  try {
    // 加载用户档案
    await loadProfile()

    // 加载简历列表（仅求职者）
    if (!isHR.value) {
      try {
        const resumeRes = await axios.get(`/api/user/${user.value.id}/resumes`)
        if (resumeRes.data.success) {
          const resumeIds = resumeRes.data.data
          const resumes = []
          for (const id of resumeIds) {
            const res = await axios.get(`/api/resume/${id}`)
            if (res.data.success) resumes.push(res.data.data)
          }
          resumeList.value = resumes
        }
      } catch (e) {
        // 接口可能还未实现
      }

      // 加载投递记录
      try {
        const appRes = await getMyApplications()
        if (appRes?.data) {
          applicationList.value = appRes.data || []
        }
      } catch (e) {
        // 接口可能还未实现
      }

      // 加载收藏
      try {
        const favRes = await getMyFavorites()
        if (favRes.success) {
          favoriteList.value = favRes.data || []
        }
      } catch (e) {
        // 接口可能还未实现
      }

      // 加载关注的公司
      try {
        const followRes = await getUserFollowedCompanies()
        if (followRes?.data) {
          followedList.value = followRes.data || []
        }
      } catch (e) {
        // 接口可能还未实现
      }
    }

    // 加载面试记录
    try {
      const interviewRes = await axios.get(`/api/interview/user/${user.value.id}`)
      if (interviewRes.data.success) {
        interviewList.value = interviewRes.data.data || []
      }
    } catch (e) {
      // 接口可能还未实现
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

const startInterview = (resumeId) => {
  router.push(`/upload?resumeId=${resumeId}`)
}

const deleteResume = async (resumeId) => {
  try {
    await axios.delete(`/api/user/${user.value.id}/resumes/${resumeId}`)
    ElMessage.success('简历已删除')
    await loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const handleWithdraw = async (app) => {
  try {
    await ElMessageBox.confirm('确定要撤回该投递吗？撤回后将无法恢复。', '撤回投递', {
      confirmButtonText: '确定撤回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await withdrawApplication(app.id)
    ElMessage.success('已撤回投递')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '撤回失败')
    }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const formatSalary = (min, max) => {
  if (min && max) return `${(min / 1000).toFixed(0)}K-${(max / 1000).toFixed(0)}K`
  if (min) return `${(min / 1000).toFixed(0)}K起`
  if (max) return `最高${(max / 1000).toFixed(0)}K`
  return '面议'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return `${Math.floor(days / 30)}月前`
}

const getInterviewStatusType = (status) => {
  const map = { PENDING: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success' }
  return map[status] || 'info'
}

const getInterviewStatusText = (status) => {
  const map = { PENDING: '待开始', IN_PROGRESS: '进行中', COMPLETED: '已完成' }
  return map[status] || status
}

const getApplicationStatusType = (status) => {
  const map = {
    pending: 'info',
    screening: 'warning',
    interview: '',
    offer: 'success',
    rejected: 'danger',
    withdrawn: 'info'
  }
  return map[status] || 'info'
}

const getApplicationStatusText = (status) => {
  const map = {
    pending: '待处理',
    screening: '筛选中',
    interview: '面试中',
    offer: '已录用',
    rejected: '已拒绝',
    withdrawn: '已撤回'
  }
  return map[status] || status
}
</script>

<style scoped>
.user-center {
  min-height: calc(100vh - 64px);
  background: #f5f6f8;
  padding: 32px 40px;
}

.user-center__container {
  display: flex;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 左侧边栏 */
.sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 用户信息卡片 */
.user-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px 24px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.user-card__avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, #10b981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.user-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-card__avatar span {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
}

.user-card__name {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 4px;
}

.user-card__id {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 20px;
}

.user-card__info {
  text-align: left;
  padding: 16px;
  background: #f8fafc;
  border-radius: 10px;
  margin-bottom: 16px;
}

.user-card__item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 13px;
}

.user-card__item + .user-card__item {
  border-top: 1px solid #e5e7eb;
}

.user-card__label {
  color: #64748b;
}

.user-card__value {
  color: #334155;
  font-weight: 500;
}

.user-card__edit {
  font-size: 13px;
}

/* 菜单导航 */
.sidebar-menu {
  background: #fff;
  border-radius: 16px;
  padding: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.sidebar-menu__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #475569;
}

.sidebar-menu__item:hover {
  background: #f1f5f9;
  color: #10b981;
}

.sidebar-menu__item.active {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  color: #059669;
  font-weight: 600;
}

.sidebar-menu__item .el-icon {
  font-size: 18px;
}

/* 右侧内容区 */
.content {
  flex: 1;
  min-width: 0;
}

.content-panel {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.content-header {
  padding: 24px 28px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

/* 筛选栏 */
.filter-bar {
  padding: 16px 28px;
  border-bottom: 1px solid #f1f5f9;
  background: #fafbfc;
}

/* 简历列表 */
.resume-list {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resume-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: #fafbfc;
  border-radius: 12px;
  transition: all 0.2s;
}

.resume-item:hover {
  background: #f0fdf4;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.1);
}

.resume-item__icon {
  font-size: 36px;
}

.resume-item__info {
  flex: 1;
}

.resume-item__name {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 4px;
}

.resume-item__meta {
  font-size: 13px;
  color: #64748b;
}

.resume-item__time {
  color: #94a3b8;
}

.resume-item__actions {
  display: flex;
  gap: 10px;
}

/* AI面试简历选择 */
.ai-resume-select {
  padding: 24px 28px;
}

.ai-resume-select h4 {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 16px;
}

.ai-resume-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-resume-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: #fafbfc;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.ai-resume-item:hover {
  background: #f0fdf4;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.1);
}

.ai-resume-item__icon {
  font-size: 32px;
}

.ai-resume-item__info {
  flex: 1;
}

.ai-resume-item__name {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 4px;
}

.ai-resume-item__meta {
  font-size: 13px;
  color: #64748b;
}

/* 投递卡片列表 */
.job-list {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.application-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  background: #fafbfc;
  border-radius: 12px;
  transition: all 0.2s;
  cursor: pointer;
}

.application-card:hover {
  background: #f0fdf4;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.1);
}

.application-card.is-deleted {
  opacity: 0.6;
  background: #fafafa;
  cursor: default;
}

.application-card.is-deleted:hover {
  background: #fafafa;
  box-shadow: none;
}

.application-card.is-deleted .application-card__title {
  color: #999;
}

.application-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 6px;
}

.application-card__company {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.application-card__meta {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  gap: 12px;
}

.application-card__status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.application-card__right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.application-card__time {
  font-size: 12px;
  color: #94a3b8;
}

/* 关注公司卡片 */
.followed-list {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.followed-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: #fafbfc;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.followed-card:hover {
  background: #f0fdf4;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.1);
}

.followed-card__logo {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.followed-card__logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.followed-card__logo span {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.followed-card__info {
  flex: 1;
  min-width: 0;
}

.followed-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 6px;
}

.followed-card__meta {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
}

.followed-card__tags {
  display: flex;
  gap: 6px;
}

/* 面试卡片 */
.interview-list {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.interview-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  background: #fafbfc;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.interview-card:hover {
  background: #f0fdf4;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.1);
}

.interview-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 8px;
}

.interview-card__meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.interview-card__score {
  font-size: 14px;
  font-weight: 600;
  color: #10b981;
}

.interview-card__right {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
}

.interview-card__time {
  font-size: 13px;
}

/* AI面试卡片 */
.ai-interview-card {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 32px 28px;
  margin: 20px 28px;
  background: url('@/assets/image/background3.webp') no-repeat center center;
  background-size: cover;
  border-radius: 14px;
  color: #fff;
  position: relative;
}

.ai-interview-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 14px;
}

.ai-interview-card__icon {
  font-size: 64px;
  position: relative;
  z-index: 1;
}

.ai-interview-card__content {
  position: relative;
  z-index: 1;
}

.ai-interview-card__content h3 {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px;
}

.ai-interview-card__content p {
  font-size: 14px;
  margin: 0 0 16px;
  opacity: 0.9;
}

.empty-state {
  padding: 60px 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .user-center {
    padding: 16px;
  }

  .user-center__container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .sidebar-menu {
    display: flex;
    overflow-x: auto;
    padding: 4px;
  }

  .sidebar-menu__item {
    white-space: nowrap;
    padding: 10px 14px;
    font-size: 13px;
  }

  .application-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .application-card__status {
    flex-direction: row;
    align-items: center;
    width: 100%;
    justify-content: space-between;
  }

  .followed-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .followed-card .el-button {
    align-self: flex-end;
  }
}

/* 简历详情弹窗 */
.resume-detail__section {
  margin-bottom: 20px;
}

.resume-detail__section h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f3f4f6;
}

.resume-detail__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.resume-detail__item {
  font-size: 14px;
  color: #374151;
  display: flex;
  gap: 8px;
}

.resume-detail__item .label {
  color: #6b7280;
  min-width: 60px;
  flex-shrink: 0;
}

.resume-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.resume-detail__text {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  white-space: pre-wrap;
}

.resume-detail__raw {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #f3f4f6;
}

.resume-detail__pdf {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.resume-detail__iframe {
  width: 100%;
  height: 680px;
  border: none;
}

/* ========== 我的档案 ========== */
.profile-section {
  padding: 20px 28px;
  border-bottom: 1px solid #f1f5f9;
}

.profile-section:last-child {
  border-bottom: none;
}

.profile-section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-section__title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 12px;
}

.profile-section__header .profile-section__title {
  margin-bottom: 0;
}

.profile-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.profile-field {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.profile-field__label {
  font-size: 13px;
  color: #94a3b8;
  width: 72px;
  flex-shrink: 0;
}

.profile-field__value {
  font-size: 14px;
  color: #0f172a;
  font-weight: 500;
}

.profile-summary {
  font-size: 14px;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.profile-empty {
  font-size: 13px;
  color: #94a3b8;
  padding: 16px 0;
  text-align: center;
}

.profile-exp-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.profile-exp-card {
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 10px;
  border-left: 3px solid #10b981;
}

.profile-exp-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.profile-exp-card__company {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.profile-exp-card__time {
  font-size: 12px;
  color: #94a3b8;
}

.profile-exp-card__position {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
}

.profile-exp-card__desc {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
}

.profile-exp-card__actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}
</style>
