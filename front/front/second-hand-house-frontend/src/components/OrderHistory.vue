<template>
  <section class="order-history">
    <h2>房屋订单</h2>
    <p v-if="!currentUser" class="hint">请登录后查看订单记录与退换流程。</p>

    <div v-else>
      <div v-if="loading" class="loading">订单数据加载中...</div>
      <div v-else-if="!orders || orders.length === 0" class="empty">暂无订单记录。</div>
      <ul v-else class="order-list">
        <li v-for="order in orders" :key="order.id" class="order-card">
          <header>
            <h3>{{ order.houseTitle }}</h3>
            <span class="status" :class="order.status.toLowerCase()">
              {{ statusLabels[order.status] ?? order.status }}
            </span>
          </header>
          <dl>
            <div>
              <dt>订单编号</dt>
              <dd>{{ order.id }}</dd>
            </div>
            <div>
              <dt>金额（元）</dt>
              <dd>{{ formatAmount(order.amount) }}</dd>
            </div>
            <div>
              <dt>买家</dt>
              <dd>{{ order.buyerDisplayName }}（{{ order.buyerUsername }}）</dd>
            </div>
            <div>
              <dt>买家联系方式</dt>
              <dd>{{ order.buyerPhoneMasked || '—' }}</dd>
            </div>
            <div>
              <dt>卖家</dt>
              <dd>{{ order.sellerDisplayName }}（{{ order.sellerUsername }}）</dd>
            </div>
            <div>
              <dt>卖家联系方式</dt>
              <dd>{{ order.sellerPhoneMasked || '—' }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatTime(order.createdAt) }}</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ formatTime(order.updatedAt) }}</dd>
            </div>
            <div v-if="order.returnReason">
              <dt>退换原因</dt>
              <dd>{{ order.returnReason }}</dd>
            </div>
          </dl>
          <div class="progress">
            <h4>交易进度</h4>
            <ol>
              <li
                v-for="step in getProgress(order)"
                :key="step.stage"
                :class="{ completed: step.completed }"
              >
                {{ step.label }}
              </li>
            </ol>
          </div>
          <footer v-if="canRequestReturn(order) || canUpdateProgress(order)" class="actions">
            <button
              v-if="canRequestReturn(order)"
              type="button"
              :disabled="loading"
              @click="requestReturn(order)"
            >
              申请退换
            </button>
            <button
              v-if="canUpdateProgress(order)"
              type="button"
              :disabled="loading"
              @click="updateProgress(order)"
            >
              标记为{{ nextStageLabel(order) }}
            </button>
          </footer>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  orders: {
    type: Array,
    default: () => []
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

const emit = defineEmits(['request-return', 'update-progress']);

const statusLabels = {
  PENDING: '待支付',
  RESERVED: '已预定',
  PAID: '已支付',
  RETURNED: '已退换',
  CANCELLED: '已取消'
};

const progressLabels = {
  RESERVED: '预定',
  VIEWED: '已看房',
  BALANCE_PAID: '已支付尾款',
  HANDED_OVER: '已交房'
};

const progressOrder = ['RESERVED', 'VIEWED', 'BALANCE_PAID', 'HANDED_OVER'];

const isSeller = computed(() => props.currentUser?.role === 'SELLER');

const canRequestReturn = (order) => {
  if (!order) {
    return false;
  }
  return (
    order.status === 'PAID' &&
    props.currentUser &&
    order.buyerUsername === props.currentUser.username
  );
};

const getNextStage = (stage) => {
  const index = progressOrder.indexOf(stage);
  if (index === -1 || index >= progressOrder.length - 1) {
    return null;
  }
  return progressOrder[index + 1];
};

const canUpdateProgress = (order) => {
  if (!order || !isSeller.value || !props.currentUser) {
    return false;
  }
  if (order.sellerUsername !== props.currentUser.username) {
    return false;
  }
  return Boolean(getNextStage(order.progressStage));
};

const nextStageLabel = (order) => {
  const next = getNextStage(order.progressStage);
  return next ? progressLabels[next] ?? next : '';
};

const requestReturn = (order) => {
  if (!canRequestReturn(order) || props.loading) {
    return;
  }
  const reason = window.prompt('请输入退换原因（可选）', order.returnReason ?? '');
  if (reason === null) {
    return;
  }
  emit('request-return', { orderId: order.id, reason: reason.trim() });
};

const updateProgress = (order) => {
  if (!canUpdateProgress(order) || props.loading) {
    return;
  }
  const next = getNextStage(order.progressStage);
  if (!next) {
    return;
  }
  emit('update-progress', { orderId: order.id, stage: next });
};

const getProgress = (order) => {
  const currentIndex = progressOrder.indexOf(order?.progressStage);
  const safeIndex = currentIndex < 0 ? 0 : currentIndex;
  return progressOrder.map((stage, index) => ({
    stage,
    label: progressLabels[stage] ?? stage,
    completed: index <= safeIndex
  }));
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

const formatTime = (value) => {
  if (!value) {
    return '';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
};
</script>

<style scoped>
.order-history {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  background: var(--surface-secondary);
  border-radius: 26px;
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-strong);
  padding: 1.75rem;
  color: var(--text-primary);
}

.order-history h2 {
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

.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.order-card {
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 20px;
  padding: 1.35rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(16px);
}

.order-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.order-card h3 {
  margin: 0;
  font-weight: 600;
}

.status {
  border-radius: 999px;
  padding: 0.3rem 0.85rem;
  font-size: 0.85rem;
  font-weight: 600;
  background: rgba(148, 163, 184, 0.25);
  color: var(--text-primary);
}

.status.paid {
  background: rgba(52, 211, 153, 0.25);
  color: var(--success);
}

.status.pending {
  background: rgba(96, 165, 250, 0.25);
  color: var(--accent);
}

.status.returned {
  background: rgba(248, 113, 113, 0.25);
  color: var(--danger);
}

.status.reserved {
  background: rgba(251, 191, 36, 0.25);
  color: var(--warning);
}

.status.cancelled {
  background: rgba(148, 163, 184, 0.25);
  color: var(--text-secondary);
}

dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.85rem 1.35rem;
  margin: 0;
}

dl div {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

dl dt {
  color: var(--text-muted);
  font-size: 0.85rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

dl dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.95rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.actions button {
  background: linear-gradient(135deg, #f97316, #fb923c);
  border: none;
  border-radius: 0.9rem;
  color: var(--text-primary);
  cursor: pointer;
  font-weight: 600;
  padding: 0.6rem 1.45rem;
  box-shadow: 0 18px 32px rgba(249, 115, 22, 0.25);
}

.actions button:hover {
  transform: translateY(-1px);
  box-shadow: 0 20px 36px rgba(249, 115, 22, 0.35);
}

.actions button:disabled {
  background: rgba(249, 115, 22, 0.35);
  cursor: not-allowed;
  box-shadow: none;
}

.progress {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.progress h4 {
  margin: 0;
  color: var(--text-secondary);
}

.progress ol {
  list-style: none;
  display: flex;
  gap: 0.75rem;
  padding: 0;
  margin: 0;
  flex-wrap: wrap;
}

.progress li {
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.25);
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.progress li.completed {
  background: rgba(59, 130, 246, 0.25);
  color: var(--accent);
  font-weight: 600;
}
</style>
