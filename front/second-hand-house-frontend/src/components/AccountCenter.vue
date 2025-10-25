<template>
  <div v-if="visible" class="account-center-backdrop" @click.self="emit('close')">
    <section class="account-center-panel">
      <header class="panel-header">
        <div>
          <h2>{{ t('accountCenter.title') }}</h2>
          <p class="panel-hint">{{ t('accountCenter.subtitle') }}</p>
        </div>
        <button type="button" class="close" @click="emit('close')">×</button>
      </header>

      <section class="profile-summary">
        <h3>{{ t('accountCenter.summaryTitle') }}</h3>
        <div class="summary-grid">
          <div class="summary-item">
            <span class="label">{{ t('accountCenter.summary.username') }}</span>
            <strong>{{ currentUser?.username ?? '—' }}</strong>
          </div>
          <div class="summary-item">
            <span class="label">{{ t('accountCenter.summary.displayName') }}</span>
            <strong>{{ currentUser?.displayName ?? '—' }}</strong>
          </div>
          <div class="summary-item">
            <span class="label">{{ t('accountCenter.summary.role') }}</span>
            <strong>{{ roleLabel || currentUser?.role || '—' }}</strong>
          </div>
          <div class="summary-item">
            <span class="label">{{ t('accountCenter.summary.verification') }}</span>
            <strong>{{ verificationStatus }}</strong>
          </div>
          <div class="summary-item">
            <span class="label">{{ t('accountCenter.summary.realName') }}</span>
            <strong>{{ realName }}</strong>
          </div>
          <div class="summary-item">
            <span class="label">{{ t('accountCenter.summary.phone') }}</span>
            <strong>{{ phoneNumber }}</strong>
          </div>
          <div class="summary-item">
            <span class="label">{{ t('wallet.labels.balance') }}</span>
            <strong>￥{{ walletBalance }}</strong>
          </div>
          <div class="summary-item">
            <span class="label">{{ t('wallet.labels.virtualPort') }}</span>
            <strong>{{ walletPort }}</strong>
          </div>
        </div>
      </section>

      <form class="credentials" @submit.prevent="submit">
        <h3>{{ t('accountCenter.credentialTitle') }}</h3>
        <p class="panel-hint">{{ t('accountCenter.credentialHint') }}</p>

        <label>
          {{ t('accountCenter.fields.username') }}
          <input
            v-model.trim="form.username"
            type="text"
            :placeholder="currentUser?.username ?? ''"
            :disabled="saving"
            maxlength="50"
          />
        </label>

        <label>
          {{ t('accountCenter.fields.displayName') }}
          <input
            v-model.trim="form.displayName"
            type="text"
            :placeholder="currentUser?.displayName ?? ''"
            :disabled="saving"
            maxlength="100"
          />
        </label>

        <label>
          {{ t('accountCenter.fields.newPassword') }}
          <input
            v-model.trim="form.password"
            type="password"
            :placeholder="t('accountCenter.placeholders.password')"
            :disabled="saving"
            minlength="6"
            maxlength="100"
          />
        </label>

        <label>
          {{ t('accountCenter.fields.confirmPassword') }}
          <input
            v-model.trim="form.confirm"
            type="password"
            :placeholder="t('accountCenter.placeholders.confirmPassword')"
            :disabled="saving"
            minlength="6"
            maxlength="100"
          />
        </label>

        <p v-if="localError" class="form-error">{{ localError }}</p>
        <p v-else-if="error" class="form-error">{{ error }}</p>

        <div class="actions">
          <button type="submit" class="primary" :disabled="saving">
            {{ saving ? t('accountCenter.actions.saving') : t('accountCenter.actions.save') }}
          </button>
          <button type="button" class="ghost" :disabled="saving" @click="emit('close')">
            {{ t('accountCenter.actions.cancel') }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, inject, reactive, ref, watch } from 'vue';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  currentUser: {
    type: Object,
    default: null
  },
  wallet: {
    type: Object,
    default: null
  },
  roleLabel: {
    type: String,
    default: ''
  },
  saving: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['close', 'submit']);

const translate = inject('translate', (key) => key);
const t = (key, vars) => translate(key, vars);

const form = reactive({
  username: '',
  displayName: '',
  password: '',
  confirm: ''
});

const localError = ref('');

function formatCurrency(value) {
  if (value == null) {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

const walletBalance = computed(() => formatCurrency(props.wallet?.balance));
const walletPort = computed(() => props.wallet?.virtualPort ?? '—');
const realName = computed(() => props.currentUser?.realName ?? '—');
const phoneNumber = computed(
  () => props.currentUser?.maskedPhoneNumber ?? props.currentUser?.phoneNumber ?? '—'
);
const verificationStatus = computed(() =>
  props.currentUser?.realNameVerified ? t('accountCenter.summary.verified') : t('accountCenter.summary.pending')
);

const syncForm = () => {
  form.username = props.currentUser?.username ?? '';
  form.displayName = props.currentUser?.displayName ?? '';
  form.password = '';
  form.confirm = '';
  localError.value = '';
};

watch(
  () => props.visible,
  (value) => {
    if (value) {
      syncForm();
    } else {
      localError.value = '';
    }
  }
);

watch(
  () => props.currentUser,
  () => {
    if (props.visible) {
      syncForm();
    }
  }
);

const hasChanges = computed(() => {
  const currentUsername = props.currentUser?.username ?? '';
  const currentDisplayName = props.currentUser?.displayName ?? '';
  const trimmedUsername = form.username?.trim() ?? '';
  const trimmedDisplayName = form.displayName?.trim() ?? '';
  const usernameChanged = trimmedUsername && trimmedUsername !== currentUsername;
  const displayNameChanged = trimmedDisplayName && trimmedDisplayName !== currentDisplayName;
  const passwordChanged = Boolean(form.password);
  return usernameChanged || displayNameChanged || passwordChanged;
});

const buildPayload = () => {
  const payload = {};
  const currentUsername = props.currentUser?.username ?? '';
  const currentDisplayName = props.currentUser?.displayName ?? '';
  const trimmedUsername = form.username?.trim();
  const trimmedDisplayName = form.displayName?.trim();
  if (trimmedUsername && trimmedUsername !== currentUsername) {
    payload.newUsername = trimmedUsername;
  }
  if (trimmedDisplayName && trimmedDisplayName !== currentDisplayName) {
    payload.displayName = trimmedDisplayName;
  }
  if (form.password) {
    payload.newPassword = form.password;
  }
  return payload;
};

const submit = () => {
  if (props.saving) {
    return;
  }
  localError.value = '';
  if (!hasChanges.value) {
    localError.value = t('accountCenter.errors.noChanges');
    return;
  }
  if (form.password || form.confirm) {
    if (form.password !== form.confirm) {
      localError.value = t('accountCenter.errors.passwordMismatch');
      return;
    }
    if ((form.password?.length ?? 0) < 6) {
      localError.value = t('accountCenter.errors.passwordLength');
      return;
    }
  }
  const payload = buildPayload();
  if (Object.keys(payload).length === 0) {
    localError.value = t('accountCenter.errors.noChanges');
    return;
  }
  emit('submit', payload);
};

</script>

<style scoped>
.account-center-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(6px);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 4vh 1.5rem;
  z-index: 1200;
}

.account-center-panel {
  width: min(960px, 100%);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: var(--shadow-xl);
  padding: 2rem;
  max-height: calc(100vh - 8vh);
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.panel-header h2 {
  margin: 0;
  font-size: 1.6rem;
  color: var(--color-text-strong);
}

.panel-hint {
  margin: 0.25rem 0 0;
  color: var(--color-text-soft);
  font-size: 0.95rem;
}

.close {
  border: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-pill);
  width: 2.4rem;
  height: 2.4rem;
  font-size: 1.35rem;
  color: var(--color-text-strong);
  cursor: pointer;
  transition: background var(--transition-base), transform var(--transition-base);
}

.close:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: translateY(-1px);
}

.profile-summary {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.profile-summary h3 {
  margin: 0;
  color: var(--color-text-strong);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem;
}

.summary-item {
  background: rgba(248, 250, 252, 0.85);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(148, 163, 184, 0.35);
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.summary-item .label {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.summary-item strong {
  font-size: 1rem;
  color: var(--color-text-strong);
}

.credentials {
  display: grid;
  gap: 1rem;
}

.credentials label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.credentials input {
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.9);
}

.form-error {
  margin: 0;
  color: #dc2626;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 0.85rem;
  justify-content: flex-end;
}

.actions button {
  padding: 0.65rem 1.5rem;
  border-radius: var(--radius-pill);
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.actions .primary {
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  box-shadow: 0 18px 32px rgba(37, 99, 235, 0.28);
}

.actions .primary:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 20px 36px rgba(37, 99, 235, 0.32);
}

.actions .ghost {
  background: transparent;
  border: 1px solid rgba(148, 163, 184, 0.35);
  color: var(--color-text-strong);
}

@media (max-width: 768px) {
  .account-center-panel {
    padding: 1.5rem;
    max-height: calc(100vh - 4vh);
  }

  .actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
