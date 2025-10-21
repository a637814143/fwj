<template>
  <section class="wallet-panel">
    <h2>我的钱包</h2>
    <p v-if="!currentUser" class="hint">请登录后查看钱包信息与充值功能。</p>

    <div v-else>
      <div v-if="loading" class="loading">钱包数据加载中...</div>
      <div v-else-if="!wallet" class="empty">暂无钱包数据，稍后再试。</div>
      <div v-else class="content">
        <div class="summary">
          <div class="balance">
            <span class="label">当前余额（元）</span>
            <strong>{{ formatAmount(wallet.balance) }}</strong>
          </div>
          <div class="port">
            <span class="label">虚拟端口号</span>
            <code>{{ wallet.virtualPort }}</code>
          </div>
        </div>

        <form class="top-up" @submit.prevent="submitTopUp">
          <h3>充值钱包</h3>
          <label>
            充值金额（元）
            <input
              v-model.number="form.amount"
              type="number"
              min="0.01"
              step="0.01"
              required
              placeholder="请输入金额，如 5000"
              :disabled="loading || submitting"
            />
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
            <li v-for="tx in wallet.transactions" :key="tx.id" :class="['transaction', transactionClass(tx)]">
              <span class="type">{{ typeLabels[tx.type] ?? tx.type }}</span>
              <span class="amount">{{ formatSigned(tx.amount) }}</span>
              <span class="time">{{ formatTime(tx.createdAt) }}</span>
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
import { reactive, ref, watch } from 'vue';

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
  const normalized = Number((amountValue / 10000).toFixed(2));
  emit('top-up', {
    amount: normalized,
    reference: form.reference || undefined
  });
};

const formatAmount = (value) => {
  if (value == null) {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  const yuan = num * 10000;
  return yuan.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatSigned = (value) => {
  if (value == null) {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  const yuan = num * 10000;
  const sign = yuan > 0 ? '+' : yuan < 0 ? '-' : '';
  return `${sign}${Math.abs(yuan).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`;
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
  gap: 1.35rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 1.85rem;
  border: 1px solid var(--color-border);
  backdrop-filter: blur(var(--glass-blur));
}

.wallet-panel h2 {
  margin: 0;
  font-size: 1.55rem;
  color: var(--color-text-strong);
}

.hint {
  margin: 0;
  color: var(--color-text-soft);
}

.loading,
.empty {
  background: rgba(248, 250, 252, 0.85);
  border-radius: var(--radius-md);
  padding: 1.1rem 1.35rem;
  color: var(--color-text-muted);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
}

.summary {
  display: flex;
  gap: 1.6rem;
  flex-wrap: wrap;
  justify-content: space-between;
  background: linear-gradient(135deg, rgba(49, 46, 129, 0.92), rgba(29, 78, 216, 0.92));
  border-radius: var(--radius-lg);
  padding: 1.4rem 1.6rem;
  color: #f8fafc;
  box-shadow: 0 26px 48px rgba(30, 64, 175, 0.35);
}

.summary .label {
  display: block;
  font-size: 0.88rem;
  opacity: 0.85;
}

.summary strong {
  font-size: 2.1rem;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.summary code {
  background: rgba(255, 255, 255, 0.18);
  border-radius: var(--radius-md);
  padding: 0.3rem 0.65rem;
  color: #e0e7ff;
  font-weight: 600;
}

.top-up {
  display: grid;
  gap: 0.9rem;
}

.top-up h3 {
  margin: 0;
  color: var(--color-text-strong);
}

.top-up label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  color: var(--color-text-muted);
  font-weight: 500;
}

.top-up input {
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: var(--radius-md);
  padding: 0.65rem 0.9rem;
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.92);
}

.top-up button {
  align-self: flex-start;
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-pill);
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  padding: 0.65rem 1.55rem;
  box-shadow: 0 18px 35px rgba(37, 99, 235, 0.28);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.top-up button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 24px 45px rgba(37, 99, 235, 0.3);
}

.top-up button:disabled {
  background: rgba(165, 180, 252, 0.85);
  cursor: not-allowed;
  box-shadow: none;
}

.transactions h3 {
  margin: 0 0 0.6rem;
  color: var(--color-text-strong);
}

.transactions ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.transaction {
  display: grid;
  grid-template-columns: 110px 1fr 160px 1fr;
  align-items: center;
  gap: 0.85rem;
  padding: 0.85rem 1.1rem;
  border-radius: var(--radius-md);
  background: rgba(248, 250, 252, 0.85);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.transaction .amount {
  font-weight: 600;
  text-align: right;
}

.transaction.positive .amount {
  color: #16a34a;
}

.transaction.negative .amount {
  color: #dc2626;
}

.transaction .reference {
  color: var(--color-text-soft);
  font-size: 0.92rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transaction .time {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .transaction {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
