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
  gap: 1rem;
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.08);
  padding: 1.5rem;
}

.wallet-panel h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #1e293b;
}

.hint {
  margin: 0;
  color: #64748b;
}

.loading,
.empty {
  background: #f8fafc;
  border-radius: 0.75rem;
  padding: 1rem 1.25rem;
  color: #475569;
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
  background: linear-gradient(135deg, #312e81, #1d4ed8);
  border-radius: 0.75rem;
  padding: 1.25rem;
  color: #f8fafc;
}

.summary .label {
  display: block;
  font-size: 0.85rem;
  opacity: 0.8;
}

.summary strong {
  font-size: 2rem;
  font-weight: 700;
}

.summary code {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 0.5rem;
  padding: 0.25rem 0.5rem;
  color: #e0e7ff;
  font-weight: 600;
}

.top-up {
  display: grid;
  gap: 0.75rem;
}

.top-up h3 {
  margin: 0;
  color: #1e293b;
}

.top-up label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: #334155;
  font-weight: 500;
}

.top-up input {
  border: 1px solid #cbd5f5;
  border-radius: 0.65rem;
  padding: 0.6rem 0.8rem;
  font-size: 1rem;
}

.top-up button {
  align-self: start;
  background: #2563eb;
  border: none;
  border-radius: 0.75rem;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  padding: 0.6rem 1.4rem;
}

.top-up button:disabled {
  background: #a5b4fc;
  cursor: not-allowed;
}

.transactions h3 {
  margin: 0 0 0.5rem;
  color: #1e293b;
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
  grid-template-columns: 100px 1fr 150px 1fr;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  background: #f8fafc;
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
  color: #64748b;
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transaction .time {
  color: #475569;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .transaction {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
