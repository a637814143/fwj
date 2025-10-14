<template>
  <div class="app">
    <header class="header">
      <h1>二手房屋管理系统</h1>
      <p>请先登录或注册账号后再管理房源信息。</p>
      <div v-if="currentUser" class="session">
        <span>
          当前角色：<strong>{{ roleLabels[currentUser.role] }}</strong>（{{ currentUser.displayName }}）
        </span>
        <button type="button" class="logout" @click="handleLogout">退出登录</button>
      </div>
      <p v-if="messages.success" class="success">{{ messages.success }}</p>
    </header>

    <section v-if="!currentUser" class="login-section">
      <RoleLogin :api-base-url="apiBaseUrl" @login-success="handleLoginSuccess" />
    </section>

    <template v-else>
      <section v-if="messages.error" class="alert">
        <strong>提示：</strong> {{ messages.error }}
      </section>

      <main class="content">
        <section class="form-section">
          <HouseForm
            :initial-house="selectedHouse"
            :loading="loading"
            :can-manage="canManageHouses"
            @submit="handleSubmit"
            @cancel="handleCancel"
          />
        </section>

        <section class="list-section">
          <HouseList
            :houses="houses"
            :loading="loading"
            :can-manage="canManageHouses"
            @edit="handleEdit"
            @remove="handleRemove"
          />
        </section>
      </main>
    </template>

    <footer class="footer">
      <small>后端接口地址：{{ apiBaseUrl }}</small>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import axios from 'axios';
import HouseForm from './components/HouseForm.vue';
import HouseList from './components/HouseList.vue';
import RoleLogin from './components/RoleLogin.vue';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';
const houses = ref([]);
const loading = ref(false);
const selectedHouse = ref(null);
const currentUser = ref(null);
const messages = reactive({ error: '', success: '' });
const storageKey = 'secondhand-house-current-user';

const client = axios.create({
  baseURL: apiBaseUrl,
  headers: { 'Content-Type': 'application/json' }
});

const roleLabels = {
  LANDLORD: '房东',
  BUYER: '买家',
  ADMIN: '系统管理员'
};

const canManageHouses = computed(
  () => currentUser.value && ['LANDLORD', 'ADMIN'].includes(currentUser.value.role)
);

const fetchHouses = async () => {
  loading.value = true;
  messages.error = '';
  try {
    const { data } = await client.get('/houses');
    houses.value = data.map((house) => ({
      ...house,
      listingDate: house.listingDate ?? ''
    }));
  } catch (error) {
    messages.error = error.response?.data?.detail ?? '加载房源数据失败，请检查后端服务。';
  } finally {
    loading.value = false;
  }
};

const guardReadOnly = () => {
  if (!canManageHouses.value) {
    messages.error = '当前角色仅支持浏览房源，如需维护房源请使用房东或系统管理员账号。';
    return false;
  }
  return true;
};

const handleSubmit = async (payload) => {
  if (!guardReadOnly()) {
    return;
  }

  loading.value = true;
  messages.error = '';

  try {
    if (selectedHouse.value) {
      const { data } = await client.put(`/houses/${selectedHouse.value.id}`, payload);
      houses.value = houses.value.map((house) =>
        house.id === data.id ? data : house
      );
    } else {
      const { data } = await client.post('/houses', payload);
      houses.value = [...houses.value, data];
    }
    selectedHouse.value = null;
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
  selectedHouse.value = { ...house };
};

const handleCancel = () => {
  selectedHouse.value = null;
};

const handleRemove = async (house) => {
  if (!guardReadOnly()) {
    return;
  }

  if (!confirm(`确定要删除房源：${house.title} 吗？`)) {
    return;
  }

  loading.value = true;
  messages.error = '';
  try {
    await client.delete(`/houses/${house.id}`);
    houses.value = houses.value.filter((item) => item.id !== house.id);
  } catch (error) {
    messages.error = error.response?.data?.detail ?? '删除房源失败。';
  } finally {
    loading.value = false;
  }
};

const handleLoginSuccess = (user) => {
  currentUser.value = user;
  messages.success = user.message ?? '';
  messages.error = '';
  try {
    localStorage.setItem(storageKey, JSON.stringify(user));
  } catch (error) {
    console.warn('无法持久化登录状态：', error);
  }
  fetchHouses();
};

const handleLogout = () => {
  currentUser.value = null;
  houses.value = [];
  selectedHouse.value = null;
  messages.error = '';
  messages.success = '';
  localStorage.removeItem(storageKey);
};

onMounted(() => {
  try {
    const cached = localStorage.getItem(storageKey);
    if (cached) {
      const user = JSON.parse(cached);
      currentUser.value = user;
      messages.success = '已恢复上次的登录状态。';
      fetchHouses();
    }
  } catch (error) {
    console.warn('恢复登录状态失败：', error);
    localStorage.removeItem(storageKey);
  }
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

.header h1 {
  margin: 0 0 0.5rem;
  font-size: 2rem;
}

.session {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.logout {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 999px;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  padding: 0.5rem 1.25rem;
  transition: background 0.2s ease, transform 0.2s ease;
}

.logout:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: translateY(-1px);
}

.login-section {
  display: flex;
  justify-content: center;
}

.alert {
  background: #fee2e2;
  border-left: 4px solid #ef4444;
  border-radius: 0.75rem;
  color: #991b1b;
  padding: 1rem 1.5rem;
}

.success {
  background: rgba(34, 197, 94, 0.15);
  border-left: 4px solid #22c55e;
  border-radius: 0.75rem;
  color: #f0fdf4;
  margin: 0;
  padding: 0.75rem 1rem;
}

.content {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.form-section,
.list-section {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.1);
  padding: 1.5rem;
}

.footer {
  text-align: center;
  color: #6b7280;
  padding: 1.5rem 0 0.5rem;
}
</style>
