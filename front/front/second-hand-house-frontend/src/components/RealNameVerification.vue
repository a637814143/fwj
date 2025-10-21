<template>
  <section class="verification" v-if="currentUser">
    <header>
      <h2>实名认证</h2>
      <span :class="['status', currentUser.realNameVerified ? 'verified' : 'pending']">
        {{ currentUser.realNameVerified ? '已认证' : '未认证' }}
      </span>
    </header>
    <p class="hint">
      实名认证通过后才能预定或购买房源，并且将提升账户信誉分。
    </p>

    <div v-if="currentUser.realNameVerified" class="summary">
      <p><strong>姓名：</strong>{{ currentUser.realName || '—' }}</p>
      <p><strong>手机号：</strong>{{ currentUser.maskedPhoneNumber || '—' }}</p>
      <button type="button" @click="editing = !editing">
        {{ editing ? '取消修改' : '更新认证信息' }}
      </button>
    </div>

    <form v-if="!currentUser.realNameVerified || editing" class="form" @submit.prevent="submit">
      <div class="field">
        <label for="real-name">真实姓名</label>
        <input
          id="real-name"
          v-model.trim="form.realName"
          type="text"
          placeholder="请输入真实姓名"
          :disabled="loading"
          required
        />
      </div>
      <div class="field">
        <label for="id-number">身份证号</label>
        <input
          id="id-number"
          v-model.trim="form.idNumber"
          type="text"
          placeholder="请输入身份证号"
          :disabled="loading"
          required
        />
      </div>
      <div class="field">
        <label for="phone-number">手机号</label>
        <input
          id="phone-number"
          v-model.trim="form.phoneNumber"
          type="tel"
          placeholder="请输入手机号"
          :disabled="loading"
          required
        />
      </div>
      <button type="submit" :disabled="loading">
        {{ loading ? '提交中...' : currentUser.realNameVerified ? '更新认证信息' : '立即认证' }}
      </button>
    </form>

    <p v-if="error" class="error">{{ error }}</p>
  </section>
</template>

<script setup>
import { reactive, ref, watch } from 'vue';
import axios from 'axios';

const props = defineProps({
  apiBaseUrl: {
    type: String,
    required: true
  },
  currentUser: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['verified']);

const client = axios.create({
  baseURL: props.apiBaseUrl,
  headers: { 'Content-Type': 'application/json' }
});

const form = reactive({
  realName: '',
  idNumber: '',
  phoneNumber: ''
});

const loading = ref(false);
const error = ref('');
const editing = ref(false);

const resetForm = () => {
  form.realName = props.currentUser?.realName ?? '';
  form.idNumber = '';
  form.phoneNumber = '';
  error.value = '';
};

watch(
  () => props.currentUser,
  () => {
    resetForm();
    editing.value = false;
  },
  { immediate: true }
);

const submit = async () => {
  if (!props.currentUser) {
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const { data } = await client.post(`/auth/verify/${props.currentUser.username}`, {
      realName: form.realName,
      idNumber: form.idNumber,
      phoneNumber: form.phoneNumber
    });
    emit('verified', data);
    editing.value = false;
  } catch (err) {
    const detail = err.response?.data;
    if (detail?.errors) {
      const firstError = Object.values(detail.errors)[0];
      error.value = Array.isArray(firstError) ? firstError[0] : firstError;
    } else {
      error.value = detail?.detail ?? '实名认证提交失败，请稍后再试。';
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.verification {
  background: var(--app-surface);
  border-radius: 1rem;
  box-shadow: var(--app-shadow);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

header h2 {
  margin: 0;
  color: var(--app-text);
}

.status {
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
}

.status.verified {
  background: rgba(34, 197, 94, 0.15);
  color: #15803d;
}

.status.pending {
  background: rgba(248, 113, 113, 0.15);
  color: #b91c1c;
}

.hint {
  margin: 0;
  color: var(--app-muted);
}

.summary {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: center;
}

.summary p {
  margin: 0;
}

.summary button {
  border: none;
  background: var(--accent-gradient);
  color: var(--accent-contrast);
  padding: 0.5rem 1.1rem;
  border-radius: 0.75rem;
  cursor: pointer;
  box-shadow: 0 10px 18px var(--accent-soft);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.summary button:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px var(--accent-soft);
}

.form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

label {
  font-weight: 600;
  color: var(--app-text);
}

input {
  padding: 0.65rem 0.75rem;
  border-radius: 0.7rem;
  border: 1px solid var(--app-border);
  background: var(--app-surface-alt);
  color: var(--app-text);
}

input:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

button[type='submit'] {
  padding: 0.75rem 1.4rem;
  border-radius: 0.8rem;
  border: none;
  background: var(--accent-gradient);
  color: var(--accent-contrast);
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 12px 24px var(--accent-soft);
  transition: transform 0.25s ease, box-shadow 0.25s ease, opacity 0.25s ease;
}

button[type='submit']:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 26px var(--accent-soft);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.error {
  margin: 0;
  color: #dc2626;
}
</style>
