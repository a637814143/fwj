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
              placeholder="请输入金额，如 50"
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
  submitting.value = true;
  emit('top-up', {
    amount: Number(form.amount),
    reference: form.reference || undefined
  });
};

const formatAmount = (value) => {
  if (value == null) {
    return '0.00';
  }
  return Number(value).toLocaleString('zh-CN', {
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
  const sign = num > 0 ? '+' : '';
  return `${sign}${num.toFixed(2)}`;
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
  background: var(--surface-secondary);
  border-radius: 26px;
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-strong);
  padding: 1.75rem;
  color: var(--text-primary);
}

.wallet-panel h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.hint {
  margin: 0;
  color: var(--text-secondary);
}

.loading,
.empty {
  background: rgba(15, 23, 42, 0.55);
  border-radius: 1rem;
  padding: 1rem 1.25rem;
  color: var(--text-secondary);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.summary {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
  justify-content: space-between;
  background: linear-gradient(145deg, rgba(37, 99, 235, 0.8), rgba(56, 189, 248, 0.65));
  border-radius: 18px;
  padding: 1.35rem;
  color: var(--text-primary);
  box-shadow: 0 25px 45px rgba(14, 116, 204, 0.35);
}

.summary .label {
  display: block;
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.8;
}

.summary strong {
  font-size: 2.1rem;
  font-weight: 700;
}

.summary code {
  background: rgba(15, 23, 42, 0.35);
  border-radius: 0.6rem;
  padding: 0.25rem 0.6rem;
  color: #e0f2fe;
  font-weight: 600;
}

.top-up {
  display: grid;
  gap: 0.85rem;
  background: rgba(15, 23, 42, 0.55);
  border-radius: 18px;
  padding: 1.25rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.top-up h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 1.1rem;
}

.top-up label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: var(--text-secondary);
  font-weight: 500;
}

.top-up input {
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 0.85rem;
  padding: 0.6rem 0.85rem;
  font-size: 1rem;
  background: rgba(8, 15, 35, 0.75);
  color: var(--text-primary);
}

.top-up button {
  align-self: start;
  background: linear-gradient(135deg, var(--accent), rgba(56, 189, 248, 0.8));
  border: none;
  border-radius: 0.9rem;
  color: var(--text-primary);
  cursor: pointer;
  font-weight: 600;
  padding: 0.65rem 1.55rem;
  box-shadow: 0 18px 32px rgba(37, 99, 235, 0.35);
}

.top-up button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.transactions h3 {
  margin: 0 0 0.65rem;
  color: var(--text-primary);
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
  grid-template-columns: 120px 1fr 160px 1fr;
  align-items: center;
  gap: 0.85rem;
  padding: 0.85rem 1.15rem;
  border-radius: 1rem;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(148, 163, 184, 0.2);
  backdrop-filter: blur(12px);
}

.transaction .amount {
  font-weight: 600;
  text-align: right;
}

.transaction.positive .amount {
  color: var(--success);
}

.transaction.negative .amount {
  color: var(--danger);
}

.transaction .reference {
  color: var(--text-secondary);
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transaction .time {
  color: var(--text-muted);
  font-size: 0.85rem;
}

@media (max-width: 768px) {
  .transaction {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
