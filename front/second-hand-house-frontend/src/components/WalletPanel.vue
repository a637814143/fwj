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
              :disabled="isFormBusy || hasPendingPayment"
            />
            <span v-if="amountPreview" class="amount-preview">￥{{ amountPreview }}</span>
          </label>
          <label>
            {{ t('wallet.topUp.referenceLabel') }}
            <input
              v-model.trim="form.reference"
              type="text"
              maxlength="100"
              :placeholder="t('wallet.topUp.referencePlaceholder')"
              :disabled="isFormBusy || hasPendingPayment"
            />
          </label>
          <button type="submit" :disabled="isFormBusy || hasPendingPayment">
            {{ generatingQr ? t('wallet.actions.generatingQr') : t('wallet.actions.generateQr') }}
          </button>
        </form>

        <p v-if="topUpError" class="top-up-error">{{ topUpError }}</p>

        <section v-if="pendingPayment" class="top-up-confirmation">
          <h4>{{ t('wallet.topUp.qrTitle') }}</h4>
          <p class="top-up-hint">{{ t('wallet.topUp.qrHint') }}</p>
          <div class="qr-wrapper">
            <img :src="pendingPayment.qrCode" :alt="t('wallet.topUp.qrTitle')" />
            <div class="qr-details">
              <span class="summary">{{ t('wallet.topUp.summaryAmount', { amount: formatYuan(pendingPayment.amount) }) }}</span>
              <span v-if="pendingPayment.reference" class="summary">
                {{ t('wallet.topUp.summaryReference', { reference: pendingPayment.reference }) }}
              </span>
            </div>
          </div>
          <p class="top-up-hint notice">{{ t('wallet.topUp.qrNotice') }}</p>
          <div class="confirmation-actions">
            <button
              type="button"
              class="confirm"
              :disabled="confirming || loading"
              @click="confirmTopUp"
            >
              {{ confirming ? t('wallet.actions.processing') : t('wallet.topUp.confirmPaid') }}
            </button>
            <button type="button" class="cancel" :disabled="confirming" @click="reportTopUpFailure">
              {{ t('wallet.topUp.reportFailure') }}
            </button>
          </div>
        </section>

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
import QRCode from 'qrcode';

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
const generatingQr = ref(false);
const confirming = ref(false);
const pendingPayment = ref(null);
const topUpError = ref('');

const typeLabels = computed(() => ({
  TOP_UP: t('wallet.transactions.types.topUp'),
  PAYMENT: t('wallet.transactions.types.payment'),
  RECEIVE: t('wallet.transactions.types.receive'),
  REFUND: t('wallet.transactions.types.refund')
}));

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

const hasPendingPayment = computed(() => Boolean(pendingPayment.value));
const isFormBusy = computed(() => props.loading || generatingQr.value || confirming.value);

watch(
  () => props.wallet,
  () => {
    form.amount = null;
    form.reference = '';
    pendingPayment.value = null;
    topUpError.value = '';
  }
);

watch(
  () => props.loading,
  (value) => {
    if (!value) {
      generatingQr.value = false;
      confirming.value = false;
    }
  }
);

watch(
  () => props.currentUser,
  () => {
    pendingPayment.value = null;
    topUpError.value = '';
  }
);

const buildQrPayload = (amount, reference) => ({
  username: props.currentUser?.username ?? '未知用户',
  amount,
  reference: reference ?? '',
  timestamp: Date.now()
});

const submitTopUp = async () => {
  if (isFormBusy.value) {
    return;
  }
  const amountValue = Number(form.amount);
  if (!Number.isFinite(amountValue) || amountValue <= 0) {
    topUpError.value = t('wallet.topUp.errors.invalidAmount');
    return;
  }
  const normalized = Math.round(amountValue * 100) / 100;
  generatingQr.value = true;
  topUpError.value = '';
  try {
    const qrContent = JSON.stringify(buildQrPayload(normalized, form.reference));
    const qrCode = await QRCode.toDataURL(qrContent, { width: 320, margin: 1 });
    pendingPayment.value = {
      amount: normalized,
      reference: form.reference || undefined,
      qrCode,
      createdAt: new Date()
    };
  } catch (error) {
    console.error('生成二维码失败：', error);
    topUpError.value = t('wallet.topUp.errors.qrFailed');
  } finally {
    generatingQr.value = false;
  }
};

const confirmTopUp = () => {
  if (!pendingPayment.value || confirming.value) {
    return;
  }
  confirming.value = true;
  emit('top-up', {
    amount: pendingPayment.value.amount,
    reference: pendingPayment.value.reference
  });
};

const reportTopUpFailure = () => {
  if (confirming.value) {
    return;
  }
  pendingPayment.value = null;
  topUpError.value = '';
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

const transactionLabel = (type) => typeLabels.value[type] ?? type;

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
  border: 1px solid rgba(148, 163, 184, 0.25);
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
  border: 1px dashed rgba(148, 163, 184, 0.4);
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
  background: var(--gradient-surface);
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.12);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.summary-card .label {
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.figures {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.amount-yuan {
  font-size: 1.8rem;
  color: var(--color-text-strong);
  font-weight: 700;
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
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.9);
}

.amount-preview {
  font-size: 0.85rem;
  color: var(--color-text-soft);
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
  box-shadow: 0 15px 30px rgba(37, 99, 235, 0.24);
}

.top-up button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.top-up-error {
  margin: 0;
  color: #dc2626;
  font-weight: 600;
}

.top-up-confirmation {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(59, 130, 246, 0.25);
  background: rgba(59, 130, 246, 0.06);
}

.qr-wrapper {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.qr-wrapper img {
  width: 180px;
  height: 180px;
  object-fit: contain;
  border-radius: var(--radius-md);
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.35);
  padding: 0.5rem;
  box-shadow: 0 10px 25px rgba(37, 99, 235, 0.18);
}

.qr-details {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-weight: 600;
  color: var(--color-text-strong);
}

.qr-details .summary {
  font-size: 0.95rem;
}

.top-up-hint.notice {
  color: var(--color-text-strong);
  font-weight: 500;
}

.confirmation-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.confirmation-actions button {
  padding: 0.55rem 1.4rem;
  border-radius: var(--radius-pill);
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.confirmation-actions .confirm {
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.24);
}

.confirmation-actions .confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.confirmation-actions .cancel {
  background: rgba(254, 226, 226, 0.9);
  color: #b91c1c;
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
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.25);
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
  color: #047857;
}

.transaction.negative .transaction-amount {
  color: #dc2626;
}

.reference {
  color: var(--color-text-soft);
  font-size: 0.85rem;
}
</style>
