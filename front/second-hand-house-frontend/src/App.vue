<template>
  <div class="app">
    <header class="header">
      <h1>二手房屋管理系统</h1>
      <p>请选择角色登录后管理房源信息。</p>
      <div v-if="currentUser" class="session">
        <span>
          当前角色：<strong>{{ roleLabels[currentUser.role] }}</strong>（{{ currentUser.displayName }}）
        </span>
        <button type="button" class="logout" @click="handleLogout">退出登录</button>
      </div>
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
            @submit="handleSubmit"
            @cancel="handleCancel"
          />
        </section>

        <section class="list-section">
          <HouseList
            :houses="houses"
            :loading="loading"
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
import { reactive, ref } from 'vue';
import axios from 'axios';
import HouseForm from './components/HouseForm.vue';
import HouseList from './components/HouseList.vue';
import RoleLogin from './components/RoleLogin.vue';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';
const houses = ref([]);
const loading = ref(false);
const selectedHouse = ref(null);
const currentUser = ref(null);
const messages = reactive({ error: '' });

const client = axios.create({
  baseURL: apiBaseUrl,
  headers: { 'Content-Type': 'application/json' }
});

const roleLabels = {
  LANDLORD: '房东',
  BUYER: '买家',
  ADMIN: '系统管理员'
};

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

const handleSubmit = async (payload) => {
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
  fetchHouses();
};

const handleLogout = () => {
  currentUser.value = null;
  houses.value = [];
  selectedHouse.value = null;
  messages.error = '';
};
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
  max-width: 600px;
  margin: 0 auto;
  width: 100%;
}

.content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.form-section,
.list-section {
  background: #ffffff;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.08);
}

.alert {
  background: #fee2e2;
  border: 1px solid #f87171;
  color: #b91c1c;
  padding: 1rem 1.25rem;
  border-radius: 0.75rem;
}

.footer {
  text-align: center;
  color: #475569;
  font-size: 0.9rem;
  padding: 1rem 0;
}

@media (max-width: 768px) {
  .app {
    padding: 1rem;
  }

  .header {
    text-align: center;
  }
}
</style>
