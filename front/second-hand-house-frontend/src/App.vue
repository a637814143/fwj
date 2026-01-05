<template>
  <div class="app">
    <header class="header">
      <HeroCarousel class="header-carousel">
        <div class="hero-surface">
          <div class="header-foreground">
            <div class="header-top">
              <div class="branding">
                <h1>{{ t('header.title') }}</h1>
                <p>{{ t('header.subtitle') }}</p>
              </div>
              <div class="header-actions">
                <button
                  v-if="currentUser && canUseMessaging"
                  type="button"
                  class="messages-trigger"
                  @click="openConversationPanel"
                >
                  {{ t('header.messages') }}
                </button>
                <InterfaceSettings />
              </div>
            </div>
            <p v-if="messages.success" class="success">{{ messages.success }}</p>
          </div>

          <section v-if="!currentUser" class="login-section">
            <RoleLogin :api-base-url="apiBaseUrl" @login-success="handleLoginSuccess" />
          </section>

          <template v-else>
            <div class="workspace-shell">
              <div class="workspace-layout" :class="{ 'menu-open': menuOpen }">
                <div class="menu-wrapper">
                  <button
                    type="button"
                    class="menu-toggle"
                    :aria-expanded="menuOpen ? 'true' : 'false'"
                    aria-controls="app-navigation"
                    @click="toggleMenu"
                  >
                    ≡
                  </button>
                  <transition name="menu-fade">
                    <aside v-if="menuOpen" id="app-navigation" class="sidebar">
                      <div class="menu-panel">
                        <div class="menu-header" v-if="currentUser">
                          <div class="menu-user">
                            <span class="menu-user-name">{{ currentUser.displayName }}</span>
                            <span class="menu-user-role">{{ roleLabels[currentUser.role] }}</span>
                            <span class="menu-user-account">@{{ currentUser.username }}</span>
                          </div>
                          <div class="menu-wallet">
                            <div class="wallet-line">
                              <span class="wallet-label">{{ t('menu.walletBalance') }}</span>
                              <strong class="wallet-value">￥{{ walletBalanceLabel }}</strong>
                            </div>
                            <div class="wallet-line">
                              <span class="wallet-label">{{ t('menu.walletPoints') }}</span>
                              <strong class="wallet-value">{{ walletPointsLabel }}</strong>
                            </div>
                          </div>
                        </div>
                        <nav class="menu">
                          <button
                            v-for="tab in navigationTabs"
                            :key="tab.value"
                            type="button"
                            :class="['menu-item', { active: tab.value === activeTab }]"
                            :aria-label="tab.ariaLabel || tab.label"
                            @click="switchTab(tab.value)"
                          >
                            <span class="menu-item__label">{{ tab.label }}</span>
                            <span v-if="tab.alert" class="menu-item__alert" aria-hidden="true"></span>
                          </button>
                        </nav>
                        <button type="button" class="menu-logout" @click="handleLogout">
                          {{ t('header.logout') }}
                        </button>
                      </div>
                    </aside>
                  </transition>
                </div>

                <section class="workspace" :class="{ 'home-bleed': activeTab === 'home' }">
                  <section v-if="messages.error" class="alert">
                    <strong>{{ t('alerts.errorPrefix') }}</strong> {{ messages.error }}
                  </section>

                  <section v-if="contractDownload" class="contract-download">
                    <div>
                      <h3>{{ t('contracts.generated.downloadTitle') }}</h3>
                      <p class="contract-download__meta">
                        {{ t('contracts.generated.summary', {
                          title: contractDownload.title,
                          buyer: contractDownload.buyerName,
                          seller: contractDownload.sellerName,
                          amount: contractDownload.amount
                        }) }}
                      </p>
                    </div>
                    <div class="contract-download__actions">
                      <a :href="contractDownload.url" :download="contractDownload.buyerFileName" class="cta primary">
                        {{ t('contracts.generated.downloadBuyer') }}
                      </a>
                      <a :href="contractDownload.url" :download="contractDownload.sellerFileName" class="cta ghost">
                        {{ t('contracts.generated.downloadSeller') }}
                      </a>
                    </div>
                  </section>

                  <main class="main-content">
                    <HouseExplorer
                      v-if="activeTab === 'home'"
                      :houses="marketplaceHouses"
                      :loading="loading"
                      :current-user="currentUser"
                      :can-view-sensitive-info="canViewSensitiveInfo"
                      :filters="houseFilters"
                      :recommendations="recommendations"
                      :purchase-loading="ordersLoading"
                      :reservation-loading="reservationLoading"
                      :reservation-target="reservationTarget"
                      :api-base-url="apiBaseUrl"
                      :favorite-ids="favoriteIdList"
                      :can-favorite="Boolean(currentUser)"
                      :page-size="4"
                      @search="handleFilterSearch"
                      @reserve="handleReserve"
                      @purchase="handlePurchase"
                      @contact-seller="handleContactSeller"
                      @toggle-favorite="handleToggleFavorite"
                    />

                    <HouseExplorer
                      v-else-if="activeTab === 'favorites'"
                      :houses="favoriteHouses"
                      :loading="loading"
                      :current-user="currentUser"
                      :can-view-sensitive-info="canViewSensitiveInfo"
                      :filters="houseFilters"
                      :recommendations="recommendations"
                      :purchase-loading="ordersLoading"
                      :reservation-loading="reservationLoading"
                      :reservation-target="reservationTarget"
                      :api-base-url="apiBaseUrl"
                      :favorite-ids="favoriteIdList"
                      :can-favorite="Boolean(currentUser)"
                      :page-size="4"
                      :show-filters="false"
                      :show-recommendations="false"
                      :empty-message="t('favorites.empty')"
                      @reserve="handleReserve"
                      @purchase="handlePurchase"
                      @contact-seller="handleContactSeller"
                      @toggle-favorite="handleToggleFavorite"
                    />

                    <AIAssistant
                      v-else-if="activeTab === 'assistant'"
                      :api-base-url="apiBaseUrl"
                    />

                    <PricePredictor
                      v-else-if="activeTab === 'predictor'"
                      :api-base-url="apiBaseUrl"
                      :current-user="currentUser"
                      :wallet="wallet"
                      :consume-points="consumePredictionPoints"
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

                    <HouseList
                      v-else-if="activeTab === 'drafts'"
                      :houses="sellerDraftHouses"
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

                    <HouseReviews
                      v-else-if="activeTab === 'feedback'"
                      :houses="houses"
                      :reviews="houseReviews"
                      :current-user="currentUser"
                      :eligible-house-ids="reviewableHouseIds"
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
                      <AdminTopUpReview
                        :top-ups="adminPendingTopUps"
                        :loading="adminTopUpsLoading"
                        @refresh="loadAdminTopUps"
                        @decide="handleAdminTopUpReview"
                      />
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
            </div>
          </template>
        </div>
      </HeroCarousel>
    </header>

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

    <ContractAgreement
      :visible="purchaseContractVisible"
      context="buyer"
      @agree="handlePurchaseAgreementAgree"
      @reject="handlePurchaseAgreementReject"
    />

    <footer class="footer">
      <small>{{ t('footer.apiBaseLabel') }}{{ apiBaseUrl }}</small>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, provide, reactive, ref, watch } from 'vue';
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
import AdminTopUpReview from './components/AdminTopUpReview.vue';
import AdminHouseManager from './components/AdminHouseManager.vue';
import ConversationPanel from './components/ConversationPanel.vue';
import InterfaceSettings from './components/InterfaceSettings.vue';
import UrgentTasks from './components/UrgentTasks.vue';
import ReviewModeration from './components/ReviewModeration.vue';
import AccountCenter from './components/AccountCenter.vue';
import AIAssistant from './components/AIAssistant.vue';
import PricePredictor from './components/PricePredictor.vue';
import ContractAgreement from './components/ContractAgreement.vue';
import HeroCarousel from './components/HeroCarousel.vue';
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
const purchaseContractVisible = ref(false);
const pendingPurchaseOptions = ref(null);
const contractDownload = ref(null);
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
const adminPendingTopUps = ref([]);
const adminTopUpsLoading = ref(false);
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
const menuOpen = ref(true);
const favoriteHouseIds = ref(new Set());
const favoriteIdList = computed(() => Array.from(favoriteHouseIds.value));
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
const favoritesStoragePrefix = 'shh-favorites-';

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

const updateMenuForViewport = () => {
  if (typeof window === 'undefined') {
    return;
  }
  if (window.innerWidth >= 1024) {
    menuOpen.value = true;
  }
};

const toggleMenu = () => {
  menuOpen.value = !menuOpen.value;
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

const buildFavoriteStorageKey = (username) => `${favoritesStoragePrefix}${username}`;

const loadFavoriteIds = (username) => {
  if (typeof window === 'undefined' || !username) {
    return new Set();
  }
  try {
    const raw = window.localStorage.getItem(buildFavoriteStorageKey(username));
    if (!raw) {
      return new Set();
    }
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return new Set(parsed.map((value) => String(value)));
    }
  } catch (error) {
    console.warn('Failed to restore favorite houses:', error);
  }
  return new Set();
};

const persistFavoriteIds = (username, ids) => {
  if (typeof window === 'undefined' || !username) {
    return;
  }
  try {
    window.localStorage.setItem(buildFavoriteStorageKey(username), JSON.stringify(ids));
  } catch (error) {
    console.warn('Failed to persist favorite houses:', error);
  }
};

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
      favorites: '我的收藏',
      predictor: '房价预测',
      aiAssistant: 'AI 智能助手',
      manage: '房源管理',
      drafts: '草稿箱',
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
    menu: {
      walletBalance: '钱包余额',
      walletPoints: '积分余额'
    },
    footer: {
      apiBaseLabel: '后端接口地址：'
    },
    alerts: {
      errorPrefix: '提示：'
    },
    contracts: {
      common: {
        open: '阅读并签署《二手房买卖合同》',
        scrollNotice: '请完整阅读合同条款，滚动至最底部后方可选择是否同意。',
        disagree: '不同意',
        agree: '我已阅读并同意合同条款',
        agreeLabels: {
          default: '我已阅读并同意合同条款',
          buyer: '我已阅读并同意，继续购买',
          seller: '我已阅读并同意，提交房源'
        },
        buyer: '买方',
        seller: '卖方'
      },
      status: {
        accepted: '已确认合同条款',
        pending: '尚未确认合同条款'
      },
      errors: {
        sellerAgreeRequired: '请先阅读并同意《二手房买卖合同》后再提交房源。',
        sellerDeclined: '您已拒绝合同条款，房源未提交。',
        purchaseDeclined: '您已拒绝合同条款，购买流程已终止。'
      },
      usage: {
        seller: {
          title: '卖家合同确认',
          notice:
            '发布房源前须阅读并同意平台示范《二手房买卖合同》，并保证房源信息真实、权属清晰且不存在查封、抵押等限制。'
        },
        buyer: {
          title: '买家合同确认',
          notice:
            '下单前请完整阅读合同条款，确认自身符合当地购房资格、资信条件及资金监管要求。'
        }
      },
      generated: {
        downloadTitle: '电子购房合同已生成',
        summary: '《{title}》签署双方：甲方（卖方）{seller} / 乙方（买方）{buyer}，交易金额 {amount}。',
        downloadBuyer: '下载乙方签署版',
        downloadSeller: '下载甲方签署版',
        title: '电子购房合同',
        parties: '甲方（卖方）：{seller}（账号：{sellerAccount}）\n乙方（买方）：{buyer}（账号：{buyerAccount}）',
        house: '房屋信息：{address}，建筑面积 {area}，成交价款 {price}',
        commitments:
          '双方承诺所填信息真实、遵守平台监管与预约看房安排，并在资金监管与线下交接完成后共同确认交易状态。',
        signatures: '请在下方留出甲乙双方签字及日期位置：',
        signatureLines: '甲方（卖方）签字：_____________   日期：__________\n乙方（买方）签字：_____________   日期：__________'
      },
      secondHand: {
        title: '二手房买卖合同（平台示范文本）',
        preface:
          '为保障买受人（买方）与出卖人（卖方）的合法权益，根据《中华人民共和国民法典》《城市房地产管理法》《不动产登记暂行条例》《关于规范房屋买卖合同网签备案管理的通知》等现行法律法规及属地调控政策，双方通过平台达成如下线上示范合同：',
        sections: [
          {
            heading: '一、房屋基本信息',
            clauses: [
              '1. 房屋坐落位置、用途、建筑面积、房屋性质及不动产权证号以卖方提交的权属证明材料为准，卖方须保证资料真实、合法、完整。',
              '2. 房屋应不存在查封、预告登记冲突、司法冻结、共有权争议等影响交易的情形，如存在抵押或租赁关系须提前书面告知买方并依法处理。',
              '3. 平台提示双方核验房地产登记信息、土地年限、规划用途、公共维修基金结存及物业费缴纳情况。'
            ]
          },
          {
            heading: '二、价款与支付安排',
            clauses: [
              '1. 双方协商房屋总价为人民币（大写）及（小写）金额，以平台订单确认信息为准。',
              '2. 买方应按约定支付定金、首付款及尾款。定金或预订金由平台托管，违约责任按照《民法典》第五百八十七条执行。',
              '3. 买方选择贷款的，应在约定期限内向符合监管要求的金融机构提交按揭申请，卖方应配合提供相关资料。'
            ]
          },
          {
            heading: '三、资金监管与贷款发放',
            clauses: [
              '1. 平台支持使用监管专户或商业银行托管方式存放交易价款，未经双方确认及监管机构核验不得提前划转。',
              '2. 贷款发放前，卖方不得擅自解除抵押或另行处分房屋；贷款到账后，卖方应按监管银行指令完成解押、交付。',
              '3. 因买方资信不足导致贷款审批未通过的，双方可协商延期或终止交易，相关损失依合同约定承担。'
            ]
          },
          {
            heading: '四、交付与权属转移',
            clauses: [
              '1. 双方应在价款支付完成后按照当地不动产登记部门流程办理过户手续，卖方应在约定时间内协助买方完成网签、税务申报和登记申请。',
              '2. 房屋交付时，卖方应移交房屋钥匙、物业交接单、水电燃气、供暖、有线电视、网络等相关账户，保证欠费已结清。',
              '3. 买方应在取得不动产权属证书后，及时向平台确认交易完成状态，以便解除资金监管。'
            ]
          },
          {
            heading: '五、税费及其他费用承担',
            clauses: [
              '1. 契税、增值税、个人所得税等法定税费按国家及地方政策规定承担；如政策调整导致税费变化，按调整后标准执行。',
              '2. 交易过程中产生的评估费、公证费、贷款服务费、过户手续费等，按照双方约定执行；无约定的，依照交易习惯各自承担。',
              '3. 平台服务费、资金监管手续费等应由订单确认页面载明，双方确认后不得擅自变更。'
            ]
          },
          {
            heading: '六、房屋现状与质量责任',
            clauses: [
              '1. 卖方应保证房屋结构、装饰、附属设施现状与披露信息一致，无违法加建、重大质量缺陷或影响安全使用的情形。',
              '2. 买方在签约前可申请第三方查验，发现影响交易的重要问题的，有权要求卖方整改或解除合同。',
              '3. 对于物业管理、车位、公共收益分配等权利义务，双方应按照现行管理规约及业主大会决定执行。'
            ]
          },
          {
            heading: '七、违约责任',
            clauses: [
              '1. 任一方未按约定期限履行支付、交付、配合办理登记等主要义务的，应在接到催告后三日内纠正；逾期仍未履行的，构成违约。',
              '2. 违约方应向守约方支付房屋总价款5%-10%的违约金（具体比例以双方线下补充协议或地方惯例确定），不足以弥补损失的，还应继续赔偿。',
              '3. 因虚假陈述、隐瞒重要事实或提供伪造材料导致合同无法履行的，违约方须承担全部损失并接受平台信誉扣减、账户限制等措施。'
            ]
          },
          {
            heading: '八、风险提示与政策遵循',
            clauses: [
              '1. 双方应确认符合限购、限售、购房资格审查、征信审核等政策条件，因个人原因不符合法律法规导致的风险自行承担。',
              '2. 资金往来必须使用实名银行账户，严格遵守反洗钱、反恐怖融资等监管要求，不得使用现金或虚假交易流水。',
              '3. 买方应关注按揭利率、还款压力及税费政策变化，合理安排资金；卖方应妥善处理原有贷款、共有权人意见等事项。'
            ]
          },
          {
            heading: '九、争议解决',
            clauses: [
              '1. 双方应首先通过友好协商解决争议，协商不成的，可申请平台调解或向属地房地产交易纠纷调解机构申请调解。',
              '2. 调解不成的，可向房屋所在地人民法院提起诉讼或提交经双方约定的仲裁机构仲裁。',
              '3. 在争议处理期间，双方应配合平台采取必要的资金冻结、信息公示等风险控制措施。'
            ]
          }
        ],
        appendix: {
          heading: '附加约定',
          clauses: [
            '1. 平台提供的示范合同不替代双方线下签署的正式买卖合同，线下补充条款不得与国家政策及本合同精神相违背。',
            '2. 双方应保护彼此个人信息安全，未经允许不得向无关第三方泄露。',
            '3. 如需变更或解除合同，应通过平台提交申请，并依据法律法规及平台规则执行。'
          ]
        },
        conclusion: [
          '本合同经双方在线确认后生效，视为双方已充分理解全部条款并自愿承担相应权利义务。',
          '线下办理产权转移、贷款、缴税等手续时，双方应继续遵守国家最新政策规定并及时向平台备案进展。'
        ]
      }
    },
    favorites: {
      empty: '暂未收藏任何房源。',
      added: '已收藏该房源。',
      removed: '已取消收藏。',
      loginRequired: '请登录后再收藏房源。',
      add: '收藏该房源',
      remove: '取消收藏'
    },
    houseDetail: {
      price: '总价',
      downPayment: '首付款',
      area: '建筑面积',
      areaValue: '{area} ㎡',
      listedOn: '上架日期',
      floor: '楼层',
      floorValue: '{floor} 层',
      status: '状态',
      orderState: {
        label: '交易状态',
        reserved: '已预定',
        reservedByYou: '您已预定',
        sold: '已售出',
        available: '在售'
      },
      keywords: '房源关键词',
      gallery: '图片集',
      galleryCount: '{count} 张图片',
      galleryEmpty: '暂无上传的图片。',
      galleryItem: '图片 {index}',
      galleryAlt: '图片 {index}',
      seller: '卖家：{name}',
      contact: '联系方式：{phone}',
      close: '关闭',
      addressFallback: '地址待补充'
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
      fallbackAnswer: '抱歉，未能生成有效的回答，请换个问题试试。',
      degradedNotice: '已为您展示离线备选答案（云端服务暂不可用）。'
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
        area: '建筑面积',
        listedOn: '上架时间',
        floor: '楼层'
      },
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
      },
      actions: {
        accept: '接受',
        reject: '驳回'
      }
    },
    adminTopUps: {
      title: '充值审核',
      subtitle: '确认买家与卖家的充值申请，审核通过后钱包余额才会变更。',
      refresh: '刷新待审核列表',
      loading: '正在加载充值审核…',
      empty: '暂无待审核的充值记录。',
      columns: {
        id: '流水号',
        user: '账户',
        role: '角色',
        amount: '充值金额',
        reference: '备注/凭证',
        createdAt: '提交时间',
        actions: '操作'
      },
      actions: {
        approve: '通过充值',
        reject: '驳回充值'
      }
    },
    adminTopUps: {
      title: 'Top-up approval',
      subtitle: 'Approve buyer and seller wallet top-ups so balances update only after review.',
      refresh: 'Refresh pending list',
      loading: 'Loading top-up requests…',
      empty: 'No top-up requests require review.',
      columns: {
        id: 'Request ID',
        user: 'Account',
        role: 'Role',
        amount: 'Amount',
        reference: 'Reference',
        createdAt: 'Submitted at',
        actions: 'Actions'
      },
      actions: {
        approve: 'Approve',
        reject: 'Reject'
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
          sellerUsername: '请输入卖家账号',
          sellerName: '请输入卖家昵称',
          contactNumber: '请输入联系电话',
          floor: '例如 10',
          description: '补充房源亮点或其他信息',
          keywords: '例如 市中心、近地铁、朝南'
        },
        hints: {
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
          downPaymentLimit: '首付需低于总价的120%，请检查金额输入是否合理。',
          area: '请输入有效的建筑面积。'
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
          downPayment: '首付：￥{amount}'
        },
        keywordPlaceholder: '未设置',
        actions: {
          edit: '编辑',
          delete: '删除',
          approve: '通过',
          reject: '驳回',
          paymentLabel: '支付方式',
          paymentFull: '全款',
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
            phoneNumber: '请输入11位手机号'
          },
          actions: {
            submit: '立即认证',
            update: '保存更新',
            submitting: '提交中…'
          }
        },
        errors: {
          idNumber: '身份证号码需为18位数字。',
          phoneNumber: '手机号需为11位数字。',
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
        captcha: '验证码',
        email: '邮箱',
        verificationCode: '邮箱验证码'
      },
      placeholders: {
        username: '请输入用户名',
        password: '请输入密码',
        passwordWithHint: '请输入密码（至少6位）',
        displayName: '请输入昵称',
        confirmPassword: '请再次输入密码',
        captchaAnswer: '请输入答案',
        email: '请输入常用邮箱',
        verificationCode: '请输入6位验证码'
      },
      roles: {
        seller: '卖家',
        buyer: '买家'
      },
      actions: {
        login: '登录',
        loggingIn: '登录中...',
        register: '注册并登录',
        registering: '注册中...',
        sendCode: '发送验证码',
        sendingCode: '发送中...',
        resendIn: '{seconds} 秒后可重发'
      },
      captcha: {
        refresh: '换一题'
      },
      password: {
        label: '密码强度',
        strength: {
          invalid: '需包含大小写字母和数字且不可连续',
          weak: '强度：较弱',
          medium: '强度：良好',
          strong: '强度：安全'
        }
      },
      messages: {
        codeSent: '验证码已发送至 {email}，请注意查收。'
      },
      errors: {
        loginRequired: '请输入用户名和密码',
        loginFailed: '登录失败，请稍后再试。',
        registerRequired: '请输入完整注册信息',
        passwordMismatch: '两次输入的密码不一致',
        passwordLength: '请输入至少6位密码',
        registerFailed: '注册失败，请稍后重试',
        emailRequired: '请填写邮箱地址',
        emailInvalid: '请输入有效的邮箱地址',
        verificationFormat: '请输入6位数字验证码',
        passwordWeak: '密码需包含大小写字母与数字且不可包含连续字符',
        sendCodeFailed: '验证码发送失败，请稍后重试',
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
      purchaseWithReminder: '成功以{method}方式购买房源《{title}》，支付金额 ￥{amount}，请按预约时间完成线下看房。',
      reservation: '已成功预定房源《{title}》，定金 ￥{amount}。',
      orderReleasedSeller: '订单资金已发放给卖家。',
      orderReleasedBuyer: '订单资金已退回买家。',
      walletTopUpPending: '已提交钱包充值 ￥{amount}（含赠送 {points} 积分），资金将汇入管理员账户等待审核。',
      topUpReviewAccepted: '已通过该充值申请，资金将同步到账。',
      topUpReviewRejected: '已驳回该充值申请，资金未入账。',
      accountUpdated: '账号信息已更新。',
      orderReturned: '订单《{title}》已退换成功。',
      viewingScheduled: '已为房源《{title}》安排看房，时间 {time}。',
      progressUpdated: '交易进度已更新至 {stage}。',
      viewingConfirmed: '已确认看房状态。',
      orderReviewAccepted: '审核通过，资金安排已更新。',
      orderReviewRejected: '已驳回该审核申请。',
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
      loadAdminTopUps: '加载待审核充值失败。',
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
      purchaseReserveFirst: '请先预约并确认看房后再进行全款支付。',
      purchaseOwnListing: '不能购买自己发布的房源。',
      purchaseFailed: '支付失败，请稍后再试。',
      reserveBuyerOnly: '只有买家角色可以预定房源。',
      reserveVerifyFirst: '预定前请先完成实名认证。',
      reserveNotApproved: '房源尚未通过审核，暂不可预定。',
      reserveFailed: '预定失败，请稍后再试。',
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
      reviewTopUp: '审核充值申请失败。',
      loadReputation: '加载信誉面板失败。',
      persistSettings: '界面设置保存失败。',
      persistUser: '无法持久化登录状态。',
      restoreSession: '恢复登录状态失败。',
      reviewLoginRequired: '请先登录后再提交评价。',
      reviewHouseMissing: '未找到要评价的房源，请刷新后重试。',
      reviewNotEligible: '仅已购买并完成交易的房源可以评价。',
      reviewModerationDenied: '只有管理员可以执行评价审核。',
      reviewNotFound: '未找到指定的评价，请刷新后重试。',
      reviewContentTooShort: '评价内容不少于8个字。',
      confirmViewing: '确认看房状态失败，请稍后再试。',
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
        area: '面积',
        listingDate: '挂牌日期',
        seller: '卖家',
        contact: '联系方式',
        paymentMethod: '支付方式'
      },
      payment: {
        full: '全款支付'
      },
      states: {
        loading: '房源数据加载中…',
        empty: '暂未查询到符合条件的房源。'
      },
      pagination: {
        prev: '上一页',
        next: '下一页',
        page: '第 {current} / {total} 页'
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
        reservedByOthers: '该房源已被其他买家预定，暂不可再次预定。'
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
        hint: '充值款项将先汇入管理员账户并等待审核；系统以人民币元为结算单位，每充值 100 元获赠 10 积分。',
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
        sellerInsufficient: '卖家钱包余额不足，无法完成退款',
        sellerRepayInsufficient: '卖家钱包余额不足，无法归还垫付金额'
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
        buyerOnlyReturn: '仅买家本人可以申请退换',
        sellerUsernameRequired: '卖家账号不能为空',
        repaySellerOnly: '仅房源卖家可以归还垫付金额',
        repayNotRequired: '当前订单暂无待归还金额',
        repayAmountInvalid: '归还金额无效'
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
          FULL: '一次性付清'
        },
        viewing: {
          title: '看房安排',
          timeLabel: '预约时间：{time}',
          appointment: '预约时间：{time}',
          notes: '备注：{message}',
          schedule: '安排看房',
          advance: '推进至 {stage}',
          markViewed: '标记已看房',
          confirmedBuyer: '买家已确认看房',
          confirmedSeller: '卖家已确认看房'
        },
        actions: {
          requestReturn: '申请退换',
          viewDetails: '查看详情',
          hideDetails: '收起详情'
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
        },
        repayment: {
          pending: '平台已垫付买家退款，请尽快归还 ￥{amount} 至平台账户。',
          completed: '已于 {time} 完成归还。'
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
        returnRequestedSeller: '买家申请退房，请尽快关注订单进度。',
        returnCompletedSeller: '退房已完成，房源《{title}》已重新上架，请及时处理。',
        upcomingViewing: '预约时间：{time}',
        markRead: '已读',
        openTarget: '前往处理',
        recoverDescription: '平台已为买家垫付 ￥{amount}，请确认回收房源并归还金额。',
        recoverAction: '确认回收',
        recoverConfirm: '确认将 ￥{amount} 归还至平台账户？',
        recoverSuccess: '已成功归还 ￥{amount} 至平台账户。'
      },
      quickMessages: {
        confirmViewing: '我已确认 {time} 的看房安排，届时见。',
        confirmViewingNoTime: '我已收到看房安排，请您确认。'
      }
    },
    reviews: {
      title: '房源评价',
      subtitle: '仅可为已购买的房源撰写评价，提交后将进入管理员审核队列。',
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
        ratingRequired: '请选择评分。',
        purchaseRequired: '仅限已购买并完成交易的房源可以评价。'
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
      favorites: 'Favorites',
      predictor: 'Price predictor',
      aiAssistant: 'AI assistant',
      manage: 'Listing management',
      drafts: 'Draft box',
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
    menu: {
      walletBalance: 'Wallet balance',
      walletPoints: 'Reward points'
    },
    footer: {
      apiBaseLabel: 'API endpoint:'
    },
    alerts: {
      errorPrefix: 'Notice:'
    },
    contracts: {
      common: {
        open: 'Review the second-hand sale agreement',
        scrollNotice: 'Please read the full agreement. Scroll to the bottom to enable the action buttons.',
        disagree: 'Decline',
        agree: 'I have read and agree',
        agreeLabels: {
          default: 'I have read and agree to the terms',
          buyer: 'I agree and continue to purchase',
          seller: 'I agree and submit the listing'
        },
        buyer: 'Buyer',
        seller: 'Seller'
      },
      status: {
        accepted: 'Agreement confirmed',
        pending: 'Agreement pending confirmation'
      },
      errors: {
        sellerAgreeRequired: 'Please review and accept the sale agreement before submitting the listing.',
        sellerDeclined: 'You declined the agreement. The listing was not submitted.',
        purchaseDeclined: 'You declined the agreement. The purchase has been cancelled.'
      },
      usage: {
        seller: {
          title: 'Seller confirmation',
          notice:
            'Read and accept the platform sample agreement before publishing. Ensure the property information is truthful and the title is free of liens or disputes.'
        },
        buyer: {
          title: 'Buyer confirmation',
          notice:
            'Read the agreement carefully before ordering and confirm that you satisfy local purchase qualifications, credit requirements, and escrow rules.'
        }
      },
      generated: {
        downloadTitle: 'Digital purchase contract ready',
        summary: 'Contract for “{title}”: Seller (Party A) {seller} / Buyer (Party B) {buyer}, amount {amount}.',
        downloadBuyer: 'Download buyer copy',
        downloadSeller: 'Download seller copy',
        title: 'Digital housing purchase contract',
        parties: 'Party A (Seller): {seller} (account: {sellerAccount})\nParty B (Buyer): {buyer} (account: {buyerAccount})',
        house: 'Property: {address}, Area: {area}, Price: {price}',
        commitments:
          'Both parties confirm the information is accurate, will follow platform escrow and viewing appointments, and will jointly confirm status after offline handover.',
        signatures: 'Please reserve space below for signatures and dates:',
        signatureLines: 'Seller signature: ______________   Date: __________\nBuyer signature: ______________   Date: __________'
      },
      secondHand: {
        title: 'Second-hand Housing Sale Agreement (Sample)',
        preface:
          'To protect the legitimate rights of the seller and buyer, and pursuant to the PRC Civil Code, the Urban Real Estate Administration Law, the Provisional Regulations on Real Estate Registration, and applicable local housing policies, both parties agree to the following terms through the platform:',
        sections: [
          {
            heading: '1. Property information',
            clauses: [
              '1.1 The property location, usage, floor area, tenure, and title certificate number shall match the documents submitted by the seller, who guarantees their authenticity and completeness.',
              '1.2 The property must be free from seizure, conflicting pre-registrations, judicial restrictions, or ownership disputes. Existing mortgages or leases must be disclosed in writing and properly resolved.',
              '1.3 The platform reminds both parties to verify registration records, land-use terms, public maintenance funds, and property fee balances.'
            ]
          },
          {
            heading: '2. Price and payment schedule',
            clauses: [
              '2.1 The parties agree on the total price in Renminbi as shown on the confirmed order.',
              '2.2 Deposits and down payments shall be held in platform escrow. Breach liabilities follow Article 587 of the PRC Civil Code unless otherwise agreed.',
              '2.3 If the buyer applies for a mortgage, the application must be filed with a qualified lender within the agreed period and the seller shall provide the required documents.'
            ]
          },
          {
            heading: '3. Fund supervision and loan release',
            clauses: [
              '3.1 Transaction funds may be placed in an escrow or supervised bank account. Funds cannot be released without mutual confirmation and regulatory approval.',
              '3.2 Before loan disbursement the seller shall not dispose of the property. After disbursement the seller must cooperate with mortgage release and delivery as instructed by the supervising bank.',
              '3.3 If financing is rejected because of the buyer\'s credit profile, the parties may extend or terminate the deal. Resulting losses are allocated under the agreement.'
            ]
          },
          {
            heading: '4. Delivery and title transfer',
            clauses: [
              '4.1 After the agreed payments are completed, both parties shall complete online filing, tax declaration, and real-estate registration within the statutory timeline. The seller must cooperate in person when required.',
              '4.2 Upon delivery the seller shall hand over keys, property handover documents, and settled utility, heating, cable, and broadband accounts.',
              '4.3 The buyer shall confirm completion on the platform after obtaining the ownership certificate so the supervised funds can be released.'
            ]
          },
          {
            heading: '5. Taxes and other fees',
            clauses: [
              '5.1 Deed tax, value-added tax, individual income tax, and other statutory levies are borne according to national and local rules. Policy adjustments apply immediately.',
              '5.2 Appraisal, notary, loan service, and transfer handling fees are shared as agreed; if no agreement exists, customary local practice applies.',
              '5.3 Platform service charges and escrow fees must be disclosed on the confirmation page and may not be changed unilaterally.'
            ]
          },
          {
            heading: '6. Property condition and disclosure',
            clauses: [
              '6.1 The seller warrants that the structure, finishes, and fixtures match the disclosed condition and that no illegal alterations or major defects exist.',
              '6.2 The buyer may commission third-party inspections before signing. Material issues entitle the buyer to request remediation or contract termination.',
              '6.3 Rights and obligations regarding property management, parking, and shared income follow the current community regulations.'
            ]
          },
          {
            heading: '7. Default liabilities',
            clauses: [
              '7.1 Failure to pay, deliver, or cooperate with registration within the agreed time constitutes default if not remedied within three days of a written notice.',
              '7.2 The defaulting party shall pay liquidated damages of 5%–10% of the total price (subject to supplemental agreements or local practice) and compensate additional losses if the penalty is insufficient.',
              '7.3 Providing false statements, concealing material facts, or using forged documents shall result in full liability and platform sanctions such as reputation deductions or account restrictions.'
            ]
          },
          {
            heading: '8. Risk warnings and compliance',
            clauses: [
              '8.1 Both parties must meet purchase restrictions, credit checks, and qualification reviews. Risks arising from personal non-compliance are borne by the responsible party.',
              '8.2 Fund transfers must use verified bank accounts and comply with anti-money-laundering and counter-terrorism financing rules. Cash payments or fictitious transactions are prohibited.',
              '8.3 The buyer should evaluate mortgage interest changes, repayment ability, and tax adjustments, while the seller shall resolve existing loans and co-owner consents.'
            ]
          },
          {
            heading: '9. Dispute resolution',
            clauses: [
              '9.1 Disputes should first be resolved through amicable consultation or platform mediation, or via local real-estate mediation bodies.',
              '9.2 If mediation fails, disputes may be submitted to the people\'s court where the property is located or to an agreed arbitration institution.',
              '9.3 During dispute handling the platform may freeze funds or display risk notices as necessary risk controls.'
            ]
          }
        ],
        appendix: {
          heading: 'Supplementary provisions',
          clauses: [
            'A1. This sample does not replace the formal offline agreement. Supplemental clauses must not contravene national policies or the intent of this document.',
            'A2. Both parties must protect each other\'s personal data and keep transaction information confidential.',
            'A3. Any amendment or termination shall be filed through the platform and handled according to laws and platform rules.'
          ]
        },
        conclusion: [
          'The agreement takes effect once both parties confirm it online, signifying full understanding of the rights and obligations.',
          'When handling offline registration, financing, and tax procedures, both parties must continue to comply with the latest regulations and update the platform on progress.'
        ]
      }
    },
    favorites: {
      empty: 'No favorite listings yet.',
      added: 'Listing added to favorites.',
      removed: 'Listing removed from favorites.',
      loginRequired: 'Sign in to favorite listings.',
      add: 'Add to favorites',
      remove: 'Remove from favorites'
    },
    houseDetail: {
      price: 'Total price',
      downPayment: 'Down payment',
      area: 'Floor area',
      areaValue: '{area} ㎡',
      listedOn: 'Listed on',
      floor: 'Floor',
      floorValue: 'Level {floor}',
      status: 'Status',
      orderState: {
        label: 'Order state',
        reserved: 'Reserved',
        reservedByYou: 'Reserved by you',
        sold: 'Sold',
        available: 'Available'
      },
      keywords: 'Listing keywords',
      gallery: 'Gallery',
      galleryCount: '{count} images',
      galleryEmpty: 'No images uploaded yet.',
      galleryItem: 'Image {index}',
      galleryAlt: 'Image {index}',
      seller: 'Seller: {name}',
      contact: 'Contact: {phone}',
      close: 'Close',
      addressFallback: 'Address pending'
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
      fallbackAnswer: 'Sorry, the assistant could not generate a helpful answer. Please try another question.',
      degradedNotice: 'Offline fallback answer shown (cloud service temporarily unavailable).'
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
        area: 'Floor area',
        listedOn: 'Listed on',
        floor: 'Floor'
      },
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
        full: 'Full payment'
      },
      actions: {
        accept: 'Accept',
        reject: 'Reject'
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
          sellerUsername: 'Enter seller username',
          sellerName: 'Enter seller display name',
          contactNumber: 'Enter contact number',
          floor: 'e.g. 10',
          description: 'Highlight selling points or additional details',
          keywords: 'e.g. city centre, subway nearby, south-facing'
        },
        hints: {
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
          downPaymentLimit: 'The down payment must be below 120% of the total price; please review the amount.',
          area: 'Enter a valid floor area.'
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
          downPayment: 'Down payment: ￥{amount}'
        },
        keywordPlaceholder: 'Not set',
        actions: {
          edit: 'Edit',
          delete: 'Delete',
          approve: 'Approve',
          reject: 'Reject',
          paymentLabel: 'Payment method',
          paymentFull: 'Full',
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
            phoneNumber: 'Enter your 11-digit phone number'
          },
          actions: {
            submit: 'Verify now',
            update: 'Save updates',
            submitting: 'Submitting…'
          }
        },
        errors: {
          idNumber: 'The ID number must contain exactly 18 digits.',
          phoneNumber: 'The phone number must contain exactly 11 digits.',
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
        captcha: 'Verification question',
        email: 'Email',
        verificationCode: 'Email verification code'
      },
      placeholders: {
        username: 'Enter username',
        password: 'Enter password',
        passwordWithHint: 'Enter password (min. 6 characters)',
        displayName: 'Enter display name',
        confirmPassword: 'Re-enter password',
        captchaAnswer: 'Enter the answer',
        email: 'Enter your email',
        verificationCode: 'Enter the 6-digit code'
      },
      roles: {
        seller: 'Seller',
        buyer: 'Buyer'
      },
      actions: {
        login: 'Log in',
        loggingIn: 'Logging in...',
        register: 'Register & log in',
        registering: 'Registering...',
        sendCode: 'Send code',
        sendingCode: 'Sending...',
        resendIn: 'Resend in {seconds}s'
      },
      captcha: {
        refresh: 'Try another question'
      },
      password: {
        label: 'Password strength',
        strength: {
          invalid: 'Use upper & lower case letters and numbers without sequences',
          weak: 'Strength: Weak',
          medium: 'Strength: Fair',
          strong: 'Strength: Strong'
        }
      },
      messages: {
        codeSent: 'Verification code sent to {email}. Please check your inbox.'
      },
      errors: {
        loginRequired: 'Please enter username and password.',
        loginFailed: 'Sign-in failed. Please try again later.',
        registerRequired: 'Please complete all registration fields.',
        passwordMismatch: 'The passwords do not match.',
        passwordLength: 'Please enter a password with at least 6 characters.',
        registerFailed: 'Registration failed. Please try again later.',
        emailRequired: 'Please enter your email address.',
        emailInvalid: 'Please enter a valid email address.',
        verificationFormat: 'Enter the 6-digit verification code.',
        passwordWeak: 'Use letters in both cases plus numbers without sequential characters.',
        sendCodeFailed: 'Failed to send verification code. Please try again later.',
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
      purchaseWithReminder: 'Successfully purchased “{title}” via {method}, amount paid ¥{amount}. Please attend your scheduled viewing.',
      reservation: 'Successfully reserved “{title}” with a deposit of ¥{amount}.',
      orderReleasedSeller: 'Funds released to the seller.',
      orderReleasedBuyer: 'Funds returned to the buyer.',
      walletTopUpPending: 'Top-up submitted: ¥{amount} (+{points} pts). Funds will be sent to the admin account for approval.',
      topUpReviewAccepted: 'Top-up approved and balance updated.',
      topUpReviewRejected: 'Top-up request rejected. No balance change applied.',
      accountUpdated: 'Account details updated successfully.',
      orderReturned: 'Order “{title}” has been refunded.',
      viewingScheduled: 'Viewing for “{title}” has been scheduled at {time}.',
      progressUpdated: 'Progress updated to {stage}.',
      viewingConfirmed: 'Viewing status confirmed.',
      orderReviewAccepted: 'Escrow review accepted and funds updated.',
      orderReviewRejected: 'Escrow review request rejected.',
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
      loadAdminTopUps: 'Failed to load pending top-up requests.',
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
      purchaseReserveFirst: 'Please book and confirm a viewing before paying in full.',
      purchaseOwnListing: 'You cannot purchase your own listing.',
      purchaseFailed: 'Payment failed. Please try again later.',
      reserveBuyerOnly: 'Only buyers can reserve listings.',
      reserveVerifyFirst: 'Please complete real-name verification before reserving.',
      reserveNotApproved: 'The listing has not been approved yet.',
      reserveFailed: 'Failed to reserve listing. Please try again later.',
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
      reviewTopUp: 'Failed to review top-up request.',
      loadReputation: 'Failed to load reputation dashboard.',
      persistSettings: 'Failed to save interface settings.',
      persistUser: 'Unable to persist login state.',
      restoreSession: 'Failed to restore previous session.',
      reviewLoginRequired: 'Please sign in before submitting a review.',
      reviewHouseMissing: 'The selected listing could not be found. Please refresh and try again.',
      reviewNotEligible: 'Only purchased properties can be reviewed.',
      reviewModerationDenied: 'Only administrators can moderate reviews.',
      reviewNotFound: 'The specified review no longer exists.',
      reviewContentTooShort: 'Reviews must contain at least 8 characters.',
      confirmViewing: 'Failed to confirm the viewing status.',
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
        area: 'Area',
        listingDate: 'Listed on',
        seller: 'Seller',
        contact: 'Contact',
        paymentMethod: 'Payment method'
      },
      payment: {
        full: 'Full payment'
      },
      states: {
        loading: 'Loading listings…',
        empty: 'No listings matched the filters yet.'
      },
      pagination: {
        prev: 'Previous',
        next: 'Next',
        page: 'Page {current} of {total}'
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
        reservedByOthers: 'Another buyer has reserved this listing. Reservations are temporarily unavailable.'
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
        hint: 'Funds are routed to the admin account for approval. Balances are stored in Chinese Yuan. Earn 10 points for every ¥100 top-up.',
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
        sellerInsufficient: 'Seller wallet balance is insufficient to process the refund',
        sellerRepayInsufficient: 'Seller wallet balance is insufficient to return the advance'
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
        buyerOnlyReturn: 'Only the buyer can request a refund',
        sellerUsernameRequired: 'Seller account is required',
        repaySellerOnly: 'Only the listing seller can return the advance',
        repayNotRequired: 'There is no pending repayment for this order',
        repayAmountInvalid: 'Invalid repayment amount'
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
          INSTALLMENT: 'Installment (disabled)'
        },
        viewing: {
          title: 'Viewing appointment',
          timeLabel: 'Viewing time: {time}',
          appointment: 'Viewing time: {time}',
          notes: 'Notes: {message}',
          schedule: 'Schedule viewing',
          advance: 'Advance to {stage}',
          markViewed: 'Mark as viewed',
          confirmedBuyer: 'Buyer confirmed viewing',
          confirmedSeller: 'Seller confirmed viewing'
        },
        actions: {
          requestReturn: 'Request a return',
          viewDetails: 'View details',
          hideDetails: 'Hide details'
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
        },
        repayment: {
          pending: 'The platform advanced the buyer refund. Please return ￥{amount} to the platform.',
          completed: 'Repayment completed on {time}.'
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
        returnRequestedSeller: 'The buyer has requested a return. Please review the order promptly.',
        returnCompletedSeller: 'The return is complete. Listing “{title}” has been relisted—please follow up soon.',
        upcomingViewing: 'Appointment: {time}',
        markRead: 'Mark as read',
        openTarget: 'Go to task',
        recoverDescription: 'The platform advanced ￥{amount} to the buyer. Please confirm recovery and refund the platform.',
        recoverAction: 'Confirm recovery',
        recoverConfirm: 'Confirm refunding ￥{amount} to the platform account?',
        recoverSuccess: 'Refunded ￥{amount} back to the platform account.'
      },
      quickMessages: {
        confirmViewing: 'I confirm the viewing at {time}. See you then!',
        confirmViewingNoTime: 'I have received the viewing arrangement. Please confirm.'
      }
    },
    reviews: {
      title: 'Listing feedback',
      subtitle: 'You can only review properties you have purchased. Submitted feedback requires administrator approval before publication.',
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
        ratingRequired: 'Please choose a rating.',
        purchaseRequired: 'Only properties that you purchased can be reviewed.'
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
  '卖家钱包余额不足，无法归还垫付金额': 'serverMessages.wallet.sellerRepayInsufficient',
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
  '卖家账号不能为空': 'serverMessages.orders.sellerUsernameRequired',
  '仅房源卖家可以归还垫付金额': 'serverMessages.orders.repaySellerOnly',
  '当前订单暂无待归还金额': 'serverMessages.orders.repayNotRequired',
  '归还金额无效': 'serverMessages.orders.repayAmountInvalid',
  '买家未完成实名认证，无法预定房源': 'errors.reserveVerifyFirst',
  '买家未完成实名认证，无法购买房源': 'errors.purchaseVerifyFirst',
  '房源尚未通过审核，暂不可预定。': 'errors.reserveNotApproved',
  '房源尚未通过审核，暂不可购买。': 'errors.purchaseNotApproved',
  '积分不足': 'prediction.errors.pointsInsufficient',
  '请求参数校验失败': 'serverMessages.generic.validationFailed',
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

watch(
  () => currentUser.value?.username,
  (username) => {
    if (!username) {
      favoriteHouseIds.value = new Set();
      return;
    }
    favoriteHouseIds.value = loadFavoriteIds(username);
  },
  { immediate: true }
);

watch(
  () => favoriteIdList.value,
  (ids) => {
    const username = currentUser.value?.username;
    if (!username) {
      return;
    }
    persistFavoriteIds(username, ids);
  },
  { deep: true }
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

const walletBalanceLabel = computed(() => {
  if (!wallet.value || wallet.value.balance == null) {
    return '—';
  }
  return formatCurrencyYuan(wallet.value.balance);
});

const walletPointsLabel = computed(() => {
  if (!wallet.value || wallet.value.points == null) {
    return '—';
  }
  const locale = settings.language === 'en' ? 'en-US' : 'zh-CN';
  return Number(wallet.value.points ?? 0).toLocaleString(locale, {
    maximumFractionDigits: 0
  });
});

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

const marketableStatuses = ['APPROVED', 'SOLD'];
const isMarketVisible = (house) => {
  if (!house || typeof house !== 'object') {
    return false;
  }
  return marketableStatuses.includes(house.status);
};

const marketplaceHouses = computed(() => houses.value.filter(isMarketVisible));

const favoriteHouses = computed(() => {
  const set = favoriteHouseIds.value;
  if (!(set instanceof Set) || set.size === 0) {
    return [];
  }
  return houses.value.filter((house) => {
    if (!house || house.id == null) {
      return false;
    }
    return set.has(String(house.id)) && isMarketVisible(house);
  });
});

const reviewableHouseIds = computed(() => {
  if (!currentUser.value) {
    return [];
  }
  const username = String(currentUser.value.username ?? '');
  if (!username) {
    return [];
  }
  const list = Array.isArray(orders.value) ? orders.value : [];
  const eligible = list.filter((order) => {
    if (!order || String(order.buyerUsername ?? '') !== username) {
      return false;
    }
    const stage = order.progressStage;
    const status = order.status;
    if (status === 'RETURNED') {
      return true;
    }
    if (stage === 'HANDOVER_COMPLETED' || stage === 'FUNDS_RELEASED') {
      return true;
    }
    return order.adminReviewed && order.fundsReleasedTo === 'SELLER';
  });
  return Array.from(new Set(eligible.map((order) => String(order.houseId))));
});

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
    const status = order.status;
    const viewingDate = order.viewingTime ? new Date(order.viewingTime) : null;
    const validViewing = viewingDate && !Number.isNaN(viewingDate.getTime()) ? viewingDate : null;
    const timeValue = validViewing ? validViewing.getTime() : now;
    const timeLabel = validViewing ? formatLocalDateTime(validViewing) : null;
    const viewingSoon = validViewing ? timeValue <= soonThreshold : false;

    if (isSellerRole(role)) {
      if (order.sellerRepayRequired) {
        const reviewedAt = order.adminReviewedAt ? new Date(order.adminReviewedAt) : new Date(order.updatedAt ?? now);
        const validReviewed = reviewedAt && !Number.isNaN(reviewedAt.getTime()) ? reviewedAt : null;
        const timeValueReviewed = validReviewed ? validReviewed.getTime() : now;
        const repayAmountLabel = formatCurrencyYuan(order.sellerRepayAmount);
        list.push({
          key: `${order.id}-seller-recover`,
          orderId: order.id,
          houseTitle: order.houseTitle,
          stage,
          description: t('orders.urgent.recoverDescription', { amount: repayAmountLabel }),
          timeLabel: validReviewed ? formatLocalDateTime(validReviewed) : null,
          timeValue: timeValueReviewed,
          isSoon: true,
          action: 'seller-repay',
          amount: order.sellerRepayAmount,
          actionLabel: t('orders.urgent.recoverAction')
        });
        return;
      }
      if (status === 'RETURN_REQUESTED') {
        const updated = order.updatedAt ? new Date(order.updatedAt) : new Date(order.createdAt ?? now);
        const requestTime = Number.isNaN(updated.getTime()) ? now : updated.getTime();
        list.push({
          key: `${order.id}-return-request`,
          orderId: order.id,
          houseTitle: order.houseTitle,
          stage,
          description: t('orders.urgent.returnRequestedSeller', {
            buyer: order.buyerDisplayName ?? order.buyerUsername
          }),
          timeLabel: formatLocalDateTime(updated),
          timeValue: requestTime,
          isSoon: true
        });
      } else if (status === 'RETURNED' && order.adminReviewed) {
        const updated = order.updatedAt ? new Date(order.updatedAt) : new Date(order.createdAt ?? now);
        const completeTime = Number.isNaN(updated.getTime()) ? now : updated.getTime();
        list.push({
          key: `${order.id}-return-complete`,
          orderId: order.id,
          houseTitle: order.houseTitle,
          stage,
          description: t('orders.urgent.returnCompletedSeller', { title: order.houseTitle }),
          timeLabel: formatLocalDateTime(updated),
          timeValue: completeTime,
          isSoon: false
        });
      }
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

const hasUrgentAlerts = computed(() => urgentTasks.value.length > 0);

const showUrgentTasks = computed(() => isBuyer.value || isSeller.value);
const canAccessOrders = computed(() => showUrgentTasks.value || isAdmin.value);

const sellerDraftHouses = computed(() => {
  if (!isSeller.value || !currentUser.value?.username) {
    return [];
  }
  const username = String(currentUser.value.username);
  return houses.value.filter(
    (house) => house?.status === 'DRAFT' && String(house?.sellerUsername ?? '') === username
  );
});

const pendingReviewHouses = computed(() =>
  houses.value.filter((house) => house.status === 'PENDING_REVIEW')
);

const reviewLoading = computed(() => loading.value || adminLoading.value);

const pendingHouseReviews = computed(() =>
  houseReviews.value.filter((review) => review.status === 'PENDING')
);

const navigationTabs = computed(() => {
  const tabs = [{ value: 'home', label: t('nav.home'), ariaLabel: t('nav.home') }];
  if (currentUser.value) {
    const favoritesLabel = t('nav.favorites');
    tabs.push({ value: 'favorites', label: favoritesLabel, ariaLabel: favoritesLabel });
  }
  const predictorLabel = t('nav.predictor');
  tabs.push({ value: 'predictor', label: predictorLabel, ariaLabel: predictorLabel });
  const assistantLabel = t('nav.aiAssistant');
  tabs.push({ value: 'assistant', label: assistantLabel, ariaLabel: assistantLabel });
  if (canManageHouses.value) {
    const manageLabel = t('nav.manage');
    const draftsLabel = t('nav.drafts');
    tabs.push({ value: 'manage', label: manageLabel, ariaLabel: manageLabel });
    tabs.push({ value: 'drafts', label: draftsLabel, ariaLabel: draftsLabel });
  }
  const feedbackLabel = t('nav.feedback');
  tabs.push({ value: 'feedback', label: feedbackLabel, ariaLabel: feedbackLabel });
  if (showUrgentTasks.value) {
    const urgentLabel = t('nav.urgent');
    const urgentCount = urgentTasks.value.length;
    const urgentAria = urgentCount
      ? `${urgentLabel} · ${t('orders.urgent.badge', { count: urgentCount })}`
      : urgentLabel;
    tabs.push({
      value: 'urgent',
      label: urgentLabel,
      ariaLabel: urgentAria,
      alert: hasUrgentAlerts.value,
      badgeCount: urgentCount
    });
  }
  if (canAccessOrders.value) {
    const ordersLabel = t('nav.orders');
    tabs.push({ value: 'orders', label: ordersLabel, ariaLabel: ordersLabel });
  }
  const accountLabel = t('nav.account');
  tabs.push({ value: 'account', label: accountLabel, ariaLabel: accountLabel });
  if (isAdmin.value) {
    const pendingLabel = pendingReviewHouses.value.length
      ? t('nav.reviewWithCount', { count: pendingReviewHouses.value.length })
      : t('nav.review');
    tabs.push({ value: 'review', label: pendingLabel, ariaLabel: pendingLabel });
    const adminHousesLabel = t('nav.adminHouses');
    const adminLabel = t('nav.admin');
    const reputationLabel = t('nav.reputation');
    tabs.push({ value: 'admin-houses', label: adminHousesLabel, ariaLabel: adminHousesLabel });
    tabs.push({ value: 'admin', label: adminLabel, ariaLabel: adminLabel });
    tabs.push({ value: 'reputation', label: reputationLabel, ariaLabel: reputationLabel });
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
  if (typeof window !== 'undefined' && window.innerWidth < 1024) {
    menuOpen.value = false;
  }
  if (tab === 'admin') {
    loadAdminOrders();
    loadAdminTopUps();
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

const normalizeHouse = (house = {}) => ({
  id: house.id,
  title: house.title ?? '',
  address: house.address ?? '',
  price: house.price != null ? Number(house.price) : null,
  downPayment: house.downPayment != null ? Number(house.downPayment) : null,
  area: house.area != null ? Number(house.area) : null,
  description: house.description ?? '',
  sellerUsername: house.sellerUsername ?? '',
  sellerName: house.sellerName ?? '',
  contactNumber: house.contactNumber ?? '',
  listingDate: house.listingDate ?? '',
  floor: house.floor != null ? Number(house.floor) : null,
  latitude: house.latitude != null ? Number(house.latitude) : null,
  longitude: house.longitude != null ? Number(house.longitude) : null,
  imageUrls: Array.isArray(house.imageUrls) ? house.imageUrls : [],
  keywords: Array.isArray(house.keywords) ? house.keywords : [],
  status: house.status ?? 'PENDING_REVIEW',
  reviewMessage: house.reviewMessage ?? '',
  reviewedBy: house.reviewedBy ?? '',
  reviewedAt: house.reviewedAt ?? '',
  sensitiveMasked: Boolean(house.sensitiveMasked),
  updatedAt: house.updatedAt ?? '',
  createdAt: house.createdAt ?? '',
  reservationActive: Boolean(house.reservationActive),
  reservationOwnedByRequester: Boolean(house.reservationOwnedByRequester),
  reservationBuyerUsername: house.reservationBuyerUsername ?? ''
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
    maybeBuildSellerContract();
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
  if (!currentUser.value || (!isBuyer.value && !isSeller.value)) {
    return;
  }
  if (!orderId) {
    return;
  }
  const list = Array.isArray(orders.value) ? orders.value : [];
  const order = list.find((item) => String(item.id) === String(orderId));
  if (!order) {
    return;
  }
  if (isBuyer.value && currentUser.value.username !== order.buyerUsername) {
    return;
  }
  if (isSeller.value && currentUser.value.username !== order.sellerUsername) {
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    await client.post(`/orders/${orderId}/viewing/confirm`, {
      requesterUsername: currentUser.value.username
    });
    messages.success = t('success.viewingConfirmed');
    await fetchOrders({ silent: true });
  } catch (error) {
    messages.error = resolveError(error, 'errors.confirmViewing');
  } finally {
    ordersLoading.value = false;
  }
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
  const safePayload = payload ?? {};

  const result = {
    title: (safePayload.title ?? '').trim(),
    address: safePayload.address ?? '',
    price: Number(safePayload.price ?? 0),
    downPayment: Number(safePayload.downPayment ?? 0),
    area: Number(safePayload.area ?? 0),
    description: safePayload.description ? safePayload.description.trim() : '',
    sellerUsername: (safePayload.sellerUsername ?? '').trim(),
    sellerName: (safePayload.sellerName ?? '').trim(),
    contactNumber: (safePayload.contactNumber ?? '').trim(),
    listingDate: safePayload.listingDate ?? '',
    floor: safePayload.floor != null ? Number(safePayload.floor) : null,
    imageUrls: Array.isArray(safePayload.imageUrls)
      ? safePayload.imageUrls
          .map((url) => (url ?? '').trim())
          .filter((url) => url.length > 0)
      : [],
    keywords: Array.isArray(safePayload.keywords)
      ? safePayload.keywords
          .map((keyword) => (keyword ?? '').trim())
          .filter((keyword) => keyword.length > 0)
      : [],
    saveAsDraft: draft
  };

  if (!Number.isFinite(result.price)) {
    result.price = 0;
  }
  if (!Number.isFinite(result.downPayment)) {
    result.downPayment = 0;
  }
  if (!Number.isFinite(result.area)) {
    result.area = 0;
  }
  if (!Number.isFinite(result.floor)) {
    result.floor = null;
  }

  if (isSeller.value) {
    result.sellerUsername = currentUser.value.username;
    if (!result.sellerName) {
      result.sellerName = currentUser.value.displayName ?? '';
    }
  }

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

const preparePurchase = ({ house, paymentMethod }) => {
  if (!isBuyer.value) {
    messages.error = t('errors.purchaseBuyerOnly');
    messages.success = '';
    return null;
  }
  if (!isRealNameVerified.value) {
    messages.error = t('errors.purchaseVerifyFirst');
    messages.success = '';
    return null;
  }
  if (!house || house.status !== 'APPROVED') {
    messages.error = t('errors.purchaseNotApproved');
    messages.success = '';
    return null;
  }
  if (!house.reservationActive || !house.reservationOwnedByRequester) {
    messages.error = t('errors.purchaseReserveFirst');
    messages.success = '';
    return null;
  }
  const username = currentUser.value?.username;
  if (isSeller.value && username && username === house.sellerUsername) {
    messages.error = t('errors.purchaseOwnListing');
    messages.success = '';
    return null;
  }
  messages.error = '';
  messages.success = '';
  return { house, paymentMethod: paymentMethod || 'FULL' };
};

const executePurchase = async ({ house, paymentMethod }) => {
  if (!house?.id || !currentUser.value?.username) {
    return;
  }
  ordersLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    const payload = {
      houseId: house.id,
      buyerUsername: currentUser.value.username,
      paymentMethod: paymentMethod || 'FULL'
    };
    const { data } = await client.post('/orders', payload);
    const payment = formatCurrencyYuan(data.amount);
    const methodLabel = t('payments.full');
    messages.success = t('success.purchaseWithReminder', {
      method: methodLabel,
      title: data.houseTitle,
      amount: payment
    });
    buildContractDownload({ house, amount: payment });
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

const handlePurchase = (options = {}) => {
  const prepared = preparePurchase(options);
  if (!prepared) {
    return;
  }
  pendingPurchaseOptions.value = prepared;
  purchaseContractVisible.value = true;
};

const handlePurchaseAgreementAgree = async () => {
  const prepared = pendingPurchaseOptions.value;
  purchaseContractVisible.value = false;
  pendingPurchaseOptions.value = null;
  if (!prepared) {
    return;
  }
  await executePurchase(prepared);
};

const handlePurchaseAgreementReject = () => {
  purchaseContractVisible.value = false;
  pendingPurchaseOptions.value = null;
  messages.error = t('contracts.errors.purchaseDeclined');
  messages.success = '';
};

const revokeContractDownload = () => {
  if (contractDownload.value?.url) {
    URL.revokeObjectURL(contractDownload.value.url);
  }
  contractDownload.value = null;
};

const normalizeStatusKey = (value) => {
  if (value == null) {
    return '';
  }
  const text = String(value).trim();
  return text ? text.toUpperCase() : '';
};

const buildContractDownload = ({
  house,
  amount,
  buyerNameOverride,
  sellerNameOverride,
  buyerUsernameOverride,
  sellerUsernameOverride
} = {}) => {
  if (!house) {
    return;
  }
  revokeContractDownload();
  const buyerName =
    buyerNameOverride || currentUser.value?.displayName || currentUser.value?.username || t('contracts.common.buyer');
  const sellerName = sellerNameOverride || house.sellerName || house.sellerUsername || t('contracts.common.seller');
  const sellerUsername =
    sellerUsernameOverride || (house.sellerUsername ? `@${house.sellerUsername}` : t('contracts.common.seller'));
  const buyerUsername =
    buyerUsernameOverride || (currentUser.value?.username ? `@${currentUser.value.username}` : t('contracts.common.buyer'));
  const contractText = [
    t('contracts.generated.title', { title: house.title ?? '' }),
    '',
    t('contracts.generated.parties', { seller: sellerName, sellerAccount: sellerUsername, buyer: buyerName, buyerAccount: buyerUsername }),
    t('contracts.generated.house', { address: house.address ?? '', area: house.area ?? '—', price: amount ?? formatCurrencyYuan(house.price) }),
    t('contracts.generated.commitments'),
    '',
    t('contracts.generated.signatures'),
    '',
    t('contracts.generated.signatureLines')
  ].join('\n');
  const blob = new Blob([contractText], { type: 'text/plain' });
  const url = URL.createObjectURL(blob);
  const base = `contract-${house.id ?? Date.now()}`;
  contractDownload.value = {
    url,
    buyerFileName: `${base}-buyer.txt`,
    sellerFileName: `${base}-seller.txt`,
    title: house.title ?? '',
    buyerName,
    sellerName,
    amount: amount ?? formatCurrencyYuan(house.price)
  };
};

const sellerContractStatuses = ['PAID', 'COMPLETED', 'SUCCESS', 'FUNDS_RELEASED'];

const isSellerContractEligible = (order) => {
  if (!order) {
    return false;
  }
  const statusKey = normalizeStatusKey(order.status);
  if (sellerContractStatuses.includes(statusKey)) {
    return true;
  }
  if (order.adminReviewed && order.fundsReleasedTo === 'SELLER') {
    return true;
  }
  return normalizeStatusKey(order.progressStage) === 'FUNDS_RELEASED';
};

const maybeBuildSellerContract = () => {
  if (!isSeller.value || !currentUser.value) {
    return;
  }
  const username = currentUser.value.username;
  if (!username || !Array.isArray(orders.value)) {
    return;
  }
  const eligible = orders.value.filter(
    (order) => order && order.sellerUsername === username && isSellerContractEligible(order)
  );
  if (!eligible.length) {
    return;
  }
  const latest = eligible.sort((a, b) => {
    const timeA = new Date(a.updatedAt || a.createdAt || 0).getTime();
    const timeB = new Date(b.updatedAt || b.createdAt || 0).getTime();
    return timeB - timeA;
  })[0];

  const buyerName = latest.buyerDisplayName || latest.buyerUsername || t('contracts.common.buyer');
  buildContractDownload({
    house: {
      id: latest.houseId,
      title: latest.houseTitle,
      address: latest.houseAddress ?? latest.houseTitle ?? '',
      area: latest.houseArea ?? '—',
      price: latest.amount,
      sellerName: latest.sellerDisplayName,
      sellerUsername: latest.sellerUsername
    },
    amount: formatCurrencyYuan(latest.amount),
    buyerNameOverride: buyerName,
    sellerNameOverride: latest.sellerDisplayName || latest.sellerUsername,
    buyerUsernameOverride: latest.buyerUsername ? `@${latest.buyerUsername}` : latest.buyerUsername,
    sellerUsernameOverride: latest.sellerUsername ? `@${latest.sellerUsername}` : latest.sellerUsername
  });
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

const handleUrgentTaskNavigate = async (task) => {
  if (!task) {
    return;
  }
  if (task.action === 'seller-repay') {
    const username = currentUser.value?.username;
    if (!username) {
      activeTab.value = 'orders';
      return;
    }
    const amountLabel = formatCurrencyYuan(task.amount);
    const confirmed = window.confirm(t('orders.urgent.recoverConfirm', { amount: amountLabel }));
    if (!confirmed) {
      return;
    }
    try {
      await client.post(`/orders/${task.orderId}/seller-repay`, { sellerUsername: username });
      messages.error = '';
      messages.success = t('orders.urgent.recoverSuccess', { amount: amountLabel });
      const dismissKey = task.key ?? task.orderId;
      if (dismissKey != null) {
        handleUrgentTaskRead(dismissKey);
      }
      await Promise.all([
        fetchOrders({ silent: true }),
        fetchWallet({ silent: true })
      ]);
    } catch (error) {
      messages.error = resolveError(error, 'errors.genericServerError');
      messages.success = '';
    }
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
    const bonusPoints = Math.floor((Number(amount) || 0) / 100) * 10;
    await fetchWallet({ silent: true });
    messages.success = t('success.walletTopUpPending', {
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

const handleToggleFavorite = (house) => {
  if (!house || house.id == null) {
    return;
  }
  if (!currentUser.value) {
    messages.error = t('favorites.loginRequired');
    messages.success = '';
    return;
  }
  const id = String(house.id);
  const next = new Set(favoriteHouseIds.value);
  if (next.has(id)) {
    next.delete(id);
    messages.success = t('favorites.removed');
  } else {
    next.add(id);
    messages.success = t('favorites.added');
  }
  messages.error = '';
  favoriteHouseIds.value = next;
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
  if (!reviewableHouseIds.value.includes(normalizedHouseId)) {
    messages.error = t('errors.reviewNotEligible');
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

const loadAdminTopUps = async ({ silent = false } = {}) => {
  if (!isAdmin.value || !currentUser.value) {
    adminPendingTopUps.value = [];
    adminTopUpsLoading.value = false;
    return;
  }
  if (!silent) {
    adminTopUpsLoading.value = true;
  }
  try {
    const { data } = await client.get('/admin/wallet-topups/pending', {
      params: { requester: currentUser.value.username }
    });
    adminPendingTopUps.value = Array.isArray(data) ? data : [];
  } catch (error) {
    messages.error = resolveError(error, 'errors.loadAdminTopUps');
  } finally {
    if (!silent) {
      adminTopUpsLoading.value = false;
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
      decision === 'ACCEPT'
        ? t('success.orderReviewAccepted')
        : t('success.orderReviewRejected');
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

const handleAdminTopUpReview = async ({ topUpId, decision }) => {
  if (!isAdmin.value || !currentUser.value || !topUpId || !decision) {
    return;
  }
  adminTopUpsLoading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    await client.patch(`/admin/wallet-topups/${topUpId}/review`, {
      requesterUsername: currentUser.value.username,
      decision
    });
    messages.success =
      decision === 'APPROVE'
        ? t('success.topUpReviewAccepted')
        : t('success.topUpReviewRejected');
    await Promise.all([
      loadAdminTopUps({ silent: true }),
      fetchWallet({ silent: true })
    ]);
  } catch (error) {
    messages.error = resolveError(error, 'errors.reviewTopUp');
  } finally {
    adminTopUpsLoading.value = false;
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
    loadAdminTopUps();
  }
};

const handleLogout = () => {
  revokeContractDownload();
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
  adminPendingTopUps.value = [];
  adminTopUpsLoading.value = false;
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
      loadAdminTopUps({ silent: true });
    } else {
      adminUsers.value = [];
      adminReputation.value = null;
      adminPendingOrders.value = [];
      adminPendingTopUps.value = [];
    }
    if (role === 'BUYER' || isSellerRole(role)) {
      loadConversations({ silent: true });
    } else {
      resetConversationState();
    }
  }
);

onMounted(() => {
  if (typeof window !== 'undefined') {
    menuOpen.value = window.innerWidth >= 1024;
    window.addEventListener('resize', updateMenuForViewport);
  }
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
        loadAdminTopUps();
        loadAdminOrders();
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

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', updateMenuForViewport);
  }
  revokeContractDownload();
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
  background: transparent;
  border-radius: 0;
  border: none;
  box-shadow: none;
  backdrop-filter: none;
  position: relative;
  overflow: hidden;
}

.app::before,
.app::after {
  display: none;
}

.header {
  padding: 0;
}

.header-carousel {
  width: 100%;
}

.hero-surface {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(2px);
  border-radius: calc(var(--radius-lg) + 0.35rem);
  padding: clamp(1.5rem, 3vw, 2.4rem);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 18px 44px rgba(8, 28, 68, 0.12);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.header-foreground {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  height: 100%;
  color: var(--color-text-strong);
}

.header-top {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) auto;
  align-items: center;
  gap: 1.5rem;
  width: 100%;
}

@media (max-width: 960px) {
  .header-top {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .header-actions {
    justify-content: flex-start;
  }
}

.branding {
  position: relative;
  z-index: 1;
}

.branding h1 {
  margin: 0;
  font-size: 2.65rem;
  letter-spacing: 0.01em;
  text-shadow: 0 10px 28px rgba(32, 74, 132, 0.14);
}

.branding p {
  margin: 0.35rem 0 0;
  font-size: 1.06rem;
  opacity: 0.9;
  color: color-mix(in srgb, var(--color-text-strong) 88%, transparent);
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 1rem;
  flex-wrap: wrap;
  position: relative;
  z-index: 1;
}


.header-actions :deep(.settings-toggle) {
  background: rgba(255, 255, 255, 0.28);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: var(--color-text-on-emphasis);
  box-shadow: 0 12px 28px rgba(55, 112, 178, 0.18);
}

.header-actions :deep(.settings-toggle:hover) {
  background: rgba(255, 255, 255, 0.4);
  color: var(--color-text-on-emphasis);
}

.messages-trigger {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.42), rgba(198, 228, 255, 0.7));
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: var(--radius-pill);
  color: var(--color-text-on-emphasis);
  font-weight: 700;
  letter-spacing: 0.01em;
  padding: 0.72rem 1.8rem;
  transition: background var(--transition-base), transform var(--transition-base),
    box-shadow var(--transition-base);
  backdrop-filter: blur(10px);
  box-shadow: 0 18px 32px rgba(55, 112, 178, 0.2);
}

.messages-trigger:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.55), rgba(187, 221, 255, 0.85));
  transform: translateY(-1px);
  box-shadow: 0 18px 36px rgba(55, 112, 178, 0.25);
}

.login-section {
  display: flex;
  justify-content: center;
  align-items: stretch;
  width: 100%;
  padding: clamp(1.5rem, 6vw, 3rem);
  box-sizing: border-box;
}

.workspace-shell {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid color-mix(in srgb, var(--color-border) 30%, transparent);
  border-radius: calc(var(--radius-lg) + 0.35rem);
  padding: clamp(1.35rem, 3.2vw, 2.4rem);
  box-shadow: 0 18px 42px rgba(24, 48, 88, 0.12);
}

.workspace-layout {
  display: grid;
  grid-template-columns: minmax(72px, 260px) 1fr;
  gap: 1.75rem;
  align-items: start;
}

.menu-wrapper {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  align-items: stretch;
}

.menu-toggle {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.45rem 1.2rem;
  background: color-mix(in srgb, var(--color-surface) 85%, transparent);
  color: var(--color-text-strong);
  font-size: 1.4rem;
  font-weight: 700;
  box-shadow: 0 14px 28px rgba(120, 110, 100, 0.22);
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
}

.menu-toggle:hover,
.menu-toggle:focus-visible {
  transform: translateY(-1px);
  box-shadow: 0 18px 36px rgba(120, 110, 100, 0.26);
  outline: none;
}

.sidebar {
  position: sticky;
  top: 1.5rem;
  align-self: start;
}

.menu-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.35rem 1.2rem;
  background: rgba(255, 255, 255, 0.04);
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 40%, transparent);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
  backdrop-filter: none;
}

.menu-header {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
}

.menu-user {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  color: var(--color-text-strong);
}

.menu-user-name {
  font-size: 1.1rem;
  font-weight: 700;
}

.menu-user-role {
  font-size: 0.95rem;
  color: var(--color-text-muted);
}

.menu-user-account {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.menu-wallet {
  display: grid;
  gap: 0.45rem;
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

.wallet-line {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  padding: 0.65rem 0.75rem;
  background: color-mix(in srgb, var(--panel-card-bg) 35%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border) 60%, transparent);
  border-radius: calc(var(--radius-md) + 0.15rem);
}

.wallet-label {
  color: var(--color-text-muted);
  font-weight: 600;
}

.wallet-value {
  color: var(--color-text-strong);
  font-size: 1.05rem;
  letter-spacing: 0.01em;
  word-break: break-all;
}

.workspace {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
  width: 100%;
  max-width: none;
  margin: 0;
  background: rgba(255, 255, 255, 0.05);
  border-radius: calc(var(--radius-xl) + 0.2rem);
  padding: clamp(1.2rem, 2.6vw, 2.4rem);
  border: 1px solid color-mix(in srgb, var(--color-border) 30%, transparent);
  box-shadow: 0 20px 50px rgba(73, 128, 189, 0.14);
  overflow: hidden;
}

.workspace.home-bleed :deep(.explorer-header) {
  border-radius: calc(var(--radius-xl) + 0.25rem);
  padding: clamp(1.35rem, 3vw, 2.2rem);
}

.workspace.home-bleed :deep(.house-grid) {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: clamp(1.15rem, 2vw, 1.8rem);
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.65rem;
}

.menu-item__label {
  flex: 1;
}

.menu-item__alert {
  width: 0.6rem;
  height: 0.6rem;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.85);
}

:global(body[data-theme='dark']) .menu-item__alert {
  box-shadow: 0 0 0 2px rgba(30, 41, 59, 0.85);
}

.menu-logout {
  align-self: stretch;
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.75rem 1.35rem;
  font-weight: 600;
  color: var(--color-text-on-emphasis);
  background: linear-gradient(135deg, rgba(180, 140, 110, 0.32), rgba(154, 161, 168, 0.36));
  box-shadow: 0 14px 28px rgba(150, 132, 118, 0.24);
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
}

.menu-logout:hover,
.menu-logout:focus-visible {
  transform: translateY(-1px);
  box-shadow: 0 18px 36px rgba(150, 132, 118, 0.3);
  outline: none;
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.2s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
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
  background: color-mix(in srgb, rgba(198, 106, 96, 0.22) 60%, transparent);
  border-radius: var(--radius-md);
  color: color-mix(in srgb, #7b3730 75%, var(--color-text-strong));
  padding: 1.05rem 1.45rem;
  border: 1px solid rgba(198, 106, 96, 0.24);
  backdrop-filter: none;
}

.success {
  background: rgba(46, 130, 86, 0.18);
  border-radius: var(--radius-md);
  color: rgba(233, 255, 241, 0.95);
  margin: 0;
  padding: 0.9rem 1.2rem;
  border: 1px solid rgba(146, 174, 150, 0.35);
  backdrop-filter: none;
}

.contract-download {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.2rem;
  padding: 1rem 1.2rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  background: color-mix(in srgb, var(--panel-card-bg) 95%, transparent);
  box-shadow: 0 20px 45px rgba(93, 220, 255, 0.18);
}

.contract-download__meta {
  margin: 0.2rem 0 0;
  color: var(--color-text-soft);
}

.contract-download__actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.contract-download .cta {
  padding: 0.65rem 1.1rem;
  border-radius: var(--radius-pill);
  text-decoration: none;
  font-weight: 700;
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  color: var(--color-text-strong);
  background: color-mix(in srgb, var(--color-accent) 12%, transparent);
}

.contract-download .cta.primary {
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  box-shadow: var(--button-primary-shadow);
  border: none;
}

.contract-download .cta:hover {
  transform: translateY(-1px);
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

:global(body[data-theme='dark']) .menu-panel {
  background: color-mix(in srgb, var(--color-surface) 78%, transparent);
  border-color: color-mix(in srgb, var(--color-border) 85%, transparent);
}

:global(body[data-theme='dark']) .menu-toggle {
  background: color-mix(in srgb, var(--color-surface) 70%, transparent);
  color: var(--color-text-on-emphasis);
}

:global(body[data-theme='dark']) .menu-wallet {
  color: color-mix(in srgb, var(--color-text-soft) 85%, var(--color-text-on-emphasis));
}

:global(body[data-theme='dark']) .menu-logout {
  background: linear-gradient(135deg, rgba(143, 162, 179, 0.32), rgba(178, 141, 156, 0.35));
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
    grid-template-columns: minmax(0, 1fr);
    gap: 1.25rem;
  }

  .sidebar {
    position: static;
  }

  .menu-wrapper {
    flex-direction: row;
    align-items: center;
    gap: 1rem;
  }

  .menu-panel {
    width: 100%;
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

  .menu-wrapper {
    align-items: stretch;
  }
}
</style>
