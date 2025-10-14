<template>
  <div class="role-login">
    <div class="role-tabs" role="tablist">
      <button
        v-for="role in roles"
        :key="role.value"
        :class="['role-tab', { active: role.value === selectedRole }]"
        type="button"
        role="tab"
        @click="changeRole(role.value)"
      >
        {{ role.label }}登录
      </button>
    </div>

    <form class="login-form" @submit.prevent="submit">
      <div class="field">
        <label for="username">用户名</label>
        <input
          id="username"
          v-model.trim="form.username"
          type="text"
          placeholder="请输入用户名"
          :disabled="loading"
          required
        />
      </div>

      <div class="field">
        <label for="password">密码</label>
        <input
          id="password"
          v-model.trim="form.password"
          type="password"
          placeholder="请输入密码"
          :disabled="loading"
          required
        />
      </div>

      <p v-if="error" class="error">{{ error }}</p>

      <button class="submit" type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import axios from 'axios';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['login-success']);

const roles = [
  { value: 'LANDLORD', label: '房东' },
  { value: 'BUYER', label: '买家' },
  { value: 'ADMIN', label: '系统管理员' }
];

const selectedRole = ref('LANDLORD');
const form = reactive({ username: '', password: '' });
const loading = ref(false);
const error = ref('');

const resetForm = () => {
  form.username = '';
  form.password = '';
  error.value = '';
};

const changeRole = (role) => {
  selectedRole.value = role;
  resetForm();
};

const submit = async () => {
  if (!form.username || !form.password) {
    error.value = '请输入完整的用户名和密码';
    return;
  }

  loading.value = true;
  error.value = '';

  try {
    const { data } = await axios.post(`${props.apiBaseUrl}/auth/login`, {
      role: selectedRole.value,
      username: form.username,
      password: form.password
    });
    emit('login-success', data);
    resetForm();
  } catch (err) {
    const detail = err.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      error.value = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      error.value = detail?.detail ?? '登录失败，请稍后再试。';
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.role-login {
  background: #ffffff;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 15px 40px rgba(79, 70, 229, 0.15);
  display: grid;
  gap: 1.5rem;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.role-tab {
  background: #f3f4f6;
  border: none;
  border-radius: 9999px;
  color: #374151;
  cursor: pointer;
  font-weight: 600;
  padding: 0.75rem 1rem;
  transition: all 0.2s ease;
}

.role-tab:hover,
.role-tab:focus {
  outline: none;
  background: #e0e7ff;
  color: #1d4ed8;
}

.role-tab.active {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #ffffff;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.35);
}

.login-form {
  display: grid;
  gap: 1rem;
}

.field {
  display: grid;
  gap: 0.5rem;
}

label {
  font-weight: 600;
  color: #374151;
}

input {
  border: 1px solid #e5e7eb;
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  transition: border 0.2s ease, box-shadow 0.2s ease;
}

input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}

.error {
  color: #dc2626;
  font-size: 0.95rem;
}

.submit {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  border: none;
  border-radius: 0.75rem;
  color: white;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  padding: 0.75rem 1rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 15px 35px rgba(37, 99, 235, 0.25);
}

.submit:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}
</style>
