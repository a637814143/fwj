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
          @search="handleFilterSearch"
          @reserve="handleReserve"
          @purchase="handlePurchase"
          @contact-seller="handleContactSeller"
        />

        <div v-else-if="activeTab === 'manage'" class="manage-grid">
          <HouseForm
            :initial-house="selectedHouse"
            :loading="loading"
            :can-manage="canManageHouses"
            :current-user="currentUser"
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

        <AdminHouseReview
          v-else-if="activeTab === 'review'"
          :houses="pendingReviewHouses"
          :loading="reviewLoading"
          @refresh="fetchHouses({ silent: false })"
          @review="handleReview"
        />

        <div v-else-if="activeTab === 'orders'" class="orders-grid">
          <UrgentTasks
            v-if="showUrgentTasks"
            :tasks="urgentTasks"
            :progress-labels="orderProgressLabels"
          />
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

        <RealNameVerification
          v-else-if="activeTab === 'verify'"
          :api-base-url="apiBaseUrl"
          :current-user="currentUser"
          @verified="handleVerificationUpdate"
        />

        <AdminReputationBoard
          v-else-if="activeTab === 'admin'"
          :loading="adminLoading"
          :overview="adminReputation"
          :users="adminUsers"
          :current-user="currentUser"
          @refresh="loadAdminData"
          @toggle-blacklist="handleToggleBlacklist"
          @delete-user="handleDeleteUser"
        />
      </main>
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
import AdminHouseReview from './components/AdminHouseReview.vue';
import AdminReputationBoard from './components/AdminReputationBoard.vue';
import ConversationPanel from './components/ConversationPanel.vue';
import RealNameVerification from './components/RealNameVerification.vue';
import InterfaceSettings from './components/InterfaceSettings.vue';
import UrgentTasks from './components/UrgentTasks.vue';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';
const houses = ref([]);
const loading = ref(false);
const selectedHouse = ref(null);
const currentUser = ref(null);
const wallet = ref(null);
const walletLoading = ref(false);
const orders = ref([]);
const ordersLoading = ref(false);
const reservationLoading = ref(false);
const reservationTarget = ref(null);
const messages = reactive({ error: '', success: '' });
const recommendations = reactive({ sellers: [], buyers: [] });
const adminUsers = ref([]);
const adminReputation = ref(null);
const adminLoading = ref(false);
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
  maxArea: ''
});
const storageKey = 'secondhand-house-current-user';

const client = axios.create({
  baseURL: apiBaseUrl,
  headers: { 'Content-Type': 'application/json' }
});

const settingsStorageKey = 'shh-interface-settings';
const defaultSettings = Object.freeze({
  fontSize: 'medium',
  language: 'zh',
  theme: 'light'
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
      verify: '实名认证',
      verifyCompleted: '实名认证（已完成）',
      manage: '房源管理',
      orders: '订单与钱包',
      review: '房源审核',
      reviewWithCount: '房源审核（{count}）',
      admin: '信誉面板'
    },
    footer: {
      apiBaseLabel: '后端接口地址：'
    },
    alerts: {
      errorPrefix: '提示：'
    },
    roles: {
      buyer: '买家',
      seller: '卖家',
      admin: '系统管理员'
    },
    statuses: {
      pending: '待审核',
      approved: '已通过',
      rejected: '已驳回'
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
        en: 'English'
      },
      fontSize: {
        title: '字体大小',
        small: '小',
        medium: '中',
        large: '大'
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
        role: '选择角色'
      },
      placeholders: {
        username: '请输入用户名',
        password: '请输入密码',
        passwordWithHint: '请输入密码（至少6位）',
        displayName: '请输入昵称',
        confirmPassword: '请再次输入密码'
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
      errors: {
        loginRequired: '请输入用户名和密码',
        loginFailed: '登录失败，请稍后再试。',
        registerRequired: '请输入完整注册信息',
        passwordMismatch: '两次输入的密码不一致',
        passwordLength: '请输入至少6位密码',
        registerFailed: '注册失败，请稍后重试'
      }
    },
    success: {
      verificationUpdated: '实名认证信息已更新。',
      deleteHouse: '已删除房源《{title}》。',
      houseUpdatedApproved: '房源《{title}》已更新并重新上架。',
      houseUpdatedPending: '房源《{title}》已更新，当前状态：{status}。',
      houseCreatedApproved: '已新增房源《{title}》，已通过审核并上架。',
      houseCreatedPending: '已提交房源《{title}》，当前状态：{status}。',
      purchase: '成功以{method}方式购买房源《{title}》，支付金额 ￥{amount}。',
      reservation: '已成功预定房源《{title}》，定金 ￥{amount}。',
      walletTopUp: '钱包充值成功，充值金额 ￥{amount}。',
      orderReturned: '订单《{title}》已退换成功。',
      viewingScheduled: '已为房源《{title}》安排看房，时间 {time}。',
      progressUpdated: '交易进度已更新至 {stage}。',
      blacklistAdded: '已将账号 {username} 加入黑名单。',
      blacklistRemoved: '已解除账号 {username} 的黑名单状态。',
      accountDeleted: '账号 {username} 已被删除。',
      reviewApproved: '已审核通过房源《{title}》。',
      reviewRejected: '已驳回房源《{title}》，状态：{status}，原因：{reason}。',
      login: '登录成功。',
      loginReputationSuffix: ' 当前信誉分：{score}。',
      restoredSession: '已恢复上次的登录状态。'
    },
    errors: {
      loadHouses: '加载房源数据失败，请检查后端服务。',
      loadRecommendations: '加载推荐用户失败。',
      loadWallet: '加载钱包信息失败。',
      loadOrders: '加载订单信息失败。',
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
      walletLoginRequired: '请先登录后再使用钱包功能。',
      walletTopUp: '钱包充值失败。',
      returnLoginRequired: '请先登录后再申请退换。',
      returnFailed: '退换请求失败。',
      updateBlacklist: '更新黑名单状态失败。',
      deleteAccount: '删除账号失败。',
      reviewRequireReason: '驳回操作需要填写原因。',
      submitReview: '提交审核结果失败。',
      loadReputation: '加载信誉面板失败。',
      persistSettings: '界面设置保存失败。',
      persistUser: '无法持久化登录状态。',
      restoreSession: '恢复登录状态失败。'
    },
    prompts: {
      reviewRejectReason: '请输入驳回原因',
      reviewRemark: '审核备注（可选）',
      reviewDefaultRemark: '审核通过',
      reviewFallbackReason: '未填写',
      deleteHouseConfirm: '确定要删除房源《{title}》吗？',
      deleteAccountConfirm: '确定要删除账号 {username} 吗？该操作不可撤销。'
    },
    payments: {
      installment: '分期',
      full: '全款'
    },
    orders: {
      progress: {
        DEPOSIT_PAID: '定金交付阶段',
        VIEWING_SCHEDULED: '预约看房阶段',
        FEEDBACK_SUBMITTED: '看房反馈阶段',
        HANDOVER_COMPLETED: '交房阶段'
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
        upcomingViewing: '预约时间：{time}'
      },
      quickMessages: {
        confirmViewing: '我已确认 {time} 的看房安排，届时见。',
        confirmViewingNoTime: '我已收到看房安排，请您确认。'
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
      verify: 'Real-name verification',
      verifyCompleted: 'Real-name verification (completed)',
      manage: 'Listing management',
      orders: 'Orders & wallet',
      review: 'Listing review',
      reviewWithCount: 'Listing review ({count})',
      admin: 'Reputation board'
    },
    footer: {
      apiBaseLabel: 'API endpoint:'
    },
    alerts: {
      errorPrefix: 'Notice:'
    },
    roles: {
      buyer: 'Buyer',
      seller: 'Seller',
      admin: 'Administrator'
    },
    statuses: {
      pending: 'Pending review',
      approved: 'Approved',
      rejected: 'Rejected'
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
        en: 'English'
      },
      fontSize: {
        title: 'Font size',
        small: 'Small',
        medium: 'Medium',
        large: 'Large'
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
        role: 'Choose role'
      },
      placeholders: {
        username: 'Enter username',
        password: 'Enter password',
        passwordWithHint: 'Enter password (min. 6 characters)',
        displayName: 'Enter display name',
        confirmPassword: 'Re-enter password'
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
      errors: {
        loginRequired: 'Please enter username and password.',
        loginFailed: 'Sign-in failed. Please try again later.',
        registerRequired: 'Please complete all registration fields.',
        passwordMismatch: 'The passwords do not match.',
        passwordLength: 'Please enter a password with at least 6 characters.',
        registerFailed: 'Registration failed. Please try again later.'
      }
    },
    success: {
      verificationUpdated: 'Real-name verification updated successfully.',
      deleteHouse: 'Listing “{title}” has been removed.',
      houseUpdatedApproved: 'Listing “{title}” has been updated and republished.',
      houseUpdatedPending: 'Listing “{title}” has been updated. Current status: {status}.',
      houseCreatedApproved: 'Listing “{title}” has been created and approved.',
      houseCreatedPending: 'Listing “{title}” has been submitted. Current status: {status}.',
      purchase: 'Successfully purchased “{title}” via {method}, amount paid ¥{amount}.',
      reservation: 'Successfully reserved “{title}” with a deposit of ¥{amount}.',
      walletTopUp: 'Wallet top-up successful, amount ¥{amount}.',
      orderReturned: 'Order “{title}” has been refunded.',
      viewingScheduled: 'Viewing for “{title}” has been scheduled at {time}.',
      progressUpdated: 'Progress updated to {stage}.',
      blacklistAdded: 'Account {username} added to blacklist.',
      blacklistRemoved: 'Account {username} removed from blacklist.',
      accountDeleted: 'Account {username} has been deleted.',
      reviewApproved: 'Listing “{title}” approved.',
      reviewRejected: 'Rejected listing “{title}”, status: {status}, reason: {reason}.',
      login: 'Signed in successfully.',
      loginReputationSuffix: ' Current reputation score: {score}.',
      restoredSession: 'Previous session restored.'
    },
    errors: {
      loadHouses: 'Failed to load listings. Please check the backend service.',
      loadRecommendations: 'Failed to load recommended users.',
      loadWallet: 'Failed to load wallet information.',
      loadOrders: 'Failed to load orders.',
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
      walletLoginRequired: 'Please sign in before using wallet features.',
      walletTopUp: 'Wallet top-up failed.',
      returnLoginRequired: 'Please sign in before requesting a refund.',
      returnFailed: 'Failed to submit refund request.',
      updateBlacklist: 'Failed to update blacklist status.',
      deleteAccount: 'Failed to delete account.',
      reviewRequireReason: 'A reason is required to reject a listing.',
      submitReview: 'Failed to submit review decision.',
      loadReputation: 'Failed to load reputation dashboard.',
      persistSettings: 'Failed to save interface settings.',
      persistUser: 'Unable to persist login state.',
      restoreSession: 'Failed to restore previous session.'
    },
    prompts: {
      reviewRejectReason: 'Enter rejection reason',
      reviewRemark: 'Review remark (optional)',
      reviewDefaultRemark: 'Approved',
      reviewFallbackReason: 'Not provided',
      deleteHouseConfirm: 'Are you sure you want to delete listing “{title}”?',
      deleteAccountConfirm: 'Delete account {username}? This action cannot be undone.'
    },
    payments: {
      installment: 'instalments',
      full: 'full payment'
    },
    orders: {
      progress: {
        DEPOSIT_PAID: 'Deposit stage',
        VIEWING_SCHEDULED: 'Viewing appointment',
        FEEDBACK_SUBMITTED: 'Viewing feedback',
        HANDOVER_COMPLETED: 'Handover'
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
        upcomingViewing: 'Appointment: {time}'
      },
      quickMessages: {
        confirmViewing: 'I confirm the viewing at {time}. See you then!',
        confirmViewingNoTime: 'I have received the viewing arrangement. Please confirm.'
      }
    }
  }
};

const currentLocale = computed(() => settings.language || 'zh');

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
  (lang) => {
    applyLanguage(lang);
    persistSettings();
  },
  { immediate: true }
);

const parentheses = computed(() =>
  settings.language === 'en'
    ? { left: '(', right: ')' }
    : { left: '（', right: '）' }
);

const formatCurrencyYuan = (value) => {
  const numeric = Number(value ?? 0);
  if (!Number.isFinite(numeric)) {
    return '0.00';
  }
  const locale = settings.language === 'en' ? 'en-US' : 'zh-CN';
  return (numeric * 10000).toLocaleString(locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

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

const roleLabels = computed(() => ({
  SELLER: t('roles.seller'),
  BUYER: t('roles.buyer'),
  ADMIN: t('roles.admin')
}));

const listingStatusLabels = computed(() => ({
  PENDING_REVIEW: t('statuses.pending'),
  APPROVED: t('statuses.approved'),
  REJECTED: t('statuses.rejected')
}));

const orderProgressLabels = computed(() => ({
  DEPOSIT_PAID: t('orders.progress.DEPOSIT_PAID'),
  VIEWING_SCHEDULED: t('orders.progress.VIEWING_SCHEDULED'),
  FEEDBACK_SUBMITTED: t('orders.progress.FEEDBACK_SUBMITTED'),
  HANDOVER_COMPLETED: t('orders.progress.HANDOVER_COMPLETED')
}));

const orderProgressSequence = Object.freeze([
  'DEPOSIT_PAID',
  'VIEWING_SCHEDULED',
  'FEEDBACK_SUBMITTED',
  'HANDOVER_COMPLETED'
]);

const isSeller = computed(() => currentUser.value?.role === 'SELLER');
const isBuyer = computed(() => currentUser.value?.role === 'BUYER');
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');
const canUseMessaging = computed(() => currentUser.value && ['BUYER', 'SELLER'].includes(currentUser.value.role));
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

const canManageHouses = computed(() => currentUser.value?.role === 'SELLER');

const urgentTasks = computed(() => {
  if (!currentUser.value || !['BUYER', 'SELLER'].includes(currentUser.value.role)) {
    return [];
  }
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

    if (role === 'SELLER') {
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
    .sort((a, b) => a.timeValue - b.timeValue)
    .slice(0, 5);
});

const showUrgentTasks = computed(() => isBuyer.value || isSeller.value);

const pendingReviewHouses = computed(() =>
  houses.value.filter((house) => house.status === 'PENDING_REVIEW')
);

const reviewLoading = computed(() => loading.value || adminLoading.value);

const navigationTabs = computed(() => {
  const tabs = [{ value: 'home', label: t('nav.home') }];
  tabs.push({
    value: 'verify',
    label: currentUser.value?.realNameVerified ? t('nav.verifyCompleted') : t('nav.verify')
  });
  if (canManageHouses.value) {
    tabs.push({ value: 'manage', label: t('nav.manage') });
  }
  tabs.push({ value: 'orders', label: t('nav.orders') });
  if (isAdmin.value) {
    const pendingLabel = pendingReviewHouses.value.length
      ? t('nav.reviewWithCount', { count: pendingReviewHouses.value.length })
      : t('nav.review');
    tabs.push({ value: 'review', label: pendingLabel });
    tabs.push({ value: 'admin', label: t('nav.admin') });
  }
  return tabs;
});

const switchTab = (tab) => {
  activeTab.value = tab;
  if (tab === 'admin') {
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

const normalizeHouse = (house) => ({
  ...house,
  price: house?.price != null ? Number(house.price) : null,
  installmentMonthlyPayment:
    house?.installmentMonthlyPayment != null ? Number(house.installmentMonthlyPayment) : null,
  installmentMonths: house?.installmentMonths != null ? Number(house.installmentMonths) : null,
  listingDate: house?.listingDate ?? '',
  imageUrls: Array.isArray(house?.imageUrls) ? house.imageUrls : [],
  keywords: Array.isArray(house?.keywords) ? house.keywords : [],
  propertyCertificateUrl: house?.propertyCertificateUrl ?? '',
  status: house?.status ?? 'PENDING_REVIEW',
  reviewMessage: house?.reviewMessage ?? '',
  reviewedBy: house?.reviewedBy ?? '',
  reviewedAt: house?.reviewedAt ?? '',
  sensitiveMasked: Boolean(house?.sensitiveMasked)
});

const buildFilterParams = (filters) => {
  const params = {};
  Object.entries(filters).forEach(([key, value]) => {
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
  } catch (error) {
    messages.error = error.response?.data?.detail ?? t('errors.loadHouses');
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
      messages.error = error.response?.data?.detail ?? t('errors.loadRecommendations');
    }
  }
};

const fetchWallet = async ({ silent = false } = {}) => {
  if (!currentUser.value) {
    wallet.value = null;
    return;
  }
  if (!silent) {
    walletLoading.value = true;
  }
  try {
    const { data } = await client.get(`/wallets/${currentUser.value.username}`);
    wallet.value = data;
  } catch (error) {
    const detail = error.response?.data?.detail ?? t('errors.loadWallet');
    messages.error = detail;
  } finally {
    if (!silent) {
      walletLoading.value = false;
    }
  }
};

const fetchOrders = async ({ silent = false } = {}) => {
  if (!currentUser.value) {
    orders.value = [];
    return;
  }
  if (!silent) {
    ordersLoading.value = true;
  }
  try {
    const { data } = await client.get(`/orders/by-user/${currentUser.value.username}`);
    orders.value = data;
  } catch (error) {
    const detail = error.response?.data?.detail ?? t('errors.loadOrders');
    messages.error = detail;
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
    conversationError.value = error.response?.data?.detail ?? t('errors.loadConversations');
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
    conversationError.value = error.response?.data?.detail ?? t('errors.loadMessages');
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
    conversationError.value = error.response?.data?.detail ?? t('errors.sendMessage');
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
  if (activeTab.value === 'verify' && updated.realNameVerified) {
    activeTab.value = 'home';
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
    conversationError.value = error.response?.data?.detail ?? t('errors.createConversation');
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
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.scheduleViewing');
    }
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
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.progressUpdate');
    }
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
  try {
    const { data } = await client.get(`/auth/profile/${currentUser.value.username}`);
    const updated = {
      ...currentUser.value,
      ...data,
      message: data.message ?? currentUser.value.message
    };
    currentUser.value = updated;
    persistUser(updated);
  } catch (error) {
    if (!silent) {
      messages.error = error.response?.data?.detail ?? t('errors.refreshUser');
    }
  }
};

const guardReadOnly = () => {
  if (!canManageHouses.value) {
    messages.error = t('errors.manageHousesPermission');
    messages.success = '';
    return false;
  }
  return true;
};

const normalizeHousePayload = (payload) => {
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
  result.installmentMonthlyPayment = Number(result.installmentMonthlyPayment ?? 0);
  result.installmentMonths = Number.parseInt(result.installmentMonths ?? 0, 10);
  if (!Number.isFinite(result.price)) {
    result.price = 0;
  }
  if (!Number.isFinite(result.installmentMonthlyPayment)) {
    result.installmentMonthlyPayment = 0;
  }
  if (!Number.isFinite(result.installmentMonths)) {
    result.installmentMonths = 0;
  }
  if (typeof result.propertyCertificateUrl === 'string') {
    result.propertyCertificateUrl = result.propertyCertificateUrl.trim();
  }
  if (isSeller.value) {
    result.sellerUsername = currentUser.value.username;
    if (!result.sellerName) {
      result.sellerName = currentUser.value.displayName ?? '';
    }
  }
  return result;
};

const handleSubmit = async (payload) => {
  if (!guardReadOnly()) {
    return;
  }
  loading.value = true;
  messages.error = '';
  messages.success = '';
  const requestPayload = normalizeHousePayload(payload);
  try {
    if (selectedHouse.value) {
      const { data } = await client.put(`/houses/${selectedHouse.value.id}`, requestPayload);
      const statusLabel = listingStatusLabels.value[data.status] ?? t('statuses.pending');
      messages.success =
        data.status === 'APPROVED'
          ? t('success.houseUpdatedApproved', { title: data.title })
          : t('success.houseUpdatedPending', { title: data.title, status: statusLabel });
    } else {
      const { data } = await client.post('/houses', requestPayload);
      const statusLabel = listingStatusLabels.value[data.status] ?? t('statuses.pending');
      messages.success =
        data.status === 'APPROVED'
          ? t('success.houseCreatedApproved', { title: data.title })
          : t('success.houseCreatedPending', { title: data.title, status: statusLabel });
    }
    selectedHouse.value = null;
    await fetchHouses({ silent: true });
    await loadRecommendations();
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.saveHouse');
    }
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
    messages.error = error.response?.data?.detail ?? t('errors.deleteHouse');
  } finally {
    loading.value = false;
  }
};

const handlePurchase = async ({ house, paymentMethod }) => {
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
  if (isSeller.value && currentUser.value.username === house.sellerUsername) {
    messages.error = t('errors.purchaseOwnListing');
    messages.success = '';
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post('/orders', {
      houseId: house.id,
      buyerUsername: currentUser.value.username,
      paymentMethod
    });
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
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.purchaseFailed');
    }
  } finally {
    ordersLoading.value = false;
  }
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
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.reserveFailed');
    }
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
    const { data } = await client.post(`/wallets/${currentUser.value.username}/top-up`, {
      amount,
      reference
    });
    wallet.value = data;
    messages.success = t('success.walletTopUp', { amount: formatCurrencyYuan(amount) });
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.walletTopUp');
    }
  } finally {
    walletLoading.value = false;
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
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.returnFailed');
    }
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
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.updateBlacklist');
    }
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
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.deleteAccount');
    }
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
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? t('errors.submitReview');
    }
  } finally {
    adminLoading.value = false;
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
    messages.error = error.response?.data?.detail ?? t('errors.loadReputation');
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
  if (user.role === 'BUYER' || user.role === 'SELLER') {
    loadConversations({ silent: true });
  }
  if (user.role === 'ADMIN') {
    loadAdminData();
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
    } else {
      adminUsers.value = [];
      adminReputation.value = null;
    }
    if (role === 'BUYER' || role === 'SELLER') {
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
  margin: 2.75rem auto;
  max-width: 1260px;
  padding: 2.25rem;
  gap: 2rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-lg);
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
  background: radial-gradient(circle, rgba(14, 165, 233, 0.4), rgba(14, 165, 233, 0));
  top: -160px;
  right: -110px;
}

.app::after {
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(129, 140, 248, 0.42), rgba(129, 140, 248, 0));
  bottom: -140px;
  left: -100px;
}

.header {
  background: linear-gradient(130deg, rgba(14, 165, 233, 0.95), rgba(37, 99, 235, 0.95));
  color: var(--color-text-on-emphasis);
  padding: 2.35rem;
  border-radius: calc(var(--radius-lg) + 0.35rem);
  box-shadow: 0 26px 60px rgba(14, 165, 233, 0.35);
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
  background: radial-gradient(circle, rgba(255, 255, 255, 0.35), transparent 70%);
  top: -160px;
  left: -80px;
}

.header::after {
  width: 380px;
  height: 380px;
  background: radial-gradient(circle at top right, rgba(125, 211, 252, 0.7), transparent 70%);
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
  background: color-mix(in srgb, var(--color-text-on-emphasis) 18%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.45);
  color: var(--color-text-on-emphasis);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.2);
}

.header-actions :deep(.settings-toggle:hover) {
  background: color-mix(in srgb, var(--color-text-on-emphasis) 28%, transparent);
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
  background: color-mix(in srgb, var(--color-text-on-emphasis) 22%, transparent);
  border: 1px solid rgba(255, 255, 255, 0.55);
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
  background: color-mix(in srgb, var(--color-text-on-emphasis) 32%, transparent);
  transform: translateY(-1px);
  box-shadow: 0 14px 26px rgba(15, 23, 42, 0.18);
}

.login-section {
  display: flex;
  justify-content: center;
}

.menu {
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  padding: 0.85rem;
  background: color-mix(in srgb, var(--color-surface) 78%, transparent);
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(calc(var(--glass-blur) / 2));
}

.menu-item {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.5rem;
  background: transparent;
  color: var(--color-accent);
  font-weight: 600;
  letter-spacing: 0.01em;
  transition: all var(--transition-base);
}

.menu-item.active {
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  box-shadow: 0 18px 34px rgba(37, 99, 235, 0.28);
}

.menu-item:not(.active):hover {
  background: var(--color-accent-soft);
  color: var(--color-text-strong);
}

.alert {
  background: color-mix(in srgb, rgba(248, 113, 113, 0.35) 55%, transparent);
  border-radius: var(--radius-md);
  color: color-mix(in srgb, #7f1d1d 80%, var(--color-text-strong));
  padding: 1.05rem 1.45rem;
  border: 1px solid rgba(239, 68, 68, 0.32);
  backdrop-filter: blur(calc(var(--glass-blur) / 3));
}

.success {
  background: color-mix(in srgb, rgba(34, 197, 94, 0.28) 60%, transparent);
  border-radius: var(--radius-md);
  color: color-mix(in srgb, #14532d 85%, var(--color-text-strong));
  margin: 0;
  padding: 0.9rem 1.2rem;
  border: 1px solid rgba(34, 197, 94, 0.32);
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
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.footer {
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  margin-top: auto;
  padding-top: 0.75rem;
}

:global(body[data-theme='dark']) .app {
  background: linear-gradient(165deg, rgba(15, 23, 42, 0.92), rgba(30, 41, 59, 0.88));
  border-color: color-mix(in srgb, var(--color-border) 70%, transparent);
}

:global(body[data-theme='dark']) .header {
  background: linear-gradient(130deg, rgba(56, 189, 248, 0.85), rgba(99, 102, 241, 0.88));
  box-shadow: 0 26px 60px rgba(15, 118, 221, 0.32);
}

:global(body[data-theme='dark']) .menu {
  background: color-mix(in srgb, var(--color-surface) 70%, transparent);
  border-color: color-mix(in srgb, var(--color-border) 80%, transparent);
}

@media (max-width: 1024px) {
  .app {
    margin: 2rem;
    padding: 1.75rem;
  }

  .header {
    padding: 2rem 1.75rem;
  }

  .header-actions {
    align-items: stretch;
  }
}

@media (max-width: 768px) {
  .app {
    margin: 1.35rem;
    padding: 1.4rem;
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
}
</style>
