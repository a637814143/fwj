<template>
  <div class="auth-panel">
    <div class="mode-toggle" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['mode-tab', { active: tab.value === mode }]"
        type="button"
        role="tab"
        @click="switchMode(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <form v-if="mode === 'login'" class="form" @submit.prevent="submitLogin">
      <div class="field">
        <label for="login-username">用户名</label>
        <input
          id="login-username"
          v-model.trim="loginForm.username"
          type="text"
          placeholder="请输入用户名"
          :disabled="loading"
          required
        />
      </div>

      <div class="field">
        <label for="login-password">密码</label>
        <input
          id="login-password"
          v-model.trim="loginForm.password"
          type="password"
          placeholder="请输入密码"
          :disabled="loading"
          required
        />
      </div>

      <p v-if="loginError" class="error">{{ loginError }}</p>

      <button class="submit" type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>

    <form v-else class="form" @submit.prevent="submitRegister">
      <div class="field">
        <label for="register-username">用户名</label>
        <input
          id="register-username"
          v-model.trim="registerForm.username"
          type="text"
          placeholder="请输入用户名"
          :disabled="loading"
          required
        />
      </div>

      <div class="field">
        <label for="register-display-name">昵称</label>
        <input
          id="register-display-name"
          v-model.trim="registerForm.displayName"
          type="text"
          placeholder="请输入昵称"
          :disabled="loading"
          required
        />
      </div>

      <div class="field">
        <label for="register-password">密码</label>
        <input
          id="register-password"
          v-model.trim="registerForm.password"
          type="password"
          placeholder="请输入密码（至少6位）"
          :disabled="loading"
          required
        />
      </div>

      <div class="field">
        <label for="register-confirm">确认密码</label>
        <input
          id="register-confirm"
          v-model.trim="registerForm.confirm"
          type="password"
          placeholder="请再次输入密码"
          :disabled="loading"
          required
        />
      </div>

      <div class="field">
        <span class="label">选择角色</span>
        <div class="roles">
          <label v-for="role in roles" :key="role.value" class="role-option">
            <input
              v-model="registerForm.role"
              type="radio"
              name="register-role"
              :value="role.value"
              :disabled="loading"
            />
            <span>{{ role.label }}</span>
          </label>
        </div>
      </div>

      <p v-if="registerError" class="error">{{ registerError }}</p>

      <button class="submit" type="submit" :disabled="loading">
        {{ loading ? '注册中...' : '注册并登录' }}
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

const tabs = [
  { value: 'login', label: '账号登录' },
  { value: 'register', label: '注册新账号' }
];

const roles = [
  { value: 'SELLER', label: '卖家' },
  { value: 'BUYER', label: '买家' },
  { value: 'ADMIN', label: '系统管理员' }
];

const mode = ref('login');
const loading = ref(false);
const loginError = ref('');
const registerError = ref('');

const loginForm = reactive({ username: '', password: '' });
const registerForm = reactive({
  username: '',
  password: '',
  confirm: '',
  displayName: '',
  role: roles[0].value
});

const client = axios.create({
  baseURL: props.apiBaseUrl,
  headers: { 'Content-Type': 'application/json' }
});

const resetForms = () => {
  loginForm.username = '';
  loginForm.password = '';
  registerForm.username = '';
  registerForm.password = '';
  registerForm.confirm = '';
  registerForm.displayName = '';
  registerForm.role = roles[0].value;
};

const switchMode = (value) => {
  mode.value = value;
  loading.value = false;
  loginError.value = '';
  registerError.value = '';
  resetForms();
};

const submitLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    loginError.value = '请输入用户名和密码';
    return;
  }

  loading.value = true;
  loginError.value = '';

  try {
    const { data } = await client.post('/auth/login', {
      username: loginForm.username,
      password: loginForm.password
    });
    emit('login-success', data);
    resetForms();
  } catch (err) {
    const detail = err.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      loginError.value = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      loginError.value = detail?.detail ?? '登录失败，请稍后再试。';
    }
  } finally {
    loading.value = false;
  }
};

const submitRegister = async () => {
  if (!registerForm.username || !registerForm.password || !registerForm.confirm || !registerForm.displayName) {
    registerError.value = '请完整填写注册信息';
    return;
  }

  if (registerForm.password !== registerForm.confirm) {
    registerError.value = '两次输入的密码不一致';
    return;
  }

  loading.value = true;
  registerError.value = '';

  try {
    const { data } = await client.post('/auth/register', {
      username: registerForm.username,
      password: registerForm.password,
      displayName: registerForm.displayName,
      role: registerForm.role
    });
    emit('login-success', data);
    resetForms();
    switchMode('login');
  } catch (err) {
    const detail = err.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      registerError.value = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      registerError.value = detail?.detail ?? '注册失败，请稍后再试。';
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-panel {
  background: #ffffff;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 15px 40px rgba(79, 70, 229, 0.15);
  display: grid;
  gap: 1.5rem;
}

.mode-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.mode-tab {
  background: #f3f4f6;
  border: none;
  border-radius: 9999px;
  color: #374151;
  cursor: pointer;
  font-weight: 600;
  padding: 0.75rem 1rem;
  transition: all 0.2s ease;
}

.mode-tab:hover,
.mode-tab:focus {
  outline: none;
  background: #e0e7ff;
  color: #1d4ed8;
}

.mode-tab.active {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #ffffff;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.35);
}

.form {
  display: grid;
  gap: 1rem;
}

.field {
  display: grid;
  gap: 0.5rem;
}

.label,
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

.roles {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.role-option {
  align-items: center;
  background: #f3f4f6;
  border-radius: 9999px;
  color: #374151;
  display: inline-flex;
  gap: 0.35rem;
  padding: 0.4rem 0.9rem;
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
  box-shadow: 0 12px 25px rgba(37, 99, 235, 0.25);
}

.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
