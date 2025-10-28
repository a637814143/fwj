<template>
  <section class="wallet-panel surface-card">
    <h2>{{ t('wallet.title') }}</h2>
    <p v-if="!currentUser" class="hint">{{ t('wallet.hints.login') }}</p>

    <div v-else>
      <div v-if="loading" class="loading">{{ t('wallet.states.loading') }}</div>
      <div v-else-if="!wallet" class="empty">{{ t('wallet.states.empty') }}</div>
      <div v-else class="content">
        <div class="summary">
          <div class="summary-card balance">
            <span class="label">{{ t('wallet.labels.balance') }}</span>
            <div class="figures">
              <strong class="amount-yuan">￥{{ formatYuan(wallet.balance) }}</strong>
            </div>
          </div>
          <div class="summary-card points">
            <span class="label">{{ t('wallet.labels.points') }}</span>
            <div class="figures">
              <strong class="amount-points">{{ pointsBalance }}</strong>
            </div>
            <small>{{ t('wallet.labels.pointsHint') }}</small>
          </div>
          <div class="summary-card port">
            <span class="label">{{ t('wallet.labels.virtualPort') }}</span>
            <code>{{ wallet.virtualPort }}</code>
            <small>{{ t('wallet.labels.virtualPortHint') }}</small>
          </div>
        </div>

        <form class="top-up" @submit.prevent="submitTopUp">
          <h3>{{ t('wallet.topUp.title') }}</h3>
          <p class="top-up-hint">{{ t('wallet.topUp.hint') }}</p>
          <label>
            {{ t('wallet.topUp.amountLabel') }}
            <input
              v-model.number="form.amount"
              type="number"
              min="0.01"
              step="0.01"
              required
              :placeholder="t('wallet.topUp.amountPlaceholder')"
              :disabled="loading || submitting"
            />
            <span v-if="amountPreview" class="amount-preview">￥{{ amountPreview }}</span>
            <span v-if="bonusPreview" class="points-preview">
              +{{ bonusPreview }} {{ t('wallet.labels.pointsUnit') }}
            </span>
          </label>
          <label>
            {{ t('wallet.topUp.referenceLabel') }}
            <input
              v-model.trim="form.reference"
              type="text"
              maxlength="100"
              :placeholder="t('wallet.topUp.referencePlaceholder')"
              :disabled="loading || submitting"
            />
          </label>
          <button type="submit" :disabled="loading || submitting">
            {{ submitting ? t('wallet.actions.processing') : t('wallet.actions.submitTopUp') }}
          </button>
        </form>

        <div class="transactions">
          <h3>{{ t('wallet.transactions.title') }}</h3>
          <p v-if="!wallet.transactions || wallet.transactions.length === 0" class="empty">
            {{ t('wallet.transactions.empty') }}
          </p>
          <ul v-else>
            <li v-for="tx in transactions" :key="tx.id" :class="['transaction', transactionClass(tx)]">
              <div class="transaction-main">
                <span class="type">{{ transactionLabel(tx.type) }}</span>
                <span class="time">{{ formatTime(tx.createdAt) }}</span>
              </div>
              <div class="transaction-amount">
                <span class="amount">{{ tx.amountParts.sign }}￥{{ tx.amountParts.value }}</span>
              </div>
              <span class="reference" :title="formatReference(tx)">
                {{ formatReference(tx) }}
              </span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, inject, reactive, ref, watch } from 'vue';

const props = defineProps({
  wallet: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  currentUser: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['top-up']);

const settings = inject('appSettings', { language: 'zh' });
const translate = inject('translate', (key) => key);
const t = (key, vars) => translate(key, vars);
const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));
const containsCJK = (value) => /[\u3400-\u9FFF]/.test(value ?? '');

const form = reactive({ amount: null, reference: '' });
const submitting = ref(false);

const typeLabels = computed(() => {
  const unknown = t('wallet.transactions.types.unknown');
  return {
    TOP_UP: t('wallet.transactions.types.topUp'),
    PAYMENT: t('wallet.transactions.types.payment'),
    RECEIVE: t('wallet.transactions.types.receive'),
    REFUND: t('wallet.transactions.types.refund'),
    WITHDRAW: t('wallet.transactions.types.withdraw'),
    WITHDRAWAL: t('wallet.transactions.types.withdraw'),
    TRANSFER_IN: t('wallet.transactions.types.transferIn'),
    TRANSFER_OUT: t('wallet.transactions.types.transferOut'),
    ADJUSTMENT: t('wallet.transactions.types.adjustment'),
    UNKNOWN: unknown,
    DEFAULT: unknown
  };
});

const transactions = computed(() => {
  if (!props.wallet || !Array.isArray(props.wallet.transactions)) {
    return [];
  }
  return props.wallet.transactions.map((tx) => ({
    ...tx,
    amountParts: currencyParts(tx.amount)
  }));
});

const amountPreview = computed(() => {
  if (form.amount == null || form.amount === '') {
    return '';
  }
  const numeric = Number(form.amount);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return '';
  }
  return formatYuan(numeric);
});

const pointsBalance = computed(() => {
  const value = Number(props.wallet?.points ?? 0);
  return Number.isFinite(value) ? Math.max(0, Math.floor(value)) : 0;
});

const bonusPreview = computed(() => {
  if (form.amount == null || form.amount === '') {
    return 0;
  }
  const numeric = Number(form.amount);
  if (!Number.isFinite(numeric) || numeric < 0) {
    return 0;
  }
  return Math.floor(numeric / 100) * 10;
});

watch(
  () => props.wallet,
  () => {
    form.amount = null;
    form.reference = '';
  }
);

watch(
  () => props.loading,
  (value) => {
    if (!value) {
      submitting.value = false;
    }
  }
);

const submitTopUp = () => {
  if (submitting.value || props.loading || !form.amount || Number(form.amount) <= 0) {
    return;
  }
  const amountValue = Number(form.amount);
  if (!Number.isFinite(amountValue) || amountValue <= 0) {
    return;
  }
  submitting.value = true;
  const normalized = Math.round(amountValue * 100) / 100;
  emit('top-up', {
    amount: normalized,
    reference: form.reference || undefined
  });
};

const formatYuan = (value) => {
  return formatCurrency(value, 2);
};

const formatCurrency = (value, fractionDigits = 2) => {
  if (value == null) {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return num.toLocaleString(locale.value, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits
  });
};

const currencyParts = (value) => {
  if (value == null) {
    return { sign: '', value: '0.00' };
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return { sign: '', value: '0.00' };
  }
  const sign = num > 0 ? '+' : num < 0 ? '−' : '';
  const absolute = Math.abs(num);
  return {
    sign,
    value: formatYuan(absolute)
  };
};

const transactionClass = (tx) => {
  if (!tx || !tx.amountParts) {
    return '';
  }
  const sign = tx.amountParts.sign;
  if (sign === '+') {
    return 'positive';
  }
  if (sign === '−') {
    return 'negative';
  }
  return '';
};

const transactionLabel = (type) => {
  const key = String(type ?? '').toUpperCase();
  return typeLabels.value[key] ?? typeLabels.value.UNKNOWN ?? key;
};

const formatReference = (tx) => {
  if (!tx) {
    return t('wallet.transactions.noReference');
  }
  if (tx.reference && tx.reference.trim()) {
    return tx.reference.trim();
  }
  if (tx.description && tx.description.trim()) {
    const trimmed = tx.description.trim();
    if (settings?.language === 'en' && containsCJK(trimmed)) {
      return transactionLabel(tx.type);
    }
    return trimmed;
  }
  const fallback = transactionLabel(tx.type);
  if (fallback && fallback !== String(tx.type ?? '')) {
    return fallback;
  }
  return t('wallet.transactions.noReference');
};

const formatTime = (value) => {
  if (!value) {
    return t('wallet.transactions.unknownTime');
  }
  try {
    return new Date(value).toLocaleString(locale.value, {
      hour12: settings?.language === 'en'
    });
  } catch (error) {
    return String(value);
  }
};
</script>

<style scoped>
.wallet-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
}

.wallet-panel h2 {
  margin: 0;
  font-size: 1.45rem;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.loading,
.empty {
  padding: 1.25rem;
  border-radius: var(--radius-md);
  border: 1px dashed color-mix(in srgb, var(--color-border) 70%, transparent);
  text-align: center;
  color: var(--color-text-muted);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.1rem;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  padding: 1.1rem;
  border-radius: var(--radius-lg);
  background: var(--wallet-card-bg);
  box-shadow: var(--wallet-card-shadow);
  border: 1px solid color-mix(in srgb, var(--color-border) 82%, transparent);
}

.summary-card .label {
  font-size: 0.9rem;
  color: var(--color-text-soft);
  letter-spacing: 0.02em;
}

.figures {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.amount-yuan {
  font-size: 1.85rem;
  color: var(--color-text-strong);
  font-weight: 700;
}

.amount-points {
  font-size: 1.65rem;
  color: var(--wallet-points-color);
  font-weight: 700;
}

.summary-card.points {
  background: var(--wallet-card-points-bg);
}

.summary-card.points small {
  color: var(--color-text-muted);
}

.summary-card.port code {
  font-family: 'Fira Code', 'SFMono-Regular', monospace;
  font-size: 1.1rem;
}

.summary-card.port small {
  color: var(--color-text-soft);
}

.top-up {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.top-up h3 {
  margin: 0;
}

.top-up-hint {
  margin: 0;
  color: var(--color-text-soft);
}

.top-up label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.top-up input {
  padding: 0.6rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  background: color-mix(in srgb, var(--color-surface) 92%, transparent);
}

.amount-preview {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.points-preview {
  font-size: 0.85rem;
  color: var(--wallet-points-color);
  font-weight: 600;
}

.top-up button {
  align-self: flex-start;
  padding: 0.65rem 1.5rem;
  border: none;
  border-radius: var(--radius-pill);
  font-weight: 600;
  cursor: pointer;
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: var(--wallet-button-shadow);
}

.top-up button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: var(--wallet-button-shadow-hover, var(--wallet-button-shadow));
}

.top-up button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.transactions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.transactions ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.transaction {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.65rem;
  padding: 0.85rem 1rem;
  border-radius: var(--radius-lg);
  background: var(--wallet-card-bg);
  border: 1px solid color-mix(in srgb, var(--color-border) 76%, transparent);
}

.transaction-main {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.transaction-main .type {
  font-weight: 600;
  color: var(--color-text-strong);
}

.transaction-main .time {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.transaction-amount {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  font-weight: 600;
  font-size: 1.05rem;
  color: var(--color-text-strong);
}

.transaction-amount .amount {
  display: inline-flex;
  align-items: baseline;
}

.transaction.positive .transaction-amount {
  color: var(--wallet-positive-color);
}

.transaction.negative .transaction-amount {
  color: var(--wallet-negative-color);
}

.reference {
  color: var(--color-text-soft);
  font-size: 0.85rem;
}
</style>
