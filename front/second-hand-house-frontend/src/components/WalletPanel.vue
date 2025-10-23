<template>
  <section class="wallet-panel">
    <h2>我的钱包</h2>
    <p v-if="!currentUser" class="hint">请登录后查看钱包信息与充值功能。</p>

    <div v-else>
      <div v-if="loading" class="loading">钱包数据加载中...</div>
      <div v-else-if="!wallet" class="empty">暂无钱包数据，稍后再试。</div>
      <div v-else class="content">
        <div class="summary">
          <div class="summary-card balance">
            <span class="label">当前余额</span>
            <div class="figures">
              <strong class="amount-wan">￥{{ formatWan(wallet.balance) }}<small>万</small></strong>
              <span class="amount-yuan">≈ ￥{{ formatYuan(wallet.balance) }} 元</span>
            </div>
          </div>
          <div class="summary-card port">
            <span class="label">虚拟端口号</span>
            <code>{{ wallet.virtualPort }}</code>
            <small>该编号用于识别充值与收款</small>
          </div>
        </div>

        <form class="top-up" @submit.prevent="submitTopUp">
          <h3>充值钱包</h3>
          <p class="top-up-hint">系统以万元为结算单位，界面会自动折算成人民币金额。</p>
          <label>
            充值金额（万元）
            <input
              v-model.number="form.amount"
              type="number"
              min="0.01"
              step="0.01"
              required
              placeholder="请输入金额，如 50"
              :disabled="loading || submitting"
            />
            <span v-if="amountPreview" class="amount-preview">≈ ￥{{ amountPreview }} 元</span>
          </label>
          <label>
            备注（选填）
            <input
              v-model.trim="form.reference"
              type="text"
              maxlength="100"
              placeholder="如：线上充值"
              :disabled="loading || submitting"
            />
          </label>
          <button type="submit" :disabled="loading || submitting">
            {{ submitting ? '处理中...' : '立即充值' }}
          </button>
        </form>

        <div class="transactions">
          <h3>最近流水</h3>
          <p v-if="!wallet.transactions || wallet.transactions.length === 0" class="empty">暂无交易记录。</p>
          <ul v-else>
            <li v-for="tx in transactions" :key="tx.id" :class="['transaction', transactionClass(tx)]">
              <div class="transaction-main">
                <span class="type">{{ typeLabels[tx.type] ?? tx.type }}</span>
                <span class="time">{{ formatTime(tx.createdAt) }}</span>
              </div>
              <div class="transaction-amount">
                <span class="wan">{{ tx.amountParts.sign }}{{ tx.amountParts.wan }} 万</span>
                <span class="yuan">≈ {{ tx.amountParts.sign }}￥{{ tx.amountParts.yuan }}</span>
              </div>
              <span class="reference" :title="tx.reference || tx.description || ''">
                {{ tx.reference || tx.description || '—' }}
              </span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';

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

const form = reactive({ amount: null, reference: '' });
const submitting = ref(false);

const typeLabels = {
  TOP_UP: '充值',
  PAYMENT: '支付',
  RECEIVE: '收款',
  REFUND: '退款'
};

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

const formatWan = (value) => {
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
};

const formatYuan = (value) => {
  if (value == null) {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return (num * 10000).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const currencyParts = (value) => {
  if (value == null) {
    return { sign: '', wan: '0.00', yuan: '0.00' };
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return { sign: '', wan: '0.00', yuan: '0.00' };
  }
  const sign = num > 0 ? '+' : num < 0 ? '−' : '';
  const wan = Math.abs(num).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
  const yuan = Math.abs(num * 10000).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
  return { sign, wan, yuan };
};

const formatTime = (value) => {
  if (!value) {
    return '';
  }
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
};

const transactionClass = (tx) => {
  if (!tx || tx.amount == null) {
    return 'neutral';
  }
  const amount = Number(tx.amount);
  if (!Number.isFinite(amount)) {
    return 'neutral';
  }
  if (amount > 0) {
    return 'positive';
  }
  if (amount < 0) {
    return 'negative';
  }
  return 'neutral';
};
</script>

<style scoped>
.wallet-panel {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  padding: 2rem;
  border: 1px solid var(--color-border);
  backdrop-filter: blur(var(--glass-blur));
}

.wallet-panel h2 {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--color-text-strong);
  letter-spacing: 0.01em;
}

.hint {
  margin: 0;
  color: var(--color-text-soft);
}

.loading,
.empty {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 1.1rem 1.4rem;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 1.8rem;
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1.2rem;
}

.summary-card {
  position: relative;
  border-radius: calc(var(--radius-lg) - 0.2rem);
  padding: 1.5rem;
  overflow: hidden;
  border: 1px solid var(--color-border);
  backdrop-filter: blur(calc(var(--glass-blur) / 2));
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

.summary-card.balance {
  background: var(--gradient-primary);
  color: var(--color-text-on-emphasis);
  box-shadow: 0 28px 60px rgba(37, 99, 235, 0.32);
}

.summary-card.port {
  background: var(--color-surface);
  color: var(--color-text-strong);
}

.summary-card .label {
  display: block;
  font-size: 0.9rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.75;
}

.summary-card .figures {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-top: 0.75rem;
}

.amount-wan {
  font-size: 2.25rem;
  font-weight: 700;
  display: inline-flex;
  align-items: baseline;
  gap: 0.35rem;
}

.amount-wan small {
  font-size: 1rem;
  font-weight: 600;
  opacity: 0.8;
}

.amount-yuan {
  font-size: 0.95rem;
  opacity: 0.85;
}

.summary-card.port code {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-surface-soft) 70%, transparent);
  font-weight: 600;
  letter-spacing: 0.05em;
}

.summary-card.port small {
  display: block;
  margin-top: 0.6rem;
  color: var(--color-text-soft);
}

.top-up {
  display: grid;
  gap: 1rem;
}

.top-up h3 {
  margin: 0;
  font-size: 1.2rem;
  color: var(--color-text-strong);
}

.top-up-hint {
  margin: 0;
  font-size: 0.9rem;
  color: var(--color-text-soft);
}

.top-up label {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  color: var(--color-text-muted);
  font-weight: 500;
}

.top-up input {
  border: 1px solid color-mix(in srgb, var(--color-border) 70%, transparent);
  border-radius: var(--radius-lg);
  padding: 0.75rem 1rem;
  font-size: 1.05rem;
  background: color-mix(in srgb, var(--color-surface) 88%, transparent);
  transition: border-color var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
}

.top-up input:focus {
  border-color: color-mix(in srgb, var(--color-accent) 55%, transparent);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--color-accent) 25%, transparent);
}

.amount-preview {
  font-size: 0.88rem;
  color: var(--color-text-soft);
}

.top-up button {
  justify-self: flex-start;
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-pill);
  color: var(--color-text-on-emphasis);
  font-weight: 600;
  padding: 0.7rem 1.8rem;
  box-shadow: 0 22px 48px rgba(37, 99, 235, 0.3);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.top-up button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 30px 60px rgba(37, 99, 235, 0.35);
}

.top-up button:disabled {
  background: color-mix(in srgb, var(--color-accent) 35%, transparent);
  box-shadow: none;
}

.transactions h3 {
  margin: 0 0 0.75rem;
  color: var(--color-text-strong);
}

.transactions ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.transaction {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  align-items: start;
  gap: 0.75rem;
  padding: 1rem 1.2rem;
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-surface) 92%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border) 80%, transparent);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.transaction-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.65rem;
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

.transaction-main .type {
  font-weight: 600;
  color: var(--color-text-strong);
}

.transaction-amount {
  text-align: right;
  font-weight: 600;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
  color: var(--color-text-strong);
}

.transaction-amount .wan {
  font-size: 1.15rem;
}

.transaction-amount .yuan {
  font-size: 0.88rem;
  color: var(--color-text-soft);
}

.transaction.positive .transaction-amount {
  color: #16a34a;
}

.transaction.negative .transaction-amount {
  color: #ef4444;
}

.transaction .reference {
  grid-column: 1 / -1;
  color: var(--color-text-soft);
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transactions .empty {
  background: none;
  border: none;
  padding: 0;
  color: var(--color-text-soft);
}

@media (max-width: 720px) {
  .wallet-panel {
    padding: 1.6rem;
  }

  .transaction {
    grid-template-columns: 1fr;
  }

  .transaction-amount {
    align-items: flex-start;
    text-align: left;
  }
}
</style>
