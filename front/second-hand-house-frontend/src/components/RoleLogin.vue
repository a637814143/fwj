<template>
  <div class="auth-layout">
    <aside class="brand-showcase">
      <div class="brand-copy">
        <p>以 “真” 立企：严筛每套二手房，让信任贯穿安家全程</p>
        <p>守 “诚” 服务：透明交易无套路，二手置业更安心</p>
        <p>持 “暖” 同行：不止匹配房源，更懂你对家的期待</p>
        <p>以 “信” 致远：严控房源质量，让二手房也有品质保障</p>
        <p>承 “责” 前行：从选房到过户，全程护航二手置业路</p>
        <p>循 “善” 服务：倾听需求、精准匹配，让二手买房更省心</p>
      </div>
    </aside>

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
          <label for="login-username">{{ t('auth.fields.username') }}</label>
          <input
            id="login-username"
            v-model.trim="loginForm.username"
            type="text"
            :placeholder="t('auth.placeholders.username')"
            :disabled="loading"
            required
          />
        </div>

        <div class="field">
          <label for="login-password">{{ t('auth.fields.password') }}</label>
          <input
            id="login-password"
            v-model.trim="loginForm.password"
            type="password"
            :placeholder="t('auth.placeholders.password')"
            :disabled="loading"
            required
          />
        </div>

        <div class="field captcha-field">
          <label for="login-captcha">{{ t('auth.fields.captcha') }}</label>
          <div class="captcha-display">
            <span>{{ captcha.question }}</span>
            <button type="button" class="refresh" @click="refreshCaptcha" :disabled="loading">
              {{ t('auth.captcha.refresh') }}
            </button>
          </div>
          <input
            id="login-captcha"
            v-model.trim="loginForm.captcha"
            type="text"
            inputmode="numeric"
            :placeholder="t('auth.placeholders.captchaAnswer')"
            :disabled="loading"
            required
          />
        </div>

        <p v-if="loginError" class="error">{{ loginError }}</p>

        <button class="submit" type="submit" :disabled="loading">
          {{ loading ? t('auth.actions.loggingIn') : t('auth.actions.login') }}
        </button>
      </form>

      <form v-else class="form" @submit.prevent="submitRegister">
        <div class="field">
          <label for="register-username">{{ t('auth.fields.username') }}</label>
          <input
            id="register-username"
            v-model.trim="registerForm.username"
            type="text"
            :placeholder="t('auth.placeholders.username')"
            :disabled="loading"
            required
          />
        </div>

        <div class="field">
          <label for="register-display-name">{{ t('auth.fields.displayName') }}</label>
          <input
            id="register-display-name"
            v-model.trim="registerForm.displayName"
            type="text"
            :placeholder="t('auth.placeholders.displayName')"
            :disabled="loading"
            required
          />
        </div>

        <div class="field">
          <label for="register-password">{{ t('auth.fields.password') }}</label>
          <input
            id="register-password"
            v-model.trim="registerForm.password"
            type="password"
            :placeholder="t('auth.placeholders.passwordWithHint')"
            :disabled="loading"
            required
          />
        </div>

        <div class="field">
          <label for="register-confirm">{{ t('auth.fields.confirmPassword') }}</label>
          <input
            id="register-confirm"
            v-model.trim="registerForm.confirm"
            type="password"
            :placeholder="t('auth.placeholders.confirmPassword')"
            :disabled="loading"
            required
          />
        </div>

        <div class="field">
          <span class="label">{{ t('auth.fields.role') }}</span>
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
          {{ loading ? t('auth.actions.registering') : t('auth.actions.register') }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, inject, reactive, ref, onMounted } from 'vue';
import axios from 'axios';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['login-success']);

const translate = inject('translate', (key) => key);
const t = (key, vars) => translate(key, vars);

const mode = ref('login');
const loading = ref(false);
const loginError = ref('');
const registerError = ref('');

const loginForm = reactive({ username: '', password: '', captcha: '' });
const registerForm = reactive({
  username: '',
  password: '',
  confirm: '',
  displayName: '',
  role: 'SELLER'
});

const captcha = reactive({ question: '', answer: '0' });

const randomInt = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;

const multiplicationPairs = Array.from({ length: 11 })
  .flatMap((_, a) =>
    Array.from({ length: 11 }, (_, b) => ({ a, b, result: a * b }))
  )
  .filter((pair) => pair.result <= 10);

const generateCaptcha = () => {
  const operations = ['+', '-', '×'];
  const op = operations[Math.floor(Math.random() * operations.length)];
  let a = 0;
  let b = 0;
  let answer = 0;
  switch (op) {
    case '+': {
      a = randomInt(0, 10);
      b = randomInt(0, 10 - a);
      answer = a + b;
      break;
    }
    case '×': {
      const pair = multiplicationPairs[randomInt(0, multiplicationPairs.length - 1)];
      a = pair.a;
      b = pair.b;
      answer = pair.result;
      break;
    }
    case '-':
    default: {
      a = randomInt(0, 10);
      b = randomInt(0, a);
      answer = a - b;
      break;
    }
  }
  return {
    question: `${a} ${op} ${b} = ?`,
    answer: String(answer)
  };
};

const refreshCaptcha = () => {
  const next = generateCaptcha();
  captcha.question = next.question;
  captcha.answer = next.answer;
  loginForm.captcha = '';
};

const tabs = computed(() => [
  { value: 'login', label: t('auth.tabs.login') },
  { value: 'register', label: t('auth.tabs.register') }
]);

const roles = computed(() => [
  { value: 'SELLER', label: t('auth.roles.seller') },
  { value: 'BUYER', label: t('auth.roles.buyer') }
]);

const client = axios.create({
  baseURL: props.apiBaseUrl,
  headers: { 'Content-Type': 'application/json' }
});

onMounted(refreshCaptcha);

const resetForms = () => {
  loginForm.username = '';
  loginForm.password = '';
  loginForm.captcha = '';
  registerForm.username = '';
  registerForm.password = '';
  registerForm.confirm = '';
  registerForm.displayName = '';
  registerForm.role = 'SELLER';
};

const switchMode = (value) => {
  mode.value = value;
  loading.value = false;
  loginError.value = '';
  registerError.value = '';
  resetForms();
  refreshCaptcha();
};

const submitLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    loginError.value = t('auth.errors.loginRequired');
    return;
  }

  const captchaAnswer = (loginForm.captcha ?? '').replace(/\s+/g, '');
  if (!captchaAnswer) {
    loginError.value = t('auth.errors.captchaRequired');
    return;
  }
  if (captchaAnswer !== captcha.answer) {
    loginError.value = t('auth.errors.captchaInvalid');
    refreshCaptcha();
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
      loginError.value = detail?.detail ?? t('auth.errors.loginFailed');
    }
  } finally {
    refreshCaptcha();
    loading.value = false;
  }
};

const submitRegister = async () => {
  if (!registerForm.username || !registerForm.password || !registerForm.confirm || !registerForm.displayName) {
    registerError.value = t('auth.errors.registerRequired');
    return;
  }

  if (registerForm.password !== registerForm.confirm) {
    registerError.value = t('auth.errors.passwordMismatch');
    return;
  }

  if (registerForm.password.length < 6) {
    registerError.value = t('auth.errors.passwordLength');
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
      registerError.value = detail?.detail ?? t('auth.errors.registerFailed');
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Ma+Shan+Zheng&display=swap');

.auth-layout {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 420px);
  gap: 2rem;
  align-items: stretch;
}

.brand-showcase {
  border-radius: var(--radius-lg);
  min-height: 420px;
  background: linear-gradient(145deg, rgba(255, 250, 244, 0.92), rgba(239, 223, 210, 0.92));
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  box-shadow: 0 28px 45px rgba(150, 140, 130, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2.75rem 2.25rem;
}

.brand-copy {
  display: grid;
  gap: 1.25rem;
  font-family: 'Ma Shan Zheng', 'STKaiti', 'KaiTi', '楷体', serif;
  font-size: 1.55rem;
  color: rgba(98, 74, 56, 0.92);
  line-height: 1.45;
  text-align: left;
  letter-spacing: 0.015em;
}

.brand-copy p {
  margin: 0;
  text-shadow: 0 12px 24px rgba(120, 95, 75, 0.25);
}

.auth-panel {
  position: relative;
  background: rgba(255, 255, 255, 0.92);
  border-radius: var(--radius-lg);
  padding: 2rem;
  box-shadow: 0 24px 50px rgba(120, 110, 100, 0.2);
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  display: grid;
  gap: 1.75rem;
}

.mode-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.mode-tab {
  background: rgba(248, 244, 239, 0.9);
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  border-radius: var(--radius-pill);
  color: var(--color-text-strong);
  cursor: pointer;
  font-weight: 600;
  padding: 0.75rem 1rem;
  transition: all var(--transition-base);
}

.mode-tab:hover,
.mode-tab:focus {
  outline: none;
  color: #a06f4c;
  border-color: rgba(176, 132, 99, 0.6);
  box-shadow: 0 16px 30px rgba(176, 132, 99, 0.22);
}

.mode-tab.active {
  background: linear-gradient(135deg, #b48c6e, #9aa1a8);
  color: #fff;
  box-shadow: 0 20px 36px rgba(165, 137, 114, 0.3);
  border-color: transparent;
}

.form {
  display: grid;
  gap: 1.1rem;
}

.field {
  display: grid;
  gap: 0.55rem;
}

.captcha-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.6rem 0.9rem;
  border: 1px dashed color-mix(in srgb, var(--color-border) 75%, transparent);
  border-radius: var(--radius-md);
  background: rgba(244, 240, 235, 0.9);
}

.captcha-display span {
  font-weight: 600;
  color: var(--color-text-strong);
}

.captcha-display .refresh {
  border: none;
  background: rgba(180, 140, 110, 0.18);
  color: #a06f4c;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  cursor: pointer;
  font-weight: 600;
  transition: background var(--transition-base), box-shadow var(--transition-base);
}

.captcha-display .refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.captcha-display .refresh:not(:disabled):hover,
.captcha-display .refresh:not(:disabled):focus {
  outline: none;
  background: rgba(180, 140, 110, 0.28);
  box-shadow: 0 10px 18px rgba(180, 140, 110, 0.2);
}

.label,
label {
  font-weight: 600;
  color: var(--color-text-strong);
}

input {
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--color-border) 75%, transparent);
  padding: 0.8rem 1rem;
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.92);
  transition: border-color var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
}

input:focus {
  outline: none;
  border-color: rgba(176, 132, 99, 0.55);
  box-shadow: 0 0 0 4px rgba(176, 132, 99, 0.18);
  background: rgba(255, 255, 255, 0.98);
}

.roles {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.role-option {
  align-items: center;
  background: rgba(250, 246, 240, 0.9);
  border-radius: var(--radius-pill);
  color: var(--color-text-strong);
  display: inline-flex;
  gap: 0.35rem;
  padding: 0.5rem 1rem;
  border: 1px solid color-mix(in srgb, var(--color-border) 75%, transparent);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}

.role-option input {
  accent-color: #b48c6e;
}

.role-option:hover {
  border-color: rgba(176, 132, 99, 0.55);
  box-shadow: 0 14px 28px rgba(176, 132, 99, 0.18);
}

.error {
  color: #b35c4d;
  font-size: 0.95rem;
  background: rgba(235, 202, 195, 0.6);
  border-radius: var(--radius-md);
  padding: 0.6rem 0.9rem;
  border: 1px solid rgba(222, 180, 170, 0.5);
}

.submit {
  background: linear-gradient(135deg, #b48c6e, #9aa1a8);
  border: none;
  border-radius: var(--radius-pill);
  color: #fff;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  padding: 0.85rem 1.4rem;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  box-shadow: 0 22px 40px rgba(150, 132, 118, 0.26);
  justify-self: start;
}

.submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 26px 46px rgba(150, 132, 118, 0.3);
}

.submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 960px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .brand-showcase {
    order: 2;
  }

  .brand-copy {
    text-align: center;
    font-size: 1.35rem;
  }
}

@media (max-width: 640px) {
  .auth-panel {
    padding: 1.6rem;
  }

  .brand-copy {
    font-size: 1.2rem;
    gap: 1rem;
  }
}
</style>
