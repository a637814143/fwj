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
  background: var(--surface-secondary);
  border-radius: 24px;
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-strong);
  padding: 1.65rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  color: var(--text-primary);
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

header h2 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 600;
}

.status {
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  background: rgba(148, 163, 184, 0.25);
  color: var(--text-secondary);
}

.status.verified {
  background: rgba(52, 211, 153, 0.25);
  color: var(--success);
}

.status.pending {
  background: rgba(248, 113, 113, 0.25);
  color: var(--danger);
}

.hint {
  margin: 0;
  color: var(--text-secondary);
}

.summary {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: center;
}

.summary p {
  margin: 0;
  color: var(--text-secondary);
}

.summary button {
  border: none;
  background: linear-gradient(135deg, var(--accent), rgba(56, 189, 248, 0.8));
  color: var(--text-primary);
  padding: 0.55rem 1.2rem;
  border-radius: 0.85rem;
  cursor: pointer;
  box-shadow: 0 15px 28px rgba(37, 99, 235, 0.3);
}

.form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  background: rgba(15, 23, 42, 0.55);
  border-radius: 18px;
  padding: 1.25rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

label {
  font-weight: 600;
  color: var(--text-secondary);
}

input {
  padding: 0.7rem 0.85rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(8, 15, 35, 0.75);
  color: var(--text-primary);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

input:focus {
  outline: none;
  border-color: rgba(96, 165, 250, 0.8);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.25);
}

button[type='submit'] {
  padding: 0.75rem 1.5rem;
  border-radius: 0.95rem;
  border: none;
  background: linear-gradient(135deg, var(--accent), rgba(56, 189, 248, 0.8));
  color: var(--text-primary);
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 18px 30px rgba(37, 99, 235, 0.35);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.error {
  margin: 0;
  color: var(--danger);
}
</style>
