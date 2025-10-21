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
  { value: 'BUYER', label: '买家' }
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
  position: relative;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  padding: 2rem;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border);
  backdrop-filter: blur(var(--glass-blur));
  display: grid;
  gap: 1.75rem;
  overflow: hidden;
}

.auth-panel::before,
.auth-panel::after {
  content: '';
  position: absolute;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.5;
  pointer-events: none;
}

.auth-panel::before {
  top: -140px;
  left: -120px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.5), transparent 70%);
}

.auth-panel::after {
  bottom: -160px;
  right: -120px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.45), transparent 70%);
}

.mode-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
  position: relative;
  z-index: 1;
}

.mode-tab {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: var(--radius-pill);
  color: var(--color-text-strong);
  cursor: pointer;
  font-weight: 600;
  padding: 0.75rem 1rem;
  transition: all var(--transition-base);
  backdrop-filter: blur(calc(var(--glass-blur) / 2));
}

.mode-tab:hover,
.mode-tab:focus {
  outline: none;
  color: #1d4ed8;
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.18);
}

.mode-tab.active {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #ffffff;
  box-shadow: 0 16px 32px rgba(99, 102, 241, 0.28);
  border-color: transparent;
}

.form {
  display: grid;
  gap: 1.1rem;
  position: relative;
  z-index: 1;
}

.field {
  display: grid;
  gap: 0.55rem;
}

.label,
label {
  font-weight: 600;
  color: var(--color-text-strong);
}

input {
  border-radius: var(--radius-md);
  border: 1px solid rgba(148, 163, 184, 0.35);
  padding: 0.8rem 1rem;
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.85);
  transition: border-color var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(calc(var(--glass-blur) / 3));
}

input:focus {
  outline: none;
  border-color: rgba(99, 102, 241, 0.65);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.2);
  background: rgba(255, 255, 255, 0.95);
}

.roles {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.role-option {
  align-items: center;
  background: rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-pill);
  color: var(--color-text-strong);
  display: inline-flex;
  gap: 0.35rem;
  padding: 0.5rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}

.role-option input {
  accent-color: #4f46e5;
}

.role-option:hover {
  border-color: rgba(99, 102, 241, 0.55);
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.18);
}

.error {
  color: #dc2626;
  font-size: 0.95rem;
  background: rgba(254, 226, 226, 0.65);
  border-radius: var(--radius-md);
  padding: 0.6rem 0.9rem;
  border: 1px solid rgba(248, 113, 113, 0.35);
}

.submit {
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-pill);
  color: white;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  padding: 0.85rem 1.4rem;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  box-shadow: 0 18px 35px rgba(37, 99, 235, 0.28);
  justify-self: start;
}

.submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 24px 42px rgba(37, 99, 235, 0.32);
}

.submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 640px) {
  .auth-panel {
    padding: 1.5rem;
  }
}
</style>
