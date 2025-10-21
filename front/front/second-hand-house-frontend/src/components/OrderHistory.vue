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
  gap: 1rem;
  background: var(--app-surface);
  border-radius: 1.15rem;
  box-shadow: var(--app-shadow);
  padding: 1.5rem;
}

.order-history h2 {
  margin: 0;
  font-size: 1.5rem;
  color: var(--app-text);
}

.hint {
  margin: 0;
  color: var(--app-muted);
}

.loading,
.empty {
  background: var(--app-surface-alt);
  border-radius: 0.75rem;
  padding: 1rem 1.25rem;
  color: var(--app-muted);
}

.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.order-card {
  border: 1px solid var(--app-border);
  border-radius: 0.9rem;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-alt) 100%);
}

.order-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.order-card h3 {
  margin: 0;
  color: var(--app-text);
}

.status {
  border-radius: 999px;
  padding: 0.25rem 0.75rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.status.paid {
  background: rgba(34, 197, 94, 0.15);
  color: #15803d;
}

.status.pending {
  background: rgba(59, 130, 246, 0.2);
  color: #1d4ed8;
}

.status.returned {
  background: rgba(248, 113, 113, 0.2);
  color: #b91c1c;
}

.status.reserved {
  background: rgba(248, 180, 0, 0.2);
  color: #b45309;
}

.status.cancelled {
  background: rgba(148, 163, 184, 0.25);
  color: var(--app-muted);
}

dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem 1.25rem;
  margin: 0;
}

dl div {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

dl dt {
  color: var(--app-muted);
  font-size: 0.9rem;
}

dl dd {
  margin: 0;
  color: var(--app-text);
  font-size: 0.95rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.actions button {
  background: var(--accent-gradient);
  border: none;
  border-radius: 0.75rem;
  color: var(--accent-contrast);
  cursor: pointer;
  font-weight: 600;
  padding: 0.55rem 1.4rem;
  box-shadow: 0 10px 20px var(--accent-soft);
  transition: transform 0.25s ease, box-shadow 0.25s ease, opacity 0.25s ease;
}

.actions button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px var(--accent-soft);
}

.actions button:disabled {
  background: var(--accent-soft);
  color: var(--accent-soft-text);
  cursor: not-allowed;
  box-shadow: none;
}

.progress {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.progress h4 {
  margin: 0;
  color: var(--app-text);
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
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: var(--app-surface-alt);
  color: var(--app-muted);
  font-size: 0.85rem;
}

.progress li.completed {
  background: var(--accent-soft);
  color: var(--accent-color);
  font-weight: 600;
}
</style>
