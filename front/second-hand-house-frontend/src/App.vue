<template>
  <div class="app">
    <header class="header">
      <div class="branding">
        <h1>二手房屋售卖系统</h1>
        <p>支持线上预定、购房支付与信誉评估的综合平台。</p>
      </div>
      <div v-if="currentUser" class="session">
        <div class="identity">
          <span>
            当前角色：<strong>{{ roleLabels[currentUser.role] }}</strong>（{{ currentUser.displayName }}）
          </span>
          <span class="reputation">信誉分：{{ currentUser.reputationScore ?? '—' }}</span>
          <span class="verification-status">
            实名认证：
            <strong>{{ currentUser.realNameVerified ? '已完成' : '待完成' }}</strong>
          </span>
        </div>
        <div class="session-actions">
          <button
            v-if="canUseMessaging"
            type="button"
            class="messages-trigger"
            @click="openConversationPanel"
          >
            消息中心
          </button>
          <button type="button" class="logout" @click="handleLogout">退出登录</button>
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
        <strong>提示：</strong> {{ messages.error }}
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
            @request-return="handleRequestReturn"
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
      @close="conversationPanelVisible = false"
      @refresh-conversations="loadConversations()"
      @select-conversation="handleSelectConversation"
      @send-message="handleSendConversationMessage"
    />

    <footer class="footer">
      <small>后端接口地址：{{ apiBaseUrl }}</small>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
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

const formatCurrencyYuan = (value) => {
  const numeric = Number(value ?? 0);
  if (!Number.isFinite(numeric)) {
    return '0.00';
  }
  return (numeric * 10000).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const roleLabels = {
  SELLER: '卖家',
  BUYER: '买家',
  ADMIN: '系统管理员'
};

const listingStatusLabels = {
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
};

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

const pendingReviewHouses = computed(() =>
  houses.value.filter((house) => house.status === 'PENDING_REVIEW')
);

const reviewLoading = computed(() => loading.value || adminLoading.value);

const navigationTabs = computed(() => {
  const tabs = [{ value: 'home', label: '购买首页' }];
  tabs.push({
    value: 'verify',
    label: currentUser.value?.realNameVerified ? '实名认证（已完成）' : '实名认证'
  });
  if (canManageHouses.value) {
    tabs.push({ value: 'manage', label: '房源管理' });
  }
  tabs.push({ value: 'orders', label: '订单与钱包' });
  if (isAdmin.value) {
    const pendingLabel = pendingReviewHouses.value.length
      ? `房源审核（${pendingReviewHouses.value.length}）`
      : '房源审核';
    tabs.push({ value: 'review', label: pendingLabel });
    tabs.push({ value: 'admin', label: '信誉面板' });
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
    console.warn('无法持久化登录状态：', error);
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
    messages.error = error.response?.data?.detail ?? '加载房源数据失败，请检查后端服务。';
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
      messages.error = error.response?.data?.detail ?? '加载推荐用户失败。';
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
    const detail = error.response?.data?.detail ?? '加载钱包信息失败。';
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
    const detail = error.response?.data?.detail ?? '加载订单信息失败。';
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
    conversationError.value = error.response?.data?.detail ?? '加载对话失败。';
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
    conversationError.value = error.response?.data?.detail ?? '加载消息失败。';
  } finally {
    if (!silent) {
      conversationMessagesLoading.value = false;
    }
  }
};

const openConversationPanel = async () => {
  if (!currentUser.value || !canUseMessaging.value) {
    messages.error = '当前角色暂不支持对话功能。';
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
    conversationError.value = error.response?.data?.detail ?? '发送消息失败。';
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
  messages.success = response.message ?? '实名认证信息已更新。';
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

const handleContactSeller = async ({ sellerUsername }) => {
  if (!isBuyer.value || !currentUser.value) {
    messages.error = '只有买家可以主动联系卖家。';
    messages.success = '';
    return;
  }
  if (!isRealNameVerified.value) {
    messages.error = '请先完成实名认证后再联系卖家。';
    messages.success = '';
    return;
  }
  conversationPanelVisible.value = true;
  conversationError.value = '';
  try {
    const { data } = await client.post('/conversations', {
      buyerUsername: currentUser.value.username,
      sellerUsername
    });
    activeConversationId.value = data.id;
    await Promise.all([
      loadConversationMessages(data.id),
      loadConversations({ silent: true })
    ]);
  } catch (error) {
    conversationError.value = error.response?.data?.detail ?? '创建对话失败，请稍后再试。';
  }
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
      messages.error = error.response?.data?.detail ?? '刷新用户信息失败。';
    }
  }
};

const guardReadOnly = () => {
  if (!canManageHouses.value) {
    messages.error = '当前角色仅支持浏览房源，如需维护房源请使用卖家账号。';
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
      const statusLabel = listingStatusLabels[data.status] ?? '待审核';
      messages.success =
        data.status === 'APPROVED'
          ? `房源《${data.title}》已更新并重新上架。`
          : `房源《${data.title}》已更新，当前状态：${statusLabel}。`;
    } else {
      const { data } = await client.post('/houses', requestPayload);
      const statusLabel = listingStatusLabels[data.status] ?? '待审核';
      messages.success =
        data.status === 'APPROVED'
          ? `已新增房源《${data.title}》，已通过审核并上架。`
          : `已提交房源《${data.title}》，当前状态：${statusLabel}。`;
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
      messages.error = detail?.detail ?? '保存房源信息失败。';
    }
  } finally {
    loading.value = false;
  }
};

const handleEdit = (house) => {
  if (isSeller.value && currentUser.value.username !== house.sellerUsername) {
    messages.error = '卖家只能编辑自己发布的房源。';
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
    messages.error = '卖家只能删除自己发布的房源。';
    return;
  }
  if (!window.confirm(`确定要删除房源：${house.title} 吗？`)) {
    return;
  }
  loading.value = true;
  messages.error = '';
  messages.success = '';
  try {
    await client.delete(`/houses/${house.id}`, {
      params: { requester: currentUser.value.username }
    });
    messages.success = `已删除房源《${house.title}》。`;
    await fetchHouses({ silent: true });
    await loadRecommendations();
  } catch (error) {
    messages.error = error.response?.data?.detail ?? '删除房源失败。';
  } finally {
    loading.value = false;
  }
};

const handlePurchase = async ({ house, paymentMethod }) => {
  if (!isBuyer.value) {
    messages.error = '只有买家角色可以发起购买。';
    messages.success = '';
    return;
  }
  if (!isRealNameVerified.value) {
    messages.error = '购买前请先完成实名认证。';
    messages.success = '';
    return;
  }
  if (!house || house.status !== 'APPROVED') {
    messages.error = '房源尚未通过审核，暂不可购买。';
    return;
  }
  if (!paymentMethod) {
    messages.error = '请选择支付方式后再尝试购买。';
    return;
  }
  if (isSeller.value && currentUser.value.username === house.sellerUsername) {
    messages.error = '不能购买自己发布的房源。';
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
    const methodLabel = data.paymentMethod === 'INSTALLMENT' ? '分期' : '全款';
    messages.success = `成功以${methodLabel}方式购买房源《${data.houseTitle}》，支付金额 ￥${payment}。`;
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
      messages.error = detail?.detail ?? '支付失败，请稍后再试。';
    }
  } finally {
    ordersLoading.value = false;
  }
};

const handleReserve = async (house) => {
  if (!isBuyer.value) {
    messages.error = '只有买家角色可以预定房源。';
    messages.success = '';
    return;
  }
  if (!isRealNameVerified.value) {
    messages.error = '预定前请先完成实名认证。';
    messages.success = '';
    return;
  }
  if (!house || house.status !== 'APPROVED') {
    messages.error = '房源尚未通过审核，暂不可预定。';
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
    messages.success = `已成功预定房源《${data.houseTitle}》，定金 ￥${deposit}。`;
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
      messages.error = detail?.detail ?? '预定失败，请稍后再试。';
    }
  } finally {
    reservationLoading.value = false;
    reservationTarget.value = null;
  }
};

const handleTopUp = async ({ amount, reference }) => {
  if (!currentUser.value) {
    messages.error = '请先登录后再使用钱包功能。';
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
    messages.success = `钱包充值成功，充值金额 ￥${formatCurrencyYuan(amount)}。`;
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? '钱包充值失败。';
    }
  } finally {
    walletLoading.value = false;
  }
};

const handleRequestReturn = async ({ orderId, reason }) => {
  if (!currentUser.value) {
    messages.error = '请先登录后再申请退换。';
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
    messages.success = `订单《${data.houseTitle}》已退换成功。`;
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
      messages.error = detail?.detail ?? '退换请求失败。';
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
    messages.success = blacklisted ? `已将账号 ${username} 加入黑名单。` : `已解除账号 ${username} 的黑名单状态。`;
    await loadAdminData();
    await loadRecommendations();
  } catch (error) {
    const detail = error.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      messages.error = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      messages.error = detail?.detail ?? '更新黑名单状态失败。';
    }
  } finally {
    adminLoading.value = false;
  }
};

const handleDeleteUser = async ({ username }) => {
  if (!isAdmin.value || !currentUser.value || !username || username === currentUser.value.username) {
    return;
  }
  const confirmed = window.confirm(`确定要删除账号 ${username} 吗？该操作不可撤销。`);
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
    messages.success = `账号 ${username} 已被删除。`;
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
      messages.error = detail?.detail ?? '删除账号失败。';
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
    const reason = window.prompt('请输入驳回原因', target?.reviewMessage ?? '');
    if (reason === null) {
      return;
    }
    reviewMessage = reason.trim();
    if (!reviewMessage) {
      messages.error = '驳回操作需要填写原因。';
      return;
    }
  } else {
    const remark = window.prompt('审核备注（可选）', target?.reviewMessage ?? '审核通过');
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
    const statusLabel = listingStatusLabels[data.status] ?? '';
    messages.success =
      data.status === 'APPROVED'
        ? `已审核通过房源《${data.title}》。`
        : `已驳回房源《${data.title}》，状态：${statusLabel}，原因：${data.reviewMessage ?? (reviewMessage || '未填写')}`;
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
      messages.error = detail?.detail ?? '提交审核结果失败。';
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
    messages.error = error.response?.data?.detail ?? '加载信誉面板失败。';
  } finally {
    adminLoading.value = false;
  }
};

const handleLoginSuccess = (user) => {
  currentUser.value = user;
  resetConversationState();
  const reputationText = user.reputationScore != null ? ` 当前信誉分：${user.reputationScore}` : '';
  messages.success = `${user.message ?? '登录成功。'}${reputationText}`;
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
      messages.success = '已恢复上次的登录状态。';
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
    console.warn('恢复登录状态失败：', error);
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
  color: #fff;
  padding: 2.35rem;
  border-radius: calc(var(--radius-lg) + 0.35rem);
  box-shadow: 0 26px 60px rgba(14, 165, 233, 0.35);
  display: grid;
  gap: 1.5rem;
  position: relative;
  overflow: hidden;
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
  background: rgba(255, 255, 255, 0.24);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: var(--radius-pill);
  color: #fff;
  font-weight: 600;
  padding: 0.6rem 1.5rem;
  transition: background var(--transition-base), transform var(--transition-base),
    box-shadow var(--transition-base);
  backdrop-filter: blur(8px);
}

.logout:hover,
.messages-trigger:hover {
  background: rgba(255, 255, 255, 0.35);
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
  background: rgba(255, 255, 255, 0.65);
  border-radius: var(--radius-pill);
  border: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(calc(var(--glass-blur) / 2));
}

.menu-item {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.55rem 1.5rem;
  background: transparent;
  color: rgba(37, 99, 235, 0.85);
  font-weight: 600;
  letter-spacing: 0.01em;
  transition: all var(--transition-base);
}

.menu-item.active {
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 18px 34px rgba(37, 99, 235, 0.28);
}

.menu-item:not(.active):hover {
  background: rgba(37, 99, 235, 0.12);
  color: #0f172a;
}

.alert {
  background: rgba(248, 113, 113, 0.16);
  border-radius: var(--radius-md);
  color: #7f1d1d;
  padding: 1.05rem 1.45rem;
  border: 1px solid rgba(239, 68, 68, 0.32);
  backdrop-filter: blur(calc(var(--glass-blur) / 3));
}

.success {
  background: rgba(34, 197, 94, 0.18);
  border-radius: var(--radius-md);
  color: #14532d;
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

@media (max-width: 1024px) {
  .app {
    margin: 2rem;
    padding: 1.75rem;
  }

  .header {
    padding: 2rem 1.75rem;
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

  .logout,
  .messages-trigger {
    width: 100%;
    justify-content: center;
  }
}
</style>
