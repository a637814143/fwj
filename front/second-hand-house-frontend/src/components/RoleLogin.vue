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
          <label for="register-email">{{ t('auth.fields.email') }}</label>
          <input
            id="register-email"
            v-model.trim="registerForm.email"
            type="email"
            :placeholder="t('auth.placeholders.email')"
            :disabled="loading"
            required
          />
        </div>

        <div class="field">
          <label for="register-code">{{ t('auth.fields.verificationCode') }}</label>
          <div class="verification-field">
            <input
              id="register-code"
              v-model.trim="registerForm.verificationCode"
              type="text"
              inputmode="numeric"
              maxlength="6"
              :placeholder="t('auth.placeholders.verificationCode')"
              :disabled="loading"
              required
            />
            <button
              type="button"
              class="code-button"
              @click="requestVerificationCode"
              :disabled="!canRequestCode"
            >
              <span v-if="verificationCountdown > 0">
                {{ t('auth.actions.resendIn', { seconds: verificationCountdown }) }}
              </span>
              <span v-else>
                {{ codeLoading ? t('auth.actions.sendingCode') : t('auth.actions.sendCode') }}
              </span>
            </button>
          </div>
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
          <div
            v-if="registerForm.password"
            class="password-strength"
            :data-level="passwordStrength.level"
          >
            <div class="password-strength__label">{{ t('auth.password.label') }}</div>
            <div class="password-strength__meter"></div>
            <div class="password-strength__text">{{ passwordStrength.label }}</div>
          </div>
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

        <p v-if="registerInfo" class="info">{{ registerInfo }}</p>
        <p v-if="registerError" class="error">{{ registerError }}</p>

        <button class="submit" type="submit" :disabled="loading">
          {{ loading ? t('auth.actions.registering') : t('auth.actions.register') }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, inject, reactive, ref, onMounted, onUnmounted } from 'vue';
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
  email: '',
  verificationCode: '',
  role: 'SELLER'
});

const captcha = reactive({ question: '', answer: '0' });

const registerInfo = ref('');
const codeLoading = ref(false);
const verificationCountdown = ref(0);
let countdownTimer = null;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/i;

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
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
});

const startCountdown = (seconds = 60) => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
  verificationCountdown.value = seconds;
  countdownTimer = setInterval(() => {
    if (verificationCountdown.value <= 1) {
      verificationCountdown.value = 0;
      clearInterval(countdownTimer);
      countdownTimer = null;
    } else {
      verificationCountdown.value -= 1;
    }
  }, 1000);
};

const containsSequentialPattern = (value) => {
  if (!value) {
    return false;
  }

  const chars = Array.from(value);
  let ascending = 1;
  let descending = 1;
  const isSequential = (prev, current, step) => {
    if (/\d/.test(prev) && /\d/.test(current)) {
      return current.charCodeAt(0) - prev.charCodeAt(0) === step;
    }
    if (/[a-z]/i.test(prev) && /[a-z]/i.test(current)) {
      return current.toLowerCase().charCodeAt(0) - prev.toLowerCase().charCodeAt(0) === step;
    }
    return false;
  };
  for (let i = 1; i < chars.length; i += 1) {
    const prev = chars[i - 1];
    const current = chars[i];
    if (isSequential(prev, current, 1)) {
      ascending += 1;
    } else {
      ascending = 1;
    }
    if (isSequential(prev, current, -1)) {
      descending += 1;
    } else {
      descending = 1;
    }
    if (ascending >= 3 || descending >= 3) {
      return true;
    }
  }

  const keyboardRows = ['1234567890', 'qwertyuiop', 'asdfghjkl', 'zxcvbnm'];
  const lower = value.toLowerCase();
  for (let i = 0; i <= lower.length - 3; i += 1) {
    const slice = lower.slice(i, i + 3);
    const reversed = slice.split('').reverse().join('');
    if (keyboardRows.some((row) => row.includes(slice) || row.includes(reversed))) {
      return true;
    }
  }

  return false;
};

const evaluatePasswordStrength = (value) => {
  const password = (value ?? '').trim();
  if (!password) {
    return { level: 'empty', label: '', valid: false };
  }
  const hasUpper = /[A-Z]/.test(password);
  const hasLower = /[a-z]/.test(password);
  const hasDigit = /\d/.test(password);
  const hasSequential = containsSequentialPattern(password);
  const valid = hasUpper && hasLower && hasDigit && !hasSequential && password.length >= 6;
  let level = 'weak';
  if (!valid) {
    level = 'invalid';
  } else if (password.length >= 12) {
    level = 'strong';
  } else if (password.length >= 8) {
    level = 'medium';
  }
  const labelKey = level === 'invalid' ? 'auth.password.strength.invalid' : `auth.password.strength.${level}`;
  return {
    level,
    valid,
    label: t(labelKey)
  };
};

const passwordStrength = computed(() => evaluatePasswordStrength(registerForm.password));

const canRequestCode = computed(() => verificationCountdown.value === 0 && !codeLoading.value && !loading.value);

const resetForms = () => {
  loginForm.username = '';
  loginForm.password = '';
  loginForm.captcha = '';
  registerForm.username = '';
  registerForm.password = '';
  registerForm.confirm = '';
  registerForm.displayName = '';
  registerForm.email = '';
  registerForm.verificationCode = '';
  registerForm.role = 'SELLER';
  registerInfo.value = '';
};

const switchMode = (value) => {
  mode.value = value;
  loading.value = false;
  loginError.value = '';
  registerError.value = '';
  registerInfo.value = '';
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
  verificationCountdown.value = 0;
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
  if (!registerForm.username || !registerForm.password || !registerForm.confirm || !registerForm.displayName || !registerForm.email || !registerForm.verificationCode) {
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

  if (!passwordStrength.value.valid) {
    registerError.value = t('auth.errors.passwordWeak');
    return;
  }

  const email = (registerForm.email ?? '').trim();
  if (!emailPattern.test(email)) {
    registerError.value = t('auth.errors.emailInvalid');
    return;
  }

  const code = (registerForm.verificationCode ?? '').trim();
  if (!/^[0-9]{6}$/.test(code)) {
    registerError.value = t('auth.errors.verificationFormat');
    return;
  }

  loading.value = true;
  registerError.value = '';
  registerInfo.value = '';

  try {
    const { data } = await client.post('/auth/register', {
      username: registerForm.username,
      password: registerForm.password,
      displayName: registerForm.displayName,
      role: registerForm.role,
      email,
      verificationCode: code
    });
    emit('login-success', data);
    resetForms();
    switchMode('login');
    verificationCountdown.value = 0;
    if (countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
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

const requestVerificationCode = async () => {
  if (!canRequestCode.value) {
    return;
  }
  const email = (registerForm.email ?? '').trim();
  if (!email) {
    registerError.value = t('auth.errors.emailRequired');
    return;
  }
  if (!emailPattern.test(email)) {
    registerError.value = t('auth.errors.emailInvalid');
    return;
  }

  registerError.value = '';
  registerInfo.value = '';
  codeLoading.value = true;
  try {
    await client.post('/auth/register/verification-code', { email });
    registerInfo.value = t('auth.messages.codeSent', { email });
    startCountdown();
  } catch (err) {
    const detail = err.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      registerError.value = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      registerError.value = detail?.detail ?? t('auth.errors.sendCodeFailed');
    }
  } finally {
    codeLoading.value = false;
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Ma+Shan+Zheng&display=swap');

.auth-layout {
  position: relative;
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 420px);
  gap: 2rem;
  align-items: stretch;
  justify-content: center;
  width: min(100%, 1080px);
  margin: 0 auto;
  padding: clamp(1.5rem, 5vw, 3rem);
  box-sizing: border-box;
  min-height: 100vh;
  isolation: isolate;
}

.auth-layout::before,
.auth-layout::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -2;
}

.auth-layout::before {
  background: linear-gradient(180deg, rgba(12, 35, 68, 0.35) 0%, rgba(12, 35, 68, 0.45) 100%),
    url('https://images.pexels.com/photos/460672/pexels-photo-460672.jpeg?auto=compress&cs=tinysrgb&w=1920')
      center/cover no-repeat;
  filter: saturate(1.08);
}

.auth-layout::after {
  background: radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.24), transparent 38%),
    radial-gradient(circle at 80% 10%, rgba(255, 255, 255, 0.18), transparent 45%),
    linear-gradient(135deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.3));
  z-index: -1;
}

.brand-showcase {
  border-radius: var(--radius-lg);
  min-height: 420px;
  background: rgba(255, 255, 255, 0.42);
  border: 1px solid rgba(255, 255, 255, 0.28);
  box-shadow: 0 28px 45px rgba(12, 35, 68, 0.18);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2.75rem 2.25rem;
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
}

.brand-copy {
  display: grid;
  gap: 1.25rem;
  font-family: 'Ma Shan Zheng', 'STKaiti', 'KaiTi', '楷体', serif;
  font-size: 1.55rem;
  color: rgba(24, 76, 120, 0.95);
  line-height: 1.45;
  text-align: left;
  letter-spacing: 0.015em;
}

.brand-copy p {
  margin: 0;
  text-shadow: 0 12px 24px rgba(61, 112, 168, 0.2);
}

.auth-panel {
  position: relative;
  background: rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-lg);
  padding: 1.9rem;
  box-shadow: 0 20px 44px rgba(12, 35, 68, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.45);
  display: grid;
  gap: 1.65rem;
  width: 100%;
  max-width: 460px;
  margin: 0 auto;
  backdrop-filter: blur(12px) saturate(1.05);
}

.mode-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.25rem;
}

.mode-tab {
  background: rgba(240, 247, 255, 0.96);
  border: 1px solid color-mix(in srgb, var(--color-border) 90%, transparent);
  border-radius: 999px;
  color: var(--color-text-muted);
  cursor: pointer;
  font-weight: 700;
  padding: 1.4rem 2rem;
  min-width: 160px;
  min-height: 150px;
  transition: all var(--transition-base);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  letter-spacing: 0.02em;
}

.mode-tab:hover,
.mode-tab:focus {
  outline: none;
  color: #1f70c8;
  border-color: rgba(80, 140, 210, 0.7);
  box-shadow: 0 16px 30px rgba(80, 140, 210, 0.16);
}

.mode-tab.active {
  background: linear-gradient(135deg, #6db7ff, #5a94ff);
  color: #0a1a2f;
  box-shadow: 0 20px 36px rgba(80, 140, 210, 0.28);
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

.verification-field {
  display: flex;
  gap: 0.75rem;
  align-items: stretch;
  flex-wrap: wrap;
}

.verification-field input {
  flex: 1;
}

.code-button {
  border: none;
  background: linear-gradient(135deg, #b48c6e, #9aa1a8);
  color: #fff;
  font-weight: 600;
  padding: 0 1.2rem;
  border-radius: var(--radius-md);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 9rem;
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    filter var(--transition-base);
}

.code-button:not(:disabled):hover,
.code-button:not(:disabled):focus {
  outline: none;
  transform: translateY(-1px);
  box-shadow: 0 12px 20px rgba(165, 137, 114, 0.28);
}

.code-button:disabled {
  cursor: not-allowed;
  filter: grayscale(0.15);
  opacity: 0.7;
}

.password-strength {
  display: grid;
  gap: 0.35rem;
  margin-top: 0.5rem;
}

.password-strength__label {
  font-size: 0.85rem;
  color: color-mix(in srgb, var(--color-text) 60%, transparent);
  font-weight: 600;
}

.password-strength__meter {
  position: relative;
  height: 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-border) 65%, transparent);
  overflow: hidden;
}

.password-strength__meter::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 0;
  background: linear-gradient(135deg, #f39c12, #f1c40f);
  transition: width var(--transition-base), background var(--transition-base);
}

.password-strength[data-level='invalid'] .password-strength__meter::after {
  width: 25%;
  background: linear-gradient(135deg, #d64541, #c0392b);
}

.password-strength[data-level='weak'] .password-strength__meter::after {
  width: 33%;
  background: linear-gradient(135deg, #f39c12, #f1c40f);
}

.password-strength[data-level='medium'] .password-strength__meter::after {
  width: 66%;
  background: linear-gradient(135deg, #f39c12, #27ae60);
}

.password-strength[data-level='strong'] .password-strength__meter::after {
  width: 100%;
  background: linear-gradient(135deg, #2ecc71, #27ae60);
}

.password-strength__text {
  font-size: 0.9rem;
  color: color-mix(in srgb, var(--color-text-strong) 80%, transparent);
  font-weight: 600;
}

.password-strength[data-level='invalid'] .password-strength__text {
  color: #c0392b;
}

.password-strength[data-level='weak'] .password-strength__text {
  color: #f39c12;
}

.password-strength[data-level='medium'] .password-strength__text {
  color: #e67e22;
}

.password-strength[data-level='strong'] .password-strength__text {
  color: #27ae60;
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
  border: 1px solid rgba(255, 255, 255, 0.6);
  padding: 0.8rem 1rem;
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.9);
  transition: border-color var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
  color: var(--color-text-strong);
  backdrop-filter: blur(4px);
}

input:focus {
  outline: none;
  border-color: rgba(176, 132, 99, 0.75);
  box-shadow: 0 0 0 4px rgba(176, 132, 99, 0.22);
  background: rgba(255, 255, 255, 0.96);
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

.info {
  color: #257a7a;
  font-size: 0.95rem;
  background: rgba(204, 236, 228, 0.65);
  border-radius: var(--radius-md);
  padding: 0.6rem 0.9rem;
  border: 1px solid rgba(150, 210, 200, 0.5);
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
    padding: clamp(1rem, 6vw, 2rem);
    gap: 1.5rem;
  }

  .brand-showcase {
    order: 2;
    max-width: 620px;
    margin: 0 auto;
  }

  .brand-copy {
    text-align: center;
    font-size: 1.35rem;
  }
}

@media (max-width: 720px) {
  .auth-layout {
    padding: clamp(0.75rem, 5vw, 1.5rem);
    gap: 1.4rem;
  }

  .brand-showcase {
    padding: 2.25rem 1.75rem;
  }

  .mode-toggle {
    gap: 0.85rem;
  }

  .mode-tab {
    min-height: 120px;
    padding: 1.1rem 1.25rem;
    flex: 1;
  }
}

@media (max-width: 640px) {
  .auth-panel {
    padding: 1.6rem;
    width: 100%;
  }

  .brand-copy {
    font-size: 1.2rem;
    gap: 1rem;
  }
}

@media (max-width: 560px) {
  .verification-field {
    flex-direction: column;
  }

  .code-button {
    width: 100%;
    min-width: 0;
  }

  .brand-showcase {
    padding: 2rem 1.5rem;
  }

  .auth-panel {
    padding: 1.4rem;
  }
}
</style>
