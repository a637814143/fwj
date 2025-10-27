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
  background: var(--surface-secondary);
  border-radius: 24px;
  padding: 1.75rem;
  box-shadow: var(--shadow-strong);
  display: grid;
  gap: 1.5rem;
  border: 1px solid var(--surface-border);
  color: var(--text-primary);
}

.mode-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  background: rgba(15, 23, 42, 0.55);
  border-radius: 9999px;
  padding: 0.35rem;
}

.mode-tab {
  background: transparent;
  border: none;
  border-radius: 9999px;
  color: var(--text-secondary);
  cursor: pointer;
  font-weight: 600;
  padding: 0.65rem 1rem;
  transition: all 0.25s ease;
}

.mode-tab:hover,
.mode-tab:focus {
  outline: none;
  color: var(--text-primary);
  background: rgba(96, 165, 250, 0.18);
}

.mode-tab.active {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.85), rgba(99, 102, 241, 0.85));
  color: var(--text-primary);
  box-shadow: 0 16px 30px rgba(59, 130, 246, 0.35);
}

.form {
  display: grid;
  gap: 1rem;
}

.field {
  display: grid;
  gap: 0.55rem;
}

.label,
label {
  font-weight: 600;
  color: var(--text-secondary);
}

input {
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 0.9rem;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  background: rgba(8, 15, 35, 0.75);
  color: var(--text-primary);
  transition: border 0.2s ease, box-shadow 0.2s ease;
}

input:focus {
  outline: none;
  border-color: rgba(96, 165, 250, 0.8);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.25);
}

.roles {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.role-option {
  align-items: center;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 9999px;
  color: var(--text-secondary);
  display: inline-flex;
  gap: 0.4rem;
  padding: 0.45rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.role-option input {
  accent-color: var(--accent);
}

.error {
  color: var(--danger);
  font-size: 0.95rem;
}

.submit {
  background: linear-gradient(135deg, var(--accent), rgba(56, 189, 248, 0.85));
  border: none;
  border-radius: 0.95rem;
  color: var(--text-primary);
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  padding: 0.8rem 1.1rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 18px 34px rgba(37, 99, 235, 0.35);
}

.submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 22px 36px rgba(37, 99, 235, 0.45);
}

.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
