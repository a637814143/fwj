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
            :orders-loading="ordersLoading"
            @edit="handleEdit"
            @remove="handleRemove"
            @purchase="handlePurchase"
            @contact-seller="handleContactSeller"
          />
        </div>

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
            @request-return="handleRequestReturn"
          />
        </div>

        <AdminReputationBoard
          v-else-if="activeTab === 'admin'"
          :loading="adminLoading"
          :overview="adminReputation"
          :users="adminUsers"
          :current-user="currentUser"
          @refresh="loadAdminData"
          @toggle-blacklist="handleToggleBlacklist"
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
import AdminReputationBoard from './components/AdminReputationBoard.vue';
import ConversationPanel from './components/ConversationPanel.vue';

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

const roleLabels = {
  SELLER: '卖家',
  BUYER: '买家',
  ADMIN: '系统管理员'
};

const isSeller = computed(() => currentUser.value?.role === 'SELLER');
const isBuyer = computed(() => currentUser.value?.role === 'BUYER');
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');
const canUseMessaging = computed(() => currentUser.value && ['BUYER', 'SELLER'].includes(currentUser.value.role));

const canManageHouses = computed(
  () => currentUser.value && ['SELLER', 'ADMIN'].includes(currentUser.value.role)
);

const navigationTabs = computed(() => {
  const tabs = [{ value: 'home', label: '购买首页' }];
  if (canManageHouses.value) {
    tabs.push({ value: 'manage', label: '房源管理' });
  }
  tabs.push({ value: 'orders', label: '订单与钱包' });
  if (isAdmin.value) {
    tabs.push({ value: 'admin', label: '信誉面板' });
  }
  return tabs;
});

const switchTab = (tab) => {
  activeTab.value = tab;
  if (tab === 'admin') {
    loadAdminData();
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
  listingDate: house.listingDate ?? '',
  imageUrls: Array.isArray(house.imageUrls) ? house.imageUrls : []
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

const handleContactSeller = async ({ sellerUsername }) => {
  if (!isBuyer.value || !currentUser.value) {
    messages.error = '只有买家可以主动联系卖家。';
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
    messages.error = '当前角色仅支持浏览房源，如需维护房源请使用卖家或系统管理员账号。';
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
      messages.success = `房源《${data.title}》已更新。`;
    } else {
      const { data } = await client.post('/houses', requestPayload);
      messages.success = `已新增房源《${data.title}》。`;
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

const handlePurchase = async (house) => {
  if (!isBuyer.value) {
    messages.error = '只有买家角色可以发起购买。';
    messages.success = '';
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
      buyerUsername: currentUser.value.username
    });
    messages.success = `成功购买房源《${data.houseTitle}》，支付金额 ${Number(data.amount).toFixed(2)} 万元。`;
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
  reservationLoading.value = true;
  reservationTarget.value = house.id;
  messages.error = '';
  messages.success = '';
  try {
    const { data } = await client.post('/orders/reserve', {
      houseId: house.id,
      buyerUsername: currentUser.value.username
    });
    const deposit = Number(data.amount ?? 0).toFixed(2);
    messages.success = `已成功预定房源《${data.houseTitle}》，定金 ${deposit} 万元。`;
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
    messages.success = '钱包充值成功。';
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
  margin: 0 auto;
  max-width: 1200px;
  padding: 1.5rem;
  gap: 1.5rem;
}

.header {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  padding: 1.5rem;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(37, 99, 235, 0.2);
  display: grid;
  gap: 1rem;
}

.branding h1 {
  margin: 0;
  font-size: 2.2rem;
}

.branding p {
  margin: 0;
  font-size: 0.95rem;
  opacity: 0.9;
}

.session {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.session-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.identity {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.95rem;
}

.reputation {
  font-weight: 600;
}

.logout,
.messages-trigger {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 999px;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  padding: 0.5rem 1.25rem;
  transition: background 0.2s ease, transform 0.2s ease;
}

.logout:hover,
.messages-trigger:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: translateY(-1px);
}

.login-section {
  display: flex;
  justify-content: center;
}

.menu {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.menu-item {
  border: none;
  border-radius: 999px;
  padding: 0.5rem 1.25rem;
  background: #e0e7ff;
  color: #1e3a8a;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.menu-item.active {
  background: #1d4ed8;
  color: #fff;
  box-shadow: 0 8px 16px rgba(29, 78, 216, 0.25);
}

.menu-item:not(.active):hover {
  background: #c7d2fe;
}

.alert {
  background: #fee2e2;
  border-left: 4px solid #ef4444;
  border-radius: 0.75rem;
  color: #991b1b;
  padding: 1rem 1.5rem;
}

.success {
  background: rgba(34, 197, 94, 0.18);
  border-left: 4px solid #22c55e;
  border-radius: 0.75rem;
  color: #f8fafc;
  margin: 0;
  padding: 0.75rem 1rem;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.manage-grid,
.orders-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.footer {
  text-align: center;
  color: #475569;
  font-size: 0.85rem;
  margin-top: auto;
}

@media (max-width: 768px) {
  .header {
    text-align: center;
  }

  .identity {
    align-items: center;
  }

  .logout {
    width: 100%;
  }
}
</style>
