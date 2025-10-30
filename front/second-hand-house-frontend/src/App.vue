<template>
  <div class="app">
    <header class="header">
      <div class="branding">
        <h1>{{ t('header.title') }}</h1>
        <p>{{ t('header.subtitle') }}</p>
      </div>
      <div class="header-actions">
        <InterfaceSettings />
        <div v-if="currentUser" class="session">
          <div class="identity">
            <span>
              {{ t('header.currentRoleLabel') }}
              <strong>{{ roleLabels[currentUser.role] }}</strong>
              {{ parentheses.left }}{{ currentUser.displayName }}{{ parentheses.right }}
            </span>
            <span class="reputation">
              {{ t('header.reputationLabel') }}{{ currentUser.reputationScore ?? '—' }}
            </span>
            <span class="verification-status">
              {{ t('header.verificationLabel') }}
              <strong>{{ currentUser.realNameVerified ? t('header.verified') : t('header.pending') }}</strong>
            </span>
          </div>
          <div class="session-actions">
            <button
              v-if="canUseMessaging"
              type="button"
              class="messages-trigger"
              @click="openConversationPanel"
            >
              {{ t('header.messages') }}
            </button>
            <button type="button" class="logout" @click="handleLogout">{{ t('header.logout') }}</button>
          </div>
        </div>
      </div>
      <p v-if="messages.success" class="success">{{ messages.success }}</p>
    </header>

    <section v-if="!currentUser" class="login-section">
      <RoleLogin :api-base-url="apiBaseUrl" @login-success="handleLoginSuccess" />
    </section>

    <template v-else>
      <div class="workspace-layout">
        <aside class="sidebar">
          <nav class="menu">
            <button
              v-for="tab in navigationTabs"
              :key="tab.value"
              type="button"
              :class="['menu-item', { active: tab.value === activeTab }]"
              @click="switchTab(tab.value)"
            >
              {{ tab.label }}
            </button>
          </nav>
        </aside>

        <section class="workspace">
          <section v-if="messages.error" class="alert">
            <strong>{{ t('alerts.errorPrefix') }}</strong> {{ messages.error }}
          </section>

          <main class="main-content">
        <HouseExplorer
          v-if="activeTab === 'home'"
          :houses="houses"
          :loading="loading"
          :current-user="currentUser"
          :can-view-sensitive-info="canViewSensitiveInfo"
          :filters="houseFilters"
          :recommendations="recommendations"
          :purchase-loading="ordersLoading"
          :reservation-loading="reservationLoading"
          :reservation-target="reservationTarget"
          :api-base-url="apiBaseUrl"
          :wallet="wallet"
          :consume-points="consumePredictionPoints"
          @search="handleFilterSearch"
          @reserve="handleReserve"
          @purchase="handlePurchase"
          @contact-seller="handleContactSeller"
        />

        <AIAssistant
          v-else-if="activeTab === 'assistant'"
          :api-base-url="apiBaseUrl"
        />

        <div v-else-if="activeTab === 'manage'" class="manage-grid">
          <HouseForm
            :initial-house="selectedHouse"
            :loading="loading"
            :can-manage="canManageHouses"
            :current-user="currentUser"
            :reset-key="formResetKey"
            :api-base-url="apiBaseUrl"
            @submit="handleSubmit"
            @cancel="handleCancel"
          />
          <HouseList
            :houses="houses"
            :loading="loading"
            :can-manage="canManageHouses"
            :current-user="currentUser"
            :can-view-sensitive-info="canViewSensitiveInfo"
            :orders-loading="ordersLoading"
            @edit="handleEdit"
            @remove="handleRemove"
            @review="handleReview"
            @purchase="handlePurchase"
            @contact-seller="handleContactSeller"
          />
        </div>

        <HouseReviews
          v-else-if="activeTab === 'feedback'"
          :houses="houses"
          :reviews="houseReviews"
          :current-user="currentUser"
          @submit="handleSubmitHouseReview"
        />

        <AdminHouseReview
          v-else-if="activeTab === 'review'"
          :houses="pendingReviewHouses"
          :loading="reviewLoading"
          :allow-delete="isAdmin"
          @refresh="fetchHouses({ silent: false })"
          @review="handleReview"
          @delete-house="handleAdminDeleteRequest"
        />

        <AdminHouseManager
          v-else-if="activeTab === 'admin-houses'"
          :houses="houses"
          :loading="loading || adminLoading"
          :status-labels="listingStatusLabels"
          @refresh="fetchHouses({ silent: false })"
          @unlist="handleAdminUnlist"
          @delete="handleAdminDeleteRequest"
        />

        <UrgentTasks
          v-else-if="activeTab === 'urgent'"
          :tasks="urgentTasks"
          :progress-labels="orderProgressLabels"
          @mark-read="handleUrgentTaskRead"
          @navigate="handleUrgentTaskNavigate"
        />

        <div v-else-if="activeTab === 'orders'" class="orders-grid">
          <WalletPanel
            :wallet="wallet"
            :loading="walletLoading"
            :current-user="currentUser"
            @top-up="handleTopUp"
          />
          <OrderHistory
            :orders="orders"
            :loading="ordersLoading"
            :current-user="currentUser"
            :can-view-sensitive-info="canViewSensitiveInfo"
            :progress-labels="orderProgressLabels"
            :progress-order="orderProgressSequence"
            @request-return="handleRequestReturn"
            @schedule-viewing="handleScheduleViewing"
            @advance-progress="handleAdvanceProgress"
            @confirm-viewing="handleViewingConfirmation"
          />
        </div>

        <AccountCenter
          v-else-if="activeTab === 'account'"
          inline
          :visible="true"
          :current-user="currentUser"
          :wallet="wallet"
          :role-label="roleLabels[currentUser?.role]"
          :saving="accountSaving"
          :error="accountError"
          :api-base-url="apiBaseUrl"
          @submit="handleAccountSubmit"
          @verified="handleVerificationUpdate"
        />

        <div v-else-if="activeTab === 'admin'" class="admin-panels">
          <AdminOrderReview
            :orders="adminPendingOrders"
            :loading="adminOrdersLoading"
            @refresh="loadAdminOrders"
            @release="handleAdminOrderRelease"
          />
          <ReviewModeration
            :reviews="pendingHouseReviews"
            :moderating="reviewLoading"
            @moderate="handleModerateHouseReview"
          />
        </div>

        <div v-else-if="activeTab === 'reputation'" class="reputation-panel">
          <AdminReputationBoard
            :loading="adminLoading"
            :overview="adminReputation"
            :users="adminUsers"
            :current-user="currentUser"
            @refresh="loadAdminData"
            @toggle-blacklist="handleToggleBlacklist"
            @delete-user="handleDeleteUser"
          />
        </div>
          </main>
        </section>
      </div>
    </template>

    <ConversationPanel
      :visible="conversationPanelVisible"
      :conversations="conversations"
      :active-conversation-id="activeConversationId"
      :messages="conversationMessages"
      :loading-conversations="conversationListLoading"
      :loading-messages="conversationMessagesLoading"
      :sending-message="conversationSending"
      :current-user="currentUser"
      :error="conversationError"
      :prefill="conversationPrefill"
      @close="closeConversationPanel"
      @refresh-conversations="loadConversations()"
      @select-conversation="handleSelectConversation"
      @send-message="handleSendConversationMessage"
      @prefill-consumed="handleConversationPrefillConsumed"
    />

    <footer class="footer">
      <small>{{ t('footer.apiBaseLabel') }}{{ apiBaseUrl }}</small>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, provide, reactive, ref, watch } from 'vue';
import axios from 'axios';
import HouseForm from './components/HouseForm.vue';
import HouseList from './components/HouseList.vue';
import RoleLogin from './components/RoleLogin.vue';
import WalletPanel from './components/WalletPanel.vue';
import OrderHistory from './components/OrderHistory.vue';
import HouseExplorer from './components/HouseExplorer.vue';
import HouseReviews from './components/HouseReviews.vue';
import AdminHouseReview from './components/AdminHouseReview.vue';
import AdminReputationBoard from './components/AdminReputationBoard.vue';
import AdminOrderReview from './components/AdminOrderReview.vue';
import AdminHouseManager from './components/AdminHouseManager.vue';
import ConversationPanel from './components/ConversationPanel.vue';
import InterfaceSettings from './components/InterfaceSettings.vue';
import UrgentTasks from './components/UrgentTasks.vue';
import ReviewModeration from './components/ReviewModeration.vue';
import AccountCenter from './components/AccountCenter.vue';
import AIAssistant from './components/AIAssistant.vue';
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';
const houses = ref([]);
const loading = ref(false);
const selectedHouse = ref(null);
const formResetKey = ref(0);
const currentUser = ref(null);
const wallet = ref(null);
const walletLoading = ref(false);
const orders = ref([]);
const ordersLoading = ref(false);
const reservationLoading = ref(false);
const reservationTarget = ref(null);
const dismissedUrgentTaskKeys = ref([]);
const messages = reactive({ error: '', success: '' });
const accountSaving = ref(false);
const accountError = ref('');
const houseReviews = ref([]);
const recommendations = reactive({ sellers: [], buyers: [] });
const adminUsers = ref([]);
const adminReputation = ref(null);
const adminLoading = ref(false);
const adminPendingOrders = ref([]);
const adminOrdersLoading = ref(false);
const conversationPanelVisible = ref(false);
const conversationPrefill = ref('');
const conversations = ref([]);
const activeConversationId = ref(null);
const conversationMessages = ref([]);
const conversationListLoading = ref(false);
const conversationMessagesLoading = ref(false);
const conversationSending = ref(false);
const conversationError = ref('');
const activeTab = ref('home');
const houseFilters = reactive({
  keyword: '',
  minPrice: '',
  maxPrice: '',
  minArea: '',
  maxArea: '',
  sellerUsername: '',
  sellerDisplayName: ''
});
const storageKey = 'secondhand-house-current-user';
const urgentDismissedStoragePrefix = 'shh-urgent-dismissed-';
const reviewStorageKey = 'shh-house-reviews-v1';

const client = axios.create({
  baseURL: apiBaseUrl,
  headers: { 'Content-Type': 'application/json' }
});

const coerceNumber = (value, fallback = 0) => {
  const num = Number(value);
  if (!Number.isFinite(num)) {
    const fallbackNum = Number(fallback);
    return Number.isFinite(fallbackNum) ? fallbackNum : 0;
  }
  return num;
};

const normalizeWallet = (next, previous = null) => {
  const prev = previous && typeof previous === 'object' ? previous : {};
  const source = next && typeof next === 'object' ? next : {};
  const normalizedBalance = coerceNumber(source.balance, prev.balance ?? 0);
  const normalizedPoints = Math.max(0, coerceNumber(source.points, prev.points ?? 0));
  const transactions = Array.isArray(source.transactions)
    ? source.transactions
    : Array.isArray(prev.transactions)
      ? prev.transactions
      : [];
  return {
    ...prev,
    ...source,
    balance: normalizedBalance,
    points: normalizedPoints,
    transactions,
    virtualPort: source.virtualPort ?? prev.virtualPort ?? ''
  };
};

const extractErrorMessage = (error) => {
  const detail = error?.response?.data;
  if (!detail) {
    return '';
  }
  if (typeof detail === 'string') {
    return detail;
  }
  if (typeof detail?.detail === 'string') {
    return detail.detail;
  }
  if (typeof detail?.message === 'string') {
    return detail.message;
  }
  return '';
};

const shouldRetryWithoutRequester = (error) => {
  const status = error?.response?.status;
  if (!status) {
    return false;
  }
  if ([404, 405, 415].includes(status)) {
    return true;
  }
  if (status === 400) {
    if (error?.response?.data?.errors) {
      return false;
    }
    const message = extractErrorMessage(error);
    return message ? /requester/i.test(message) : false;
  }
  return false;
};

const shouldRetryProfileFetch = (error) => {
  const status = error?.response?.status;
  return status === 404 || status === 405;
};

const settingsStorageKey = 'shh-interface-settings';
const defaultSettings = Object.freeze({
  fontSize: 'medium',
  language: 'zh',
  theme: 'dark'
});

const loadStoredSettings = () => {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    const raw = window.localStorage.getItem(settingsStorageKey);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed === 'object') {
      return parsed;
    }
  } catch (error) {
    console.warn('Failed to restore interface settings:', error);
  }
  return null;
};

const storedSettings = loadStoredSettings();

const settings = reactive({
  fontSize: storedSettings?.fontSize ?? defaultSettings.fontSize,
  language: storedSettings?.language ?? defaultSettings.language,
  theme: storedSettings?.theme ?? defaultSettings.theme
});

provide('appSettings', settings);

const translations = {
  zh: {
    header: {
      title: '二手房屋售卖系统',
      subtitle: '支持线上预定、购房支付与信誉评估的综合平台。',
      currentRoleLabel: '当前角色：',
      reputationLabel: '信誉分：',
      verificationLabel: '实名认证：',
      verified: '已完成',
      pending: '待完成',
      messages: '消息中心',
      logout: '退出登录'
    },
    nav: {
      home: '购买首页',
      aiAssistant: 'AI 智能助手',
      manage: '房源管理',
      feedback: '房源评价',
      urgent: '紧急待办',
      orders: '订单与钱包',
      account: '个人中心',
      review: '房源审核',
      reviewWithCount: '房源审核（{count}）',
      adminHouses: '管理员房源',
      admin: '管理员面板',
      reputation: '信誉面板'
    },
    footer: {
      apiBaseLabel: '后端接口地址：'
    },
    alerts: {
      errorPrefix: '提示：'
    },
    aiAssistant: {
      title: '智能购房顾问',
      description: '向 AI 咨询二手房购置流程、风险提示与政策信息。',
      modelLabel: '当前模型：{model}',
      refresh: '换一批问题',
      relatedTitle: '相关问题',
      relatedDescription: '直接点击以下问题，快速获得针对二手房交易的建议。',
      inputLabel: '向 AI 提问',
      inputPlaceholder: '请输入关于购置二手房的疑问，例如贷款流程、交易风险等…',
      missingKey: 'AI 服务尚未配置，请联系管理员在后端填写密钥。',
      ask: '提交问题',
      asking: '正在回答…',
      conversationTitle: '对话记录',
      emptyState: '尚未开始对话，尝试向 AI 提一个问题吧。',
      questionLabel: '我的提问',
      answerLabel: 'AI 的建议',
      pendingAnswer: '正在生成回答…',
      genericError: '暂时无法获取 AI 回答，请稍后再试。',
      fallbackAnswer: '抱歉，未能生成有效的回答，请换个问题试试。'
    },
    conversation: {
      title: '消息中心',
      subtitle: '实时与卖家沟通，确认房源细节与交易安排。',
      actions: {
        refresh: '刷新列表',
        refreshing: '刷新中…',
        close: '关闭',
        back: '返回列表',
        send: '发送',
        sending: '发送中…'
      },
      list: {
        title: '对话列表',
        count: '{count}',
        loading: '正在加载对话…',
        empty: '暂无对话。'
      },
      preview: {
        you: '我',
        partner: '对方',
        empty: '暂无消息'
      },
      empty: {
        title: '请选择一个对话',
        description: '在左侧选择卖家以查看历史消息并发送新消息。'
      },
      chat: {
        subtitle: '与卖家协调看房、付款等具体事项。',
        loading: '正在加载消息…',
        none: '暂无消息，向卖家发起第一条交流吧。',
        placeholder: '请输入消息（最多2000字）',
        headerFallback: '对话详情'
      },
      participant: {
        seller: '卖家',
        buyer: '买家'
      }
    },
    roles: {
      buyer: '买家',
      seller: '卖家',
      admin: '系统管理员'
    },
    statuses: {
      draft: '草稿',
      pending: '待审核',
      approved: '已通过',
      rejected: '已驳回',
      sold: '已售出（已下架）'
    },
    adminReview: {
      title: '房源审核工作台',
      subtitle: '共有 {count} 套房源待审核，请尽快处理。',
      subtitleSingle: '当前有 1 套房源待审核，请尽快处理。',
      refresh: '刷新列表',
      loading: '正在加载待审核房源…',
      emptyTitle: '暂无待审核房源',
      emptyDescription: '卖家提交的房源将在此处等待审核。',
      noImage: '暂无封面图',
      badgePending: '待审核',
      fields: {
        price: '总价',
        downPayment: '首付',
        installment: '分期方案',
        area: '建筑面积',
        listedOn: '上架时间',
        floor: '楼层'
      },
      installmentUnavailable: '未提供分期方案',
      installmentUnknownMonths: '未提供',
      installmentValue: '￥{amount} × {months} 期',
      floorValue: '第 {floor} 层',
      sellerAccount: '卖家账号',
      contactName: '联系人',
      contactPhone: '联系电话',
      actions: {
        approve: '通过',
        reject: '驳回',
        delete: '删除房源'
      }
    },
    adminManage: {
      title: '房源管理工作台',
      subtitle: '当前共 {count} 套房源，可按状态筛选、下架或删除。',
      refresh: '同步房源',
      refreshing: '同步中…',
      loading: '正在加载房源列表…',
      empty: '暂未找到匹配的房源。',
      filters: {
        query: '关键字筛选',
        queryPlaceholder: '输入标题、地址或卖家信息',
        status: '状态筛选',
        statusAll: '全部状态'
      },
      table: {
        listing: '房源',
        status: '状态',
        seller: '卖家/联系方式',
        updated: '最近更新',
        actions: '操作'
      },
      actions: {
        view: '详情',
        unlist: '下架',
        unlistDisabled: '不可下架',
        delete: '删除'
      },
      unknownTitle: '未命名房源',
      unknownAddress: '地址待补充',
      unknownSeller: '未知卖家',
      unknownStatus: '未知状态'
    },
    adminOrders: {
      title: '资金托管审核',
      subtitle: '买家付款后资金进入托管账户，需管理员审核后才能发放。',
      refresh: '刷新列表',
      loading: '正在加载托管订单…',
      empty: '暂无待审核的托管订单。',
      columns: {
        id: '订单编号',
        listing: '房源信息',
        buyer: '买家',
        seller: '卖家',
        escrow: '托管资金',
        payment: '支付方式',
        createdAt: '创建时间',
        actions: '操作'
      },
      escrowBalance: '托管：￥{amount}',
      platformFee: '手续费：￥{amount}',
      payment: {
        full: '全款支付',
        installment: '分期支付'
      },
      actions: {
        releaseSeller: '放款给卖家',
        refundBuyer: '退款给买家'
      }
    },
    adminReputation: {
      title: '信誉数据面板',
      subtitle: '系统会根据交易行为动态调整买卖双方信誉分，管理员可以拉黑或恢复账号。',
      refresh: '刷新数据',
      loading: '正在加载信誉数据…',
      summary: {
        blacklisted: '黑名单账号',
        sellers: '活跃卖家',
        buyers: '活跃买家'
      },
      tables: {
        sellers: {
          title: '卖家榜单',
          columns: {
            account: '卖家',
            reputation: '信誉分',
            breaches: '违约次数'
          },
          empty: '暂无卖家数据。'
        },
        buyers: {
          title: '买家榜单',
          columns: {
            account: '买家',
            reputation: '信誉分',
            returns: '退单次数'
          },
          empty: '暂无买家数据。'
        }
      },
      blacklist: {
        title: '账号黑名单管理',
        columns: {
          account: '账号',
          role: '角色',
          reputation: '信誉分',
          behaviour: '违约 / 退单',
          status: '状态',
          actions: '操作'
        },
        breaches: '违约 {count} 次',
        returns: '退单 {count} 次',
        empty: '暂无账号数据。'
      },
      status: {
        blacklisted: '已拉黑',
        active: '正常'
      },
      actions: {
        addBlacklist: '加入黑名单',
        removeBlacklist: '移出黑名单',
        deleteAccount: '删除账号'
      }
    },
    settings: {
      trigger: '界面设置',
      title: '界面设置',
      close: '收起设置',
      theme: {
        title: '主题',
        light: '默认主题',
        dark: '深色主题'
      },
      language: {
        title: '语言',
        zh: '中文',
        en: 'English',
        notice: '界面支持中英文切换。'
      },
      fontSize: {
        title: '字体大小',
        small: '小',
        medium: '中',
        large: '大'
      }
    },
    manage: {
      form: {
        headingCreate: '新增房源',
        headingEdit: '编辑房源',
        noticeBrowseOnly: '当前账号仅可浏览房源，请切换为卖家账号以管理房源。',
        fields: {
          title: '房源标题',
          address: '房源地址',
          price: '房源总价（元）',
          downPayment: '首付款（元）',
          area: '建筑面积（㎡）',
          installmentMonthly: '预估月供（元）',
          installmentMonths: '分期期限（月）',
          sellerUsername: '卖家账号',
          sellerName: '卖家昵称',
          contactNumber: '联系方式',
          listingDate: '上架日期',
          floor: '楼层（可选）',
          description: '房源描述',
          keywords: '房源关键词',
          images: '房源图片'
        },
        placeholders: {
          title: '请输入房源标题',
          address: '请输入房源地址',
          price: '例如 2000000',
          downPayment: '例如 600000',
          area: '例如 89',
          installmentMonthly: '系统将根据价格、首付与分期自动计算',
          installmentMonths: '例如 24',
          sellerUsername: '请输入卖家账号',
          sellerName: '请输入卖家昵称',
          contactNumber: '请输入联系电话',
          floor: '例如 10',
          description: '补充房源亮点或其他信息',
          keywords: '例如 市中心、近地铁、朝南'
        },
        hints: {
          installmentCalculation: '系统会根据首付与分期设置自动预估月供。',
          keywordPreview: '将提交的关键词：',
          uploadLimit: '最多上传 {count} 张 PNG、JPG、GIF 或 WEBP 图片。',
          noImages: '尚未上传图片。'
        },
        actions: {
          upload: '上传图片',
          uploading: '上传中…',
          removeImage: '移除',
          submitting: '提交中…',
          submitCreate: '提交审核',
          submitEdit: '保存修改',
          saveDraft: '保存草稿',
          cancel: '取消'
        },
        imageAlt: '房源图片 {index}',
        upload: {
          limit: '最多可上传 {count} 张图片。',
          extraIgnored: '仅保留前 {count} 张图片，多余文件已忽略。',
          invalidType: '仅支持上传图片文件。',
          tooLarge: '单张图片大小需不超过5MB。',
          failure: '图片上传失败，请稍后再试。'
        },
        validation: {
          required: '请完善必填的房源信息。',
          listingDate: '请选择上架日期。',
          listingDateFuture: '挂牌日期需晚于今天，请重新选择。',
          price: '请输入有效的房源总价。',
          downPayment: '请输入有效的首付款金额。',
          downPaymentLimit: '首付需低于总价的120%，否则无法计算分期。',
          area: '请输入有效的建筑面积。',
          installmentMonthly: '无法计算有效的月供，请检查首付或分期设置。',
          installmentMonths: '分期期限必须大于0。'
        }
      },
      list: {
        heading: '房源概览',
        total: '共 {count} 套房源',
        notice: '当前账号仅可浏览房源，请切换为卖家或管理员账号以管理房源。',
        loading: '房源数据加载中…',
        emptyManage: '暂未添加房源，请通过左侧表单发布第一套房源。',
        emptyView: '暂未发布房源，请稍后再来。',
        headers: {
          listing: '房源信息',
          pricing: '价格',
          area: '面积',
          floor: '楼层',
          date: '上架时间',
          status: '状态',
          seller: '卖家账号',
          contact: '联系方式',
          keywords: '关键词',
          actions: '操作'
        },
        photoCount: '{count} 张图片',
        pricing: {
          full: '总价：￥{price}',
          downPayment: '首付：￥{amount}',
          installment: '分期：￥{amount} × {months} 期'
        },
        keywordPlaceholder: '未设置',
        actions: {
          edit: '编辑',
          delete: '删除',
          approve: '通过',
          reject: '驳回',
          paymentLabel: '支付方式',
          paymentFull: '全款',
          paymentInstallment: '分期',
          sold: '已售出',
          processing: '处理中…',
          purchase: '立即购买',
          contact: '联系卖家',
          viewOnly: '仅可查看'
        },
        floorValue: '{floor} 层',
        keywordSeparator: '、'
      }
    },
    verify: {
      panel: {
        title: '实名认证',
        status: {
          verified: '已认证',
          pending: '未认证'
        },
        hint: '实名认证通过后才能预定或购买房源，并将提升账户信誉分。',
        summary: {
          name: '姓名：',
          phone: '手机号：'
        },
        actions: {
          edit: '更新认证',
          cancel: '取消'
        },
        form: {
          name: '真实姓名',
          idNumber: '身份证号码',
          phoneNumber: '手机号',
          placeholders: {
            name: '请输入真实姓名',
            idNumber: '请输入18位身份证号码',
            phoneNumber: '请输入13位手机号'
          },
          actions: {
            submit: '立即认证',
            update: '保存更新',
            submitting: '提交中…'
          }
        },
        errors: {
          idNumber: '身份证号码需为18位数字。',
          phoneNumber: '手机号需为13位数字。',
          generic: '实名认证失败，请稍后再试。'
        }
      }
    },
    auth: {
      tabs: {
        login: '账号登录',
        register: '注册新账号'
      },
      fields: {
        username: '用户名',
        password: '密码',
        displayName: '昵称',
        confirmPassword: '确认密码',
        role: '选择角色',
        captcha: '验证码'
      },
      placeholders: {
        username: '请输入用户名',
        password: '请输入密码',
        passwordWithHint: '请输入密码（至少6位）',
        displayName: '请输入昵称',
        confirmPassword: '请再次输入密码',
        captchaAnswer: '请输入答案'
      },
      roles: {
        seller: '卖家',
        buyer: '买家'
      },
      actions: {
        login: '登录',
        loggingIn: '登录中...',
        register: '注册并登录',
        registering: '注册中...'
      },
      captcha: {
        refresh: '换一题'
      },
      errors: {
        loginRequired: '请输入用户名和密码',
        loginFailed: '登录失败，请稍后再试。',
        registerRequired: '请输入完整注册信息',
        passwordMismatch: '两次输入的密码不一致',
        passwordLength: '请输入至少6位密码',
        registerFailed: '注册失败，请稍后重试',
        captchaRequired: '请先完成验证码',
        captchaInvalid: '验证码回答不正确，请重试'
      }
    },
    success: {
      verificationUpdated: '实名认证信息已更新。',
      deleteHouse: '已删除房源《{title}》。',
      unlistHouse: '已手动下架房源《{title}》，当前状态：{status}，备注：{reason}。',
      houseUpdatedApproved: '房源《{title}》已更新并重新上架。',
      houseUpdatedPending: '房源《{title}》已更新，当前状态：{status}。',
      houseDraftUpdated: '房源《{title}》草稿已保存，可稍后继续编辑。',
      houseCreatedApproved: '已新增房源《{title}》，已通过审核并上架。',
      houseCreatedPending: '已提交房源《{title}》，当前状态：{status}。',
      houseDraftCreated: '已保存房源《{title}》草稿。',
      purchase: '成功以{method}方式购买房源《{title}》，支付金额 ￥{amount}。',
      reservation: '已成功预定房源《{title}》，定金 ￥{amount}。',
      orderReleasedSeller: '订单资金已发放给卖家。',
      orderReleasedBuyer: '订单资金已退回买家。',
      walletTopUp: '钱包充值成功，充值金额 ￥{amount}，获赠 {points} 积分。',
      accountUpdated: '账号信息已更新。',
      orderReturned: '订单《{title}》已退换成功。',
      viewingScheduled: '已为房源《{title}》安排看房，时间 {time}。',
      progressUpdated: '交易进度已更新至 {stage}。',
      blacklistAdded: '已将账号 {username} 加入黑名单。',
      blacklistRemoved: '已解除账号 {username} 的黑名单状态。',
      accountDeleted: '账号 {username} 已被删除。',
      reviewApproved: '已审核通过房源《{title}》。',
      reviewRejected: '已驳回房源《{title}》，状态：{status}，原因：{reason}。',
      reviewSubmitted: '评价已提交，待管理员审核后发布。',
      reviewModeratedApproved: '评价已通过审核并公开展示。',
      reviewModeratedRejected: '已驳回该评价。',
      login: '登录成功。',
      loginReputationSuffix: ' 当前信誉分：{score}。',
      restoredSession: '已恢复上次的登录状态。'
    },
    errors: {
      loadHouses: '加载房源数据失败，请检查后端服务。',
      loadRecommendations: '加载推荐用户失败。',
      loadWallet: '加载钱包信息失败。',
      loadOrders: '加载订单信息失败。',
      loadAdminOrders: '加载待审核订单失败。',
      loadConversations: '加载对话失败。',
      loadMessages: '加载消息失败。',
      messagingUnsupported: '当前角色暂不支持对话功能。',
      sendMessage: '发送消息失败。',
      scheduleViewing: '预约看房失败，请稍后再试。',
      progressUpdate: '更新交易进度失败。',
      scheduleViewingInvalid: '请选择有效的预约时间。',
      contactSellerBuyerOnly: '只有买家可以主动联系卖家。',
      contactSellerVerifyFirst: '请先完成实名认证后再联系卖家。',
      createConversation: '创建对话失败，请稍后再试。',
      refreshUser: '刷新用户信息失败。',
      manageHousesPermission: '当前角色仅支持浏览房源，如需维护房源请使用卖家账号。',
      saveHouse: '保存房源信息失败。',
      editOwnHouse: '卖家只能编辑自己发布的房源。',
      deleteOwnHouse: '卖家只能删除自己发布的房源。',
      deleteHouse: '删除房源失败。',
      unlistHouse: '下架房源失败。',
      purchaseBuyerOnly: '只有买家角色可以发起购买。',
      purchaseVerifyFirst: '购买前请先完成实名认证。',
      purchaseNotApproved: '房源尚未通过审核，暂不可购买。',
      purchaseSelectMethod: '请选择支付方式后再尝试购买。',
      purchaseOwnListing: '不能购买自己发布的房源。',
      purchaseFailed: '支付失败，请稍后再试。',
      reserveBuyerOnly: '只有买家角色可以预定房源。',
      reserveVerifyFirst: '预定前请先完成实名认证。',
      reserveNotApproved: '房源尚未通过审核，暂不可预定。',
      reserveFailed: '预定失败，请稍后再试。',
      installmentCardRequired: '填写19位数字',
      walletLoginRequired: '请先登录后再使用钱包功能。',
      walletTopUp: '钱包充值失败。',
      consumePoints: '扣减积分失败，请稍后重试。',
      updateAccount: '更新账号信息失败。',
      accountLoginRequired: '请登录后再管理账号信息。',
      returnLoginRequired: '请先登录后再申请退换。',
      returnFailed: '退换请求失败。',
      updateBlacklist: '更新黑名单状态失败。',
      deleteAccount: '删除账号失败。',
      reviewRequireReason: '驳回操作需要填写原因。',
      submitReview: '提交审核结果失败。',
      reviewOrder: '审核交易资金失败。',
      loadReputation: '加载信誉面板失败。',
      persistSettings: '界面设置保存失败。',
      persistUser: '无法持久化登录状态。',
      restoreSession: '恢复登录状态失败。',
      reviewLoginRequired: '请先登录后再提交评价。',
      reviewHouseMissing: '未找到要评价的房源，请刷新后重试。',
      reviewModerationDenied: '只有管理员可以执行评价审核。',
      reviewNotFound: '未找到指定的评价，请刷新后重试。',
      reviewContentTooShort: '评价内容不少于8个字。',
      genericServerError: '发生未知错误，请稍后再试。'
    },
    prompts: {
      reviewRejectReason: '请输入驳回原因',
      reviewRemark: '审核备注（可选）',
      reviewDefaultRemark: '审核通过',
      reviewFallbackReason: '未填写',
      deleteHouseConfirm: '确定要删除房源《{title}》吗？',
      deleteAccountConfirm: '确定要删除账号 {username} 吗？该操作不可撤销。',
      unlistReason: '请输入下架原因（房源：{title}）',
      unlistDefaultReason: '管理员手动下架'
    },
    payments: {
      installment: '分期',
      full: '全款'
    },
    explorer: {
      title: '购房首页',
      subtitle: '按关键字、价格或面积筛选房源，系统将展示通过审核的优质房源并支持智能预测。',
      searchPlaceholder: '输入房源关键词（标题、地址或描述）',
      history: {
        toggle: '历史搜索'
      },
      filters: {
        minPrice: '最低价格',
        maxPrice: '最高价格',
        minArea: '最小面积',
        maxArea: '最大面积',
        pricePlaceholder: '元',
        areaPlaceholder: '㎡'
      },
      actions: {
        search: '搜索房源',
        applyFilters: '应用筛选',
        reset: '重置',
        contactSeller: '联系卖家',
        reserve: '预定（定金 ￥{deposit}）',
        reserving: '预定中…',
        purchase: '立即购买',
        processing: '处理中…'
      },
      labels: {
        noImage: '暂无图片',
        fullPrice: '全款价格',
        downPayment: '首付',
        installment: '分期（月供）',
        installmentMonths: '× {count} 期',
        area: '面积',
        listingDate: '挂牌日期',
        seller: '卖家',
        contact: '联系方式',
        paymentMethod: '支付方式',
        installmentCard: '分期银行卡号'
      },
      inputs: {
        installmentCard: '请输入19位银行卡号'
      },
      payment: {
        full: '全款支付',
        installment: '分期付款'
      },
      states: {
        loading: '房源数据加载中…',
        empty: '暂未查询到符合条件的房源。'
      },
      recommendations: {
        sellers: {
          title: '优质卖家',
          score: '信誉分 {score}',
          cta: '查看房源',
          active: '正在查看卖家「{name}」的房源',
          clear: '清除筛选'
        },
        buyers: {
          title: '优质买家',
          score: '信誉分 {score}'
        }
      },
      tips: {
        requireVerification: '完成实名认证后才能查看完整信息并进行交易。',
        awaitingApproval: '该房源尚待管理员审核，通过后方可预定或购买。',
        soldOut: '该房源已售出，系统已自动下架。',
        loginAsBuyer: '登录买家账号后可进行预定或购买。',
        reservedByYou: '您已预定该房源，请留意卖家安排。',
        reservedByOthers: '该房源已被其他买家预定，暂不可再次预定。',
        installmentCardError: '填写19位数字'
      }
    },
    wallet: {
      title: '我的钱包',
      hints: {
        login: '请登录后查看钱包信息与充值功能。'
      },
      states: {
        loading: '钱包数据加载中…',
        empty: '暂无钱包数据，稍后再试。'
      },
      labels: {
        balance: '当前余额',
        unitYuan: '元',
        points: '成长积分',
        pointsHint: '充值返利与功能兑换积分',
        pointsUnit: '积分',
        virtualPort: '虚拟端口号',
        virtualPortHint: '该编号用于识别充值与收款'
      },
      topUp: {
        title: '充值钱包',
        hint: '系统以人民币元为结算单位，每充值 100 元获赠 10 积分。',
        amountLabel: '充值金额（元）',
        amountPlaceholder: '例如：5000',
        referenceLabel: '备注（选填）',
        referencePlaceholder: '如：线上充值'
      },
      actions: {
        processing: '处理中…',
        submitTopUp: '立即充值'
      },
      transactions: {
        title: '最近流水',
        empty: '暂无交易记录。',
        noReference: '—',
        unknownTime: '时间未知',
        types: {
          topUp: '充值',
          payment: '支付',
          receive: '收款',
          refund: '退款',
          withdraw: '提现',
          transferIn: '转入',
          transferOut: '转出',
          adjustment: '调账',
          unknown: '其他交易'
        }
      }
    },
    prediction: {
      title: '房价预测助手',
      subtitle: '基于波士顿房价思路的轻量模型，结合房源特点预估成交价格区间。',
      fields: {
        area: {
          label: '建筑面积（㎡）',
          placeholder: '如：120'
        },
        rooms: {
          label: '平均卧室数量',
          placeholder: '如：3.2'
        },
        age: {
          label: '房龄（年）',
          placeholder: '如：10'
        },
        subway: {
          label: '距离地铁（公里）',
          placeholder: '如：1.5'
        },
        crime: {
          label: '社区治安指数',
          placeholder: '0 表示极佳，建议 0-30'
        },
        school: {
          label: '学区评分',
          placeholder: '0-100'
        }
      },
      actions: {
        submit: '开始预测',
        calculating: '模型计算中…',
        reset: '恢复默认参数'
      },
      limits: {
        label: '今日剩余免费次数：{count} 次',
        loginLabel: '登录以使用预测功能',
        loginHint: '登录后可享受每日 3 次免费预测，之后使用积分继续体验。',
        freeAvailable: '今日仍可免费预测 {count} 次。',
        paidHint: '今日免费次数已用完，继续使用需消耗 {cost} 积分 / 次。',
        pointsReady: '当前积分余额 {balance}，可消耗 {cost} 积分继续预测。',
        pointsBalance: '当前积分余额 {balance}。'
      },
      result: {
        estimate: '预测成交价',
        lower: '参考下限',
        upper: '参考上限',
        confidence: '可信度',
        contributionsTitle: '因素贡献分析'
      },
      features: {
        area: '建筑面积',
        rooms: '卧室数量',
        age: '房龄影响',
        subway: '交通便利度',
        crime: '治安状况',
        school: '教育资源'
      },
      errors: {
        generic: '预测失败，请稍后再试或检查输入。',
        areaRequired: '请填写房屋面积',
        areaMin: '房屋面积必须大于 10 平方米',
        roomsRequired: '请填写平均卧室数量',
        roomsMin: '平均卧室数量必须大于 0',
        ageRequired: '请填写房龄信息',
        ageMin: '房龄不能为负数',
        ageMax: '房龄不能超过 120 年',
        subwayRequired: '请填写距离地铁的距离',
        subwayMin: '地铁距离不能为负数',
        crimeRequired: '请填写社区治安指数',
        crimeMin: '治安指数不能为负数',
        schoolRequired: '请填写学校评分',
        schoolMin: '学校评分不能为负数',
        schoolMax: '学校评分不能超过 100 分',
        loginRequired: '请登录后再使用房价预测功能。',
        pointsUnsupported: '当前暂不支持扣减积分。',
        consumeFailed: '扣减积分失败，请稍后再试。',
        pointsInsufficient: '积分不足，请先充值获取更多积分。'
      },
      trend: {
        title: '近两年房价走势',
        subtitle: '综合相似房源的历史均价与预测结果，展示最近两年的变化趋势。',
        loading: '正在加载房价走势…',
        unavailable: '暂时无法获取历史房价，已根据预测结果生成参考走势。',
        generated: '已根据当前预测生成参考走势。',
        change: '两年涨幅',
        ariaLabel: '近两年房价走势折线图',
        source: {
          payload: '已使用模型返回的相似房价数据。',
          api: '已使用后端提供的历史均价数据。',
          synthetic: '已根据本次预测生成模拟走势。'
        }
      }
    },
    accountCenter: {
      title: '个人中心',
      subtitle: '查看账号摘要并更新用户名或登录密码。',
      summaryTitle: '账号概要',
      summary: {
        username: '登录账号',
        displayName: '昵称',
        role: '当前角色',
        verification: '实名认证状态',
        realName: '实名信息',
        phone: '联系方式',
        points: '成长积分',
        verified: '已认证',
        pending: '待认证'
      },
      credentialTitle: '安全设置',
      credentialHint: '可修改登录账号、展示昵称或重置密码。',
      passwordSection: {
        title: '修改密码',
        hint: '填写当前密码并设置新的登录密码，确保账户安全。'
      },
      fields: {
        username: '新的登录账号',
        displayName: '新的昵称',
        currentPassword: '当前登录密码',
        newPassword: '新的登录密码',
        confirmPassword: '确认新密码'
      },
      placeholders: {
        currentPassword: '请输入当前密码',
        password: '至少 6 位密码',
        confirmPassword: '请再次输入新密码'
      },
      actions: {
        save: '保存修改',
        saving: '保存中…',
        cancel: '返回'
      },
      errors: {
        noChanges: '请修改后再提交。',
        passwordRequired: '请填写新的登录密码并确认。',
        passwordMismatch: '两次输入的新密码不一致。',
        passwordLength: '密码长度至少 6 位。',
        currentPasswordRequired: '请先输入当前密码。'
      }
    },
    serverMessages: {
      wallet: {
        amountRequired: '充值金额不能为空',
        amountPositive: '充值金额必须大于0',
        paymentAmountPositive: '支付金额必须大于0',
        refundAmountPositive: '退款金额必须大于0',
        referenceLength: '备注长度不能超过100个字符',
        balanceInsufficient: '买家钱包余额不足',
        sellerInsufficient: '卖家钱包余额不足，无法完成退款'
      },
      accounts: {
        notFound: '未找到指定用户账号',
        buyerMissing: '买家账号不存在',
        sellerMissing: '卖家账号不存在'
      },
      houses: {
        notFound: '房源不存在',
        missingSeller: '房源缺少卖家账号，无法发起支付',
        priceInvalid: '房源价格异常，无法预定',
        notApproved: '房源尚未通过审核，暂不可交易'
      },
      orders: {
        alreadyReserved: '该房源已有其他买家预定',
        reservedSelf: '您已预定该房源，请耐心等待卖家处理',
        statusUnsupported: '当前订单状态不支持退换',
        buyerOnlyReturn: '仅买家本人可以申请退换'
      },
      generic: {
        validationFailed: '请求参数校验失败'
      }
    },
    orders: {
      progress: {
        DEPOSIT_PAID: '定金交付阶段',
        VIEWING_SCHEDULED: '预约看房阶段',
        FEEDBACK_SUBMITTED: '看房反馈阶段',
        HANDOVER_COMPLETED: '交房阶段',
        FUNDS_RELEASED: '资金结算完成'
      },
      history: {
        title: '订单记录',
        hint: '请登录后查看订单历史及退换进度。',
        loading: '订单数据加载中…',
        empty: '暂无相关订单记录。',
        progressTitle: '交易进度',
        noData: '—',
        fields: {
          orderId: '订单编号',
          amount: '成交金额（元）',
          paymentMethod: '支付方式',
          buyer: '买家',
          seller: '卖家',
          createdAt: '创建时间',
          updatedAt: '更新时间',
          escrowStatus: '托管状态',
          platformFee: '平台服务费',
          releasedAmount: '已结算金额',
          reviewedBy: '审核管理员',
          returnReason: '退换原因'
        },
        status: {
          PENDING: '待支付',
          RESERVED: '已预定',
          PAID: '已付款',
          RETURNED: '已退换',
          CANCELLED: '已取消',
          COMPLETED: '已完成',
          IN_PROGRESS: '处理中',
          DISPUTED: '纠纷处理',
          PROCESSING: '处理中',
          SUCCESS: '已完成',
          REFUNDED: '已退款'
        },
        paymentMethods: {
          FULL: '一次性付清',
          INSTALLMENT: '分期付款'
        },
        viewing: {
          title: '看房安排',
          timeLabel: '预约时间：{time}',
          appointment: '预约时间：{time}',
          notes: '备注：{message}',
          schedule: '安排看房',
          advance: '推进至 {stage}',
          confirm: '确认已看房'
        },
        actions: {
          requestReturn: '申请退换'
        },
        schedule: {
          dialogTitle: '安排看房',
          subtitle: '当前订单：{title}',
          time: '看房时间',
          notes: '补充说明（选填）',
          notesPlaceholder: '可填写给买家的补充说明',
          cancel: '取消',
          confirm: '确认提交',
          validation: '请填写看房时间。'
        },
        prompts: {
          returnReason: '请输入退换理由（可留空）'
        },
        escrow: {
          awaiting: '待管理员审核',
          releasedSeller: '已将资金结算至卖家',
          releasedBuyer: '已将资金退还给买家',
          settled: '托管资金已结算',
          hold: '托管金额：￥{amount}',
          released: '已结算：￥{amount}'
        }
      },
      urgent: {
        title: '紧急待办',
        subtitle: '查看最近的预约情况与提醒。',
        empty: '最近暂无紧急事项。',
        badge: '{count} 条待办',
        awaitingScheduleSeller: '买家 {buyer} 已预定，尽快安排看房时间。',
        awaitingScheduleBuyer: '等待卖家安排看房，如有需要可主动联系。',
        scheduledReminderSeller: '已预约 {time} 看房，请准时到场并提前准备房屋。',
        scheduledReminderBuyer: '已预约 {time} 看房，请及时确认并按时到场。',
        upcomingViewing: '预约时间：{time}',
        markRead: '已读',
        openTarget: '前往处理'
      },
      quickMessages: {
        confirmViewing: '我已确认 {time} 的看房安排，届时见。',
        confirmViewingNoTime: '我已收到看房安排，请您确认。'
      }
    },
    reviews: {
      title: '房源评价',
      subtitle: '为已浏览或交易的房源撰写评价，提交后将进入管理员审核队列。',
      form: {
        heading: '提交新的评价',
        houseLabel: '选择房源',
        ratingLabel: '评分',
        ratingHint: '请选择 1 至 5 星。',
        contentLabel: '评价内容',
        contentPlaceholder: '请填写至少8个字的真实体验，帮助其他用户了解房源情况。',
        charCount: '当前字数：{count}',
        submit: '提交评价',
        submitDisabled: '请先选择房源并填写评价内容'
      },
      validation: {
        houseRequired: '请选择要评价的房源。',
        contentLength: '评价内容不少于8个字。',
        ratingRequired: '请选择评分。'
      },
      list: {
        publicTitle: '已发布的评价',
        empty: '暂时还没有公开评价。',
        myTitle: '我的评价记录',
        pending: '待审核',
        approved: '已发布',
        rejected: '已驳回',
        moderationNote: '审核说明：{note}',
        ratingLabel: '{rating} 分 · {author}',
        timeLabel: '提交时间：{time}'
      },
      moderation: {
        title: '评价审核',
        subtitle: '审核买家与卖家的评价内容，保障平台信息真实可信。',
        empty: '暂无待审核的评价。',
        approve: '通过',
        reject: '驳回',
        notePlaceholder: '可选说明，至少8个字更容易让作者理解原因。',
        ratingLabel: '{rating} 分',
        authorLabel: '来自：{author}',
        createdLabel: '提交时间：{time}'
      }
    }
  },
  en: {
    header: {
      title: 'Second-hand Housing Platform',
      subtitle: 'An integrated platform supporting reservations, payments, and reputation evaluation.',
      currentRoleLabel: 'Role:',
      reputationLabel: 'Reputation:',
      verificationLabel: 'Real-name verification:',
      verified: 'Completed',
      pending: 'Pending',
      messages: 'Messages',
      logout: 'Log out'
    },
    nav: {
      home: 'Home',
      aiAssistant: 'AI assistant',
      manage: 'Listing management',
      feedback: 'Listing feedback',
      urgent: 'Urgent tasks',
      orders: 'Orders & wallet',
      account: 'Personal hub',
      review: 'Listing review',
      reviewWithCount: 'Listing review ({count})',
      adminHouses: 'Listings admin',
      admin: 'Admin dashboard',
      reputation: 'Reputation board'
    },
    footer: {
      apiBaseLabel: 'API endpoint:'
    },
    alerts: {
      errorPrefix: 'Notice:'
    },
    aiAssistant: {
      title: 'Smart buying assistant',
      description: 'Ask the AI about second-hand purchases, risk checks, and policy guidance.',
      modelLabel: 'Model: {model}',
      refresh: 'Refresh suggestions',
      relatedTitle: 'Related questions',
      relatedDescription: 'Click a question to quickly consult the assistant about second-hand deals.',
      inputLabel: 'Ask the AI',
      inputPlaceholder: 'Type your second-hand housing question, such as mortgage steps or risk reminders…',
      missingKey: 'The AI assistant is disabled. Ask an administrator to configure the backend key.',
      ask: 'Ask now',
      asking: 'Generating answer…',
      conversationTitle: 'Conversation history',
      emptyState: 'No conversation yet. Start by asking the AI about buying a home.',
      questionLabel: 'Your question',
      answerLabel: 'AI response',
      pendingAnswer: 'Preparing a response…',
      genericError: 'Unable to fetch the AI response right now. Please try again later.',
      fallbackAnswer: 'Sorry, the assistant could not generate a helpful answer. Please try another question.'
    },
    conversation: {
      title: 'Message center',
      subtitle: 'Chat with sellers in real time to confirm listing details and schedule transactions.',
      actions: {
        refresh: 'Refresh list',
        refreshing: 'Refreshing…',
        close: 'Close',
        back: 'Back to list',
        send: 'Send',
        sending: 'Sending…'
      },
      list: {
        title: 'Conversations',
        count: '{count}',
        loading: 'Loading conversations…',
        empty: 'No conversations yet.'
      },
      preview: {
        you: 'You',
        partner: 'Partner',
        empty: 'No messages yet'
      },
      empty: {
        title: 'Select a conversation to begin',
        description: 'Choose a seller on the left to browse history and send a new message.'
      },
      chat: {
        subtitle: 'Coordinate viewings, payments, and other details with the seller.',
        loading: 'Loading messages…',
        none: 'No messages yet. Start the conversation with the seller.',
        placeholder: 'Type a message (max 2000 characters)',
        headerFallback: 'Conversation details'
      },
      participant: {
        seller: 'Seller',
        buyer: 'Buyer'
      }
    },
    roles: {
      buyer: 'Buyer',
      seller: 'Seller',
      admin: 'Administrator'
    },
    statuses: {
      draft: 'Draft',
      pending: 'Pending review',
      approved: 'Approved',
      rejected: 'Rejected',
      sold: 'Sold (unlisted)'
    },
    adminReview: {
      title: 'Listing review dashboard',
      subtitle: '{count} listings are waiting for administrator review.',
      subtitleSingle: '1 listing is waiting for administrator review.',
      refresh: 'Refresh list',
      loading: 'Loading listings awaiting review…',
      emptyTitle: 'No listings are waiting for review',
      emptyDescription: 'Listings submitted by sellers will appear here for approval.',
      noImage: 'No cover image',
      badgePending: 'Pending review',
      fields: {
        price: 'Total price',
        downPayment: 'Down payment',
        installment: 'Instalment plan',
        area: 'Floor area',
        listedOn: 'Listed on',
        floor: 'Floor'
      },
      installmentUnavailable: 'No instalment plan provided',
      installmentUnknownMonths: 'N/A',
      installmentValue: '¥{amount} × {months} months',
      floorValue: 'Level {floor}',
      sellerAccount: 'Seller account',
      contactName: 'Contact name',
      contactPhone: 'Contact phone',
      actions: {
        approve: 'Approve',
        reject: 'Reject',
        delete: 'Delete listing'
      }
    },
    adminManage: {
      title: 'Listing maintenance',
      subtitle: '{count} listings available for filtering, unlisting, or removal.',
      refresh: 'Sync listings',
      refreshing: 'Syncing…',
      loading: 'Loading listing overview…',
      empty: 'No listings matched your filters.',
      filters: {
        query: 'Search keywords',
        queryPlaceholder: 'Filter by title, address, or seller',
        status: 'Status filter',
        statusAll: 'All statuses'
      },
      table: {
        listing: 'Listing',
        status: 'Status',
        seller: 'Seller / contact',
        updated: 'Last updated',
        actions: 'Actions'
      },
      actions: {
        view: 'View',
        unlist: 'Unlist',
        unlistDisabled: 'Cannot unlist',
        delete: 'Delete'
      },
      unknownTitle: 'Untitled listing',
      unknownAddress: 'Address pending',
      unknownSeller: 'Unknown seller',
      unknownStatus: 'Unknown status'
    },
    adminOrders: {
      title: 'Escrow review',
      subtitle: 'Funds are held in escrow until an administrator approves the payout.',
      refresh: 'Refresh list',
      loading: 'Loading escrow orders…',
      empty: 'No escrow orders waiting for review.',
      columns: {
        id: 'Order ID',
        listing: 'Listing',
        buyer: 'Buyer',
        seller: 'Seller',
        escrow: 'Escrow balance',
        payment: 'Payment method',
        createdAt: 'Created at',
        actions: 'Actions'
      },
      escrowBalance: 'Escrow: ¥{amount}',
      platformFee: 'Fee: ¥{amount}',
      payment: {
        full: 'Full payment',
        installment: 'Instalments'
      },
      actions: {
        releaseSeller: 'Release to seller',
        refundBuyer: 'Refund to buyer'
      }
    },
    adminReputation: {
      title: 'Reputation dashboard',
      subtitle: 'The platform evaluates user behaviour to adjust reputation scores. Administrators can blacklist or restore accounts.',
      refresh: 'Refresh data',
      loading: 'Loading reputation data…',
      summary: {
        blacklisted: 'Blacklisted accounts',
        sellers: 'Active sellers',
        buyers: 'Active buyers'
      },
      tables: {
        sellers: {
          title: 'Top sellers',
          columns: {
            account: 'Seller',
            reputation: 'Reputation',
            breaches: 'Reservation breaches'
          },
          empty: 'No seller data available.'
        },
        buyers: {
          title: 'Top buyers',
          columns: {
            account: 'Buyer',
            reputation: 'Reputation',
            returns: 'Returns'
          },
          empty: 'No buyer data available.'
        }
      },
      blacklist: {
        title: 'Account blacklist management',
        columns: {
          account: 'Account',
          role: 'Role',
          reputation: 'Reputation',
          behaviour: 'Breaches / returns',
          status: 'Status',
          actions: 'Actions'
        },
        breaches: 'Breaches {count}',
        returns: 'Returns {count}',
        empty: 'No account data available.'
      },
      status: {
        blacklisted: 'Blacklisted',
        active: 'Active'
      },
      actions: {
        addBlacklist: 'Add to blacklist',
        removeBlacklist: 'Remove from blacklist',
        deleteAccount: 'Delete account'
      }
    },
    settings: {
      trigger: 'Interface settings',
      title: 'Interface settings',
      close: 'Hide settings',
      theme: {
        title: 'Theme',
        light: 'Default theme',
        dark: 'Dark theme'
      },
      language: {
        title: 'Language',
        zh: '中文',
        en: 'English',
        notice: 'All panels support Chinese and English.'
      },
      fontSize: {
        title: 'Font size',
        small: 'Small',
        medium: 'Medium',
        large: 'Large'
      }
    },
    manage: {
      form: {
        headingCreate: 'Add listing',
        headingEdit: 'Edit listing',
        noticeBrowseOnly:
          'This account can only browse listings. Switch to a seller account to manage listings.',
        fields: {
          title: 'Listing title',
          address: 'Listing address',
          price: 'Total price (CNY)',
          downPayment: 'Down payment (CNY)',
          area: 'Floor area (㎡)',
          installmentMonthly: 'Estimated monthly instalment (CNY)',
          installmentMonths: 'Instalment term (months)',
          sellerUsername: 'Seller username',
          sellerName: 'Seller display name',
          contactNumber: 'Contact number',
          listingDate: 'Listing date',
          floor: 'Floor (optional)',
          description: 'Listing description',
          keywords: 'Keywords',
          images: 'Listing photos'
        },
        placeholders: {
          title: 'Enter listing title',
          address: 'Enter listing address',
          price: 'e.g. 2000000',
          downPayment: 'e.g. 600000',
          area: 'e.g. 89',
          installmentMonthly: 'Calculated automatically from price, down payment and term',
          installmentMonths: 'e.g. 24',
          sellerUsername: 'Enter seller username',
          sellerName: 'Enter seller display name',
          contactNumber: 'Enter contact number',
          floor: 'e.g. 10',
          description: 'Highlight selling points or additional details',
          keywords: 'e.g. city centre, subway nearby, south-facing'
        },
        hints: {
          installmentCalculation: 'The system estimates the monthly instalment based on your down payment and term.',
          keywordPreview: 'Keywords that will be submitted:',
          uploadLimit: 'Upload up to {count} images in PNG, JPG, GIF, or WEBP format.',
          noImages: 'No images uploaded yet.'
        },
        actions: {
          upload: 'Upload images',
          uploading: 'Uploading…',
          removeImage: 'Remove',
          submitting: 'Submitting…',
          submitCreate: 'Submit for review',
          submitEdit: 'Save changes',
          saveDraft: 'Save draft',
          cancel: 'Cancel'
        },
        imageAlt: 'Listing image {index}',
        upload: {
          limit: 'You can upload up to {count} images.',
          extraIgnored: 'Only the first {count} images were kept. Extra files were ignored.',
          invalidType: 'Only image files can be uploaded.',
          tooLarge: 'Each image must be 5 MB or smaller.',
          failure: 'Image upload failed. Please try again later.'
        },
        validation: {
          required: 'Please complete all required listing information.',
          listingDate: 'Choose a listing date.',
          listingDateFuture: 'Please choose a future listing date.',
          price: 'Enter a valid total price.',
          downPayment: 'Enter a valid down payment amount.',
          downPaymentLimit: 'The down payment must be below 120% of the total price to calculate instalments.',
          area: 'Enter a valid floor area.',
          installmentMonthly: 'Unable to calculate a valid monthly instalment. Please review the down payment or term.',
          installmentMonths: 'The instalment term must be greater than zero.'
        }
      },
      list: {
        heading: 'Listing overview',
        total: '{count} listings in total',
        notice: 'Switch to a seller or administrator account to manage listings.',
        loading: 'Loading listings…',
        emptyManage: 'No listings yet. Use the form to publish your first property.',
        emptyView: 'No listings available at the moment.',
        headers: {
          listing: 'Listing',
          pricing: 'Pricing',
          area: 'Area',
          floor: 'Floor',
          date: 'Listed on',
          status: 'Status',
          seller: 'Seller',
          contact: 'Contact',
          keywords: 'Keywords',
          actions: 'Actions'
        },
        photoCount: '{count} photos',
        pricing: {
          full: 'Full price: ￥{price}',
          downPayment: 'Down payment: ￥{amount}',
          installment: 'Instalments: ￥{amount} × {months} months'
        },
        keywordPlaceholder: 'Not set',
        actions: {
          edit: 'Edit',
          delete: 'Delete',
          approve: 'Approve',
          reject: 'Reject',
          paymentLabel: 'Payment method',
          paymentFull: 'Full',
          paymentInstallment: 'Instalments',
          sold: 'Sold',
          processing: 'Processing…',
          purchase: 'Purchase now',
          contact: 'Contact seller',
          viewOnly: 'View only'
        },
        floorValue: 'Level {floor}',
        keywordSeparator: ', '
      }
    },
    verify: {
      panel: {
        title: 'Real-name verification',
        status: {
          verified: 'Verified',
          pending: 'Not verified'
        },
        hint: 'Verification is required to reserve or purchase listings and increases your reputation score.',
        summary: {
          name: 'Name:',
          phone: 'Phone:'
        },
        actions: {
          edit: 'Update verification',
          cancel: 'Cancel'
        },
        form: {
          name: 'Full name',
          idNumber: 'ID number',
          phoneNumber: 'Phone number',
          placeholders: {
            name: 'Enter your full name',
            idNumber: 'Enter your 18-digit ID number',
            phoneNumber: 'Enter your 13-digit phone number'
          },
          actions: {
            submit: 'Verify now',
            update: 'Save updates',
            submitting: 'Submitting…'
          }
        },
        errors: {
          idNumber: 'The ID number must contain exactly 18 digits.',
          phoneNumber: 'The phone number must contain exactly 13 digits.',
          generic: 'Verification failed. Please try again later.'
        }
      }
    },
    auth: {
      tabs: {
        login: 'Account login',
        register: 'Register new account'
      },
      fields: {
        username: 'Username',
        password: 'Password',
        displayName: 'Display name',
        confirmPassword: 'Confirm password',
        role: 'Choose role',
        captcha: 'Verification question'
      },
      placeholders: {
        username: 'Enter username',
        password: 'Enter password',
        passwordWithHint: 'Enter password (min. 6 characters)',
        displayName: 'Enter display name',
        confirmPassword: 'Re-enter password',
        captchaAnswer: 'Enter the answer'
      },
      roles: {
        seller: 'Seller',
        buyer: 'Buyer'
      },
      actions: {
        login: 'Log in',
        loggingIn: 'Logging in...',
        register: 'Register & log in',
        registering: 'Registering...'
      },
      captcha: {
        refresh: 'Try another question'
      },
      errors: {
        loginRequired: 'Please enter username and password.',
        loginFailed: 'Sign-in failed. Please try again later.',
        registerRequired: 'Please complete all registration fields.',
        passwordMismatch: 'The passwords do not match.',
        passwordLength: 'Please enter a password with at least 6 characters.',
        registerFailed: 'Registration failed. Please try again later.',
        captchaRequired: 'Please solve the verification question first.',
        captchaInvalid: 'Incorrect answer. Please try again.'
      }
    },
    success: {
      verificationUpdated: 'Real-name verification updated successfully.',
      deleteHouse: 'Listing “{title}” has been removed.',
      unlistHouse: 'Listing “{title}” has been manually unlisted. Current status: {status}. Note: {reason}.',
      houseUpdatedApproved: 'Listing “{title}” has been updated and republished.',
      houseUpdatedPending: 'Listing “{title}” has been updated. Current status: {status}.',
      houseDraftUpdated: 'Draft for “{title}” saved successfully. Continue editing any time.',
      houseCreatedApproved: 'Listing “{title}” has been created and approved.',
      houseCreatedPending: 'Listing “{title}” has been submitted. Current status: {status}.',
      houseDraftCreated: 'Draft for “{title}” saved successfully.',
      purchase: 'Successfully purchased “{title}” via {method}, amount paid ¥{amount}.',
      reservation: 'Successfully reserved “{title}” with a deposit of ¥{amount}.',
      orderReleasedSeller: 'Funds released to the seller.',
      orderReleasedBuyer: 'Funds returned to the buyer.',
      walletTopUp: 'Wallet top-up successful: ¥{amount} added, {points} points awarded.',
      accountUpdated: 'Account details updated successfully.',
      orderReturned: 'Order “{title}” has been refunded.',
      viewingScheduled: 'Viewing for “{title}” has been scheduled at {time}.',
      progressUpdated: 'Progress updated to {stage}.',
      blacklistAdded: 'Account {username} added to blacklist.',
      blacklistRemoved: 'Account {username} removed from blacklist.',
      accountDeleted: 'Account {username} has been deleted.',
      reviewApproved: 'Listing “{title}” approved.',
      reviewRejected: 'Rejected listing “{title}”, status: {status}, reason: {reason}.',
      reviewSubmitted: 'Your review has been submitted for moderator approval.',
      reviewModeratedApproved: 'The review has been approved and published.',
      reviewModeratedRejected: 'The review has been rejected.',
      login: 'Signed in successfully.',
      loginReputationSuffix: ' Current reputation score: {score}.',
      restoredSession: 'Previous session restored.'
    },
    errors: {
      loadHouses: 'Failed to load listings. Please check the backend service.',
      loadRecommendations: 'Failed to load recommended users.',
      loadWallet: 'Failed to load wallet information.',
      loadOrders: 'Failed to load orders.',
      loadAdminOrders: 'Failed to load pending orders for review.',
      loadConversations: 'Failed to load conversations.',
      loadMessages: 'Failed to load messages.',
      messagingUnsupported: 'Messaging is not available for this role.',
      sendMessage: 'Failed to send message.',
      scheduleViewing: 'Failed to schedule viewing. Please try again later.',
      progressUpdate: 'Failed to update progress.',
      scheduleViewingInvalid: 'Please choose a valid viewing time.',
      contactSellerBuyerOnly: 'Only buyers can contact sellers.',
      contactSellerVerifyFirst: 'Please complete real-name verification before contacting sellers.',
      createConversation: 'Failed to start conversation. Please try again later.',
      refreshUser: 'Failed to refresh user information.',
      manageHousesPermission: 'This role can only browse listings. Please switch to a seller account to manage listings.',
      saveHouse: 'Failed to save listing.',
      editOwnHouse: 'Sellers can only edit their own listings.',
      deleteOwnHouse: 'Sellers can only delete their own listings.',
      deleteHouse: 'Failed to delete listing.',
      unlistHouse: 'Failed to unlist the listing.',
      purchaseBuyerOnly: 'Only buyers can make purchases.',
      purchaseVerifyFirst: 'Please complete real-name verification before purchasing.',
      purchaseNotApproved: 'The listing has not been approved yet.',
      purchaseSelectMethod: 'Please choose a payment method first.',
      purchaseOwnListing: 'You cannot purchase your own listing.',
      purchaseFailed: 'Payment failed. Please try again later.',
      reserveBuyerOnly: 'Only buyers can reserve listings.',
      reserveVerifyFirst: 'Please complete real-name verification before reserving.',
      reserveNotApproved: 'The listing has not been approved yet.',
      reserveFailed: 'Failed to reserve listing. Please try again later.',
      installmentCardRequired: 'Please enter a 19-digit card number.',
      walletLoginRequired: 'Please sign in before using wallet features.',
      walletTopUp: 'Wallet top-up failed.',
      consumePoints: 'Failed to deduct points. Please try again later.',
      updateAccount: 'Failed to update account details.',
      accountLoginRequired: 'Please sign in before managing account details.',
      returnLoginRequired: 'Please sign in before requesting a refund.',
      returnFailed: 'Failed to submit refund request.',
      updateBlacklist: 'Failed to update blacklist status.',
      deleteAccount: 'Failed to delete account.',
      reviewRequireReason: 'A reason is required to reject a listing.',
      submitReview: 'Failed to submit review decision.',
      reviewOrder: 'Failed to review escrow transaction.',
      loadReputation: 'Failed to load reputation dashboard.',
      persistSettings: 'Failed to save interface settings.',
      persistUser: 'Unable to persist login state.',
      restoreSession: 'Failed to restore previous session.',
      reviewLoginRequired: 'Please sign in before submitting a review.',
      reviewHouseMissing: 'The selected listing could not be found. Please refresh and try again.',
      reviewModerationDenied: 'Only administrators can moderate reviews.',
      reviewNotFound: 'The specified review no longer exists.',
      reviewContentTooShort: 'Reviews must contain at least 8 characters.',
      genericServerError: 'An unexpected error occurred. Please try again later.'
    },
    prompts: {
      reviewRejectReason: 'Enter rejection reason',
      reviewRemark: 'Review remark (optional)',
      reviewDefaultRemark: 'Approved',
      reviewFallbackReason: 'Not provided',
      deleteHouseConfirm: 'Are you sure you want to delete listing “{title}”?',
      deleteAccountConfirm: 'Delete account {username}? This action cannot be undone.',
      unlistReason: 'Enter a note for unlisting (listing: “{title}”)',
      unlistDefaultReason: 'Admin manual unlisting'
    },
    payments: {
      installment: 'instalments',
      full: 'full payment'
    },
    explorer: {
      title: 'Home explorer',
      subtitle: 'Filter approved listings by keyword, price or area and access instant price predictions.',
      searchPlaceholder: 'Search listings by title, address, or description',
      history: {
        toggle: 'Search history'
      },
      filters: {
        minPrice: 'Minimum price',
        maxPrice: 'Maximum price',
        minArea: 'Minimum area',
        maxArea: 'Maximum area',
        pricePlaceholder: 'CNY',
        areaPlaceholder: '㎡'
      },
      actions: {
        search: 'Search listings',
        applyFilters: 'Apply filters',
        reset: 'Reset',
        contactSeller: 'Contact seller',
        reserve: 'Reserve (deposit ¥{deposit})',
        reserving: 'Reserving…',
        purchase: 'Buy now',
        processing: 'Processing…'
      },
      labels: {
        noImage: 'No image',
        fullPrice: 'Full payment price',
        downPayment: 'Down payment',
        installment: 'Installments (monthly)',
        installmentMonths: '× {count} months',
        area: 'Area',
        listingDate: 'Listed on',
        seller: 'Seller',
        contact: 'Contact',
        paymentMethod: 'Payment method',
        installmentCard: 'Installment card number'
      },
      inputs: {
        installmentCard: 'Enter the 19-digit card number'
      },
      payment: {
        full: 'Full payment',
        installment: 'Installments'
      },
      states: {
        loading: 'Loading listings…',
        empty: 'No listings matched the filters yet.'
      },
      recommendations: {
        sellers: {
          title: 'Top sellers',
          score: 'Reputation {score}'
        },
        buyers: {
          title: 'Top buyers',
          score: 'Reputation {score}'
        }
      },
      tips: {
        requireVerification: 'Complete real-name verification to view full details and trade.',
        awaitingApproval: 'This listing is awaiting administrator approval before reservations and purchases are available.',
        soldOut: 'This listing has been sold and automatically removed from the storefront.',
        loginAsBuyer: 'Sign in with a buyer account to reserve or purchase listings.',
        reservedByYou: 'You have already reserved this listing. Please watch for the seller’s updates.',
        reservedByOthers: 'Another buyer has reserved this listing. Reservations are temporarily unavailable.',
        installmentCardError: 'Enter a 19-digit number'
      }
    },
    wallet: {
      title: 'Wallet',
      hints: {
        login: 'Please sign in to access wallet information and top-up features.'
      },
      states: {
        loading: 'Loading wallet data…',
        empty: 'No wallet information yet. Please try again later.'
      },
      labels: {
        balance: 'Current balance',
        points: 'Reward points',
        pointsHint: 'Redeemable credits earned through top-ups',
        pointsUnit: 'pts',
        virtualPort: 'Virtual account number',
        virtualPortHint: 'Use this identifier when adding funds or receiving payments.'
      },
      topUp: {
        title: 'Top up wallet',
        hint: 'Balances are stored in Chinese Yuan. Earn 10 points for every ¥100 top-up.',
        amountLabel: 'Amount (CNY)',
        amountPlaceholder: 'e.g. 5000',
        referenceLabel: 'Reference (optional)',
        referencePlaceholder: 'e.g. Online top-up'
      },
      actions: {
        processing: 'Processing…',
        submitTopUp: 'Top up now'
      },
      transactions: {
        title: 'Recent activity',
        empty: 'No transactions recorded yet.',
        noReference: 'No reference',
        unknownTime: 'Unknown time',
        types: {
          topUp: 'Top-up',
          payment: 'Payment',
          receive: 'Incoming',
          refund: 'Refund',
          withdraw: 'Withdrawal',
          transferIn: 'Transfer in',
          transferOut: 'Transfer out',
          adjustment: 'Adjustment',
          unknown: 'Other'
        }
      }
    },
    prediction: {
      title: 'Price prediction assistant',
      subtitle: 'A lightweight model inspired by the Boston housing dataset that estimates price ranges from your inputs.',
      fields: {
        area: {
          label: 'Floor area (㎡)',
          placeholder: 'e.g. 120'
        },
        rooms: {
          label: 'Average bedrooms',
          placeholder: 'e.g. 3.2'
        },
        age: {
          label: 'Property age (years)',
          placeholder: 'e.g. 10'
        },
        subway: {
          label: 'Distance to subway (km)',
          placeholder: 'e.g. 1.5'
        },
        crime: {
          label: 'Safety index',
          placeholder: '0 is best, typically 0-30'
        },
        school: {
          label: 'School score',
          placeholder: '0-100'
        }
      },
      actions: {
        submit: 'Predict price',
        calculating: 'Calculating…',
        reset: 'Reset to defaults'
      },
      limits: {
        label: 'Free predictions left today: {count}',
        loginLabel: 'Sign in to predict prices',
        loginHint: 'Enjoy three free predictions per day when signed in. Continue with reward points afterwards.',
        freeAvailable: '{count} free prediction(s) remaining today.',
        paidHint: 'Free quota has been used. Each additional prediction costs {cost} points.',
        pointsReady: '{balance} points available. {cost} points will be used for this prediction.',
        pointsBalance: 'Current points balance: {balance}.'
      },
      result: {
        estimate: 'Estimated price',
        lower: 'Lower bound',
        upper: 'Upper bound',
        confidence: 'Confidence',
        contributionsTitle: 'Feature contribution breakdown'
      },
      features: {
        area: 'Floor area',
        rooms: 'Rooms',
        age: 'Property age',
        subway: 'Transit access',
        crime: 'Safety index',
        school: 'School quality'
      },
      errors: {
        generic: 'Unable to generate prediction. Please review the input values and try again.',
        areaRequired: 'Floor area is required.',
        areaMin: 'Floor area must be greater than 10㎡.',
        roomsRequired: 'Average bedroom count is required.',
        roomsMin: 'Average bedrooms must be greater than 0.',
        ageRequired: 'Property age is required.',
        ageMin: 'Property age cannot be negative.',
        ageMax: 'Property age cannot exceed 120 years.',
        subwayRequired: 'Distance to subway is required.',
        subwayMin: 'Distance to subway cannot be negative.',
        crimeRequired: 'Safety index is required.',
        crimeMin: 'Safety index cannot be negative.',
        schoolRequired: 'School score is required.',
        schoolMin: 'School score cannot be negative.',
        schoolMax: 'School score cannot exceed 100.',
        loginRequired: 'Please sign in before using price prediction.',
        pointsUnsupported: 'Point deduction is currently unavailable.',
        consumeFailed: 'Failed to deduct points. Please try again later.',
        pointsInsufficient: 'Insufficient points. Please top up to continue.'
      },
      trend: {
        title: 'Two-year price trend',
        subtitle: 'Combines similar listing history with the predicted outcome to show the latest two-year movement.',
        loading: 'Loading price trend…',
        unavailable: 'Historic prices are unavailable. A synthetic trend has been generated from the prediction.',
        generated: 'A synthetic trend has been generated from the current prediction.',
        change: 'Two-year change',
        ariaLabel: 'Line chart showing the two-year price trend',
        source: {
          payload: 'Using trend data returned by the prediction service.',
          api: 'Using historical averages provided by the backend.',
          synthetic: 'A synthetic trend has been generated from this prediction.'
        }
      }
    },
    accountCenter: {
      title: 'Personal hub',
      subtitle: 'Review account information and update credentials securely.',
      summaryTitle: 'Account overview',
      summary: {
        username: 'Username',
        displayName: 'Display name',
        role: 'Role',
        verification: 'Verification status',
        realName: 'Real name',
        phone: 'Phone number',
        points: 'Points',
        verified: 'Verified',
        pending: 'Pending'
      },
      credentialTitle: 'Security settings',
      credentialHint: 'Update your username, display name or reset the password.',
      passwordSection: {
        title: 'Change password',
        hint: 'Enter your current password and set a new one to keep the account secure.'
      },
      fields: {
        username: 'New username',
        displayName: 'New display name',
        currentPassword: 'Current password',
        newPassword: 'New password',
        confirmPassword: 'Confirm new password'
      },
      placeholders: {
        currentPassword: 'Enter your current password',
        password: 'Minimum 6 characters',
        confirmPassword: 'Re-enter new password'
      },
      actions: {
        save: 'Save changes',
        saving: 'Saving…',
        cancel: 'Back'
      },
      errors: {
        noChanges: 'Please modify a field before saving.',
        passwordRequired: 'Please enter and confirm a new password.',
        passwordMismatch: 'The new passwords do not match.',
        passwordLength: 'Password must contain at least 6 characters.',
        currentPasswordRequired: 'Please provide your current password first.'
      }
    },
    serverMessages: {
      wallet: {
        amountRequired: 'Top-up amount is required',
        amountPositive: 'Top-up amount must be greater than zero',
        paymentAmountPositive: 'Payment amount must be greater than zero',
        refundAmountPositive: 'Refund amount must be greater than zero',
        referenceLength: 'Reference must not exceed 100 characters',
        balanceInsufficient: 'Buyer wallet balance is insufficient',
        sellerInsufficient: 'Seller wallet balance is insufficient to process the refund'
      },
      accounts: {
        notFound: 'The specified user account could not be found',
        buyerMissing: 'Buyer account not found',
        sellerMissing: 'Seller account not found'
      },
      houses: {
        notFound: 'Listing not found',
        missingSeller: 'The listing is missing a seller account and cannot proceed',
        priceInvalid: 'Invalid listing price. Reservation cannot continue.',
        notApproved: 'The listing has not been approved yet.'
      },
      orders: {
        alreadyReserved: 'Another buyer has already reserved this listing',
        reservedSelf: 'You have already reserved this listing. Please wait for the seller.',
        statusUnsupported: 'The current order status does not allow this action',
        buyerOnlyReturn: 'Only the buyer can request a refund'
      },
      generic: {
        validationFailed: 'Request validation failed'
      }
    },
    orders: {
      progress: {
        DEPOSIT_PAID: 'Deposit stage',
        VIEWING_SCHEDULED: 'Viewing appointment',
        FEEDBACK_SUBMITTED: 'Viewing feedback',
        HANDOVER_COMPLETED: 'Handover',
        FUNDS_RELEASED: 'Funds released'
      },
      history: {
        title: 'Order history',
        hint: 'Sign in to review your past transactions and return progress.',
        loading: 'Loading orders…',
        empty: 'No orders found yet.',
        progressTitle: 'Transaction progress',
        noData: '—',
        fields: {
          orderId: 'Order ID',
          amount: 'Amount (CNY)',
          paymentMethod: 'Payment method',
          buyer: 'Buyer',
          seller: 'Seller',
          createdAt: 'Created at',
          updatedAt: 'Updated at',
          escrowStatus: 'Escrow status',
          platformFee: 'Platform fee',
          releasedAmount: 'Released amount',
          reviewedBy: 'Reviewed by',
          returnReason: 'Return reason'
        },
        status: {
          PENDING: 'Pending payment',
          RESERVED: 'Reserved',
          PAID: 'Paid',
          RETURNED: 'Returned',
          CANCELLED: 'Cancelled',
          COMPLETED: 'Completed',
          IN_PROGRESS: 'In progress',
          DISPUTED: 'Under dispute',
          PROCESSING: 'Processing',
          SUCCESS: 'Completed',
          REFUNDED: 'Refunded'
        },
        paymentMethods: {
          FULL: 'Full payment',
          INSTALLMENT: 'Installment'
        },
        viewing: {
          title: 'Viewing appointment',
          timeLabel: 'Viewing time: {time}',
          appointment: 'Viewing time: {time}',
          notes: 'Notes: {message}',
          schedule: 'Schedule viewing',
          advance: 'Advance to {stage}',
          confirm: 'Confirm viewing'
        },
        actions: {
          requestReturn: 'Request a return'
        },
        schedule: {
          dialogTitle: 'Schedule viewing',
          subtitle: 'Order: {title}',
          time: 'Viewing time',
          notes: 'Notes (optional)',
          notesPlaceholder: 'Share extra instructions for the buyer',
          cancel: 'Cancel',
          confirm: 'Confirm',
          validation: 'Please choose a viewing time.'
        },
        prompts: {
          returnReason: 'Enter a return reason (optional)'
        },
        escrow: {
          awaiting: 'Awaiting administrator review',
          releasedSeller: 'Funds released to seller',
          releasedBuyer: 'Funds refunded to buyer',
          settled: 'Escrow settled',
          hold: 'Escrow hold: ￥{amount}',
          released: 'Released amount: ￥{amount}'
        }
      },
      urgent: {
        title: 'Urgent tasks',
        subtitle: 'Stay on top of the latest viewing appointments.',
        empty: 'No urgent items recently.',
        badge: '{count} tasks',
        awaitingScheduleSeller: 'Buyer {buyer} has reserved. Please arrange a viewing.',
        awaitingScheduleBuyer: 'Waiting for the seller to schedule a viewing. Feel free to reach out proactively.',
        scheduledReminderSeller: 'Viewing scheduled at {time}. Please prepare the property in advance.',
        scheduledReminderBuyer: 'Viewing scheduled at {time}. Remember to confirm and arrive on time.',
        upcomingViewing: 'Appointment: {time}',
        markRead: 'Mark as read',
        openTarget: 'Go to task'
      },
      quickMessages: {
        confirmViewing: 'I confirm the viewing at {time}. See you then!',
        confirmViewingNoTime: 'I have received the viewing arrangement. Please confirm.'
      }
    },
    reviews: {
      title: 'Listing feedback',
      subtitle: 'Share your experience with a property. Submitted reviews require administrator approval before publication.',
      form: {
        heading: 'Submit a new review',
        houseLabel: 'Select listing',
        ratingLabel: 'Rating',
        ratingHint: 'Choose between 1 and 5 stars.',
        contentLabel: 'Review details',
        contentPlaceholder: 'Enter at least 8 characters describing your experience to help other users.',
        charCount: 'Characters: {count}',
        submit: 'Submit review',
        submitDisabled: 'Select a listing and complete the review first'
      },
      validation: {
        houseRequired: 'Please choose a listing to review.',
        contentLength: 'Reviews must contain at least 8 characters.',
        ratingRequired: 'Please choose a rating.'
      },
      list: {
        publicTitle: 'Published reviews',
        empty: 'No public reviews yet.',
        myTitle: 'My submissions',
        pending: 'Pending',
        approved: 'Published',
        rejected: 'Rejected',
        moderationNote: 'Moderator note: {note}',
        ratingLabel: '{rating} pts · {author}',
        timeLabel: 'Submitted at: {time}'
      },
      moderation: {
        title: 'Review moderation',
        subtitle: 'Moderate buyer and seller feedback to keep the marketplace trustworthy.',
        empty: 'No reviews are awaiting moderation.',
        approve: 'Approve',
        reject: 'Reject',
        notePlaceholder: 'Optional note for the author (8+ characters recommended).',
        ratingLabel: '{rating} pts',
        authorLabel: 'Submitted by: {author}',
        createdLabel: 'Submitted at: {time}'
      }
    }
  }
};

const serverMessageKeyMap = Object.freeze({
  '充值金额不能为空': 'serverMessages.wallet.amountRequired',
  '充值金额必须大于0': 'serverMessages.wallet.amountPositive',
  '支付金额必须大于0': 'serverMessages.wallet.paymentAmountPositive',
  '退款金额必须大于0': 'serverMessages.wallet.refundAmountPositive',
  '备注长度不能超过100个字符': 'serverMessages.wallet.referenceLength',
  '买家钱包余额不足': 'serverMessages.wallet.balanceInsufficient',
  '卖家钱包余额不足，无法完成退款': 'serverMessages.wallet.sellerInsufficient',
  '未找到指定用户账号': 'serverMessages.accounts.notFound',
  '买家账号不存在': 'serverMessages.accounts.buyerMissing',
  '卖家账号不存在': 'serverMessages.accounts.sellerMissing',
  '房源不存在': 'serverMessages.houses.notFound',
  '房源缺少卖家账号，无法发起支付': 'serverMessages.houses.missingSeller',
  '房源价格异常，无法预定': 'serverMessages.houses.priceInvalid',
  '房源尚未通过审核，暂不可交易': 'serverMessages.houses.notApproved',
  '该房源已有其他买家预定': 'serverMessages.orders.alreadyReserved',
  '您已预定该房源，请耐心等待卖家处理': 'serverMessages.orders.reservedSelf',
  '当前订单状态不支持退换': 'serverMessages.orders.statusUnsupported',
  '仅买家本人可以申请退换': 'serverMessages.orders.buyerOnlyReturn',
  '买家未完成实名认证，无法预定房源': 'errors.reserveVerifyFirst',
  '买家未完成实名认证，无法购买房源': 'errors.purchaseVerifyFirst',
  '房源尚未通过审核，暂不可预定。': 'errors.reserveNotApproved',
  '房源尚未通过审核，暂不可购买。': 'errors.purchaseNotApproved',
  '积分不足': 'prediction.errors.pointsInsufficient',
  '请求参数校验失败': 'serverMessages.generic.validationFailed',
  '填写19位数字': 'errors.installmentCardRequired'
});

const containsCJK = (text) => /[\u3400-\u9FFF]/.test(text);

const currentLocale = computed(() => settings.language || 'zh');

const translateServerMessage = (message, fallbackKey) => {
  if (!message) {
    return fallbackKey ? t(fallbackKey) : '';
  }
  const trimmed = String(message).trim();
  const key = serverMessageKeyMap[trimmed];
  if (key) {
    return t(key);
  }
  if (currentLocale.value === 'en' && containsCJK(trimmed)) {
    return fallbackKey ? t(fallbackKey) : t('errors.genericServerError');
  }
  return trimmed;
};

const resolveError = (error, fallbackKey) => {
  const detail = error?.response?.data;
  if (detail?.errors) {
    const firstError = Object.values(detail.errors)[0];
    const raw = Array.isArray(firstError) ? firstError[0] : firstError;
    return translateServerMessage(raw, fallbackKey);
  }
  return translateServerMessage(detail?.detail, fallbackKey);
};

const resolveTranslation = (locale, path) => {
  const segments = path.split('.');
  return segments.reduce((acc, key) => {
    if (acc && typeof acc === 'object' && key in acc) {
      return acc[key];
    }
    return undefined;
  }, translations[locale]);
};

const formatTranslation = (template, vars = {}) =>
  template.replace(/\{(\w+)\}/g, (_, key) => (vars[key] ?? `{${key}}`));

const t = (path, vars = {}) => {
  const locale = currentLocale.value;
  const fallback = 'zh';
  const template =
    resolveTranslation(locale, path) ?? resolveTranslation(fallback, path) ?? path;
  if (typeof template === 'string') {
    return formatTranslation(template, vars);
  }
  return template;
};

provide('translate', t);

const persistSettings = () => {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    window.localStorage.setItem(settingsStorageKey, JSON.stringify({ ...settings }));
  } catch (error) {
    console.warn(t('errors.persistSettings'), error);
  }
};

const applyTheme = (theme) => {
  if (typeof document === 'undefined') {
    return;
  }
  document.documentElement.setAttribute('data-theme', theme);
  document.body?.setAttribute('data-theme', theme);
};

const applyFontSize = (size) => {
  if (typeof document === 'undefined') {
    return;
  }
  document.documentElement.setAttribute('data-font', size);
};

const applyLanguage = (language) => {
  if (typeof document === 'undefined') {
    return;
  }
  const langCode = language === 'en' ? 'en' : 'zh-CN';
  document.documentElement.setAttribute('lang', langCode);
};

const updateDocumentLanguage = () => {
  applyLanguage(currentLocale.value);
};

watch(
  () => settings.theme,
  (theme) => {
    applyTheme(theme);
    persistSettings();
  },
  { immediate: true }
);

watch(
  () => settings.fontSize,
  (size) => {
    applyFontSize(size);
    persistSettings();
  },
  { immediate: true }
);

watch(
  () => settings.language,
  () => {
    persistSettings();
    updateDocumentLanguage();
  },
  { immediate: true }
);

watch(
  () => activeTab.value,
  (tab) => {
    updateDocumentLanguage();
  }
);

const parentheses = computed(() =>
  currentLocale.value === 'en'
    ? { left: '(', right: ')' }
    : { left: '（', right: '）' }
);

const formatCurrencyYuan = (value) => {
  const numeric = Number(value ?? 0);
  if (!Number.isFinite(numeric)) {
    return '0.00';
  }
  const locale = settings.language === 'en' ? 'en-US' : 'zh-CN';
  return numeric.toLocaleString(locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const sanitizeDigits = (value) => (value == null ? '' : String(value).replace(/\D/g, ''));

const formatLocalDateTime = (value) => {
  if (!value) {
    return '';
  }
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const locale = settings.language === 'en' ? 'en-US' : 'zh-CN';
  return date.toLocaleString(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  });
};

const convertLocalTimeToIso = (value) => {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toISOString();
};

const sellerRoles = ['SELLER', 'LANDLORD'];
const isSellerRole = (role) => sellerRoles.includes(role);

const roleLabels = computed(() => ({
  SELLER: t('roles.seller'),
  LANDLORD: t('roles.seller'),
  BUYER: t('roles.buyer'),
  ADMIN: t('roles.admin')
}));

const listingStatusLabels = computed(() => ({
  DRAFT: t('statuses.draft'),
  PENDING_REVIEW: t('statuses.pending'),
  APPROVED: t('statuses.approved'),
  REJECTED: t('statuses.rejected'),
  SOLD: t('statuses.sold')
}));

const orderProgressLabels = computed(() => ({
  DEPOSIT_PAID: t('orders.progress.DEPOSIT_PAID'),
  VIEWING_SCHEDULED: t('orders.progress.VIEWING_SCHEDULED'),
  FEEDBACK_SUBMITTED: t('orders.progress.FEEDBACK_SUBMITTED'),
  HANDOVER_COMPLETED: t('orders.progress.HANDOVER_COMPLETED'),
  FUNDS_RELEASED: t('orders.progress.FUNDS_RELEASED')
}));

const orderProgressSequence = Object.freeze([
  'DEPOSIT_PAID',
  'VIEWING_SCHEDULED',
  'FEEDBACK_SUBMITTED',
  'HANDOVER_COMPLETED',
  'FUNDS_RELEASED'
]);

const isSeller = computed(() => isSellerRole(currentUser.value?.role));
const isBuyer = computed(() => currentUser.value?.role === 'BUYER');
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');
const canUseMessaging = computed(() => {
  const role = currentUser.value?.role;
  return role === 'BUYER' || isSellerRole(role);
});
const isRealNameVerified = computed(() => Boolean(currentUser.value?.realNameVerified));
const canViewSensitiveInfo = computed(() => {
  const user = currentUser.value;
  if (!user) {
    return false;
  }
  if (user.role === 'ADMIN') {
    return true;
  }
  return Boolean(user.realNameVerified);
});

const canManageHouses = computed(() => isSeller.value);

const urgentTasks = computed(() => {
  if (!currentUser.value || (!isBuyer.value && !isSeller.value)) {
    return [];
  }
  const dismissed = new Set((dismissedUrgentTaskKeys.value ?? []).map((key) => String(key)));
  const role = currentUser.value.role;
  const now = Date.now();
  const soonThreshold = now + 72 * 60 * 60 * 1000;
  const list = [];
  const orderList = Array.isArray(orders.value) ? orders.value : [];

  orderList.forEach((order) => {
    if (!order) {
      return;
    }
    const stage = order.progressStage;
    const viewingDate = order.viewingTime ? new Date(order.viewingTime) : null;
    const validViewing = viewingDate && !Number.isNaN(viewingDate.getTime()) ? viewingDate : null;
    const timeValue = validViewing ? validViewing.getTime() : now;
    const timeLabel = validViewing ? formatLocalDateTime(validViewing) : null;
    const viewingSoon = validViewing ? timeValue <= soonThreshold : false;

    if (isSellerRole(role)) {
      if (stage === 'DEPOSIT_PAID') {
        list.push({
          key: `${order.id}-schedule`,
          orderId: order.id,
          houseTitle: order.houseTitle,
          stage,
          description: t('orders.urgent.awaitingScheduleSeller', {
            buyer: order.buyerDisplayName ?? order.buyerUsername
          }),
          timeLabel: null,
          timeValue: now - 1,
          isSoon: true
        });
        return;
      }
      if (stage === 'VIEWING_SCHEDULED' && validViewing) {
        list.push({
          key: `${order.id}-seller-viewing`,
          orderId: order.id,
          houseTitle: order.houseTitle,
          stage,
          description: t('orders.urgent.scheduledReminderSeller', { time: timeLabel }),
          timeLabel,
          timeValue,
          isSoon: viewingSoon
        });
        return;
      }
    }

    if (role === 'BUYER') {
      if (stage === 'DEPOSIT_PAID') {
        list.push({
          key: `${order.id}-awaiting`,
          orderId: order.id,
          houseTitle: order.houseTitle,
          stage,
          description: t('orders.urgent.awaitingScheduleBuyer'),
          timeLabel: null,
          timeValue: now - 1,
          isSoon: true
        });
        return;
      }
      if (stage === 'VIEWING_SCHEDULED' && validViewing) {
        list.push({
          key: `${order.id}-buyer-viewing`,
          orderId: order.id,
          houseTitle: order.houseTitle,
          stage,
          description: t('orders.urgent.scheduledReminderBuyer', { time: timeLabel }),
          timeLabel,
          timeValue,
          isSoon: viewingSoon
        });
      }
    }
  });

  return list
    .filter((task) => !dismissed.has(String(task.key ?? task.id)))
    .sort((a, b) => a.timeValue - b.timeValue)
    .slice(0, 5);
});

const showUrgentTasks = computed(() => isBuyer.value || isSeller.value);
const canAccessOrders = computed(() => showUrgentTasks.value || isAdmin.value);

const pendingReviewHouses = computed(() =>
  houses.value.filter((house) => house.status === 'PENDING_REVIEW')
);

const reviewLoading = computed(() => loading.value || adminLoading.value);

const pendingHouseReviews = computed(() =>
  houseReviews.value.filter((review) => review.status === 'PENDING')
);

const navigationTabs = computed(() => {
  const tabs = [
    { value: 'home', label: t('nav.home') },
    { value: 'assistant', label: t('nav.aiAssistant') }
  ];
  if (canManageHouses.value) {
    tabs.push({ value: 'manage', label: t('nav.manage') });
  }
  tabs.push({ value: 'feedback', label: t('nav.feedback') });
  if (showUrgentTasks.value) {
    tabs.push({ value: 'urgent', label: t('nav.urgent') });
  }
  if (canAccessOrders.value) {
    tabs.push({ value: 'orders', label: t('nav.orders') });
  }
  tabs.push({ value: 'account', label: t('nav.account') });
  if (isAdmin.value) {
    const pendingLabel = pendingReviewHouses.value.length
      ? t('nav.reviewWithCount', { count: pendingReviewHouses.value.length })
      : t('nav.review');
    tabs.push({ value: 'review', label: pendingLabel });
    tabs.push({ value: 'admin-houses', label: t('nav.adminHouses') });
    tabs.push({ value: 'admin', label: t('nav.admin') });
    tabs.push({ value: 'reputation', label: t('nav.reputation') });
  }
  return tabs;
});

watch(
  navigationTabs,
  (tabs) => {
    if (!tabs.some((tab) => tab.value === activeTab.value)) {
      activeTab.value = tabs[0]?.value ?? 'home';
    }
  },
  { immediate: true }
);

watch(
  () => activeTab.value,
  (value) => {
    if (value !== 'account') {
      accountError.value = '';
    }
  }
);

watch(
  () => canAccessOrders.value,
  (allowed) => {
    if (!allowed) {
      wallet.value = null;
      orders.value = [];
      walletLoading.value = false;
      ordersLoading.value = false;
    }
  }
);

watch(
  () => currentUser.value?.username,
  (username) => {
    if (!username) {
      dismissedUrgentTaskKeys.value = [];
      return;
    }
    dismissedUrgentTaskKeys.value = restoreDismissedUrgentTasks(username);
  },
  { immediate: true }
);

watch(
  houseReviews,
  (reviews) => {
    persistHouseReviews(reviews);
  },
  { deep: true }
);

const switchTab = (tab) => {
  activeTab.value = tab;
  if (tab === 'admin') {
    loadAdminOrders();
  }
  if (tab === 'reputation') {
    loadAdminData();
  }
  if (tab === 'review') {
    fetchHouses();
  }
};

const persistUser = (user) => {
  try {
    localStorage.setItem(storageKey, JSON.stringify(user));
  } catch (error) {
    console.warn(t('errors.persistUser'), error);
  }
};

const buildUrgentDismissedKey = (username) => `${urgentDismissedStoragePrefix}${username}`;

const restoreDismissedUrgentTasks = (username) => {
  if (typeof window === 'undefined' || !username) {
    return [];
  }
  try {
    const raw = window.localStorage.getItem(buildUrgentDismissedKey(username));
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => String(item));
    }
  } catch (error) {
    console.warn('Failed to restore urgent task preferences:', error);
  }
  return [];
};

const persistDismissedUrgentTasks = (username, keys) => {
  if (typeof window === 'undefined' || !username) {
    return;
  }
  try {
    window.localStorage.setItem(buildUrgentDismissedKey(username), JSON.stringify(keys));
  } catch (error) {
    console.warn('Failed to persist urgent task preferences:', error);
  }
};

const loadStoredReviews = () => {
  if (typeof window === 'undefined') {
    return [];
  }
  try {
    const raw = window.localStorage.getItem(reviewStorageKey);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed
        .map((item) => ({
          ...item,
          status: item.status === 'APPROVED' || item.status === 'REJECTED' ? item.status : 'PENDING'
        }))
        .filter((item) => item && item.id && item.houseId && item.content);
    }
  } catch (error) {
    console.warn('Failed to restore stored reviews:', error);
  }
  return [];
};

const persistHouseReviews = (reviews) => {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    window.localStorage.setItem(reviewStorageKey, JSON.stringify(reviews));
  } catch (error) {
    console.warn('Failed to persist review data:', error);
  }
};

houseReviews.value = loadStoredReviews();

const syncReviewHouseTitles = () => {
  if (!Array.isArray(houses.value) || houses.value.length === 0) {
    return;
  }
  const map = new Map(
    houses.value.filter((house) => house?.id != null).map((house) => [String(house.id), house])
  );
  let updated = false;
  houseReviews.value = houseReviews.value.map((review) => {
    const house = map.get(String(review.houseId));
    if (house && house.title && review.houseTitle !== house.title) {
      updated = true;
      return { ...review, houseTitle: house.title };
    }
    return review;
  });
  if (updated) {
    persistHouseReviews(houseReviews.value);
  }
};

const generateReviewId = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `review-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const normalizeHouse = (house) => ({
  ...house,
  price: house?.price != null ? Number(house.price) : null,
  downPayment: house?.downPayment != null ? Number(house.downPayment) : null,
  installmentMonthlyPayment:
    house?.installmentMonthlyPayment != null ? Number(house.installmentMonthlyPayment) : null,
  installmentMonths: house?.installmentMonths != null ? Number(house.installmentMonths) : null,
  latitude: house?.latitude != null ? Number(house.latitude) : null,
  longitude: house?.longitude != null ? Number(house.longitude) : null,
  listingDate: house?.listingDate ?? '',
  imageUrls: Array.isArray(house?.imageUrls) ? house.imageUrls : [],
  keywords: Array.isArray(house?.keywords) ? house.keywords : [],
  status: house?.status ?? 'PENDING_REVIEW',
  reviewMessage: house?.reviewMessage ?? '',
  reviewedBy: house?.reviewedBy ?? '',
  reviewedAt: house?.reviewedAt ?? '',
  sensitiveMasked: Boolean(house?.sensitiveMasked)
});

const buildFilterParams = (filters) => {
  const params = {};
  Object.entries(filters).forEach(([key, value]) => {
    if (key === 'sellerDisplayName') {
      return;
    }
    if (value === null || value === undefined) {
      return;
    }
    const trimmed = typeof value === 'string' ? value.trim() : value;
    if (trimmed !== '' && !Number.isNaN(trimmed)) {
      params[key] = trimmed;
    }
  });
  return params;
};

const assignFilters = (filters = {}) => {
  Object.entries(filters).forEach(([key, value]) => {
    if (key in houseFilters) {
      houseFilters[key] = value ?? '';
    }
  });
};

const fetchHouses = async ({ filters, silent = false } = {}) => {
  if (filters) {
    assignFilters(filters);
  }
  if (!silent) {
    loading.value = true;
  }
  messages.error = '';
  try {
    const params = buildFilterParams(houseFilters);
    if (currentUser.value?.username) {
      params.requester = currentUser.value.username;
    }
    const { data } = await client.get('/houses', { params });
    houses.value = data.map(normalizeHouse);
    syncReviewHouseTitles();
  } catch (error) {
    messages.error = resolveError(error, 'errors.loadHouses');
  } finally {
    if (!silent) {
      loading.value = false;
    }
  }
};

const loadRecommendations = async ({ silent = true } = {}) => {
  try {
    const { data } = await client.get('/reputations/recommended');
    recommendations.sellers = Array.isArray(data.sellers) ? data.sellers : [];
    recommendations.buyers = Array.isArray(data.buyers) ? data.buyers : [];
  } catch (error) {
    if (!silent) {
      messages.error = resolveError(error, 'errors.loadRecommendations');
    }
  }
};

const fetchWallet = async ({ silent = false } = {}) => {
  if (!currentUser.value || !canAccessOrders.value) {
    wallet.value = null;
    walletLoading.value = false;
    return;
  }
  if (!silent) {
    walletLoading.value = true;
  }
  try {
    const { data } = await client.get(`/wallets/${currentUser.value.username}`);
    wallet.value = normalizeWallet(data, wallet.value);
  } catch (error) {
    messages.error = resolveError(error, 'errors.loadWallet');
  } finally {
    if (!silent) {
      walletLoading.value = false;
    }
  }
};

const consumePredictionPoints = async (amount) => {
  if (!currentUser.value) {
    throw new Error(t('errors.walletLoginRequired'));
  }
  try {
    const { data } = await client.post(`/wallets/${currentUser.value.username}/points/consume`, {
      amount,
      reason: 'PRICE_PREDICTION'
    });
    wallet.value = normalizeWallet(data, wallet.value);
    return wallet.value;
  } catch (error) {
    if (error?.response?.status === 404 || error?.response?.status === 405) {
      throw new Error(t('prediction.errors.pointsUnsupported'));
    }
    const message = resolveError(error, 'errors.consumePoints');
    throw new Error(message);
  }
};

const fetchOrders = async ({ silent = false } = {}) => {
  if (!currentUser.value || !canAccessOrders.value) {
    orders.value = [];
    ordersLoading.value = false;
    return;
  }
  if (!silent) {
    ordersLoading.value = true;
  }
  try {
    const { data } = await client.get(`/orders/by-user/${currentUser.value.username}`);
    orders.value = data;
  } catch (error) {
    messages.error = resolveError(error, 'errors.loadOrders');
  } finally {
    if (!silent) {
      ordersLoading.value = false;
    }
  }
};

const resetConversationState = () => {
  conversations.value = [];
  activeConversationId.value = null;
  conversationMessages.value = [];
  conversationPanelVisible.value = false;
  conversationListLoading.value = false;
  conversationMessagesLoading.value = false;
  conversationSending.value = false;
  conversationError.value = '';
  conversationPrefill.value = '';
};

const loadConversations = async ({ silent = false } = {}) => {
  if (!currentUser.value || !canUseMessaging.value) {
    resetConversationState();
    return;
  }
  const previousId = activeConversationId.value;
  if (!silent) {
    conversationListLoading.value = true;
  }
  conversationError.value = '';
  try {
    const { data } = await client.get('/conversations', {
      params: { username: currentUser.value.username }
    });
    const list = Array.isArray(data) ? data : [];
    conversations.value = list;
    if (activeConversationId.value && !list.some((item) => item.id === activeConversationId.value)) {
      activeConversationId.value = list.length > 0 ? list[0].id : null;
    }
    if (!activeConversationId.value) {
      conversationMessages.value = [];
    } else if (previousId !== activeConversationId.value && conversationPanelVisible.value) {
      await loadConversationMessages(activeConversationId.value, { silent: true });
    }
  } catch (error) {
    conversationError.value = resolveError(error, 'errors.loadConversations');
  } finally {
    if (!silent) {
      conversationListLoading.value = false;
    }
  }
};

const loadConversationMessages = async (conversationId, { silent = false } = {}) => {
  if (!conversationId || !currentUser.value) {
    conversationMessages.value = [];
    return;
  }
  if (!silent) {
    conversationMessagesLoading.value = true;
  }
  conversationError.value = '';
  try {
    const { data } = await client.get(`/conversations/${conversationId}/messages`, {
      params: { requester: currentUser.value.username }
    });
    conversationMessages.value = Array.isArray(data) ? data : [];
  } catch (error) {
    conversationError.value = resolveError(error, 'errors.loadMessages');
  } finally {
    if (!silent) {
      conversationMessagesLoading.value = false;
    }
  }
};

const openConversationPanel = async () => {
  if (!currentUser.value || !canUseMessaging.value) {
    messages.error = t('errors.messagingUnsupported');
    return;
  }
  conversationPanelVisible.value = true;
  conversationError.value = '';
  await loadConversations({ silent: true });
  if (!activeConversationId.value && conversations.value.length > 0) {
    activeConversationId.value = conversations.value[0].id;
  }
  if (activeConversationId.value) {
    await loadConversationMessages(activeConversationId.value, { silent: true });
  }
};

const handleSelectConversation = async (conversationId) => {
  activeConversationId.value = conversationId;
  if (!conversationId) {
    conversationMessages.value = [];
    return;
  }
  await loadConversationMessages(conversationId);
};

const handleSendConversationMessage = async ({ conversationId, content }) => {
  if (!conversationId || !currentUser.value) {
    return;
  }
  conversationSending.value = true;
  conversationError.value = '';
  try {
    await client.post(`/conversations/${conversationId}/messages`, {
      senderUsername: currentUser.value.username,
      content
    });
    await Promise.all([
      loadConversationMessages(conversationId, { silent: true }),
      loadConversations({ silent: true })
    ]);
  } catch (error) {
    conversationError.value = resolveError(error, 'errors.sendMessage');
  } finally {
    conversationSending.value = false;
  }
};

const handleVerificationUpdate = async (response) => {
  const updated = {
    ...(currentUser.value ?? {}),
    ...response
  };
  currentUser.value = updated;
  persistUser(updated);
  messages.success = response.message ?? t('success.verificationUpdated');
  messages.error = '';
  if (updated.realNameVerified && activeTab.value !== 'account') {
    activeTab.value = 'account';
  }
  await Promise.all([
    fetchHouses({ silent: true }),
    fetchWallet({ silent: true }),
    fetchOrders({ silent: true })
  ]);
  await loadRecommendations({ silent: true });
};

const openConversation = async ({ buyerUsername, sellerUsername, initialMessage = '', prefill = '' }) => {
  if (!currentUser.value || !canUseMessaging.value) {
    conversationError.value = t('errors.messagingUnsupported');
    return;
  }
  if (
    currentUser.value.username !== buyerUsername &&
    currentUser.value.username !== sellerUsername
  ) {
    conversationError.value = t('errors.messagingUnsupported');
    return;
  }
  conversationPanelVisible.value = true;
  conversationError.value = '';
  const payload = { buyerUsername, sellerUsername };
  const trimmedMessage = typeof initialMessage === 'string' ? initialMessage.trim() : '';
  if (trimmedMessage) {
    payload.initialMessage = trimmedMessage;
  }
  try {
    const { data } = await client.post('/conversations', payload);
    activeConversationId.value = data.id;
    conversationPrefill.value = prefill ?? '';
    await Promise.all([
      loadConversationMessages(data.id),
      loadConversations({ silent: true })
    ]);
  } catch (error) {
    conversationError.value = resolveError(error, 'errors.createConversation');
    conversationPrefill.value = '';
  }
};

const handleContactSeller = async ({ sellerUsername }) => {
  if (!isBuyer.value || !currentUser.value) {
    messages.error = t('errors.contactSellerBuyerOnly');
    messages.success = '';
    return;
  }
  if (!isRealNameVerified.value) {
    messages.error = t('errors.contactSellerVerifyFirst');
    messages.success = '';
    return;
  }
  await openConversation({
    buyerUsername: currentUser.value.username,
    sellerUsername
  });
};

const handleViewingConfirmation = async ({ orderId }) => {
  if (!isBuyer.value || !currentUser.value) {
    return;
  }
  const list = Array.isArray(orders.value) ? orders.value : [];
  const order = list.find((item) => String(item.id) === String(orderId));
  if (!order) {
    return;
  }
  const timeLabel = order.viewingTime ? formatLocalDateTime(order.viewingTime) : '';
  const message = timeLabel
    ? t('orders.quickMessages.confirmViewing', { time: timeLabel })
    : t('orders.quickMessages.confirmViewingNoTime');
  await openConversation({
    buyerUsername: currentUser.value.username,
    sellerUsername: order.sellerUsername,
    prefill: message
  });
};

const handleScheduleViewing = async ({ orderId, viewingTime, message }) => {
  if (!isSeller.value || !currentUser.value) {
    messages.error = t('errors.scheduleViewing');
    messages.success = '';
    return;
  }
  const isoTime = convertLocalTimeToIso(viewingTime);
  if (!isoTime) {
    messages.error = t('errors.scheduleViewingInvalid');
    messages.success = '';
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const payload = {
      sellerUsername: currentUser.value.username,
      viewingTime: isoTime
    };
    const remark = typeof message === 'string' ? message.trim() : '';
    if (remark) {
      payload.message = remark;
    }
    const { data } = await client.post(`/orders/${orderId}/viewing`, payload);
    const timeLabel = formatLocalDateTime(data.viewingTime ?? isoTime);
    messages.success = t('success.viewingScheduled', {
      title: data.houseTitle,
      time: timeLabel || formatLocalDateTime(isoTime)
    });
    await Promise.all([
      fetchOrders({ silent: true }),
      loadConversations({ silent: true })
    ]);
  } catch (error) {
    messages.error = resolveError(error, 'errors.scheduleViewing');
  } finally {
    ordersLoading.value = false;
  }
};

const handleAdvanceProgress = async ({ orderId, stage }) => {
  if (!isSeller.value || !currentUser.value || !stage) {
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post(`/orders/${orderId}/progress`, {
      requesterUsername: currentUser.value.username,
      stage
    });
    const stageLabel = orderProgressLabels.value?.[data.progressStage] ?? data.progressStage;
    messages.success = t('success.progressUpdated', { stage: stageLabel });
    await fetchOrders({ silent: true });
  } catch (error) {
    messages.error = resolveError(error, 'errors.progressUpdate');
  } finally {
    ordersLoading.value = false;
  }
};

const closeConversationPanel = () => {
  conversationPanelVisible.value = false;
  conversationPrefill.value = '';
};

const handleConversationPrefillConsumed = () => {
  conversationPrefill.value = '';
};

const refreshCurrentUser = async ({ silent = true } = {}) => {
  if (!currentUser.value) {
    return;
  }
  const username = currentUser.value.username;
  const endpoints = [`/auth/profile/${username}`, '/auth/profile'];
  let response = null;
  for (const endpoint of endpoints) {
    try {
      response = await client.get(endpoint);
      break;
    } catch (error) {
      if (!shouldRetryProfileFetch(error)) {
        if (!silent) {
          messages.error = resolveError(error, 'errors.refreshUser');
        }
        return;
      }
    }
  }
  if (!response) {
    return;
  }
  const data = response.data;
  const updated = {
    ...currentUser.value,
    ...data,
    message: data.message ?? currentUser.value.message
  };
  currentUser.value = updated;
  persistUser(updated);
};

const guardReadOnly = () => {
  if (isAdmin.value) {
    return true;
  }
  if (!canManageHouses.value) {
    messages.error = t('errors.manageHousesPermission');
    messages.success = '';
    return false;
  }
  return true;
};

const normalizeHousePayload = (payload, { draft = false } = {}) => {
  const result = { ...payload };
  result.imageUrls = Array.isArray(result.imageUrls)
    ? result.imageUrls.map((url) => (url ?? '').trim()).filter((url) => url.length > 0)
    : [];
  result.keywords = Array.isArray(result.keywords)
    ? result.keywords
        .map((keyword) => (keyword ?? '').trim())
        .filter((keyword) => keyword.length > 0)
    : [];
  result.price = Number(result.price ?? 0);
  result.downPayment = Number(result.downPayment ?? 0);
  result.installmentMonthlyPayment = Number(result.installmentMonthlyPayment ?? 0);
  result.installmentMonths = Number.parseInt(result.installmentMonths ?? 0, 10);
  if (!Number.isFinite(result.price)) {
    result.price = 0;
  }
  if (!Number.isFinite(result.downPayment)) {
    result.downPayment = 0;
  }
  if (!Number.isFinite(result.installmentMonthlyPayment)) {
    result.installmentMonthlyPayment = 0;
  }
  if (!Number.isFinite(result.installmentMonths)) {
    result.installmentMonths = 0;
  }
  if (isSeller.value) {
    result.sellerUsername = currentUser.value.username;
    if (!result.sellerName) {
      result.sellerName = currentUser.value.displayName ?? '';
    }
  }
  result.saveAsDraft = draft;
  return result;
};

const handleSubmit = async ({ payload, draft }) => {
  if (!guardReadOnly()) {
    return;
  }
  loading.value = true;
  messages.error = '';
  messages.success = '';
  const requestPayload = normalizeHousePayload(payload, { draft: Boolean(draft) });
  try {
    if (selectedHouse.value) {
      const { data } = await client.put(`/houses/${selectedHouse.value.id}`, requestPayload);
      const statusLabel = listingStatusLabels.value[data.status] ?? t('statuses.pending');
      if (data.status === 'DRAFT') {
        messages.success = t('success.houseDraftUpdated', { title: data.title });
      } else if (data.status === 'APPROVED') {
        messages.success = t('success.houseUpdatedApproved', { title: data.title });
      } else {
        messages.success = t('success.houseUpdatedPending', { title: data.title, status: statusLabel });
      }
    } else {
      const { data } = await client.post('/houses', requestPayload);
      const statusLabel = listingStatusLabels.value[data.status] ?? t('statuses.pending');
      if (data.status === 'DRAFT') {
        messages.success = t('success.houseDraftCreated', { title: data.title });
      } else if (data.status === 'APPROVED') {
        messages.success = t('success.houseCreatedApproved', { title: data.title });
      } else {
        messages.success = t('success.houseCreatedPending', { title: data.title, status: statusLabel });
      }
    }
    formResetKey.value += 1;
    selectedHouse.value = null;
    await fetchHouses({ silent: true });
    await loadRecommendations();
  } catch (error) {
    messages.error = resolveError(error, 'errors.saveHouse');
  } finally {
    loading.value = false;
  }
};

const handleEdit = (house) => {
  if (isSeller.value && currentUser.value.username !== house.sellerUsername) {
    messages.error = t('errors.editOwnHouse');
    return;
  }
  selectedHouse.value = {
    ...house,
    imageUrls: Array.isArray(house.imageUrls) ? [...house.imageUrls] : []
  };
  messages.error = '';
};

const handleCancel = () => {
  selectedHouse.value = null;
};

const handleRemove = async (house) => {
  if (!guardReadOnly()) {
    return;
  }
  if (isSeller.value && currentUser.value.username !== house.sellerUsername) {
    messages.error = t('errors.deleteOwnHouse');
    return;
  }
  if (!window.confirm(t('prompts.deleteHouseConfirm', { title: house.title }))) {
    return;
  }
  loading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    await client.delete(`/houses/${house.id}`, {
      params: { requester: currentUser.value.username }
    });
    messages.success = t('success.deleteHouse', { title: house.title });
    await fetchHouses({ silent: true });
    await loadRecommendations();
  } catch (error) {
    messages.error = resolveError(error, 'errors.deleteHouse');
  } finally {
    loading.value = false;
  }
};

const handleAdminDeleteRequest = (house) => {
  if (!isAdmin.value || !house) {
    return;
  }
  handleRemove(house);
};

const handleAdminUnlist = async (house) => {
  if (!isAdmin.value || !house?.id || !currentUser.value) {
    return;
  }
  const defaultReason = t('prompts.unlistDefaultReason');
  const promptLabel = t('prompts.unlistReason', { title: house.title ?? '' });
  const input = window.prompt(promptLabel, defaultReason);
  if (input === null) {
    return;
  }
  const reason = (input.trim() || defaultReason).slice(0, 200);

  adminLoading.value = true;
  messages.error = '';
  messages.success = '';

  const attempts = [
    () =>
      client.patch(`/houses/${house.id}/status`, {
        status: 'SOLD',
        operator: currentUser.value.username,
        reason
      }),
    () =>
      client.patch(`/houses/${house.id}/review`, {
        reviewerUsername: currentUser.value.username,
        status: 'REJECTED',
        message: reason
      })
  ];

  try {
    let response = null;
    for (const attempt of attempts) {
      try {
        response = await attempt();
        if (response?.data) {
          break;
        }
      } catch (error) {
        if (attempt === attempts[attempts.length - 1]) {
          throw error;
        }
        console.warn('Primary unlist request failed, retrying with fallback endpoint', error);
      }
    }

    if (!response?.data) {
      throw new Error('empty-response');
    }

    const data = response.data;
    const statusLabel = listingStatusLabels.value[data.status] ?? data.status ?? '';
    messages.success = t('success.unlistHouse', {
      title: data.title ?? house.title ?? '',
      status: statusLabel,
      reason
    });

    await Promise.all([
      fetchHouses({ silent: true }),
      loadRecommendations(),
      loadAdminData()
    ]);
  } catch (error) {
    messages.error = resolveError(error, 'errors.unlistHouse');
  } finally {
    adminLoading.value = false;
  }
};

const handlePurchase = async ({ house, paymentMethod, installmentCardNumber }) => {
  if (!isBuyer.value) {
    messages.error = t('errors.purchaseBuyerOnly');
    messages.success = '';
    return;
  }
  if (!isRealNameVerified.value) {
    messages.error = t('errors.purchaseVerifyFirst');
    messages.success = '';
    return;
  }
  if (!house || house.status !== 'APPROVED') {
    messages.error = t('errors.purchaseNotApproved');
    return;
  }
  if (!paymentMethod) {
    messages.error = t('errors.purchaseSelectMethod');
    return;
  }
  let sanitizedCardNumber = '';
  if (paymentMethod === 'INSTALLMENT') {
    sanitizedCardNumber = sanitizeDigits(installmentCardNumber);
    if (sanitizedCardNumber.length !== 19) {
      messages.error = t('errors.installmentCardRequired');
      messages.success = '';
      return;
    }
  }
  if (isSeller.value && currentUser.value.username === house.sellerUsername) {
    messages.error = t('errors.purchaseOwnListing');
    messages.success = '';
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const payload = {
      houseId: house.id,
      buyerUsername: currentUser.value.username,
      paymentMethod
    };
    if (paymentMethod === 'INSTALLMENT') {
      payload.installmentCardNumber = sanitizedCardNumber;
    }
    const { data } = await client.post('/orders', payload);
    const payment = formatCurrencyYuan(data.amount);
    const methodLabel =
      data.paymentMethod === 'INSTALLMENT' ? t('payments.installment') : t('payments.full');
    messages.success = t('success.purchase', {
      method: methodLabel,
      title: data.houseTitle,
      amount: payment
    });
    await fetchWallet({ silent: true });
    await fetchOrders({ silent: true });
    await refreshCurrentUser({ silent: true });
    await loadRecommendations();
    await fetchHouses({ silent: true });
  } catch (error) {
    messages.error = resolveError(error, 'errors.purchaseFailed');
  } finally {
    ordersLoading.value = false;
  }
};

const handleUrgentTaskRead = (taskKey) => {
  const username = currentUser.value?.username;
  if (!username) {
    return;
  }
  const key = String(taskKey);
  const existing = new Set((dismissedUrgentTaskKeys.value ?? []).map((item) => String(item)));
  if (existing.has(key)) {
    return;
  }
  existing.add(key);
  const updated = Array.from(existing);
  dismissedUrgentTaskKeys.value = updated;
  persistDismissedUrgentTasks(username, updated);
};

const handleUrgentTaskNavigate = (task) => {
  if (!task) {
    return;
  }
  activeTab.value = 'orders';
};

const handleReserve = async (house) => {
  if (!isBuyer.value) {
    messages.error = t('errors.reserveBuyerOnly');
    messages.success = '';
    return;
  }
  if (!isRealNameVerified.value) {
    messages.error = t('errors.reserveVerifyFirst');
    messages.success = '';
    return;
  }
  if (house?.reservationActive) {
    messages.error = house.reservationOwnedByRequester
      ? t('serverMessages.orders.reservedSelf')
      : t('serverMessages.orders.alreadyReserved');
    messages.success = '';
    return;
  }
  if (!house || house.status !== 'APPROVED') {
    messages.error = t('errors.reserveNotApproved');
    messages.success = '';
    return;
  }
  reservationLoading.value = true;
  reservationTarget.value = house.id;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post('/orders/reserve', {
      houseId: house.id,
      buyerUsername: currentUser.value.username
    });
    const deposit = formatCurrencyYuan(data.amount);
    messages.success = t('success.reservation', {
      title: data.houseTitle,
      amount: deposit
    });
    await fetchWallet({ silent: true });
    await fetchOrders({ silent: true });
    await refreshCurrentUser({ silent: true });
    await loadRecommendations();
  } catch (error) {
    messages.error = resolveError(error, 'errors.reserveFailed');
  } finally {
    reservationLoading.value = false;
    reservationTarget.value = null;
  }
};

const handleTopUp = async ({ amount, reference }) => {
  if (!currentUser.value) {
    messages.error = t('errors.walletLoginRequired');
    messages.success = '';
    return;
  }
  walletLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const endpoint = `/wallets/${currentUser.value.username}/top-up`;
    const basePayload = { amount, reference };
    const attempts = [
      { payload: { ...basePayload, requesterUsername: currentUser.value.username }, allowFallback: true },
      { payload: basePayload, allowFallback: false }
    ];
    let response = null;
    let lastError = null;
    for (const attempt of attempts) {
      try {
        response = await client.post(endpoint, attempt.payload);
        break;
      } catch (error) {
        lastError = error;
        if (attempt.allowFallback && shouldRetryWithoutRequester(error)) {
          continue;
        }
        throw error;
      }
    }
    if (!response) {
      throw lastError ?? new Error('Top-up failed');
    }
    const { data } = response;
    wallet.value = normalizeWallet(data, wallet.value);
    const bonusPoints = Math.floor((Number(amount) || 0) / 100) * 10;
    messages.success = t('success.walletTopUp', {
      amount: formatCurrencyYuan(amount),
      points: bonusPoints
    });
  } catch (error) {
    messages.error = resolveError(error, 'errors.walletTopUp');
  } finally {
    walletLoading.value = false;
  }
};

const handleAccountSubmit = async (payload = {}) => {
  if (!currentUser.value) {
    accountError.value = t('errors.accountLoginRequired');
    return;
  }
  if (!payload || Object.keys(payload).length === 0) {
    accountError.value = t('accountCenter.errors.noChanges');
    return;
  }
  accountSaving.value = true;
  accountError.value = '';
  messages.error = '';
  try {
    const username = currentUser.value.username;
    const attempts = [
      { url: `/auth/profile/${username}`, payload: { ...payload, requesterUsername: username }, allowFallback: true },
      { url: `/auth/profile/${username}`, payload: { ...payload }, allowFallback: false },
      { url: '/auth/profile', payload: { ...payload, requesterUsername: username }, allowFallback: true },
      { url: '/auth/profile', payload: { ...payload }, allowFallback: false }
    ];
    let response = null;
    let lastError = null;
    for (const attempt of attempts) {
      try {
        response = await client.patch(attempt.url, attempt.payload);
        break;
      } catch (error) {
        lastError = error;
        if (attempt.allowFallback && shouldRetryWithoutRequester(error)) {
          continue;
        }
        if (shouldRetryProfileFetch(error)) {
          continue;
        }
        throw error;
      }
    }
    if (!response) {
      throw lastError ?? new Error(t('errors.updateAccount'));
    }
    const { data } = response;
    const merged = { ...currentUser.value, ...data };
    currentUser.value = merged;
    persistUser(merged);
    messages.success = t('success.accountUpdated');
    await Promise.all([
      refreshCurrentUser({ silent: true }),
      fetchWallet({ silent: true })
    ]);
  } catch (error) {
    accountError.value = resolveError(error, 'errors.updateAccount');
  } finally {
    accountSaving.value = false;
  }
};

const handleRequestReturn = async ({ orderId, reason }) => {
  if (!currentUser.value) {
    messages.error = t('errors.returnLoginRequired');
    messages.success = '';
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post(`/orders/${orderId}/return`, {
      requesterUsername: currentUser.value.username,
      reason
    });
    messages.success = t('success.orderReturned', { title: data.houseTitle });
    await fetchWallet({ silent: true });
    await fetchOrders({ silent: true });
    await refreshCurrentUser({ silent: true });
    await loadRecommendations();
  } catch (error) {
    messages.error = resolveError(error, 'errors.returnFailed');
  } finally {
    ordersLoading.value = false;
  }
};

const handleFilterSearch = (filters) => {
  fetchHouses({ filters });
};

const handleToggleBlacklist = async ({ username, blacklisted }) => {
  if (!isAdmin.value || !currentUser.value) {
    return;
  }
  adminLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    await client.patch(`/admin/users/${username}/blacklist`, {
      requesterUsername: currentUser.value.username,
      blacklisted
    });
    messages.success = blacklisted
      ? t('success.blacklistAdded', { username })
      : t('success.blacklistRemoved', { username });
    await loadAdminData();
    await loadRecommendations();
  } catch (error) {
    messages.error = resolveError(error, 'errors.updateBlacklist');
  } finally {
    adminLoading.value = false;
  }
};

const handleDeleteUser = async ({ username }) => {
  if (!isAdmin.value || !currentUser.value || !username || username === currentUser.value.username) {
    return;
  }
  const confirmed = window.confirm(t('prompts.deleteAccountConfirm', { username }));
  if (!confirmed) {
    return;
  }
  adminLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    await client.delete(`/admin/users/${username}`, {
      params: { requester: currentUser.value.username }
    });
    messages.success = t('success.accountDeleted', { username });
    await Promise.all([
      loadAdminData(),
      loadRecommendations()
    ]);
    await fetchHouses({ silent: true });
  } catch (error) {
    messages.error = resolveError(error, 'errors.deleteAccount');
  } finally {
    adminLoading.value = false;
  }
};

const handleReview = async ({ houseId, status }) => {
  if (!isAdmin.value || !currentUser.value || !houseId || !status) {
    return;
  }
  const target = houses.value.find((item) => item.id === houseId);
  let reviewMessage = '';
  if (status === 'REJECTED') {
    const reason = window.prompt(t('prompts.reviewRejectReason'), target?.reviewMessage ?? '');
    if (reason === null) {
      return;
    }
    reviewMessage = reason.trim();
    if (!reviewMessage) {
      messages.error = t('errors.reviewRequireReason');
      return;
    }
  } else {
    const remark = window.prompt(
      t('prompts.reviewRemark'),
      target?.reviewMessage ?? t('prompts.reviewDefaultRemark')
    );
    if (remark === null) {
      return;
    }
    reviewMessage = remark.trim();
  }

  adminLoading.value = true;
  messages.error = '';
  try {
    const { data } = await client.patch(`/houses/${houseId}/review`, {
      reviewerUsername: currentUser.value.username,
      status,
      message: reviewMessage
    });
    const statusLabel = listingStatusLabels.value[data.status] ?? '';
    messages.success =
      data.status === 'APPROVED'
        ? t('success.reviewApproved', { title: data.title })
        : t('success.reviewRejected', {
            title: data.title,
            status: statusLabel,
            reason: data.reviewMessage ?? (reviewMessage || t('prompts.reviewFallbackReason'))
          });
    await Promise.all([
      fetchHouses({ silent: true }),
      loadAdminData()
    ]);
  } catch (error) {
    messages.error = resolveError(error, 'errors.submitReview');
  } finally {
    adminLoading.value = false;
  }
};

const handleSubmitHouseReview = (payload) => {
  if (!currentUser.value) {
    messages.error = t('errors.reviewLoginRequired');
    messages.success = '';
    return;
  }
  const houseId = payload?.houseId;
  const rating = Number(payload?.rating ?? 0);
  const content = typeof payload?.content === 'string' ? payload.content.trim() : '';
  if (!houseId) {
    messages.error = t('errors.reviewHouseMissing');
    messages.success = '';
    return;
  }
  if (!Number.isFinite(rating) || rating < 1 || rating > 5) {
    messages.error = t('reviews.validation.ratingRequired');
    messages.success = '';
    return;
  }
  if (content.length < 8) {
    messages.error = t('errors.reviewContentTooShort');
    messages.success = '';
    return;
  }
  const normalizedHouseId = String(houseId);
  const targetHouse = houses.value.find((item) => String(item.id) === normalizedHouseId);
  if (!targetHouse) {
    messages.error = t('errors.reviewHouseMissing');
    messages.success = '';
    return;
  }
  const review = {
    id: generateReviewId(),
    houseId: normalizedHouseId,
    houseTitle: targetHouse.title ?? '',
    rating: Math.round(rating),
    content,
    status: 'PENDING',
    createdAt: new Date().toISOString(),
    authorUsername: currentUser.value.username,
    authorDisplayName: currentUser.value.displayName ?? currentUser.value.username,
    moderatedAt: null,
    moderatedBy: '',
    moderationNote: ''
  };
  houseReviews.value = [review, ...houseReviews.value];
  messages.error = '';
  messages.success = t('success.reviewSubmitted');
};

const handleModerateHouseReview = ({ reviewId, status, note }) => {
  if (!isAdmin.value || !currentUser.value) {
    messages.error = t('errors.reviewModerationDenied');
    messages.success = '';
    return;
  }
  const targetIndex = houseReviews.value.findIndex((item) => item.id === reviewId);
  if (targetIndex === -1) {
    messages.error = t('errors.reviewNotFound');
    messages.success = '';
    return;
  }
  const normalizedStatus = status === 'APPROVED' ? 'APPROVED' : 'REJECTED';
  const moderationNote = typeof note === 'string' ? note.trim() : '';
  const updatedReview = {
    ...houseReviews.value[targetIndex],
    status: normalizedStatus,
    moderatedAt: new Date().toISOString(),
    moderatedBy: currentUser.value.username,
    moderationNote
  };
  const updated = [...houseReviews.value];
  updated.splice(targetIndex, 1, updatedReview);
  houseReviews.value = updated;
  messages.error = '';
  messages.success =
    normalizedStatus === 'APPROVED'
      ? t('success.reviewModeratedApproved')
      : t('success.reviewModeratedRejected');
};

const loadAdminOrders = async ({ silent = false } = {}) => {
  if (!isAdmin.value || !currentUser.value) {
    adminPendingOrders.value = [];
    adminOrdersLoading.value = false;
    return;
  }
  if (!silent) {
    adminOrdersLoading.value = true;
  }
  try {
    const { data } = await client.get('/admin/orders/pending', {
      params: { requester: currentUser.value.username }
    });
    adminPendingOrders.value = Array.isArray(data) ? data : [];
  } catch (error) {
    messages.error = resolveError(error, 'errors.loadAdminOrders');
  } finally {
    if (!silent) {
      adminOrdersLoading.value = false;
    }
  }
};

const handleAdminOrderRelease = async ({ orderId, decision }) => {
  if (!isAdmin.value || !currentUser.value || !orderId || !decision) {
    return;
  }
  adminOrdersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    await client.patch(`/admin/orders/${orderId}/review`, {
      requesterUsername: currentUser.value.username,
      decision
    });
    messages.success =
      decision === 'SELLER'
        ? t('success.orderReleasedSeller')
        : t('success.orderReleasedBuyer');
    await Promise.all([
      loadAdminOrders({ silent: true }),
      fetchOrders({ silent: true }),
      fetchWallet({ silent: true })
    ]);
  } catch (error) {
    messages.error = resolveError(error, 'errors.reviewOrder');
  } finally {
    adminOrdersLoading.value = false;
  }
};

const loadAdminData = async () => {
  if (!isAdmin.value || !currentUser.value) {
    adminUsers.value = [];
    adminReputation.value = null;
    return;
  }
  adminLoading.value = true;
  try {
    const [usersRes, reputationRes] = await Promise.all([
      client.get('/admin/users', { params: { requester: currentUser.value.username } }),
      client.get('/admin/reputations', { params: { requester: currentUser.value.username } })
    ]);
    adminUsers.value = Array.isArray(usersRes.data) ? usersRes.data : [];
    adminReputation.value = reputationRes.data ?? null;
  } catch (error) {
    messages.error = resolveError(error, 'errors.loadReputation');
  } finally {
    adminLoading.value = false;
  }
};

const handleLoginSuccess = (user) => {
  currentUser.value = user;
  resetConversationState();
  const reputationText =
    user.reputationScore != null
      ? t('success.loginReputationSuffix', { score: user.reputationScore })
      : '';
  const loginMessage = user.message ?? t('success.login');
  messages.success = `${loginMessage}${reputationText}`;
  messages.error = '';
  persistUser(user);
  activeTab.value = 'home';
  fetchHouses();
  fetchWallet();
  fetchOrders();
  loadRecommendations({ silent: false });
  if (user.role === 'BUYER' || isSellerRole(user.role)) {
    loadConversations({ silent: true });
  }
  if (user.role === 'ADMIN') {
    loadAdminData();
    loadAdminOrders();
  }
};

const handleLogout = () => {
  currentUser.value = null;
  houses.value = [];
  selectedHouse.value = null;
  wallet.value = null;
  orders.value = [];
  recommendations.sellers = [];
  recommendations.buyers = [];
  adminUsers.value = [];
  adminReputation.value = null;
  adminPendingOrders.value = [];
  walletLoading.value = false;
  ordersLoading.value = false;
  reservationLoading.value = false;
  messages.error = '';
  messages.success = '';
  activeTab.value = 'home';
  localStorage.removeItem(storageKey);
  resetConversationState();
  fetchHouses();
  loadRecommendations();
};

watch(
  () => currentUser.value?.role,
  (role) => {
    if (role === 'ADMIN') {
      loadAdminData();
      loadAdminOrders();
    } else {
      adminUsers.value = [];
      adminReputation.value = null;
      adminPendingOrders.value = [];
    }
    if (role === 'BUYER' || isSellerRole(role)) {
      loadConversations({ silent: true });
    } else {
      resetConversationState();
    }
  }
);

onMounted(() => {
  try {
    const cached = localStorage.getItem(storageKey);
    if (cached) {
      const user = JSON.parse(cached);
      currentUser.value = user;
      messages.success = t('success.restoredSession');
      fetchHouses();
      fetchWallet();
      fetchOrders();
      loadRecommendations();
      if (user.role === 'ADMIN') {
        loadAdminData();
      }
      return;
    }
  } catch (error) {
    console.warn(t('errors.restoreSession'), error);
    localStorage.removeItem(storageKey);
  }
  fetchHouses();
  loadRecommendations();
});
</script>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  margin: 0;
  width: 100%;
  max-width: none;
  padding: clamp(2rem, 4vw, 3.5rem);
  gap: 2rem;
  background: var(--gradient-surface);
  border-radius: 0;
  border: none;
  box-shadow: none;
  backdrop-filter: blur(var(--glass-blur));
  position: relative;
  overflow: hidden;
}

.app::before,
.app::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.55;
  pointer-events: none;
}

.app::before {
  width: 420px;
  height: 420px;
  background: radial-gradient(circle, rgba(204, 188, 172, 0.45), rgba(204, 188, 172, 0));
  top: -160px;
  right: -110px;
}

.app::after {
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(168, 178, 188, 0.35), rgba(168, 178, 188, 0));
  bottom: -140px;
  left: -100px;
}

.header {
  background: linear-gradient(130deg, rgba(180, 140, 110, 0.92), rgba(154, 161, 168, 0.9));
  color: var(--color-text-on-emphasis);
  padding: 2.35rem;
  border-radius: calc(var(--radius-lg) + 0.35rem);
  box-shadow: 0 26px 60px rgba(150, 132, 118, 0.28);
  display: grid;
  gap: 1.5rem;
  position: relative;
  overflow: visible;
}

.header::before,
.header::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
  pointer-events: none;
}

.header::before {
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.45), transparent 70%);
  top: -160px;
  left: -80px;
}

.header::after {
  width: 380px;
  height: 380px;
  background: radial-gradient(circle at top right, rgba(196, 178, 162, 0.55), transparent 70%);
  top: -180px;
  right: -120px;
}

.branding {
  position: relative;
  z-index: 1;
}

.branding h1 {
  margin: 0;
  font-size: 2.55rem;
  letter-spacing: 0.02em;
}

.branding p {
  margin: 0.35rem 0 0;
  font-size: 1.02rem;
  opacity: 0.92;
}

.header-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1rem;
  position: relative;
  z-index: 1;
}

.header-actions :deep(.interface-settings) {
  align-self: flex-end;
}

.header-actions :deep(.settings-toggle) {
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.45);
  color: var(--color-text-on-emphasis);
  box-shadow: 0 12px 28px rgba(120, 110, 100, 0.22);
}

.header-actions :deep(.settings-toggle:hover) {
  background: rgba(255, 255, 255, 0.32);
  color: var(--color-text-on-emphasis);
}

.session {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
  position: relative;
  z-index: 1;
}

.session-actions {
  display: flex;
  gap: 0.85rem;
  align-items: center;
  flex-wrap: wrap;
}

.identity {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  font-size: 1rem;
}

.identity strong {
  font-weight: 700;
}

.reputation {
  font-weight: 600;
}

.verification-status {
  font-size: 0.92rem;
  opacity: 0.9;
}

.logout,
.messages-trigger {
  background: linear-gradient(135deg, rgba(180, 140, 110, 0.28), rgba(154, 161, 168, 0.32));
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: var(--radius-pill);
  color: var(--color-text-on-emphasis);
  font-weight: 600;
  padding: 0.6rem 1.5rem;
  transition: background var(--transition-base), transform var(--transition-base),
    box-shadow var(--transition-base);
  backdrop-filter: blur(8px);
}

.logout:hover,
.messages-trigger:hover {
  background: linear-gradient(135deg, rgba(180, 140, 110, 0.38), rgba(154, 161, 168, 0.42));
  transform: translateY(-1px);
  box-shadow: 0 14px 26px rgba(150, 132, 118, 0.22);
}

.login-section {
  display: flex;
  justify-content: center;
}

.workspace-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 1.75rem;
  align-items: start;
}

.sidebar {
  position: sticky;
  top: 1.5rem;
  align-self: start;
}

.workspace {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.25rem 1rem;
  background: color-mix(in srgb, var(--color-surface) 85%, transparent);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.24);
  backdrop-filter: blur(calc(var(--glass-blur) / 2));
}

.menu-item {
  border: none;
  border-radius: var(--radius-md);
  padding: 0.75rem 1.1rem;
  background: transparent;
  color: color-mix(in srgb, var(--color-text-soft) 75%, var(--color-text-strong));
  font-weight: 600;
  text-align: left;
  letter-spacing: 0.01em;
  transition: all var(--transition-base);
}

.menu-item.active {
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  box-shadow: 0 18px 36px rgba(176, 132, 99, 0.28);
}

.menu-item:not(.active):hover {
  background: color-mix(in srgb, var(--color-accent) 22%, rgba(255, 255, 255, 0.7));
  color: var(--color-text-strong);
  transform: translateX(2px);
}

.alert {
  background: color-mix(in srgb, rgba(198, 106, 96, 0.32) 65%, transparent);
  border-radius: var(--radius-md);
  color: color-mix(in srgb, #7b3730 75%, var(--color-text-strong));
  padding: 1.05rem 1.45rem;
  border: 1px solid rgba(198, 106, 96, 0.28);
  backdrop-filter: blur(calc(var(--glass-blur) / 3));
}

.success {
  background: color-mix(in srgb, rgba(168, 192, 170, 0.4) 60%, transparent);
  border-radius: var(--radius-md);
  color: color-mix(in srgb, #315942 80%, var(--color-text-strong));
  margin: 0;
  padding: 0.9rem 1.2rem;
  border: 1px solid rgba(146, 174, 150, 0.35);
  backdrop-filter: blur(calc(var(--glass-blur) / 3));
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 1.9rem;
}

.manage-grid,
.orders-grid {
  display: grid;
  gap: 1.6rem;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  align-items: stretch;
}

.manage-grid > *,
.orders-grid > * {
  height: 100%;
}

.admin-panels {
  display: grid;
  gap: 1.6rem;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 1fr);
  align-items: start;
}

.reputation-panel {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
}

.footer {
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  margin-top: auto;
  padding-top: 0.75rem;
}

:global(body[data-theme='dark']) .app {
  background: linear-gradient(160deg, rgba(44, 38, 34, 0.94), rgba(24, 21, 19, 0.9));
  border-color: color-mix(in srgb, var(--color-border) 70%, transparent);
}

:global(body[data-theme='dark']) .app::before {
  background: radial-gradient(circle at 20% 25%, rgba(180, 140, 110, 0.32), transparent 70%);
  opacity: 0.42;
}

:global(body[data-theme='dark']) .app::after {
  background: radial-gradient(circle at 80% 80%, rgba(122, 128, 136, 0.35), transparent 72%);
  opacity: 0.38;
}

:global(body[data-theme='dark']) .header {
  background: linear-gradient(135deg, rgba(176, 132, 99, 0.85), rgba(128, 134, 140, 0.82));
  box-shadow: 0 26px 60px rgba(70, 55, 42, 0.45);
}

:global(body[data-theme='dark']) .header::before {
  background: radial-gradient(circle, rgba(204, 189, 170, 0.32), transparent 70%);
  opacity: 0.42;
}

:global(body[data-theme='dark']) .header::after {
  background: radial-gradient(circle at top right, rgba(168, 176, 182, 0.38), transparent 72%);
  opacity: 0.38;
}

:global(body[data-theme='dark']) .menu-item {
  color: color-mix(in srgb, var(--color-text-soft) 85%, var(--color-text-on-emphasis));
}

:global(body[data-theme='dark']) .menu-item.active {
  box-shadow: 0 20px 42px rgba(120, 96, 78, 0.4);
}

:global(body[data-theme='dark']) .menu-item:not(.active):hover {
  background: color-mix(in srgb, rgba(176, 132, 99, 0.28) 55%, transparent);
  color: var(--color-text-on-emphasis);
}

:global(body[data-theme='dark']) .menu {
  background: color-mix(in srgb, var(--color-surface) 75%, transparent);
  border-color: color-mix(in srgb, var(--color-border) 80%, transparent);
}

@media (min-width: 1280px) {
  .manage-grid,
  .orders-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1024px) {
  .app {
    margin: 0;
    padding: 1.75rem;
  }

  .header {
    padding: 2rem 1.75rem;
  }

  .header-actions {
    align-items: stretch;
  }

  .workspace-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
  }

  .menu {
    flex-direction: row;
    flex-wrap: wrap;
    border-radius: var(--radius-pill);
    justify-content: center;
  }

  .menu-item {
    text-align: center;
    flex: 1 1 160px;
  }

  .admin-panels {
    grid-template-columns: 1fr;
  }

  .reputation-panel {
    gap: 1.4rem;
  }
}

@media (max-width: 768px) {
  .app {
    margin: 0;
    padding: 1.25rem;
  }

  .header {
    text-align: center;
    padding: 1.85rem 1.35rem;
  }

  .identity {
    align-items: center;
  }

  .header-actions {
    align-items: center;
    gap: 1.25rem;
  }

  .logout,
  .messages-trigger {
    width: 100%;
    justify-content: center;
  }

  .menu {
    gap: 0.6rem;
  }
}
</style>
